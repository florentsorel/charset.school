<script setup lang="ts">
const { t } = useI18n()
const route = useRoute()
const { activeModule } = useSandboxModules()

const triggerLabel = computed(() =>
  activeModule.value
    ? t(`landing.modules.${activeModule.value.catalogKey}.title`)
    : t('sandbox.landing.title')
)

const navEl = useTemplateRef<HTMLDetailsElement>('navEl')
watch(() => route.path, () => {
  if (navEl.value) navEl.value.open = false
})
</script>

<template>
  <div class="min-h-dvh flex flex-col">
    <AppHeader />
    <div class="flex-1 flex flex-col">
      <div class="mx-auto w-full max-w-6xl px-4 sm:px-6 py-10 md:py-14 flex flex-col lg:flex-row gap-6 lg:gap-12">
        <!-- Mobile / tablet collapsible -->
        <details
          ref="navEl"
          class="sandbox-nav-collapsible lg:hidden"
        >
          <summary>
            <span class="text-sm font-medium">{{ triggerLabel }}</span>
            <UIcon
              name="i-lucide-chevron-down"
              class="sandbox-nav-collapsible-chevron"
            />
          </summary>
          <div class="pt-4">
            <SandboxNav />
          </div>
        </details>

        <!-- Desktop sticky sidebar -->
        <aside class="hidden lg:block lg:w-56 lg:shrink-0 lg:sticky lg:self-start sandbox-sidebar-sticky">
          <SandboxNav />
        </aside>

        <div class="flex-1 min-w-0">
          <slot />
        </div>
      </div>
    </div>
    <AppFooter />
  </div>
</template>
