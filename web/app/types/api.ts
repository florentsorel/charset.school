/**
 * Wire format for HTTP error responses — discriminated union on `errorType`.
 *
 * Per-feature variants (`AuthErrorVariant`, later `ExerciseErrorVariant`, etc.)
 * are defined in their respective `types/<feature>.ts` file and composed here
 * into the `ErrorResponse` union.
 *
 * On the backend, this format is produced by `GlobalExceptionHandler` +
 * `ErrorResponse.kt` in `infrastructure/http/`.
 */
import type { AuthErrorVariant } from './auth'

export const ValidationErrorType = 'validation.failed' as const

export type ValidationErrorVariant = {
  errorType: typeof ValidationErrorType
  fieldErrors: Record<string, string[]>
}

export type ErrorResponse = AuthErrorVariant | ValidationErrorVariant
