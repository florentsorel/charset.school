/**
 * Custom $fetch instance for the Spring Boot API.
 *
 * - `credentials: 'include'` so the SESSION cookie is sent/received cross-origin
 *   (dev: Nuxt on :3000 → Spring on :8080. Prod: same origin via Caddy)
 * - Reads the `XSRF-TOKEN` cookie set by Spring's CSRF filter and forwards it
 *   as the `X-XSRF-TOKEN` header on mutating requests (POST/PUT/PATCH/DELETE)
 * - Inject as `$api` via Nuxt plugin
 */
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  const api = $fetch.create({
    baseURL: `${config.public.apiBase}/api`,
    credentials: 'include',
    onRequest({ options }) {
      const method = (options.method ?? 'GET').toUpperCase()
      const isMutation = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS'

      if (isMutation && import.meta.client) {
        const xsrf = readCookie('XSRF-TOKEN')
        if (xsrf) {
          const headers = new Headers(options.headers)
          headers.set('X-XSRF-TOKEN', xsrf)
          options.headers = headers
        }
      }
    }
  })

  return {
    provide: {
      api
    }
  }
})

function readCookie(name: string): string | undefined {
  if (!import.meta.client) return undefined
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]!) : undefined
}
