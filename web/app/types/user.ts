/**
 * Mirror of `school.charset.app.domain.user.User` (backend).
 * The wire shape is produced by `UserSerializer` on the Kotlin side — see
 * `infrastructure/http/auth/serde/UserSerializer.kt`.
 */
export interface User {
  id: number
  email: string
  name: string
  locale: 'fr' | 'en'
  createdAt: string // ISO-8601 UTC, e.g. "2026-05-22T12:34:56Z"
}
