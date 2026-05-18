package school.charset.app.domain.encoding

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class EncodingTest :
    FreeSpec({

        "fromId" - {
            "returns the correct encoding for valid IDs" {
                Encoding.fromId("ascii") shouldBe Encoding.Ascii
                Encoding.fromId("latin1") shouldBe Encoding.Latin1
                Encoding.fromId("windows-1252") shouldBe Encoding.Windows1252
                Encoding.fromId("utf-8") shouldBe Encoding.Utf8
                Encoding.fromId("utf-16be") shouldBe Encoding.Utf16Be
                Encoding.fromId("utf-16le") shouldBe Encoding.Utf16Le
                Encoding.fromId("utf-32be") shouldBe Encoding.Utf32Be
                Encoding.fromId("utf-32le") shouldBe Encoding.Utf32Le
            }

            "returns null for invalid IDs" {
                Encoding.fromId("invalid-encoding") shouldBe null
                Encoding.fromId("") shouldBe null
            }
        }
    })
