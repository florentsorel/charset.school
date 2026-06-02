<script setup lang="ts">
const { t, tm, rt } = useI18n()
const localePath = useLocalePath()

const NuxtLink = resolveComponent('NuxtLink')

useHead({
  titleTemplate: () => `${t('landing.title_tagline')} · charset.school`
})

// Modules listed in the landing teaser, each linking to its exercise route
// (shared with the header "Exercises" menu). ASCII / Latin-1 are folded into the
// UTF-8 module (range badge), not separate modules; Windows-1252 / mojibake /
// identify-encoding are out of scope.
const modules = useExerciseModules()

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
              :to="localePath('/exercise/encode/utf-8')"
              class="btn btn-primary"
            >
              {{ t('landing.cta_primary') }}
            </NuxtLink>
            <NuxtLink
              :to="localePath('/sandbox')"
              class="btn btn-ghost"
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
        <NuxtLink
          v-for="(m, i) in modules"
          :key="m.id"
          :to="localePath(m.to)"
          class="block p-4 rounded-md border border-rule transition-colors hover:border-rule-strong"
        >
          <div class="font-mono text-xs uppercase tracking-widest text-faint mb-1">
            {{ String(i + 1).padStart(2, '0') }}
          </div>
          <div class="text-sm font-medium leading-tight">
            {{ t(`landing.modules.${m.id}.title`) }}
          </div>
          <p class="text-xs text-mute mt-1">
            {{ t(`landing.modules.${m.id}.subtitle`) }}
          </p>
        </NuxtLink>
      </div>
    </section>
  </main>
</template>
