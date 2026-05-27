package school.charset.app.domain.sandbox

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import school.charset.app.domain.encoding.Encoding

class SandboxEndianParserTest :
    FunSpec({
        val parser = SandboxEndianParser()

        test("parses 'big' as BigEndian") {
            parser.parse("big") shouldBe Encoding.Endian.BigEndian
            parser.parse("BIG") shouldBe Encoding.Endian.BigEndian
            parser.parse("  Big  ") shouldBe Encoding.Endian.BigEndian
        }

        test("parses 'little' as LittleEndian") {
            parser.parse("little") shouldBe Encoding.Endian.LittleEndian
            parser.parse("LITTLE") shouldBe Encoding.Endian.LittleEndian
            parser.parse("Little") shouldBe Encoding.Endian.LittleEndian
        }

        test("rejects unknown values") {
            shouldThrow<SandboxEndianParseException> { parser.parse("be") }.reason shouldBe "invalid"
            shouldThrow<SandboxEndianParseException> { parser.parse("le") }.reason shouldBe "invalid"
            shouldThrow<SandboxEndianParseException> { parser.parse("") }.reason shouldBe "invalid"
            shouldThrow<SandboxEndianParseException> { parser.parse("garbage") }.reason shouldBe "invalid"
        }
    })
