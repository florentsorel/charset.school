<script setup lang="ts">
definePageMeta({ auth: false })

const { t, tm, rt } = useI18n()
const localePath = useLocalePath()
const { isAuthenticated } = useAuth()

const NuxtLink = resolveComponent('NuxtLink')

useHead({
  titleTemplate: () => `${t('landing.title_tagline')} · charset.school`
})

// Modules listed in the landing teaser. `to` is set only for modules that
// are actually playable today (the rest are previews). Built modules render
// as links to their exercise route.
const modules = [
  { id: 'utf8-encode', order: '01', to: '/exercise/encode/utf-8' },
  { id: 'utf8-decode', order: '02', to: '/exercise/decode/utf-8' },
  { id: 'utf16-encode', order: '03', to: '/exercise/encode/utf-16' },
  { id: 'utf16-decode', order: '04', to: '/exercise/decode/utf-16' },
  { id: 'utf32-encode', order: '05', to: '/exercise/encode/utf-32' },
  { id: 'utf32-decode', order: '06', to: '/exercise/decode/utf-32' },
  { id: 'latin1', order: '07', to: null },
  { id: 'identify', order: '08', to: null },
  { id: 'mojibake', order: '09', to: null }
] as const

// `tm` returns the raw messages array (vue-i18n compiles each entry to an
// AST node when the message compiler is enabled). `rt` resolves an AST node
// back to its final string for the current locale. We type the array as
// `string[]` because rt() also accepts plain strings as input, and the cast
// satisfies typecheck while staying correct at runtime in both compile modes.
const bulletItems = computed(() => tm('landing.bullets') as unknown as string[])
</script>

<template>
  <main>
    <!-- Hero -->
    <section class="mx-auto max-w-6xl px-4 sm:px-6 pt-20 pb-14">
      <div class="hero-grid">
        <div>
          <p class="font-mono text-xs uppercase tracking-widest text-faint mb-5">
            {{ t('landing.kicker') }}
          </p>
          <h1
            class="font-medium leading-[1.05] tracking-tight mb-6"
            style="font-size: clamp(2.5rem, 5vw, 3.5rem);"
          >
            <span>{{ t('landing.headline_lead') }}</span><br>
            <span class="text-mute">{{ t('landing.headline_tail') }}</span>
          </h1>
          <p class="text-base sm:text-lg leading-relaxed text-mute max-w-xl mb-7">
            {{ t('landing.lede') }}
          </p>
          <div class="flex items-center gap-3 flex-wrap">
            <NuxtLink
              v-if="!isAuthenticated"
              :to="localePath('/register')"
              class="btn btn-primary"
            >
              {{ t('landing.cta_primary') }}
            </NuxtLink>
            <NuxtLink
              v-if="!isAuthenticated"
              :to="localePath('/login')"
              class="btn btn-ghost"
            >
              {{ t('landing.cta_secondary') }}
            </NuxtLink>
            <NuxtLink
              :to="localePath('/sandbox')"
              :class="isAuthenticated ? 'btn btn-primary' : 'btn btn-ghost'"
            >
              {{ t('landing.cta_sandbox') }}
            </NuxtLink>
          </div>
        </div>
        <!-- Hidden on mobile/tablet to keep the hero focused on the headline.
             Shown from lg+ where there's room to host the animation. -->
        <div class="hidden lg:block">
          <HeroDemo />
        </div>
      </div>
    </section>

    <!-- Aperçu : pitch + UTF-8 example card -->
    <section
      class="border-t border-b border-rule"
      style="background: var(--color-subtle);"
    >
      <div class="mx-auto max-w-6xl px-4 sm:px-6 py-14">
        <p class="font-mono text-xs uppercase tracking-widest text-faint mb-4">
          {{ t('landing.preview_kicker') }}
        </p>
        <div class="grid grid-cols-1 md:grid-cols-[1fr_400px] gap-10 items-start">
          <div>
            <h2 class="text-2xl font-medium leading-tight tracking-tight mb-3">
              <span>{{ t('landing.preview_title_lead') }}</span><br>
              <span>{{ t('landing.preview_title_tail') }}</span>
            </h2>
            <p class="text-md leading-relaxed text-mute mb-5 max-w-md">
              {{ t('landing.preview_blurb') }}
            </p>
            <ul class="text-sm text-mute leading-relaxed space-y-1.5">
              <li
                v-for="(bullet, idx) in bulletItems"
                :key="idx"
                class="flex gap-3 items-baseline"
              >
                <svg
                  width="11"
                  height="11"
                  viewBox="0 0 11 11"
                  fill="none"
                  stroke="var(--color-ok)"
                  stroke-width="2"
                  class="flex-shrink-0"
                  style="margin-top: 4px;"
                  aria-hidden="true"
                >
                  <path d="M2 6l2.2 2L9 3" />
                </svg>
                <span>{{ rt(bullet) }}</span>
              </li>
            </ul>
          </div>

          <!-- Coach feedback preview — illustrates the "Un coach qui te
               corrige bit à bit" promise of this section. Distinct from
               the hero's encoding demo: this one shows a *mistake* being
               caught and explained. -->
          <CoachFeedbackPreview />
        </div>
      </div>
    </section>

    <!-- Modules grid -->
    <section class="mx-auto max-w-6xl px-4 sm:px-6 py-16">
      <h2 class="font-mono text-sm uppercase tracking-widest text-mute mb-5">
        {{ t('landing.modules_kicker') }}
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        <component
          :is="m.to ? NuxtLink : 'div'"
          v-for="m in modules"
          :key="m.id"
          :to="m.to ? localePath(m.to) : undefined"
          class="block p-4 rounded-md border border-rule transition-colors"
          :class="m.to ? 'hover:border-rule-strong' : 'opacity-55'"
        >
          <div class="font-mono text-xs uppercase tracking-widest text-faint mb-1">
            {{ m.order }}
          </div>
          <div class="text-sm font-medium leading-tight">
            {{ t(`landing.modules.${m.id}.title`) }}
          </div>
          <p class="text-xs text-mute mt-1">
            {{ t(`landing.modules.${m.id}.subtitle`) }}
          </p>
        </component>
      </div>
    </section>
  </main>
</template>
