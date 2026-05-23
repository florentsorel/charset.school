/**
 * Mirroir de `school.charset.app.domain.user.User` (backend).
 * La shape est produite par `UserSerializer` côté Kotlin — voir
 * `infrastructure/http/auth/serde/UserSerializer.kt`.
 */
export interface User {
  id: number
  email: string
  name: string
  locale: 'fr' | 'en'
  createdAt: string // ISO-8601 UTC, e.g. "2026-05-22T12:34:56Z"
}
