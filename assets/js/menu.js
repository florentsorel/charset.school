// Header menus (desktop exercises mega-menu, mobile burger, its sub-lists).
//
// Panels are plain `[data-menu]` containers shown/hidden via the `hidden`
// attribute; `[data-menu-toggle="<id>"]` buttons flip their target. Menus
// close on backdrop click (`[data-menu-close]`), Escape, outside click, and
// link navigation - same behavior as the old Vue header.

function closeAll() {
  document.querySelectorAll("[data-menu]:not([hidden])").forEach((el) => {
    el.hidden = true
  })
  document.querySelectorAll("[data-menu-toggle]").forEach((btn) => {
    btn.setAttribute("aria-expanded", "false")
  })
}

document.addEventListener("click", (event) => {
  const toggle = event.target.closest("[data-menu-toggle]")
  if (toggle) {
    const target = document.getElementById(toggle.dataset.menuToggle)
    if (target) {
      const open = target.hidden
      target.hidden = !open
      toggle.setAttribute("aria-expanded", String(open))
    }
    return
  }

  if (event.target.closest("[data-menu] a") || !event.target.closest("[data-menu]")) {
    closeAll()
  }
})

window.addEventListener("keydown", (event) => {
  if (event.key === "Escape") closeAll()
})
