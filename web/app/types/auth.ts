/**
 * Mirroir de `school.charset.app.domain.auth.AuthErrorType` (backend).
 * Identifiants stables qui voyagent vers le front comme clés i18n.
 */
export const AuthErrorType = {
  EmailAlreadyTaken: 'auth.email-already-taken',
  BadCredentials: 'auth.bad-credentials',
  SessionOrphaned: 'auth.session-orphaned'
} as const

export type AuthErrorTypeValue = (typeof AuthErrorType)[keyof typeof AuthErrorType]

/**
 * Branches `ErrorResponse` produites par les exceptions auth (cf.
 * `GlobalExceptionHandler` backend).
 */
export type AuthErrorVariant
  = | { errorType: typeof AuthErrorType.EmailAlreadyTaken, params: { email: string } }
    | { errorType: typeof AuthErrorType.BadCredentials }
    | { errorType: typeof AuthErrorType.SessionOrphaned }
