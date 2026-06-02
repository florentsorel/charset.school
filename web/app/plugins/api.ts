// `$api`: $fetch instance pointed at the Spring back. Client uses '/api'
// (Caddy in prod, devProxy in dev). SSR uses an absolute URL because Nitro
// server-side $fetch doesn't go through devProxy and can't resolve relative
// `/api/*`.
//
// Identity is an anonymous `token_id` cookie minted by the Nitro server
// middleware (server/middleware/token-id.ts). On the client the HttpOnly
// cookie is attached automatically (credentials:'include'). On SSR the
// browser cookies aren't on the server-side fetch, so we forward the incoming
// `cookie` header and make sure `token_id` is present — on a first visit the
// cookie was just minted into the request event but isn't in the incoming
// header yet. Without this, an SSR data fetch (useAsyncData -> /progress)
// would reach the back without a token and render an empty shell the client
// then has to correct on hydration.
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const baseURL = import.meta.server ? config.apiBaseServer : '/api'

  // Captured once per request at plugin init (SSR runs the plugin per request).
  // The middleware runs before render, so event.context.tokenId is always set.
  const ssrCookie = import.meta.server ? useRequestHeaders(['cookie']).cookie : undefined
  const tokenId = import.meta.server ? (useRequestEvent()?.context.tokenId as string | undefined) : undefined

  const api = $fetch.create({
    baseURL,
    credentials: 'include',
    onRequest({ options }) {
      if (import.meta.server) {
        const parts: string[] = []
        if (ssrCookie) parts.push(ssrCookie)
        if (tokenId && !ssrCookie?.includes('token_id=')) parts.push(`token_id=${tokenId}`)
        if (parts.length) {
          const headers = new Headers(options.headers)
          headers.set('cookie', parts.join('; '))
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
