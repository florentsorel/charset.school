<script setup lang="ts">
type BitRole = 'marker' | 'payload' | 'cont' | 'boundary' | 'plain'

withDefaults(
  defineProps<{
    bits: string
    role?: BitRole
    boundaryEvery?: number
  }>(),
  {
    role: 'plain',
    boundaryEvery: 0
  }
)

const roleClass: Record<BitRole, string> = {
  marker: 'bit-marker',
  payload: 'bit-payload',
  cont: 'bit-cont',
  boundary: 'bit-boundary',
  plain: ''
}
</script>

<template>
  <span class="bit-row">
    <template
      v-for="(bit, i) in bits.split('')"
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
</template>
