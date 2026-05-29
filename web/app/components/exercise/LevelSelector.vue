<script setup lang="ts">
defineProps<{
  modelValue: number
  max?: number
  suggested?: number
}>()

defineEmits<{
  'update:modelValue': [value: number]
}>()

const { t } = useI18n()
</script>

<template>
  <div class="level-selector">
    <p class="level-label">
      {{ t('exercise.level_label') }}
    </p>
    <div class="level-buttons">
      <button
        v-for="n in (max ?? 5)"
        :key="n"
        type="button"
        class="level-button"
        :class="{
          'level-button-selected': modelValue === n,
          'level-button-suggested': suggested === n && modelValue !== n
        }"
        :aria-label="t('exercise.level_option', { n })"
        @click="$emit('update:modelValue', n)"
      >
        {{ n }}
      </button>
    </div>
    <p
      v-if="suggested && suggested !== modelValue"
      class="level-suggested-hint"
    >
      {{ t('exercise.level_suggested', { n: suggested }) }}
    </p>
  </div>
</template>

<style scoped>
.level-selector {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.level-label {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-faint);
}
.level-buttons {
  display: inline-flex;
  gap: 0.35rem;
}
.level-button {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  border: 1px solid var(--color-rule);
  background: var(--color-surface);
  color: var(--color-mute);
  font-family: var(--font-mono);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease, border-color 150ms ease;
}
.level-button:hover {
  border-color: var(--color-rule-strong);
  color: var(--color-ink);
}
.level-button-selected {
  background: var(--color-accent);
  color: var(--color-accent-ink);
  border-color: var(--color-accent);
}
.level-button-suggested {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.level-suggested-hint {
  font-size: 0.78rem;
  color: var(--color-mute);
}
</style>
