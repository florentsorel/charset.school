import type { User } from '~/types/user'

/**
 * Auth state + actions, shared across the app via `useState` (Nuxt SSR-safe).
 *
 * - `user`: current authenticated user, or `null` if unauthenticated / not yet fetched
 * - `isAuthenticated`: convenience boolean
 * - `fetchMe()`: GET /api/auth/me — populates `user`, or sets to null on 401
 * - `logout()`: POST /api/auth/logout — clears the session and the local state
 */
export function useAuth() {
  const { $api } = useNuxtApp()
  const user = useState<User | null>('auth:user', () => null)

  const isAuthenticated = computed(() => user.value !== null)

  async function fetchMe(): Promise<User | null> {
    try {
      const fetched = await $api<User>('/auth/me')
      user.value = fetched
      await syncLocaleWith(fetched.locale)
      return fetched
    } catch (err) {
      const status = (err as { status?: number }).status
      if (status === 401) {
        user.value = null
        return null
      }
      throw err
    }
  }

  /**
   * When the user is authenticated, their `user.locale` (server-side preference)
   * takes priority over the i18n_redirected cookie. If they differ, switch i18n
   * — this also updates the cookie so SSR uses the right locale on next nav.
   */
  async function syncLocaleWith(target: User['locale']) {
    const i18n = useI18n()
    if (i18n.locale.value !== target) {
      await i18n.setLocale(target)
    }
  }

  async function logout(): Promise<void> {
    await $api('/auth/logout', { method: 'POST' })
    user.value = null
  }

  return {
    user: readonly(user),
    isAuthenticated,
    fetchMe,
    logout
  }
}
