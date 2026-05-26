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

        test("returns null for code points above the C1 range") {
            CodePointLabels.lookup(0xA0) shouldBe null // NBSP
            CodePointLabels.lookup(0xE9) shouldBe null // 'é'
            CodePointLabels.lookup(0x1F389) shouldBe null // '🎉'
        }
    })
