<script setup lang="ts">
const { t } = useI18n()
const colorMode = useColorMode()

const isDark = computed(() => colorMode.value === 'dark')

// Cycle: system → light → dark → system
function toggle() {
  const next = colorMode.preference === 'system'
    ? 'light'
    : colorMode.preference === 'light'
      ? 'dark'
      : 'system'
  colorMode.preference = next
}

const label = computed(() => {
  if (colorMode.preference === 'system') return t('common.theme_system')
  return isDark.value ? t('common.theme_dark') : t('common.theme_light')
})
</script>

<template>
  <ClientOnly>
    <button
      type="button"
      class="lang-pill"
      :title="label"
      :aria-label="label"
      @click="toggle"
    >
      <svg
        v-if="isDark"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
      </svg>
      <svg
        v-else
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle
          cx="12"
          cy="12"
          r="4"
        />
        <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
      </svg>
      <span>{{ isDark ? t('common.theme_dark_short') : t('common.theme_light_short') }}</span>
    </button>

    <template #fallback>
      <span class="lang-pill opacity-60">
        <span>···</span>
      </span>
    </template>
  </ClientOnly>
</template>
