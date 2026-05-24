import { beforeEach, describe, expect, it, vi } from 'vitest'
// In a non-Nuxt test environment, Vue reactivity isn't auto-imported.
// eslint-disable-next-line skill-hub/no-redundant-import
import { ref, computed, readonly } from 'vue'
import type { User } from '~/types/user'

/**
 * Pure-Vue unit tests for `useAuth`. We mock Nuxt's auto-imports
 * (`useNuxtApp`, `useState`, `useRequestHeaders`) globally before importing
 * the composable so its dependencies are controllable.
 */
const apiRawMock = vi.fn()
const apiMock = Object.assign(vi.fn(), { raw: apiRawMock })
const setLocaleMock = vi.fn(async () => {})
const localeRef = ref<'en' | 'fr'>('en')
const userState = ref<User | null>(null)

vi.stubGlobal('useNuxtApp', () => ({
  $api: apiMock,
  $i18n: { locale: localeRef, setLocale: setLocaleMock }
}))
vi.stubGlobal('useState', <T>(_key: string, init: () => T) => {
  if (userState.value === null && init() === null) {
    return userState
  }
  return userState
})
vi.stubGlobal('useRequestHeaders', () => ({}))
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

function rawResponse(data: User) {
  return { _data: data, headers: new Headers() }
}

describe('useAuth', () => {
  beforeEach(() => {
    apiMock.mockReset()
    apiRawMock.mockReset()
    setLocaleMock.mockReset()
    localeRef.value = 'en'
    userState.value = null
  })

  describe('fetchMe', () => {
    it('populates user on success', async () => {
      const fetched = aUser({ id: 42, email: 'john@example.com' })
      apiRawMock.mockResolvedValueOnce(rawResponse(fetched))

      const { fetchMe, user } = useAuth()
      const result = await fetchMe()

      expect(apiRawMock).toHaveBeenCalledWith('/auth/me', { headers: undefined })
      expect(result).toEqual(fetched)
      expect(user.value).toEqual(fetched)
    })

    it('does NOT sync locale on fetchMe (only on login/register entry points)', async () => {
      localeRef.value = 'en'
      apiRawMock.mockResolvedValueOnce(rawResponse(aUser({ locale: 'fr' })))

      const { fetchMe } = useAuth()
      await fetchMe()

      expect(setLocaleMock).not.toHaveBeenCalled()
    })

    it('sets user to null and returns null on 401', async () => {
      apiRawMock.mockRejectedValueOnce(Object.assign(new Error('Unauthorized'), { status: 401 }))

      const { fetchMe, user } = useAuth()
      const result = await fetchMe()

      expect(result).toBeNull()
      expect(user.value).toBeNull()
    })

    it('rethrows on non-401 errors', async () => {
      apiRawMock.mockRejectedValueOnce(Object.assign(new Error('Boom'), { status: 500 }))

      const { fetchMe } = useAuth()
      await expect(fetchMe()).rejects.toThrow('Boom')
    })
  })

  describe('login', () => {
    it('POSTs credentials, sets user and syncs locale', async () => {
      const fetched = aUser({ email: 'alice@example.com', locale: 'fr' })
      apiMock.mockResolvedValueOnce(fetched)

      const { login, user } = useAuth()
      const result = await login('alice@example.com', 'password123', true)

      expect(apiMock).toHaveBeenCalledWith('/auth/login', {
        method: 'POST',
        body: { email: 'alice@example.com', password: 'password123', rememberMe: true }
      })
      expect(result).toEqual(fetched)
      expect(user.value).toEqual(fetched)
      expect(setLocaleMock).toHaveBeenCalledWith('fr')
    })

    it('does NOT call setLocale when locale already matches', async () => {
      localeRef.value = 'fr'
      apiMock.mockResolvedValueOnce(aUser({ locale: 'fr' }))

      const { login } = useAuth()
      await login('a@b.com', 'pw', false)

      expect(setLocaleMock).not.toHaveBeenCalled()
    })

    it('rethrows on bad credentials (401)', async () => {
      apiMock.mockRejectedValueOnce(Object.assign(new Error('Unauthorized'), { status: 401 }))

      const { login } = useAuth()
      await expect(login('x@x.com', 'wrong', false)).rejects.toThrow('Unauthorized')
    })
  })

  describe('register', () => {
    it('POSTs full payload, sets user and syncs locale', async () => {
      const fetched = aUser({ email: 'bob@example.com', name: 'Bob', locale: 'fr' })
      apiMock.mockResolvedValueOnce(fetched)

      const { register, user } = useAuth()
      const result = await register({
        email: 'bob@example.com',
        name: 'Bob',
        password: 'password123',
        locale: 'fr'
      })

      expect(apiMock).toHaveBeenCalledWith('/auth/register', {
        method: 'POST',
        body: {
          email: 'bob@example.com',
          name: 'Bob',
          password: 'password123',
          locale: 'fr'
        }
      })
      expect(result).toEqual(fetched)
      expect(user.value).toEqual(fetched)
      expect(setLocaleMock).toHaveBeenCalledWith('fr')
    })

    it('rethrows on email already taken (409)', async () => {
      apiMock.mockRejectedValueOnce(Object.assign(new Error('Conflict'), { status: 409 }))

      const { register } = useAuth()
      await expect(
        register({ email: 'taken@x.com', name: 'X', password: 'password123', locale: 'en' })
      ).rejects.toThrow('Conflict')
    })
  })

  describe('logout', () => {
    it('POSTs /auth/logout and clears user', async () => {
      apiRawMock.mockResolvedValueOnce(rawResponse(aUser()))
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
      apiRawMock.mockResolvedValueOnce(rawResponse(aUser()))
      const { fetchMe, isAuthenticated } = useAuth()
      await fetchMe()
      expect(isAuthenticated.value).toBe(true)
    })
  })
})
