<script setup lang="ts">
defineProps<{
  modelValue: string | null
  choices: string[]
  disabled?: boolean
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()
</script>

<template>
  <div class="format-grid">
    <button
      v-for="choice in choices"
      :key="choice"
      type="button"
      class="format-card"
      :class="{ 'format-card-selected': modelValue === choice }"
      :disabled="disabled"
      @click="$emit('update:modelValue', choice)"
    >
      <span class="format-card-title">{{ t(choice) }}</span>
    </button>
  </div>
</template>

<style scoped>
.format-grid {
  display: grid;
  gap: 0.625rem;
  grid-template-columns: 1fr;
}
@media (min-width: 480px) {
  .format-grid {
    grid-template-columns: 1fr 1fr;
  }
}
.format-card {
  text-align: left;
  padding: 0.75rem 0.875rem;
  border: 1px solid var(--color-rule);
  background: var(--color-surface);
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 150ms ease, background 150ms ease;
}
.format-card:hover:not(:disabled) {
  border-color: var(--color-rule-strong);
}
.format-card-selected {
  border: 2px solid var(--color-accent);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}
.format-card-selected .format-card-title {
  font-weight: 600;
}
.format-card:disabled {
  opacity: 0.5;
  cursor: default;
}
.format-card-title {
  font-family: var(--font-mono);
  font-size: 0.8rem;
}
</style>
