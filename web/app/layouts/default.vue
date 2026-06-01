<script setup lang="ts">
</script>

<template>
  <div class="min-h-dvh flex flex-col">
    <AppHeader />
    <div class="flex-1 flex flex-col">
      <slot />
    </div>
    <AppFooter />
  </div>
</template>
