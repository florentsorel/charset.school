// Mirror of `school.charset.app.domain.auth.AuthErrorType` - keep in sync.
export const AuthErrorType = {
  EmailAlreadyTaken: 'auth.email-already-taken',
  BadCredentials: 'auth.bad-credentials',
  SessionOrphaned: 'auth.session-orphaned'
} as const

export type AuthErrorTypeValue = (typeof AuthErrorType)[keyof typeof AuthErrorType]

export type AuthErrorVariant
  = | { errorType: typeof AuthErrorType.EmailAlreadyTaken, params: { email: string } }
    | { errorType: typeof AuthErrorType.BadCredentials }
    | { errorType: typeof AuthErrorType.SessionOrphaned }
