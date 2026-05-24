<script setup lang="ts">
import type { User } from '~/types/user'

const { locale, locales, setLocale } = useI18n()

const items = computed(() =>
  (locales.value as Array<{ code: User['locale'], name: string }>).map(l => ({
    label: l.name,
    value: l.code,
    onSelect: () => setLocale(l.code)
  }))
)
</script>

<template>
  <UDropdownMenu :items="items">
    <button
      type="button"
      class="lang-pill"
      :aria-label="$t('auth.locale')"
    >
      <span>{{ locale.toUpperCase() }}</span>
      <svg
        width="10"
        height="10"
        viewBox="0 0 10 10"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path d="M2 4l3 3 3-3" />
      </svg>
    </button>
  </UDropdownMenu>
</template>
