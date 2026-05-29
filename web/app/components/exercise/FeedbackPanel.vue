<script setup lang="ts">
defineProps<{
  errorType: string | null
  params: Record<string, string>
  attempts: number
  canReveal: boolean
  threshold: number
}>()

defineEmits<{
  reveal: []
}>()

const { t, te } = useI18n()

function hintMessage(errorType: string, params: Record<string, string>): string {
  const key = `feedback.${errorType}`
  if (te(key)) return t(key, params)
  return t('feedback.default')
}
</script>

<template>
  <div
    v-if="errorType"
    class="feedback-panel"
    role="alert"
  >
    <div class="feedback-header">
      <span class="feedback-tag">{{ t('exercise.feedback.try', { n: attempts, threshold }) }}</span>
    </div>
    <p class="feedback-message">
      <InlineDesc :text="hintMessage(errorType, params)" />
    </p>
    <div
      v-if="canReveal"
      class="feedback-actions"
    >
      <button
        type="button"
        class="btn btn-ghost"
        @click="$emit('reveal')"
      >
        {{ t('exercise.feedback.reveal_button') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.feedback-panel {
  margin-top: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--color-bad-soft);
  border: 1px solid color-mix(in oklab, var(--color-bad) 30%, var(--color-bad-soft));
  border-radius: 8px;
}
.feedback-header {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.feedback-tag {
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--color-bad);
  font-weight: 600;
}
.feedback-message {
  font-size: 0.92rem;
  line-height: 1.5;
  color: var(--color-ink);
}
.feedback-actions {
  margin-top: 0.625rem;
  display: flex;
  gap: 0.5rem;
}
</style>
