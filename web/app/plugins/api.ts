// `$api`: $fetch instance pointed at the Spring back. Client uses '/api'
// (Caddy in prod, devProxy in dev). SSR uses an absolute URL because Nitro
// server-side $fetch doesn't go through devProxy and can't resolve relative
// `/api/*`. Mutating requests forward the XSRF-TOKEN cookie as a header.
//
// On SSR the browser cookies aren't attached automatically (the server-side
// $fetch is a fresh request, not the user's browser), so we forward the
// incoming request's `cookie` header on every SSR call. Without this, an
// SSR data fetch (e.g. useAsyncData -> /progress) hits the back
// unauthenticated and renders a logged-out shell that the client then has
// to correct on hydration — exactly the flash we want to avoid.
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiBaseServer : '/api'

  // Captured once per request at plugin init (SSR runs the plugin per
  // request). `useRequestHeaders` must be called in a Nuxt setup context,
  // which the plugin body is — calling it inside onRequest would be too late.
  const ssrCookie = import.meta.server ? useRequestHeaders(['cookie']).cookie : undefined

  const api = $fetch.create({
    baseURL,
    credentials: 'include',
    onRequest({ options }) {
      const method = (options.method ?? 'GET').toUpperCase()
      const isMutation = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS'

      if (import.meta.server && ssrCookie) {
        const headers = new Headers(options.headers)
        headers.set('cookie', ssrCookie)
        options.headers = headers
      }

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
