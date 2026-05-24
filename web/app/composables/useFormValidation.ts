import { useI18n } from 'vue-i18n'
import type { z } from 'zod'
import type { ErrorResponse } from '~/types/api'

// Form validation wrapper. `errors` is populated by either client-side Zod or
// server-side 422 fieldErrors — the back returns i18n keys as Bean Validation
// messages, which we translate here. `validate()` bypasses Zod when
// runtimeConfig.public.skipClientValidation is true (E2E flag).
export function useFormValidation<TSchema extends z.ZodTypeAny>(
  schema: MaybeRefOrGetter<TSchema>
) {
  type Output = z.output<TSchema>
  type FieldKey = keyof Output & string

  const config = useRuntimeConfig()
  const { t } = useI18n()
  // Internally typed as a plain string-keyed record — Vue 3.5's Reactive<T>
  // type doesn't accept indexing by a generic FieldKey. The FieldKey constraint
  // stays on setError/validate signatures for caller-side safety.
  const errors = reactive<Record<string, string | undefined>>({})

  function clearErrors() {
    for (const k of Object.keys(errors) as FieldKey[]) delete errors[k]
  }

  function setError(field: FieldKey, message: string) {
    errors[field] = message
  }

  function validate(data: unknown): boolean {
    clearErrors()
    if (config.public.skipClientValidation) return true

    const result = toValue(schema).safeParse(data)
    if (result.success) return true

    for (const issue of result.error.issues) {
      const field = issue.path[0] as FieldKey | undefined
      if (field && !errors[field]) errors[field] = issue.message
    }
    return false
  }

  function applyServerErrors(err: unknown): boolean {
    const body = (err as { data?: ErrorResponse } | null | undefined)?.data
    if (!body || body.errorType !== 'validation.failed' || !('fieldErrors' in body)) {
      return false
    }
    clearErrors()
    for (const [field, messages] of Object.entries(body.fieldErrors)) {
      const firstKey = messages[0]
      if (firstKey) errors[field as FieldKey] = t(firstKey)
    }
    return true
  }

  return { errors, validate, clearErrors, setError, applyServerErrors }
}
