export type SandboxModule = {
  id: string
  to: string
  catalogKey: string
  available: boolean
}

export type SandboxModuleGroup = {
  label: string
  modules: SandboxModule[]
}

export function useSandboxModules() {
  const { t } = useI18n()
  const localePath = useLocalePath()
  const route = useRoute()

  const encodeModules: SandboxModule[] = [
    { id: 'utf-8', to: '/sandbox/encode/utf-8', catalogKey: 'utf8-encode', available: true },
    { id: 'utf-16', to: '/sandbox/encode/utf-16', catalogKey: 'utf16-encode', available: true },
    { id: 'utf-32', to: '/sandbox/encode/utf-32', catalogKey: 'utf32-encode', available: true },
    { id: 'latin1', to: '/sandbox/encode/latin1', catalogKey: 'latin1-encode', available: false },
    { id: 'windows-1252', to: '/sandbox/encode/windows-1252', catalogKey: 'windows1252-encode', available: true }
  ]

  const decodeModules: SandboxModule[] = [
    { id: 'utf-8', to: '/sandbox/decode/utf-8', catalogKey: 'utf8-decode', available: true },
    { id: 'utf-16', to: '/sandbox/decode/utf-16', catalogKey: 'utf16-decode', available: true },
    { id: 'utf-32', to: '/sandbox/decode/utf-32', catalogKey: 'utf32-decode', available: true },
    { id: 'latin1', to: '/sandbox/decode/latin1', catalogKey: 'latin1-decode', available: false },
    { id: 'windows-1252', to: '/sandbox/decode/windows-1252', catalogKey: 'windows1252-decode', available: true }
  ]

  const otherModules: SandboxModule[] = [
    { id: 'identify', to: '/sandbox/identify', catalogKey: 'identify', available: false },
    { id: 'mojibake', to: '/sandbox/mojibake', catalogKey: 'mojibake', available: false }
  ]

  const groups = computed<SandboxModuleGroup[]>(() => [
    { label: t('sandbox.landing.section_encode'), modules: encodeModules },
    { label: t('sandbox.landing.section_decode'), modules: decodeModules },
    { label: t('sandbox.landing.section_other'), modules: otherModules }
  ])

  function isActive(to: string): boolean {
    return route.path === localePath(to)
  }

  const activeModule = computed<SandboxModule | undefined>(() =>
    [...encodeModules, ...decodeModules, ...otherModules].find(m => isActive(m.to))
  )

  return { groups, isActive, activeModule }
}
