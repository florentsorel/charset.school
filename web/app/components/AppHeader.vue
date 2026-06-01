<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const route = useRoute()
const { isAuthenticated, user, logout } = useAuth()

// The 6 playable exercise modules (shared with the home grid), with localized
// labels from `landing.modules.<id>`.
const exerciseLinks = computed(() => useExerciseModules().map(m => ({
  id: m.id,
  to: localePath(m.to),
  title: t(`landing.modules.${m.id}.title`),
  subtitle: t(`landing.modules.${m.id}.subtitle`)
})))

// Desktop "Exercises" mega-menu: a full-width panel under the header. Closed on
// Escape, outside click (backdrop), and navigation. Teleported to <body> so the
// header's backdrop-blur (a containing block) doesn't trap the fixed panel.
const exercisesOpen = ref(false)
// Burger sub-menu: "Exercises" expands its links in place (accordion that grows
// the menu height), instead of the side flyout Nuxt UI's native `children`
// renders. Reset whenever we navigate so it reopens collapsed.
const exercisesExpanded = ref(false)
function closeExercises() {
  exercisesOpen.value = false
}
function onWindowKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeExercises()
}
onMounted(() => window.addEventListener('keydown', onWindowKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onWindowKeydown))
watch(() => route.fullPath, () => {
  closeExercises()
  exercisesExpanded.value = false
})

async function onLogout() {
  try {
    await logout()
  } finally {
    await navigateTo(localePath('/login'))
  }
}

// First letters of `user.name`, falling back to the email prefix. Code-point
// aware (Array.from) so emojis / multi-byte glyphs don't break.
const initials = computed(() => {
  if (!user.value) return ''
  const source = user.value.name?.trim() || user.value.email
  const words = source.split(/\s+/)
  return words
    .map(w => Array.from(w)[0] ?? '')
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase()
})

// Dropdown items for the authenticated user avatar (desktop) and the mobile
// hamburger. The locale switcher used to live here too, but it overrode the
// per-user locale stored in the profile — language is now managed solely
// from the profile settings.
const authedItems = computed(() => [
  { label: t('header.my_profile'), icon: 'i-lucide-user', to: localePath('/profile') },
  { label: t('auth.logout'), icon: 'i-lucide-log-out', onSelect: onLogout }
])

const guestItems = computed(() => [
  { label: t('auth.login'), icon: 'i-lucide-log-in', to: localePath('/login') },
  { label: t('auth.register'), icon: 'i-lucide-user-plus', to: localePath('/register') }
])

const sandboxItem = computed(() => ({
  label: t('header.sandbox'),
  icon: 'i-lucide-flask-conical',
  to: localePath('/sandbox')
}))

// Burger groups: an expandable "Exercises" sub-menu first, then sandbox + auth.
// The toggle item calls preventDefault so selecting it grows the list in place
// rather than closing the menu; its 6 links are injected (indented) when open.
const mobileMenuItems = computed(() => {
  const exercisesToggle = {
    label: t('header.exercises'),
    icon: 'i-lucide-binary',
    trailingIcon: exercisesExpanded.value ? 'i-lucide-chevron-up' : 'i-lucide-chevron-down',
    onSelect: (event: Event) => {
      event.preventDefault()
      exercisesExpanded.value = !exercisesExpanded.value
    }
  }
  const exerciseChildren = exercisesExpanded.value
    ? exerciseLinks.value.map(e => ({ label: e.title, to: e.to, class: 'app-dropdown-subitem' }))
    : []
  return [
    [exercisesToggle, ...exerciseChildren],
    [sandboxItem.value, ...(isAuthenticated.value ? authedItems.value : guestItems.value)]
  ]
})

// Custom styling that overrides Nuxt UI's default dropdown look so it
// matches our design tokens (surface/rule/subtle colors, sans font,
// understated shadow). Same shape used by the desktop user menu.
const dropdownUi = {
  content: 'app-dropdown',
  item: 'app-dropdown-item',
  itemLeadingIcon: 'app-dropdown-item-icon',
  separator: 'app-dropdown-separator'
}

// Mobile variant: full viewport width, no rounded corners, only top/bottom
// borders so the panel touches the screen edges.
const mobileDropdownUi = {
  ...dropdownUi,
  content: 'app-dropdown app-dropdown--full'
}
</script>

<template>
  <header class="sticky top-0 z-40 bg-page/85 backdrop-blur border-b border-rule h-[var(--header-height)]">
    <div class="mx-auto max-w-6xl px-4 sm:px-6 h-full flex items-center justify-between gap-4">
      <div class="flex items-center gap-3 sm:gap-5">
        <NuxtLink
          :to="localePath('/')"
          class="flex items-center gap-2 hover:opacity-80 transition-opacity"
        >
          <img
            src="/logo.svg"
            width="22"
            height="13"
            alt="charset.school"
            aria-hidden="true"
          >
          <span class="font-mono text-md leading-none lowercase">charset.school</span>
        </NuxtLink>

        <!-- Desktop "Exercises" trigger (≥ lg). Toggles a full-width panel
             (teleported) below the header. Below lg it lives in the burger.
             Visibility goes on this wrapper, not the button: `.btn` forces
             `display: inline-flex` (unlayered) and would beat Tailwind's
             layered `hidden`. -->
        <div class="hidden lg:block">
          <button
            type="button"
            class="btn btn-ghost text-sm gap-1"
            aria-haspopup="true"
            aria-controls="header-exercises-menu"
            :aria-expanded="exercisesOpen"
            @click="exercisesOpen = !exercisesOpen"
          >
            {{ t('header.exercises') }}
            <UIcon
              name="i-lucide-chevron-down"
              class="w-4 h-4 transition-transform"
              :class="{ 'rotate-180': exercisesOpen }"
            />
          </button>
        </div>
      </div>

      <!-- Desktop nav (≥ lg). Wrapped in a div because UDropdownMenu's
           `class` prop lands on its inner trigger, not on a wrapper —
           making conditional visibility unreliable when applied directly. -->
      <div class="hidden lg:flex items-center gap-2.5">
        <NuxtLink
          :to="localePath('/sandbox')"
          class="btn btn-soft text-sm"
        >
          {{ t('header.sandbox') }}
        </NuxtLink>

        <template v-if="!isAuthenticated">
          <NuxtLink
            :to="localePath('/login')"
            class="btn btn-ghost text-sm"
          >
            {{ t('auth.login') }}
          </NuxtLink>
          <NuxtLink
            :to="localePath('/register')"
            class="btn btn-primary text-sm"
          >
            {{ t('auth.register') }}
          </NuxtLink>
        </template>

        <UDropdownMenu
          v-else
          :items="authedItems"
          :ui="dropdownUi"
        >
          <button
            type="button"
            class="user-avatar"
            :aria-label="t('header.user_menu')"
            :title="t('header.user_menu')"
          >
            {{ initials }}
          </button>
        </UDropdownMenu>
      </div>

      <!-- Burger (< lg). Same wrapper-div trick. The dropdown panel is forced
           to full viewport width via the .app-dropdown--full modifier, anchored
           to the right edge of the trigger. Holds the exercises + sandbox + auth. -->
      <div class="lg:hidden">
        <UDropdownMenu
          :items="mobileMenuItems"
          :content="{ side: 'bottom', align: 'end', sideOffset: 12, collisionPadding: 0 }"
          :ui="mobileDropdownUi"
        >
          <button
            type="button"
            class="hamburger-btn"
            :aria-label="t('header.menu')"
          >
            <UIcon
              name="i-lucide-menu"
              class="w-5 h-5"
            />
          </button>
        </UDropdownMenu>
      </div>
    </div>

    <!-- Full-width "Exercises" mega-menu, teleported to <body> so the header's
         backdrop-blur doesn't trap these fixed elements. Desktop only. -->
    <Teleport to="body">
      <div
        v-if="exercisesOpen"
        class="hidden lg:block"
      >
        <div
          class="fixed inset-0 z-30"
          aria-hidden="true"
          @click="closeExercises"
        />
        <nav
          id="header-exercises-menu"
          class="fixed left-0 right-0 z-40 bg-page border-b border-rule shadow-sm top-[var(--header-height)]"
          :aria-label="t('header.exercises')"
        >
          <div class="mx-auto max-w-6xl px-4 sm:px-6 py-6">
            <div class="grid grid-cols-2 lg:grid-cols-3 gap-3">
              <NuxtLink
                v-for="link in exerciseLinks"
                :key="link.id"
                :to="link.to"
                class="block p-4 rounded-md border border-rule hover:border-rule-strong transition-colors"
                @click="closeExercises"
              >
                <div class="text-sm font-medium leading-tight">
                  {{ link.title }}
                </div>
                <p class="text-xs text-mute mt-1">
                  {{ link.subtitle }}
                </p>
              </NuxtLink>
            </div>
          </div>
        </nav>
      </div>
    </Teleport>
  </header>
</template>
