import type { Locale } from './locale'

// Wire shape produced by `UserSerializer.kt` on the back.
export interface User {
  id: number
  email: string
  name: string
  locale: Locale
  createdAt: string
}
