import { appendResponseHeader } from 'h3'

// Forwards Set-Cookie headers from an SSR-side $fetch response to the
// browser-bound Nuxt response, so cookies the back wants the client to keep
// (e.g. XSRF-TOKEN) reach the browser instead of stopping at the Node process.
export function forwardSetCookies(responseHeaders: Headers) {
  const setCookies = responseHeaders.getSetCookie?.() ?? []
  if (!setCookies.length) return
  const event = useRequestEvent()
  if (!event) return
  for (const cookie of setCookies) {
    appendResponseHeader(event, 'set-cookie', cookie)
  }
}
