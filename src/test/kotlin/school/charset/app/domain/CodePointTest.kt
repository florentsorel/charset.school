package school.charset.app.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CodePointTest :
    FreeSpec({

        "init" - {
            "rejects negative values" { shouldThrow<IllegalArgumentException> { CodePoint(-1) } }
            "rejects values above U+10FFFF" { shouldThrow<IllegalArgumentException> { CodePoint(0x110000) } }
        }

        "isBmp" - {
            "true for U+0000 (low boundary)" { CodePoint(0x0000).isBmp shouldBe true }
            "true for U+D800 (surrogates are in the BMP)" { CodePoint(0xD800).isBmp shouldBe true }
            "true for U+FFFF (high boundary)" { CodePoint(0xFFFF).isBmp shouldBe true }
            "false for U+10000 (first code point above the BMP)" { CodePoint(0x10000).isBmp shouldBe false }
            "false for U+10FFFF (max code point)" { CodePoint(0x10FFFF).isBmp shouldBe false }
        }

        "isSurrogate" - {
            "false for U+D7FF (just before the range)" { CodePoint(0xD7FF).isSurrogate shouldBe false }
            "true for U+D800 (range start)" { CodePoint(0xD800).isSurrogate shouldBe true }
            "true for U+DFFF (range end)" { CodePoint(0xDFFF).isSurrogate shouldBe true }
            "false for U+E000 (just after the range)" { CodePoint(0xE000).isSurrogate shouldBe false }
        }
    })
