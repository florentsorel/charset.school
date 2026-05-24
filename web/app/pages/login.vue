<script setup lang="ts">
import { z } from 'zod'

definePageMeta({ auth: 'guest' })

const { t } = useI18n()
const localePath = useLocalePath()
const { login } = useAuth()
const { resolveAuthError } = useAuthErrors()

const schema = computed(() =>
  z.object({
    email: z
      .string()
      .min(1, t('auth.validation.email_required'))
      .email(t('auth.validation.email_invalid')),
    password: z.string().min(1, t('auth.validation.password_required')),
    rememberMe: z.boolean()
  })
)
type Schema = z.output<typeof schema.value>

const state = reactive<Schema>({
  email: '',
  password: '',
  rememberMe: false
})

const { errors, validate, applyServerErrors } = useFormValidation(schema)
const submitting = ref(false)
const submitError = ref<string | null>(null)

async function onSubmit() {
  submitError.value = null
  if (!validate(state)) return

  submitting.value = true
  try {
    await login(state.email, state.password, state.rememberMe)
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
    console.error('Post-login navigation failed:', err)
  }
}

useHead({
  title: () => `${t('auth.login_title')} · ${t('common.app_name')}`
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

      <h1 class="text-3xl font-medium leading-tight mb-7 tracking-tight">
        {{ t('auth.login_title') }}
      </h1>

      <FormErrorBanner
        :message="submitError"
        class="mb-4"
      />

      <form
        class="flex flex-col gap-4"
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
          v-model="state.password"
          name="password"
          type="password"
          autocomplete="current-password"
          mono
          :label="t('auth.password')"
          :error="errors.password"
        />

        <label class="checkbox-wrap text-sm text-mute mt-1">
          <input
            v-model="state.rememberMe"
            type="checkbox"
            class="cb-input"
          >
          <span>{{ t('auth.remember_me') }}</span>
        </label>

        <button
          type="submit"
          class="btn btn-primary justify-center w-full mt-2"
          :disabled="submitting"
        >
          {{ submitting ? t('common.loading') : t('auth.login_submit') }}
        </button>
      </form>

      <div class="my-7 flex items-center gap-4">
        <span class="h-px flex-1 bg-rule" />
        <span class="text-xs font-mono uppercase tracking-widest text-faint">{{ t('common.or') }}</span>
        <span class="h-px flex-1 bg-rule" />
      </div>

      <p class="text-center text-sm text-mute">
        {{ t('auth.login_no_account') }}
        <NuxtLink
          :to="localePath('/register')"
          class="btn-link ml-1"
        >
          {{ t('auth.register') }}
        </NuxtLink>
      </p>
    </div>
  </main>
</template>
