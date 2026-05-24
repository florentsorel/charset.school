<script setup lang="ts">
import { z } from 'zod'
import type { Locale } from '~/types/locale'
import { LOCALES } from '~/types/locale'

type ThemePreference = 'system' | 'light' | 'dark'
const THEMES: readonly ThemePreference[] = ['system', 'light', 'dark']

const { t } = useI18n()
const { user, updateProfile } = useAuth()
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
      </div>
    </div>
  </main>
</template>
