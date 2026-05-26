<script setup lang="ts">
definePageMeta({ auth: false })

const localePath = useLocalePath()
await navigateTo(localePath('/sandbox/encode/utf-8'), { replace: true })
</script>

<template>
  <div />
</template>
