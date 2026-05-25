import type { User } from '~/types/user'

export function useAuth() {
  const nuxtApp = useNuxtApp()
  const { $api } = nuxtApp
  const user = useState<User | null>('auth:user', () => null)

  const isAuthenticated = computed(() => user.value !== null)

  async function fetchMe(): Promise<User | null> {
    const headers = import.meta.server
      ? useRequestHeaders(['cookie'])
      : undefined
    try {
      const response = await $api.raw<User>('/auth/me', { headers })
      if (import.meta.server) forwardSetCookies(response.headers)
      const fetched = response._data!
      user.value = fetched
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

  async function login(email: string, password: string, rememberMe: boolean): Promise<User> {
    const fetched = await $api<User>('/auth/login', {
      method: 'POST',
      body: { email, password, rememberMe }
    })
    user.value = fetched
    await syncLocaleWith(fetched.locale)
    return fetched
  }

  async function register(input: {
    email: string
    name: string
    password: string
    locale: User['locale']
  }): Promise<User> {
    const fetched = await $api<User>('/auth/register', {
      method: 'POST',
      body: input
    })
    user.value = fetched
    await syncLocaleWith(fetched.locale)
    return fetched
  }

  async function logout(): Promise<void> {
    await $api('/auth/logout', { method: 'POST' })
    user.value = null
  }

  // PATCH /api/profile — partial update of name / email / locale. The back
  // returns the full updated User; we keep `user.value` in sync and re-sync
  // i18n if the locale changed.
  async function updateProfile(input: {
    name?: string
    email?: string
    locale?: User['locale']
  }): Promise<User> {
    const fetched = await $api<User>('/profile', {
      method: 'PATCH',
      body: input
    })
    user.value = fetched
    if (input.locale) await syncLocaleWith(fetched.locale)
    return fetched
  }

  // PATCH /api/profile/password — back returns 204, no body. The user record
  // doesn't visibly change (we never expose the hash) so we don't refresh
  // user.value.
  async function changePassword(input: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<void> {
    await $api('/profile/password', {
      method: 'PATCH',
      body: input
    })
  }

  async function syncLocaleWith(target: User['locale']) {
    const $i18n = nuxtApp.$i18n
    if (!$i18n || $i18n.locale.value === target) return
    try {
      await $i18n.setLocale(target)
    } catch {
      // setLocale triggers an internal navigation that can be aborted by a
      // follow-up navigateTo — swallow the NavigationFailure.
    }
  }

  return {
    user: readonly(user),
    isAuthenticated,
    fetchMe,
    login,
    register,
    logout,
    updateProfile,
    changePassword
  }
}
