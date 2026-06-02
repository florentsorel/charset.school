// Mints the anonymous-visitor identity. Runs once per request, before render,
// so the page's parallel SSR API calls (progress + current) all see the same
// token. The token is an opaque UUID in an HttpOnly cookie; it replaces user
// accounts entirely (per-browser progress, no login). Spring reads it from the
// cookie to key module_progress / exercise_attempts.
export default defineEventHandler((event) => {
  let token = getCookie(event, 'token_id')
  if (!token) {
    token = crypto.randomUUID()
    setCookie(event, 'token_id', token, {
      httpOnly: true,
      sameSite: 'lax',
      secure: !import.meta.dev,
      path: '/',
      maxAge: 60 * 60 * 24 * 365
    })
  }
  event.context.tokenId = token
})
