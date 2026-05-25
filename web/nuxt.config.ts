export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@nuxtjs/i18n',
    '@nuxt/content',
    '@nuxtjs/seo',
    'nuxt-skill-hub'
  ],

  devtools: {
    enabled: true
  },

  app: {
    head: {
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico', sizes: 'any' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'manifest', href: '/site.webmanifest' }
      ],
      meta: [
        { name: 'theme-color', content: '#0F3D6B' }
      ]
    }
  },

  css: ['~/assets/css/main.css'],

  // Site config consumed by @nuxtjs/seo: feeds `%siteName` in the default
  // titleTemplate (`%s %separator %siteName`), canonical URLs, sitemap,
  // robots.txt, OG defaults, …
  site: {
    name: 'Charset School',
    url: 'https://charset.sorel.dev',
    defaultLocale: 'en'
  },

  runtimeConfig: {
    apiBaseServer: 'http://localhost:8080/api',
    public: {
      skipClientValidation: false
    }
  },

  routeRules: {
    '/': { prerender: false }
  },

  compatibilityDate: '2025-01-15',

  // Mirror what Caddy does in prod, so the front always talks to /api/* on
  // the same origin — no CORS, no env vars to swap between envs.
  nitro: {
    devProxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true
      }
    }
  },

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  },

  i18n: {
    defaultLocale: 'en',
    strategy: 'prefix_except_default',
    locales: [
      { code: 'en', language: 'en-US', name: 'English', file: 'en.json' },
      { code: 'fr', language: 'fr-FR', name: 'Français', file: 'fr.json' }
    ],
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root',
      fallbackLocale: 'en'
    },
    vueI18n: './i18n.config.ts'
  },

  icon: {
    localApiEndpoint: '/_nuxt_icon'
  },

  sitemap: {
    exclude: [
      '/profile',
      '/profile/**',
      '/fr/profile',
      '/fr/profile/**'
    ],
    zeroRuntime: true
  },

  // Restrict to Claude Code — default auto-detects and generates `.cursor/` etc.
  skillHub: {
    targets: ['claude-code']
  }
})
