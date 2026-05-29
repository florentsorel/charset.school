<script setup lang="ts">
const props = defineProps<{
  modelValue: string
  length: number
  boundaryEvery?: number
  wrapEvery?: number
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// Default: 8 bits per row on narrow viewports, 16 once 685px is reached
// (= width below which 16 bits + container padding overflow the input box).
// Caller can override via `wrapEvery` prop if needed.
const fitsWide = useFitsWideBitRow()
const effectiveWrap = computed(() => {
  if (props.wrapEvery && props.wrapEvery > 0) return props.wrapEvery
  return fitsWide.value ? 16 : 8
})

const cells = computed(() => {
  const padded = props.modelValue.padEnd(props.length, ' ')
  return Array.from({ length: props.length }, (_, i) => padded[i] === ' ' ? '' : padded[i])
})

const rows = computed(() => {
  const size = effectiveWrap.value
  const out: number[][] = []
  for (let start = 0; start < props.length; start += size) {
    const end = Math.min(start + size, props.length)
    out.push(Array.from({ length: end - start }, (_, k) => start + k))
  }
  return out
})

const refs = ref<HTMLInputElement[]>([])

function setCell(index: number, char: string) {
  const next = cells.value.slice()
  next[index] = char
  emit('update:modelValue', next.map(c => c || ' ').join('').trimEnd())
}

function onInput(index: number, ev: Event) {
  const target = ev.target as HTMLInputElement
  const raw = target.value
  const last = raw.slice(-1)
  if (last !== '0' && last !== '1') {
    target.value = cells.value[index] || ''
    return
  }
  setCell(index, last)
  target.value = last
  focusNext(index)
}

function onKeydown(index: number, ev: KeyboardEvent) {
  if (ev.key === 'Backspace') {
    ev.preventDefault()
    if (cells.value[index]) {
      setCell(index, '')
    } else {
      focusPrev(index)
      setCell(Math.max(0, index - 1), '')
    }
  } else if (ev.key === 'ArrowLeft') {
    ev.preventDefault()
    focusPrev(index)
  } else if (ev.key === 'ArrowRight') {
    ev.preventDefault()
    focusNext(index)
  } else if (ev.key === '0' || ev.key === '1') {
    ev.preventDefault()
    setCell(index, ev.key)
    focusNext(index)
  }
}

function focusNext(index: number) {
  refs.value[Math.min(props.length - 1, index + 1)]?.focus()
}

function focusPrev(index: number) {
  refs.value[Math.max(0, index - 1)]?.focus()
}
</script>

<template>
  <span class="bit-rows">
    <span
      v-for="(row, ri) in rows"
      :key="ri"
      class="bit-row bit-row-nowrap"
    >
      <template
        v-for="(i, k) in row"
        :key="i"
      >
        <span
          v-if="boundaryEvery && boundaryEvery > 0 && k > 0 && k % boundaryEvery === 0"
          class="bit-sep-mid"
        />
        <input
          :ref="el => { if (el) refs[i] = el as HTMLInputElement }"
          class="bit bit-input"
          type="text"
          inputmode="numeric"
          maxlength="1"
          :value="cells[i]"
          :disabled="disabled"
          :aria-label="$t('exercise.bit_input_label', { n: i + 1 })"
          @input="onInput(i, $event)"
          @keydown="onKeydown(i, $event)"
        >
      </template>
    </span>
  </span>
</template>

<style scoped>
.bit-rows {
  display: inline-flex;
  flex-direction: column;
  gap: 0.4rem;
}
.bit-row-nowrap {
  flex-wrap: nowrap;
}
.bit-input {
  text-align: center;
  outline: none;
  caret-color: transparent;
}
.bit-input:focus {
  border: 2px solid var(--color-accent);
  background: var(--color-accent-soft);
  color: var(--color-accent);
}
.bit-input:disabled {
  opacity: 0.5;
}
</style>
