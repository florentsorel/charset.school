package school.charset.app.infrastructure.http.sandbox

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CodePointLabelsTest :
    FunSpec({
        test("returns C0 mnemonics for 0x00..0x1F") {
            CodePointLabels.lookup(0x00) shouldBe "NUL"
            CodePointLabels.lookup(0x07) shouldBe "BEL"
            CodePointLabels.lookup(0x0A) shouldBe "LF"
            CodePointLabels.lookup(0x0D) shouldBe "CR"
            CodePointLabels.lookup(0x0F) shouldBe "SI"
            CodePointLabels.lookup(0x1B) shouldBe "ESC"
            CodePointLabels.lookup(0x1F) shouldBe "US"
        }

        test("returns SPACE for 0x20 (would otherwise render invisibly)") {
            CodePointLabels.lookup(0x20) shouldBe "SPACE"
        }

        test("returns DEL for 0x7F") {
            CodePointLabels.lookup(0x7F) shouldBe "DEL"
        }

        test("returns C1 mnemonics for 0x80..0x9F") {
            CodePointLabels.lookup(0x80) shouldBe "PAD"
            CodePointLabels.lookup(0x85) shouldBe "NEL"
            CodePointLabels.lookup(0x9B) shouldBe "CSI"
            CodePointLabels.lookup(0x9F) shouldBe "APC"
        }

        test("returns null for printable ASCII other than space") {
            CodePointLabels.lookup(0x21) shouldBe null // '!'
            CodePointLabels.lookup(0x41) shouldBe null // 'A'
            CodePointLabels.lookup(0x7E) shouldBe null // '~'
        }

        test("returns named short labels for common format/invisible chars") {
            CodePointLabels.lookup(0x00A0) shouldBe "NBSP"
            CodePointLabels.lookup(0x00AD) shouldBe "SHY"
            CodePointLabels.lookup(0x200B) shouldBe "ZWSP"
            CodePointLabels.lookup(0x200D) shouldBe "ZWJ"
            CodePointLabels.lookup(0x200E) shouldBe "LRM"
            CodePointLabels.lookup(0x2028) shouldBe "LSEP"
            CodePointLabels.lookup(0x2029) shouldBe "PSEP"
            CodePointLabels.lookup(0x2060) shouldBe "WJ"
            CodePointLabels.lookup(0xFEFF) shouldBe "BOM"
        }

        test("returns PUA for Private Use Area code points") {
            CodePointLabels.lookup(0xE000) shouldBe "PUA"
            CodePointLabels.lookup(0xF389) shouldBe "PUA"
            CodePointLabels.lookup(0xF8FF) shouldBe "PUA"
            CodePointLabels.lookup(0xF0000) shouldBe "PUA"
            CodePointLabels.lookup(0x100000) shouldBe "PUA"
        }

        test("returns NONCHAR for Unicode non-characters") {
            CodePointLabels.lookup(0xFDD0) shouldBe "NONCHAR"
            CodePointLabels.lookup(0xFDEF) shouldBe "NONCHAR"
            CodePointLabels.lookup(0xFFFE) shouldBe "NONCHAR"
            CodePointLabels.lookup(0xFFFF) shouldBe "NONCHAR"
            CodePointLabels.lookup(0x1FFFE) shouldBe "NONCHAR"
            CodePointLabels.lookup(0x10FFFF) shouldBe "NONCHAR"
        }

        test("returns COMBINING for combining marks (would render on dotted circle in isolation)") {
            CodePointLabels.lookup(0x0301) shouldBe "COMBINING" // combining acute accent
            CodePointLabels.lookup(0x0308) shouldBe "COMBINING" // combining diaeresis
        }

        test("returns WHITESPACE for non-ASCII space separators") {
            CodePointLabels.lookup(0x2003) shouldBe "WHITESPACE" // em space
            CodePointLabels.lookup(0x202F) shouldBe "WHITESPACE" // narrow no-break space
        }

        test("returns null for printable letters and symbols above C1") {
            CodePointLabels.lookup(0xE9) shouldBe null // 'é'
            CodePointLabels.lookup(0x4E2D) shouldBe null // '中'
            CodePointLabels.lookup(0x1F389) shouldBe null // '🎉'
        }
    })
