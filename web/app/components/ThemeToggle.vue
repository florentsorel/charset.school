<script setup lang="ts">
// Theme toggle. Default preference is `system` (follows the OS), so the first
// click flips to the opposite of whatever is *currently shown* and pins an
// explicit light/dark - `system` naturally drops out once the user chooses.
// @nuxtjs/color-mode persists the choice (cookie), so it sticks across reloads.
const colorMode = useColorMode()

function toggle() {
  colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
}
</script>

<template>
  <!-- ClientOnly: the resolved mode for a `system` preference isn't known
       during SSR, so render the icon client-side to avoid a hydration mismatch. -->
  <ClientOnly>
    <button
      type="button"
      class="lang-pill"
      :aria-label="$t('header.theme')"
      :title="$t('header.theme')"
      @click="toggle"
    >
      <UIcon
        :name="colorMode.value === 'dark' ? 'i-lucide-moon' : 'i-lucide-sun'"
        class="w-4 h-4"
      />
    </button>
    <template #fallback>
      <button
        type="button"
        class="lang-pill"
        :aria-label="$t('header.theme')"
      >
        <UIcon
          name="i-lucide-sun"
          class="w-4 h-4"
        />
      </button>
    </template>
  </ClientOnly>
</template>
