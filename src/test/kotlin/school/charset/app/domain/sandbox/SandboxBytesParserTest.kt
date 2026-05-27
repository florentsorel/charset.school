package school.charset.app.domain.sandbox

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SandboxBytesParserTest :
    FunSpec({
        val parser = SandboxBytesParser()

        fun ByteArray.asUnsignedInts(): List<Int> = map { it.toInt() and 0xFF }

        test("parses contiguous hex without separator") {
            parser.parse("C3A9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
            parser.parse("F09F8E89").asUnsignedInts() shouldBe listOf(0xF0, 0x9F, 0x8E, 0x89)
        }

        test("parses hex pairs separated by spaces") {
            parser.parse("C3 A9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
            parser.parse("F0 9F 8E 89").asUnsignedInts() shouldBe listOf(0xF0, 0x9F, 0x8E, 0x89)
        }

        test("parses hex pairs separated by commas, dashes, semicolons") {
            parser.parse("C3,A9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
            parser.parse("C3-A9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
            parser.parse("C3;A9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
        }

        test("parses hex pairs with 0x / 0X prefix") {
            parser.parse("0xC3 0xA9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
            parser.parse("0XC3,0XA9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
        }

        test("accepts lower-case hex") {
            parser.parse("c3a9").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
        }

        test("trims surrounding whitespace") {
            parser.parse("   C3 A9   ").asUnsignedInts() shouldBe listOf(0xC3, 0xA9)
        }

        test("rejects empty input") {
            shouldThrow<SandboxBytesParseException> { parser.parse("") }.reason shouldBe "empty"
            shouldThrow<SandboxBytesParseException> { parser.parse("   ") }.reason shouldBe "empty"
            shouldThrow<SandboxBytesParseException> { parser.parse("0x") }.reason shouldBe "empty"
        }

        test("rejects non-hex characters") {
            shouldThrow<SandboxBytesParseException> { parser.parse("hello") }.reason shouldBe "invalid_hex"
            shouldThrow<SandboxBytesParseException> { parser.parse("ZZ") }.reason shouldBe "invalid_hex"
            shouldThrow<SandboxBytesParseException> { parser.parse("C3 GG") }.reason shouldBe "invalid_hex"
        }

        test("rejects odd-length hex (incomplete last byte)") {
            shouldThrow<SandboxBytesParseException> { parser.parse("C") }.reason shouldBe "odd_length"
            shouldThrow<SandboxBytesParseException> { parser.parse("C3A") }.reason shouldBe "odd_length"
            shouldThrow<SandboxBytesParseException> { parser.parse("C3 A") }.reason shouldBe "odd_length"
        }
    })
