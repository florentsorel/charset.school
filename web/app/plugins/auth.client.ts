// Hydrate auth state on client boot. SSR already populates user.value when
// the route's middleware runs fetchMe - and forwards Set-Cookie headers from
// the back so the browser ends up with XSRF-TOKEN for future mutations.
export default defineNuxtPlugin(async () => {
  const { fetchMe, user } = useAuth()
  if (user.value) return
  await fetchMe()
})
