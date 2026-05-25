<script setup lang="ts">
</script>

<template>
  <!-- min-h-dvh + flex-col + flex-1 on the slot pushes <AppFooter> to the
       viewport bottom when the page content is short (sticky-footer pattern,
       same as sorel.dev). -->
  <div class="min-h-dvh flex flex-col">
    <AppHeader />
    <div class="flex-1 flex flex-col">
      <slot />
    </div>
    <AppFooter />
  </div>
</template>
