<script setup lang="ts">
const { t, d } = useI18n()
const { user, fetchMe } = useAuth()

const pending = ref(true)
const errorOccurred = ref(false)

async function load() {
  pending.value = true
  errorOccurred.value = false
  try {
    await fetchMe()
  } catch {
    errorOccurred.value = true
  } finally {
    pending.value = false
  }
}

onMounted(load)

useHead({
  title: () => `${t('auth.me_title')} · ${t('common.app_name')}`
})
</script>

<template>
  <main class="mx-auto max-w-2xl p-8">
    <h1 class="text-2xl font-semibold mb-6">
      {{ t('auth.me_title') }}
    </h1>

    <div v-if="pending">
      {{ t('common.loading') }}
    </div>

    <div
      v-else-if="errorOccurred"
      class="space-y-2"
    >
      <p>{{ t('auth.errors.unknown') }}</p>
      <UButton
        variant="outline"
        @click="load"
      >
        {{ t('common.retry') }}
      </UButton>
    </div>

    <div v-else-if="!user">
      {{ t('auth.not_authenticated') }}
    </div>

    <dl
      v-else
      class="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2"
    >
      <dt class="font-medium text-gray-500">
        {{ t('auth.email') }}
      </dt>
      <dd>{{ user.email }}</dd>

      <dt class="font-medium text-gray-500">
        {{ t('auth.name') }}
      </dt>
      <dd>{{ user.name }}</dd>

      <dt class="font-medium text-gray-500">
        {{ t('auth.locale') }}
      </dt>
      <dd>{{ user.locale.toUpperCase() }}</dd>

      <dt class="font-medium text-gray-500 col-span-2 mt-4 text-sm">
        {{ t('auth.member_since', { date: d(new Date(user.createdAt), 'long') }) }}
      </dt>
    </dl>
  </main>
</template>
