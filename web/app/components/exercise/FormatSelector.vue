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

// UTF-8 marker patterns per choice. Each byte string mixes fixed marker bits
// (`0` or `1`) with payload placeholders (`x`).
const BIT_PATTERNS: Record<string, string[]> = {
  'format-choice.byte-count.1': ['0xxxxxxx'],
  'format-choice.byte-count.2': ['110xxxxx', '10xxxxxx'],
  'format-choice.byte-count.3': ['1110xxxx', '10xxxxxx', '10xxxxxx'],
  'format-choice.byte-count.4': ['11110xxx', '10xxxxxx', '10xxxxxx', '10xxxxxx']
}

function patternFor(choice: string): string[] | null {
  return BIT_PATTERNS[choice] ?? null
}

function roleFor(char: string, byteIndex: number): 'marker' | 'cont' | 'payload' {
  if (char === 'x') return 'payload'
  return byteIndex === 0 ? 'marker' : 'cont'
}
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
      <div
        v-if="patternFor(choice)"
        class="format-pattern"
      >
        <span
          v-for="(byte, byteIndex) in patternFor(choice)!"
          :key="byteIndex"
          class="format-byte"
        >
          <span
            v-for="(char, charIndex) in byte.split('')"
            :key="charIndex"
            class="bit format-pattern-bit"
            :class="{
              'bit-marker': roleFor(char, byteIndex) === 'marker',
              'bit-cont': roleFor(char, byteIndex) === 'cont',
              'bit-payload': roleFor(char, byteIndex) === 'payload'
            }"
          >{{ char }}</span>
        </span>
      </div>
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
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
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
.format-pattern {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  align-items: center;
}
.format-byte {
  display: inline-flex;
  gap: 2px;
}
.format-pattern-bit {
  width: 1.1rem;
  height: 1.4rem;
  font-size: 0.75rem;
  line-height: 1;
}
.format-card-title {
  font-family: var(--font-mono);
  font-size: 0.8rem;
}
</style>
