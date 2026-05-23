/**
 * Format wire des réponses d'erreur HTTP — discriminated union sur `errorType`.
 *
 * Les variantes par feature (`AuthErrorVariant`, plus tard `ExerciseErrorVariant`,
 * etc.) sont définies dans le fichier `types/<feature>.ts` correspondant et
 * composées ici dans l'union `ErrorResponse`.
 *
 * Côté backend, ce format est produit par `GlobalExceptionHandler` +
 * `ErrorResponse.kt` dans `infrastructure/http/`.
 */
import type { AuthErrorVariant } from './auth'

export const ValidationErrorType = 'validation.failed' as const

export type ValidationErrorVariant = {
  errorType: typeof ValidationErrorType
  fieldErrors: Record<string, string[]>
}

export type ErrorResponse = AuthErrorVariant | ValidationErrorVariant
