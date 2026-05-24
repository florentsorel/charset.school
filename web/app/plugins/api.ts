// `$api`: $fetch instance pointed at the Spring back. Client uses '/api'
// (Caddy in prod, devProxy in dev). SSR uses an absolute URL because Nitro
// server-side $fetch doesn't go through devProxy and can't resolve relative
// `/api/*`. Mutating requests forward the XSRF-TOKEN cookie as a header.
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiBaseServer : '/api'

  const api = $fetch.create({
    baseURL,
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
