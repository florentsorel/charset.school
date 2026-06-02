<script setup lang="ts">
// Two locales (FR / EN) → a toggle, not a dropdown. Shows the active locale;
// clicking flips to the other (and navigates, since the EN routes are prefixed).
const { locale, setLocale } = useI18n()

function toggle() {
  setLocale(locale.value === 'fr' ? 'en' : 'fr')
}
</script>

<template>
  <button
    type="button"
    class="lang-pill"
    :aria-label="$t('header.locale')"
    :title="$t('header.locale')"
    @click="toggle"
  >
    <span>{{ locale.toUpperCase() }}</span>
  </button>
</template>
