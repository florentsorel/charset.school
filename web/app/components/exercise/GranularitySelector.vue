<script setup lang="ts">
import type { Granularity } from '~/types/exercise'

defineProps<{
  modelValue: Granularity
}>()

defineEmits<{
  'update:modelValue': [value: Granularity]
}>()

const { t } = useI18n()

const choices: Granularity[] = ['verbose', 'standard', 'compact']
</script>

<template>
  <div class="granularity-selector">
    <p class="granularity-label">
      {{ t('exercise.granularity_label') }}
    </p>
    <div
      class="granularity-options"
      role="radiogroup"
      :aria-label="t('exercise.granularity_label')"
    >
      <button
        v-for="g in choices"
        :key="g"
        type="button"
        role="radio"
        :aria-checked="modelValue === g"
        class="granularity-option"
        :class="{ 'granularity-option-selected': modelValue === g }"
        @click="$emit('update:modelValue', g)"
      >
        {{ t(`exercise.granularity.${g}`) }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.granularity-selector {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.granularity-label {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-faint);
}
.granularity-options {
  display: inline-flex;
  border: 1px solid var(--color-rule);
  border-radius: 999px;
  padding: 2px;
  background: var(--color-surface);
}
.granularity-option {
  padding: 0.35rem 0.85rem;
  border: 0;
  background: transparent;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  color: var(--color-mute);
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.granularity-option-selected {
  background: var(--color-accent);
  color: var(--color-accent-ink);
}
</style>
