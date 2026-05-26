package school.charset.app.domain.sandbox

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.CodePoint

class SandboxInputParserTest :
    FunSpec({
        val parser = SandboxInputParser()

        test("parses U+XXXX form") {
            parser.parse("U+00E9") shouldBe CodePoint(0xE9)
            parser.parse("u+1f389") shouldBe CodePoint(0x1F389)
        }

        test("parses 0xXX form") {
            parser.parse("0xE9") shouldBe CodePoint(0xE9)
            parser.parse("0X1F389") shouldBe CodePoint(0x1F389)
        }

        test("parses decimal") {
            parser.parse("233") shouldBe CodePoint(0xE9)
            parser.parse("65") shouldBe CodePoint(0x41)
        }

        test("parses a single character via its code point") {
            parser.parse("é") shouldBe CodePoint(0xE9)
            parser.parse("A") shouldBe CodePoint(0x41)
            parser.parse("🎉") shouldBe CodePoint(0x1F389)
        }

        test("trims whitespace") {
            parser.parse("  U+00E9  ") shouldBe CodePoint(0xE9)
        }

        test("rejects empty input") {
            shouldThrow<SandboxParseException> { parser.parse("") }.reason shouldBe "empty"
            shouldThrow<SandboxParseException> { parser.parse("   ") }.reason shouldBe "empty"
        }

        test("rejects unparseable input") {
            shouldThrow<SandboxParseException> { parser.parse("abc") }.reason shouldBe "unparseable"
            shouldThrow<SandboxParseException> { parser.parse("U+ZZZZ") }.reason shouldBe "unparseable"
            shouldThrow<SandboxParseException> { parser.parse("AB") }.reason shouldBe "unparseable"
        }

        test("rejects out-of-range code points") {
            shouldThrow<SandboxParseException> { parser.parse("U+110000") }.reason shouldBe "out_of_range"
            shouldThrow<SandboxParseException> { parser.parse("1114112") }.reason shouldBe "out_of_range"
        }

        test("rejects surrogate code points") {
            shouldThrow<SandboxParseException> { parser.parse("U+D800") }.reason shouldBe "surrogate"
            shouldThrow<SandboxParseException> { parser.parse("U+DFFF") }.reason shouldBe "surrogate"
            shouldThrow<SandboxParseException> { parser.parse("55296") }.reason shouldBe "surrogate"
        }
    })
