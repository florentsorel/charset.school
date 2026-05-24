import type { ErrorResponse } from '~/types/api'

// Translates a 4xx error from the auth endpoints into a user-facing message.
// Field-level validation errors (422 + fieldErrors) are handled separately by
// useFormValidation.applyServerErrors — this util is for top-of-form banner
// messages (bad-credentials, email-already-taken, etc.).
export function useAuthErrors() {
  const { t } = useI18n()

  function resolveAuthError(err: unknown): string {
    const body = (err as { data?: ErrorResponse }).data
    if (!body || !('errorType' in body)) return t('auth.errors.unknown')
    if (body.errorType === 'auth.email-already-taken') {
      return t(`auth.errors.${body.errorType}`, { email: body.params.email })
    }
    return t(`auth.errors.${body.errorType}`)
  }

  return { resolveAuthError }
}
