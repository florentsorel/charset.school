<script setup lang="ts">
import { z } from 'zod'
import type { Locale } from '~/types/locale'
import { LOCALES } from '~/types/locale'

type ThemePreference = 'system' | 'light' | 'dark'
const THEMES: readonly ThemePreference[] = ['system', 'light', 'dark']

const { t } = useI18n()
const { user, updateProfile, changePassword, deleteAccount } = useAuth()
const { resolveAuthError } = useAuthErrors()
const colorMode = useColorMode()

// Local form state mirrors the user / colorMode, edited freely until Save / Cancel.
const accountState = reactive({
  name: user.value?.name ?? '',
  email: user.value?.email ?? ''
})

const preferencesState = reactive({
  locale: (user.value?.locale ?? 'en') as Locale,
  theme: ((colorMode.preference || 'system') as ThemePreference)
})

// Keep local state in sync when user.value changes (e.g. after a successful save).
watch(user, (u) => {
  if (!u) return
  accountState.name = u.name
  accountState.email = u.email
  preferencesState.locale = u.locale
}, { immediate: true })

// Theme preference is browser-local (cookie via @nuxtjs/color-mode). External
// changes (devtools, another tab) flow back into the form.
watch(() => colorMode.preference, (p) => {
  if (p) preferencesState.theme = p as ThemePreference
})

const accountSchema = computed(() =>
  z.object({
    name: z
      .string()
      .min(1, t('auth.validation.name_required'))
      .max(255, t('auth.validation.name_too_long')),
    email: z
      .string()
      .min(1, t('auth.validation.email_required'))
      .max(255, t('auth.validation.email_too_long'))
      .email(t('auth.validation.email_invalid'))
  })
)

const account = useFormValidation(accountSchema)
const accountSubmitting = ref(false)
const accountError = ref<string | null>(null)
const accountSavedAt = ref<number | null>(null)

const preferencesSubmitting = ref(false)
const preferencesError = ref<string | null>(null)
const preferencesSavedAt = ref<number | null>(null)

const passwordState = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordSchema = computed(() =>
  z.object({
    currentPassword: z
      .string()
      .min(1, t('auth.validation.password_required')),
    newPassword: z
      .string()
      .min(1, t('auth.validation.password_required'))
      .min(8, t('auth.validation.password_size'))
      .max(64, t('auth.validation.password_size')),
    confirmPassword: z
      .string()
      .min(1, t('auth.validation.password_required'))
      .min(8, t('auth.validation.password_size'))
      .max(64, t('auth.validation.password_size'))
  }).refine(d => d.confirmPassword === d.newPassword, {
    path: ['confirmPassword'],
    message: t('auth.validation.password_confirm_mismatch')
  })
)

const password = useFormValidation(passwordSchema)
const passwordSubmitting = ref(false)
const passwordError = ref<string | null>(null)
const passwordSavedAt = ref<number | null>(null)

const accountDirty = computed(() =>
  !!user.value
  && (accountState.name !== user.value.name || accountState.email !== user.value.email)
)

const preferencesDirty = computed(() => {
  const localeChanged = !!user.value && preferencesState.locale !== user.value.locale
  const themeChanged = preferencesState.theme !== colorMode.preference
  return localeChanged || themeChanged
})

async function submitAccount() {
  accountError.value = null
  accountSavedAt.value = null
  if (!account.validate(accountState)) return

  accountSubmitting.value = true
  try {
    await updateProfile({ name: accountState.name, email: accountState.email })
    accountSavedAt.value = Date.now()
  } catch (err) {
    if (account.applyServerErrors(err)) return
    accountError.value = resolveAuthError(err)
  } finally {
    accountSubmitting.value = false
  }
}

function cancelAccount() {
  if (!user.value) return
  accountState.name = user.value.name
  accountState.email = user.value.email
  account.clearErrors()
  accountError.value = null
}

async function submitPreferences() {
  preferencesError.value = null
  preferencesSavedAt.value = null
  preferencesSubmitting.value = true
  try {
    // Locale is server-side (DB) — only PATCH if it actually changed.
    if (user.value && preferencesState.locale !== user.value.locale) {
      await updateProfile({ locale: preferencesState.locale })
    }
    // Theme is browser-local (color-mode cookie) — applied immediately.
    if (preferencesState.theme !== colorMode.preference) {
      colorMode.preference = preferencesState.theme
    }
    preferencesSavedAt.value = Date.now()
  } catch (err) {
    preferencesError.value = resolveAuthError(err)
  } finally {
    preferencesSubmitting.value = false
  }
}

function cancelPreferences() {
  if (user.value) preferencesState.locale = user.value.locale
  preferencesState.theme = (colorMode.preference || 'system') as ThemePreference
  preferencesError.value = null
}

async function submitPassword() {
  passwordError.value = null
  passwordSavedAt.value = null
  if (!password.validate(passwordState)) return

  passwordSubmitting.value = true
  try {
    await changePassword({
      currentPassword: passwordState.currentPassword,
      newPassword: passwordState.newPassword,
      confirmPassword: passwordState.confirmPassword
    })
    passwordSavedAt.value = Date.now()
    passwordState.currentPassword = ''
    passwordState.newPassword = ''
    passwordState.confirmPassword = ''
  } catch (err) {
    if (password.applyServerErrors(err)) return
    passwordError.value = resolveAuthError(err)
  } finally {
    passwordSubmitting.value = false
  }
}

// Danger zone — delete account
const deleteDialog = useTemplateRef<HTMLDialogElement>('deleteDialog')
const deleteState = reactive({ password: '' })
const deleteSchema = computed(() =>
  z.object({
    password: z.string().min(1, t('auth.validation.password_required'))
  })
)
const deleteForm = useFormValidation(deleteSchema)
const deleteSubmitting = ref(false)
const deleteError = ref<string | null>(null)

function openDeleteDialog() {
  deleteState.password = ''
  deleteError.value = null
  deleteForm.clearErrors()
  deleteDialog.value?.showModal()
}

function closeDeleteDialog() {
  deleteDialog.value?.close()
}

// Click outside the card (on the backdrop) closes the dialog. Native <dialog>
// receives the click on itself when the user hits the backdrop area.
function onDialogClick(e: MouseEvent) {
  if (e.target === deleteDialog.value) closeDeleteDialog()
}

async function submitDelete() {
  deleteError.value = null
  if (!deleteForm.validate(deleteState)) return

  deleteSubmitting.value = true
  try {
    await deleteAccount({ password: deleteState.password })
    closeDeleteDialog()
    await navigateTo('/')
  } catch (err) {
    if (deleteForm.applyServerErrors(err)) return
    deleteError.value = resolveAuthError(err)
  } finally {
    deleteSubmitting.value = false
  }
}

const localeLabels: Record<Locale, string> = {
  fr: 'Français (FR)',
  en: 'English (EN)'
}

useHead({
  title: () => `${t('profile.title')} · ${t('common.app_name')}`
})
</script>

<template>
  <main class="px-4 sm:px-6 py-12 md:py-16">
    <div class="w-full max-w-2xl mx-auto">
      <header class="mb-8">
        <h1 class="text-3xl font-medium leading-tight tracking-tight mb-2">
          {{ t('profile.title') }}
        </h1>
        <p class="text-sm text-mute leading-normal">
          {{ t('profile.subtitle') }}
        </p>
      </header>

      <div class="flex flex-col gap-5">
        <!-- Section 1 — Account info -->
        <section class="section-card">
          <header class="flex items-baseline justify-between mb-5 gap-3">
            <h2 class="text-md font-medium">
              {{ t('profile.account.title') }}
            </h2>
          </header>

          <FormErrorBanner
            :message="accountError"
            class="mb-4 max-w-md"
          />

          <form
            class="grid grid-cols-1 gap-4 max-w-md"
            novalidate
            @submit.prevent="submitAccount"
          >
            <FormField
              v-model="accountState.name"
              name="name"
              autocomplete="name"
              :label="t('auth.name')"
              :error="account.errors.name"
            />
            <FormField
              v-model="accountState.email"
              name="email"
              type="email"
              autocomplete="email"
              :label="t('auth.email')"
              :error="account.errors.email"
            />

            <div class="flex items-center gap-2">
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="accountSubmitting || !accountDirty"
              >
                {{ accountSubmitting ? t('common.loading') : t('common.save') }}
              </button>
              <button
                v-if="accountDirty"
                type="button"
                class="btn btn-quiet"
                @click="cancelAccount"
              >
                {{ t('common.cancel') }}
              </button>
              <span
                v-if="accountSavedAt && !accountDirty"
                class="text-xs text-ok ml-1"
              >
                {{ t('profile.saved') }}
              </span>
            </div>
          </form>
        </section>

        <!-- Section 2 — Preferences (language + theme) -->
        <section class="section-card">
          <header class="flex items-baseline justify-between mb-5 gap-3">
            <h2 class="text-md font-medium">
              {{ t('profile.preferences.title') }}
            </h2>
          </header>

          <FormErrorBanner
            :message="preferencesError"
            class="mb-4 max-w-xs"
          />

          <form
            class="flex flex-col gap-4 max-w-xs"
            novalidate
            @submit.prevent="submitPreferences"
          >
            <div class="field">
              <label
                class="field-label"
                for="profile-locale"
              >{{ t('profile.preferences.language_label') }}</label>
              <select
                id="profile-locale"
                v-model="preferencesState.locale"
                class="field-input"
                style="cursor: pointer;"
              >
                <option
                  v-for="code in LOCALES"
                  :key="code"
                  :value="code"
                >
                  {{ localeLabels[code] }}
                </option>
              </select>
            </div>

            <div class="field">
              <label
                class="field-label"
                for="profile-theme"
              >{{ t('profile.preferences.theme_label') }}</label>
              <select
                id="profile-theme"
                v-model="preferencesState.theme"
                class="field-input"
                style="cursor: pointer;"
              >
                <option
                  v-for="theme in THEMES"
                  :key="theme"
                  :value="theme"
                >
                  {{ t(`common.theme_${theme}`) }}
                </option>
              </select>
            </div>

            <div class="flex items-center gap-2">
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="preferencesSubmitting || !preferencesDirty"
              >
                {{ preferencesSubmitting ? t('common.loading') : t('common.save') }}
              </button>
              <button
                v-if="preferencesDirty"
                type="button"
                class="btn btn-quiet"
                @click="cancelPreferences"
              >
                {{ t('common.cancel') }}
              </button>
              <span
                v-if="preferencesSavedAt && !preferencesDirty"
                class="text-xs text-ok ml-1"
              >
                {{ t('profile.saved') }}
              </span>
            </div>
          </form>
        </section>

        <!-- Section 3 — Change password -->
        <section class="section-card">
          <header class="flex items-baseline justify-between mb-5 gap-3">
            <div>
              <h2 class="text-md font-medium">
                {{ t('profile.password.title') }}
              </h2>
              <p class="text-xs text-mute mt-0.5">
                {{ t('profile.password.subtitle') }}
              </p>
            </div>
          </header>

          <FormErrorBanner
            :message="passwordError"
            class="mb-4 max-w-md"
          />

          <form
            class="grid grid-cols-1 gap-4 max-w-md"
            novalidate
            @submit.prevent="submitPassword"
          >
            <FormField
              v-model="passwordState.currentPassword"
              name="currentPassword"
              type="password"
              autocomplete="current-password"
              mono
              :label="t('profile.password.current_label')"
              :error="password.errors.currentPassword"
            />
            <FormField
              v-model="passwordState.newPassword"
              name="newPassword"
              type="password"
              autocomplete="new-password"
              mono
              :label="t('profile.password.new_label')"
              :error="password.errors.newPassword"
            />
            <FormField
              v-model="passwordState.confirmPassword"
              name="confirmPassword"
              type="password"
              autocomplete="new-password"
              mono
              :label="t('profile.password.confirm_label')"
              :error="password.errors.confirmPassword"
            />

            <div class="flex items-center gap-2">
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="passwordSubmitting"
              >
                {{ passwordSubmitting ? t('common.loading') : t('profile.password.submit') }}
              </button>
              <span
                v-if="passwordSavedAt"
                class="text-xs text-ok ml-1"
              >
                {{ t('profile.saved') }}
              </span>
            </div>
          </form>
        </section>

        <!-- Section 4 — Danger zone -->
        <section class="section-card section-card-danger">
          <header class="mb-3">
            <h2
              class="text-md font-medium"
              style="color: var(--color-bad);"
            >
              {{ t('profile.danger.title') }}
            </h2>
            <p class="text-xs text-mute mt-0.5">
              {{ t('profile.danger.subtitle') }}
            </p>
          </header>
          <p class="text-sm text-mute leading-relaxed max-w-md mb-5 mt-4">
            {{ t('profile.danger.description') }}
          </p>
          <button
            type="button"
            class="btn btn-danger"
            @click="openDeleteDialog"
          >
            {{ t('profile.danger.delete_button') }}
          </button>
        </section>
      </div>
    </div>

    <dialog
      ref="deleteDialog"
      class="dlg"
      aria-labelledby="delete-dialog-title"
      @click="onDialogClick"
      @close="deleteError = null"
    >
      <form
        novalidate
        @submit.prevent="submitDelete"
      >
        <div class="flex items-start gap-3 mb-4">
          <span class="dlg-icon">
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              aria-hidden="true"
            >
              <path d="M2 4h12M6 4V2.5a1 1 0 011-1h2a1 1 0 011 1V4M3.5 4l.7 9a1 1 0 001 .9h5.6a1 1 0 001-.9l.7-9" />
            </svg>
          </span>
          <div>
            <h3
              id="delete-dialog-title"
              class="text-lg font-medium leading-tight"
            >
              {{ t('profile.danger.dialog.title') }}
            </h3>
            <p class="text-sm text-mute mt-1">
              {{ t('profile.danger.dialog.subtitle') }}
            </p>
          </div>
        </div>
        <p class="text-sm text-mute leading-relaxed mb-5">
          {{ t('profile.danger.dialog.description') }}
        </p>

        <FormErrorBanner
          :message="deleteError"
          class="mb-4"
        />

        <FormField
          v-model="deleteState.password"
          name="password"
          type="password"
          autocomplete="current-password"
          mono
          :label="t('profile.danger.dialog.password_label')"
          :error="deleteForm.errors.password"
          class="mb-5"
        />

        <div class="flex items-center gap-2 justify-end flex-wrap">
          <button
            type="button"
            class="btn btn-ghost"
            :disabled="deleteSubmitting"
            @click="closeDeleteDialog"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            type="submit"
            class="btn btn-danger"
            :disabled="deleteSubmitting"
          >
            {{ deleteSubmitting ? t('common.loading') : t('profile.danger.dialog.confirm') }}
          </button>
        </div>
      </form>
    </dialog>
  </main>
</template>
