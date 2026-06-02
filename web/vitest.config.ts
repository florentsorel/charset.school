import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'happy-dom',
    include: ['app/**/*.{test,spec}.ts'],
    // No frontend unit tests remain after the auth removal; don't fail CI on an
    // empty suite (page/composable logic is covered by typecheck + build).
    passWithNoTests: true,
    globals: true
  },
  resolve: {
    alias: {
      '~': fileURLToPath(new URL('./app', import.meta.url)),
      '@': fileURLToPath(new URL('./app', import.meta.url))
    }
  }
})
