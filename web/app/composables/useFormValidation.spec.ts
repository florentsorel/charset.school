import { beforeEach, describe, expect, it, vi } from 'vitest'
// In a non-Nuxt test environment, Vue reactivity isn't auto-imported.
// eslint-disable-next-line skill-hub/no-redundant-import
import { reactive, toValue } from 'vue'
import { z } from 'zod'

const skipClientValidation = { value: false }
const tMock = vi.fn((key: string) => `T:${key}`)

// `vue-i18n` isn't hoisted to top-level node_modules in pnpm setups (it's a
// transitive dep of @nuxtjs/i18n), and Vitest doesn't follow Nuxt's symlinks
// the way the dev server does. We intercept the import here.
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: tMock }) }))

vi.stubGlobal('useRuntimeConfig', () => ({
  public: {
    get skipClientValidation() { return skipClientValidation.value }
  }
}))
vi.stubGlobal('reactive', reactive)
vi.stubGlobal('toValue', toValue)

const { useFormValidation } = await import('./useFormValidation')

const schema = z.object({
  email: z
    .string()
    .min(1, 'email_required')
    .email('email_invalid'),
  password: z
    .string()
    .min(1, 'password_required')
    .min(8, 'password_size'),
  name: z
    .string()
    .min(1, 'name_required')
})

describe('useFormValidation', () => {
  beforeEach(() => {
    tMock.mockClear()
    skipClientValidation.value = false
  })

  describe('validate', () => {
    it('returns true and leaves errors empty for valid data', () => {
      const { validate, errors } = useFormValidation(schema)
      const ok = validate({ email: 'a@b.com', password: 'longenough', name: 'Bob' })

      expect(ok).toBe(true)
      expect(errors).toEqual({})
    })

    it('returns false and populates errors per field for invalid data', () => {
      const { validate, errors } = useFormValidation(schema)
      const ok = validate({ email: '', password: '', name: '' })

      expect(ok).toBe(false)
      expect(errors.email).toBe('email_required')
      expect(errors.password).toBe('password_required')
      expect(errors.name).toBe('name_required')
    })

    it('keeps only the FIRST issue per field (validation priority)', () => {
      // Password has both min(1) AND min(8). An empty string fails both —
      // Zod emits issues in declaration order, our composable keeps the first.
      const { validate, errors } = useFormValidation(schema)
      validate({ email: 'a@b.com', password: '', name: 'Bob' })

      expect(errors.password).toBe('password_required')
    })

    it('clears previous errors before re-validating', () => {
      const { validate, errors } = useFormValidation(schema)

      validate({ email: '', password: '', name: '' })
      expect(Object.keys(errors).length).toBeGreaterThan(0)

      validate({ email: 'a@b.com', password: 'longenough', name: 'Bob' })
      expect(errors).toEqual({})
    })

    it('skips Zod and returns true when skipClientValidation is on', () => {
      skipClientValidation.value = true
      const { validate, errors } = useFormValidation(schema)

      const ok = validate({ email: '', password: '', name: '' })

      expect(ok).toBe(true)
      expect(errors).toEqual({})
    })
  })

  describe('applyServerErrors', () => {
    it('populates errors from a 422 validation.failed body, translating keys', () => {
      const { applyServerErrors, errors } = useFormValidation(schema)

      const handled = applyServerErrors({
        data: {
          errorType: 'validation.failed',
          fieldErrors: {
            email: ['auth.validation.email_required'],
            password: ['auth.validation.password_required', 'auth.validation.password_size']
          }
        }
      })

      expect(handled).toBe(true)
      expect(errors.email).toBe('T:auth.validation.email_required')
      // First message per field (back already sorts by constraint priority)
      expect(errors.password).toBe('T:auth.validation.password_required')
      expect(tMock).toHaveBeenCalledWith('auth.validation.email_required')
      expect(tMock).toHaveBeenCalledWith('auth.validation.password_required')
    })

    it('returns false and leaves errors untouched for non-validation errors', () => {
      const { applyServerErrors, errors, setError } = useFormValidation(schema)
      setError('email', 'sticky')

      const handled = applyServerErrors({
        data: { errorType: 'auth.bad-credentials' }
      })

      expect(handled).toBe(false)
      expect(errors.email).toBe('sticky')
    })

    it('returns false when err has no .data shape at all', () => {
      const { applyServerErrors } = useFormValidation(schema)
      expect(applyServerErrors(new Error('network failure'))).toBe(false)
      expect(applyServerErrors(undefined)).toBe(false)
      expect(applyServerErrors(null)).toBe(false)
    })

    it('clears previous Zod errors when applying server errors', () => {
      const { validate, applyServerErrors, errors } = useFormValidation(schema)

      validate({ email: '', password: '', name: '' })
      expect(errors.email).toBeTruthy()

      applyServerErrors({
        data: {
          errorType: 'validation.failed',
          fieldErrors: { email: ['server.email.taken'] }
        }
      })

      expect(errors.email).toBe('T:server.email.taken')
      // Other fields' previous Zod errors should be gone
      expect(errors.password).toBeUndefined()
      expect(errors.name).toBeUndefined()
    })
  })

  describe('setError / clearErrors', () => {
    it('setError writes a single field error', () => {
      const { setError, errors } = useFormValidation(schema)
      setError('email', 'manual')
      expect(errors.email).toBe('manual')
    })

    it('clearErrors empties the reactive', () => {
      const { setError, clearErrors, errors } = useFormValidation(schema)
      setError('email', 'a')
      setError('password', 'b')
      clearErrors()
      expect(errors).toEqual({})
    })
  })
})
