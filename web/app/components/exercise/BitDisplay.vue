<script setup lang="ts">
type BitRole = 'marker' | 'payload' | 'cont' | 'boundary' | 'plain'

type BitSegment = { length: number, role: BitRole }

const props = withDefaults(
  defineProps<{
    bits: string
    role?: BitRole
    boundaryEvery?: number
    wrapEvery?: number
    segments?: BitSegment[]
  }>(),
  {
    role: 'plain',
    boundaryEvery: 0
  }
)

// Mirrors BitInput: md+ shows the whole value on one line; below md a separated
// binary wraps at 8 (byte / two nibbles per line), an unseparated value stays whole.
const mdUp = useMediaQuery('(min-width: 768px)')
const effectiveWrap = computed(() => {
  if (props.wrapEvery && props.wrapEvery > 0) return props.wrapEvery
  if (mdUp.value) return props.bits.length
  return props.boundaryEvery && props.boundaryEvery > 0 ? 8 : props.bits.length
})

const roleClass: Record<BitRole, string> = {
  marker: 'bit-marker',
  payload: 'bit-payload',
  cont: 'bit-cont',
  boundary: 'bit-boundary',
  plain: ''
}

const roleByIndex = computed<BitRole[]>(() => {
  const total = props.bits.length
  if (!props.segments || props.segments.length === 0) {
    return Array.from({ length: total }, () => props.role)
  }
  const out: BitRole[] = []
  let cursor = 0
  for (const seg of props.segments) {
    for (let k = 0; k < seg.length && cursor < total; k++) {
      out.push(seg.role)
      cursor++
    }
  }
  while (cursor < total) {
    out.push(props.role)
    cursor++
  }
  return out
})

const rows = computed(() => {
  const chars = props.bits.split('')
  const size = effectiveWrap.value
  if (chars.length <= size) return [chars]
  const out: string[][] = []
  for (let start = 0; start < chars.length; start += size) {
    out.push(chars.slice(start, start + size))
  }
  return out
})
</script>

<template>
  <span class="bit-rows">
    <span
      v-for="(row, ri) in rows"
      :key="ri"
      class="bit-row bit-row-nowrap"
    >
      <template
        v-for="(bit, i) in row"
        :key="i"
      >
        <span
          v-if="boundaryEvery > 0 && i > 0 && i % boundaryEvery === 0"
          class="bit-sep-mid"
        />
        <span
          class="bit"
          :class="roleClass[roleByIndex[ri * effectiveWrap + i] ?? role]"
        >{{ bit }}</span>
      </template>
    </span>
  </span>
</template>

<style scoped>
.bit-rows {
  display: inline-flex;
  flex-direction: column;
  gap: 0.3rem;
}
.bit-row-nowrap {
  flex-wrap: nowrap;
}
</style>
