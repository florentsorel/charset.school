<script setup lang="ts">
const { t } = useI18n()
const localePath = useLocalePath()
const { groups, isActive } = useSandboxModules()
</script>

<template>
  <nav
    class="sandbox-nav"
    :aria-label="t('sandbox.landing.title')"
  >
    <div
      v-for="group in groups"
      :key="group.label"
      class="mb-5 last:mb-0"
    >
      <p class="font-mono text-[10px] uppercase tracking-widest text-mute mb-2">
        {{ group.label }}
      </p>
      <ul class="flex flex-col gap-px">
        <li
          v-for="m in group.modules"
          :key="m.to"
        >
          <NuxtLink
            :to="localePath(m.to)"
            class="sandbox-nav-link"
            :class="{ 'sandbox-nav-link-active': isActive(m.to) }"
          >
            {{ t(`landing.modules.${m.catalogKey}.title`) }}
          </NuxtLink>
        </li>
      </ul>
    </div>
  </nav>
</template>
