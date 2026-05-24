<script setup lang="ts">
defineProps<{
  message?: string | null
}>()
</script>

<template>
  <div
    v-if="message"
    class="flex gap-2 items-start text-bad bg-bad-soft p-3 rounded-md"
    role="alert"
  >
    <svg
      width="14"
      height="14"
      viewBox="0 0 14 14"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      class="flex-shrink-0 mt-0.5"
      aria-hidden="true"
    >
      <circle
        cx="7"
        cy="7"
        r="5.5"
      />
      <path
        d="M7 4v3.5M7 9.5v.5"
        stroke-linecap="round"
      />
    </svg>
    <p class="text-sm leading-normal">
      {{ message }}
    </p>
  </div>
</template>
