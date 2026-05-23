import { beforeEach, describe, expect, it, vi } from 'vitest'
// In a non-Nuxt test environment, Vue reactivity isn't auto-imported.
// eslint-disable-next-line skill-hub/no-redundant-import
import { ref, computed, readonly } from 'vue'
import type { User } from '~/types/user'

/**
 * Pure-Vue unit tests for `useAuth`. We mock Nuxt's auto-imports
 * (`useNuxtApp`, `useState`, `useI18n`) globally before importing the
 * composable so its dependencies are controllable.
 */
const apiMock = vi.fn()
const setLocaleMock = vi.fn(async () => {})
const localeRef = ref<'en' | 'fr'>('en')
const userState = ref<User | null>(null)

vi.stubGlobal('useNuxtApp', () => ({ $api: apiMock }))
vi.stubGlobal('useState', <T>(_key: string, init: () => T) => {
  // `useAuth` always asks for `auth:user`. We return the shared `userState`
  // ref so each test sees the same container — same semantics as real Nuxt.
  if (userState.value === null && init() === null) {
    return userState
  }
  return userState
})
vi.stubGlobal('useI18n', () => ({ locale: localeRef, setLocale: setLocaleMock }))
vi.stubGlobal('computed', computed)
vi.stubGlobal('readonly', readonly)

// Import AFTER stubs are installed
const { useAuth } = await import('./useAuth')

function aUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    email: 'user@example.com',
    name: 'User',
    locale: 'en',
    createdAt: '2026-05-22T12:00:00Z',
    ...overrides
  }
}

describe('useAuth', () => {
  beforeEach(() => {
    apiMock.mockReset()
    setLocaleMock.mockReset()
    localeRef.value = 'en'
    userState.value = null
  })

  describe('fetchMe', () => {
    it('populates user on success', async () => {
      const fetched = aUser({ id: 42, email: 'john@example.com' })
      apiMock.mockResolvedValueOnce(fetched)

      const { fetchMe, user } = useAuth()
      const result = await fetchMe()

      expect(apiMock).toHaveBeenCalledWith('/auth/me')
      expect(result).toEqual(fetched)
      expect(user.value).toEqual(fetched)
    })

    it('syncs i18n locale with user.locale when they differ', async () => {
      localeRef.value = 'en'
      apiMock.mockResolvedValueOnce(aUser({ locale: 'fr' }))

      const { fetchMe } = useAuth()
      await fetchMe()

      expect(setLocaleMock).toHaveBeenCalledWith('fr')
    })

    it('does NOT call setLocale when locale already matches', async () => {
      localeRef.value = 'fr'
      apiMock.mockResolvedValueOnce(aUser({ locale: 'fr' }))

      const { fetchMe } = useAuth()
      await fetchMe()

      expect(setLocaleMock).not.toHaveBeenCalled()
    })

    it('sets user to null and returns null on 401', async () => {
      apiMock.mockRejectedValueOnce(Object.assign(new Error('Unauthorized'), { status: 401 }))

      const { fetchMe, user } = useAuth()
      const result = await fetchMe()

      expect(result).toBeNull()
      expect(user.value).toBeNull()
    })

    it('rethrows on non-401 errors', async () => {
      apiMock.mockRejectedValueOnce(Object.assign(new Error('Boom'), { status: 500 }))

      const { fetchMe } = useAuth()
      await expect(fetchMe()).rejects.toThrow('Boom')
    })
  })

  describe('logout', () => {
    it('POSTs /auth/logout and clears user', async () => {
      apiMock.mockResolvedValueOnce(aUser())
      const { fetchMe, logout, user } = useAuth()
      await fetchMe()
      expect(user.value).not.toBeNull()

      apiMock.mockResolvedValueOnce(undefined)
      await logout()

      expect(apiMock).toHaveBeenLastCalledWith('/auth/logout', { method: 'POST' })
      expect(user.value).toBeNull()
    })
  })

  describe('isAuthenticated', () => {
    it('is false when no user is loaded', () => {
      const { isAuthenticated } = useAuth()
      expect(isAuthenticated.value).toBe(false)
    })

    it('becomes true after fetchMe succeeds', async () => {
      apiMock.mockResolvedValueOnce(aUser())
      const { fetchMe, isAuthenticated } = useAuth()
      await fetchMe()
      expect(isAuthenticated.value).toBe(true)
    })
  })
})
