<script setup lang="ts">
import { z } from 'zod'

definePageMeta({ auth: 'guest' })

const { t } = useI18n()
const localePath = useLocalePath()
const { register } = useAuth()
const { resolveAuthError } = useAuthErrors()
const currentLocale = useCurrentLocale()

const schema = computed(() =>
  z.object({
    email: z
      .string()
      .min(1, t('auth.validation.email_required'))
      .email(t('auth.validation.email_invalid')),
    name: z
      .string()
      .min(1, t('auth.validation.name_required'))
      .max(255, t('auth.validation.name_too_long')),
    password: z
      .string()
      .min(1, t('auth.validation.password_required'))
      .min(8, t('auth.validation.password_size'))
      .max(64, t('auth.validation.password_size'))
  })
)
type Schema = z.output<typeof schema.value>

const state = reactive<Schema>({
  email: '',
  name: '',
  password: ''
})

const { errors, validate, applyServerErrors } = useFormValidation(schema)
const submitting = ref(false)
const submitError = ref<string | null>(null)

async function onSubmit() {
  submitError.value = null
  if (!validate(state)) return

  submitting.value = true
  try {
    await register({
      ...state,
      locale: currentLocale.value
    })
  } catch (err) {
    submitting.value = false
    if (applyServerErrors(err)) return
    submitError.value = resolveAuthError(err)
    return
  }

  submitting.value = false
  try {
    await navigateTo(localePath('/profile'))
  } catch (err) {
    console.error('Post-register navigation failed:', err)
  }
}

useHead({
  title: () => `${t('auth.register_title')} · ${t('common.app_name')}`
})
</script>

<template>
  <main class="px-4 sm:px-6 py-12 md:py-16">
    <div class="w-full max-w-md mx-auto">
      <NuxtLink
        :to="localePath('/')"
        class="text-sm text-mute hover:text-ink mb-8 inline-flex items-center gap-1.5"
      >
        <svg
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          aria-hidden="true"
        >
          <path d="M7.5 2.5L4 6l3.5 3.5" />
        </svg>
        {{ t('common.back') }}
      </NuxtLink>

      <h1 class="text-3xl font-medium leading-tight mb-2 tracking-tight">
        {{ t('auth.register_title') }}
      </h1>

      <FormErrorBanner
        :message="submitError"
        class="mt-7"
      />

      <form
        class="flex flex-col gap-4 mt-7"
        novalidate
        @submit.prevent="onSubmit"
      >
        <FormField
          v-model="state.email"
          name="email"
          type="email"
          autocomplete="email"
          :label="t('auth.email')"
          :error="errors.email"
        />

        <FormField
          v-model="state.name"
          name="name"
          autocomplete="name"
          :label="t('auth.name')"
          :error="errors.name"
        />

        <FormField
          v-model="state.password"
          name="password"
          type="password"
          autocomplete="new-password"
          mono
          :label="t('auth.password')"
          :error="errors.password"
        />

        <button
          type="submit"
          class="btn btn-primary justify-center w-full mt-2"
          :disabled="submitting"
        >
          {{ submitting ? t('common.loading') : t('auth.register_submit') }}
        </button>
      </form>

      <p class="text-center text-sm text-mute mt-7">
        {{ t('auth.register_have_account') }}
        <NuxtLink
          :to="localePath('/login')"
          class="btn-link ml-1"
        >
          {{ t('auth.login') }}
        </NuxtLink>
      </p>
    </div>
  </main>
</template>
