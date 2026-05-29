<script setup lang="ts">
const props = defineProps<{
  modelValue: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const raw = ref(props.modelValue != null ? String(props.modelValue) : '')

watch(() => props.modelValue, (v) => {
  raw.value = v != null ? String(v) : ''
})

function onInput(ev: Event) {
  const target = ev.target as HTMLInputElement
  const cleaned = target.value.replace(/[^0-9]/g, '')
  raw.value = cleaned
  if (cleaned.length === 0) {
    emit('update:modelValue', null)
    return
  }
  const value = parseInt(cleaned, 10)
  emit('update:modelValue', isNaN(value) ? null : value)
}
</script>

<template>
  <div class="useful-bit-count-input">
    <input
      class="useful-bit-count-cell"
      type="text"
      inputmode="numeric"
      maxlength="2"
      :value="raw"
      :disabled="disabled"
      :aria-label="$t('exercise.useful_bit_count_input_label')"
      @input="onInput"
    >
    <span class="suffix">{{ $t('exercise.useful_bit_count_suffix') }}</span>
  </div>
</template>

<style scoped>
.useful-bit-count-input {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.useful-bit-count-cell {
  width: 3rem;
  height: 2rem;
  border: 1px solid var(--color-rule);
  border-radius: 4px;
  background: var(--color-surface);
  color: var(--color-ink);
  font-family: var(--font-mono);
  font-size: 1rem;
  font-weight: 600;
  text-align: center;
  outline: none;
}
.useful-bit-count-cell:focus {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}
.useful-bit-count-cell:disabled {
  opacity: 0.5;
}
.suffix {
  color: var(--color-mute);
  font-size: 0.9rem;
}
</style>
