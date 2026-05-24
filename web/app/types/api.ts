import type { AuthErrorVariant } from './auth'

export const ValidationErrorType = 'validation.failed' as const

export type ValidationErrorVariant = {
  errorType: typeof ValidationErrorType
  fieldErrors: Record<string, string[]>
}

export type ErrorResponse = AuthErrorVariant | ValidationErrorVariant
