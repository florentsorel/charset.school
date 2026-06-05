// Client-side cell UX for the exercise step widgets. The inputs are
// uncontrolled (their containers are phx-update="ignore"); the server only
// reads the values on form submit.

// Bit cells: one character per cell (0/1 only), auto-advance, arrow
// navigation, ArrowUp/Down as 1/0, backspace walking. Works across every
// .bit-input inside the hook root in DOM order, so a multi-group widget
// (bit-groups) flows as one sequence - filling the last bit of a group
// jumps to the next group.
const BitCells = {
  mounted() {
    this.el.addEventListener("input", (event) => {
      const cell = event.target.closest("input.bit-input")
      if (!cell) return
      const last = cell.value.slice(-1)
      cell.value = last === "0" || last === "1" ? last : ""
      if (cell.value) this.focusStep(cell, 1)
    })

    this.el.addEventListener("keydown", (event) => {
      const cell = event.target.closest("input.bit-input")
      if (!cell) return

      if (event.key === "Backspace") {
        event.preventDefault()
        if (cell.value) {
          cell.value = ""
        } else {
          const prev = this.sibling(cell, -1)
          if (prev) {
            prev.focus()
            prev.value = ""
          }
        }
      } else if (event.key === "ArrowLeft") {
        event.preventDefault()
        this.focusStep(cell, -1)
      } else if (event.key === "ArrowRight") {
        event.preventDefault()
        this.focusStep(cell, 1)
      } else if (event.key === "ArrowUp" || event.key === "ArrowDown") {
        event.preventDefault()
        cell.value = event.key === "ArrowUp" ? "1" : "0"
        this.focusStep(cell, 1)
      }
    })
  },

  cells() {
    return [...this.el.querySelectorAll("input.bit-input:not([disabled])")]
  },

  sibling(cell, delta) {
    const cells = this.cells()
    const index = cells.indexOf(cell)
    return cells[index + delta]
  },

  focusStep(cell, delta) {
    this.sibling(cell, delta)?.focus()
  },
}

// Hex byte cells: two hex chars per cell, uppercased, auto-advance once full,
// backspace walking when empty.
const HexCells = {
  mounted() {
    this.el.addEventListener("input", (event) => {
      const cell = event.target.closest("input.hex-cell")
      if (!cell) return
      cell.value = cell.value.replace(/[^0-9a-fA-F]/g, "").toUpperCase().slice(0, 2)
      if (cell.value.length === 2) this.focusStep(cell, 1)
    })

    this.el.addEventListener("keydown", (event) => {
      const cell = event.target.closest("input.hex-cell")
      if (!cell) return

      if (event.key === "Backspace" && !cell.value) {
        event.preventDefault()
        this.focusStep(cell, -1)
      } else if (event.key === "ArrowLeft" && cell.selectionStart === 0) {
        event.preventDefault()
        this.focusStep(cell, -1)
      } else if (event.key === "ArrowRight" && cell.selectionStart === cell.value.length) {
        event.preventDefault()
        this.focusStep(cell, 1)
      }
    })
  },

  focusStep(cell, delta) {
    const cells = [...this.el.querySelectorAll("input.hex-cell:not([disabled])")]
    const target = cells[cells.indexOf(cell) + delta]
    if (target) {
      target.focus()
      target.setSelectionRange(target.value.length, target.value.length)
    }
  },
}

// Single-value filtered inputs: data-filter="hex" keeps hex digits
// (uppercased, optional 0x/U+ prefix stripped), data-filter="digits" keeps
// decimal digits.
const FilteredInput = {
  mounted() {
    this.el.addEventListener("input", () => {
      if (this.el.dataset.filter === "hex") {
        this.el.value = this.el.value
          .replace(/^(0x|U\+)/i, "")
          .replace(/[^0-9a-fA-F]/g, "")
          .toUpperCase()
      } else if (this.el.dataset.filter === "digits") {
        this.el.value = this.el.value.replace(/[^0-9]/g, "")
      }
    })
  },
}

export default { BitCells, HexCells, FilteredInput }
