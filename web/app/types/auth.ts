/**
 * Mirror of `school.charset.app.domain.auth.AuthErrorType` (backend).
 * Stable identifiers that travel to the front as i18n keys.
 */
export const AuthErrorType = {
  EmailAlreadyTaken: 'auth.email-already-taken',
  BadCredentials: 'auth.bad-credentials',
  SessionOrphaned: 'auth.session-orphaned'
} as const

export type AuthErrorTypeValue = (typeof AuthErrorType)[keyof typeof AuthErrorType]

/**
 * `ErrorResponse` branches produced by auth exceptions (see
 * `GlobalExceptionHandler` on the backend).
 */
export type AuthErrorVariant
  = | { errorType: typeof AuthErrorType.EmailAlreadyTaken, params: { email: string } }
    | { errorType: typeof AuthErrorType.BadCredentials }
    | { errorType: typeof AuthErrorType.SessionOrphaned }
