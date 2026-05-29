<script setup lang="ts">
type BitRole = 'marker' | 'payload' | 'cont' | 'boundary' | 'plain'

const props = withDefaults(
  defineProps<{
    bits: string
    role?: BitRole
    boundaryEvery?: number
    wrapEvery?: number
  }>(),
  {
    role: 'plain',
    boundaryEvery: 0,
    wrapEvery: 16
  }
)

const roleClass: Record<BitRole, string> = {
  marker: 'bit-marker',
  payload: 'bit-payload',
  cont: 'bit-cont',
  boundary: 'bit-boundary',
  plain: ''
}

const rows = computed(() => {
  const chars = props.bits.split('')
  const size = props.wrapEvery > 0 ? props.wrapEvery : chars.length
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
      class="bit-row"
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
          :class="roleClass[role]"
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
</style>
