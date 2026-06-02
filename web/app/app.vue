<script setup lang="ts">
const { t } = useI18n()

useHead({
  meta: [
    { name: 'viewport', content: 'width=device-width, initial-scale=1' }
  ]
})

// Default social/SEO meta for every page. Titles stay per-page (useHead
// titleTemplate); here we set the description + the OG/Twitter card so shared
// links (Reddit, X, …) render a proper preview. Function values keep the
// description reactive to the active locale.
useSeoMeta({
  description: () => t('seo.description'),
  ogType: 'website',
  ogDescription: () => t('seo.description'),
  ogImage: 'https://charset.school/og.png',
  ogImageWidth: 1200,
  ogImageHeight: 630,
  ogImageAlt: () => t('seo.og_image_alt'),
  twitterCard: 'summary_large_image',
  twitterDescription: () => t('seo.description'),
  twitterImage: 'https://charset.school/og.png'
})
</script>

<template>
  <UApp>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
