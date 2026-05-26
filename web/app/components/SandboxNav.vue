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
            v-if="m.available"
            :to="localePath(m.to)"
            class="sandbox-nav-link"
            :class="{ 'sandbox-nav-link-active': isActive(m.to) }"
          >
            {{ t(`landing.modules.${m.catalogKey}.title`) }}
          </NuxtLink>
          <div
            v-else
            class="sandbox-nav-link sandbox-nav-link-disabled"
          >
            <span>{{ t(`landing.modules.${m.catalogKey}.title`) }}</span>
            <span class="text-[10px] uppercase tracking-wider text-faint">
              {{ t('sandbox.landing.coming_soon') }}
            </span>
          </div>
        </li>
      </ul>
    </div>
  </nav>
</template>
