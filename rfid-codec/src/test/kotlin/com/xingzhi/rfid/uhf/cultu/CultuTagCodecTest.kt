package com.xingzhi.rfid.uhf.cultu

import com.xingzhi.rfid.Hex
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CultuTagCodecTest {

    @ParameterizedTest(name = "full 96-bit {0}")
    @MethodSource("cases")
    fun decode96WrapsCReferencePayload(
        barcode: String,
        expected96: String,
        @Suppress("UNUSED_PARAMETER") expected128: String,
    ) {
        val epc = Hex.hexToBytes("00000000$expected96")
        val tag = CultuTagCodec.decode(epc)
        assertFalse(tag.security)
        assertEquals(0, tag.reserved)
        assertEquals(0, tag.sorting)
        assertEquals(0, tag.codingType)
        assertEquals(0, tag.modelVersion)
        assertEquals(0, tag.contentIndex)
        assertEquals(barcode, tag.itemIdentifier)
        assertEquals(epc.toList(), CultuTagCodec.encode(tag).toList())
    }

    @ParameterizedTest(name = "full 128-bit {0}")
    @MethodSource("cases")
    fun decode128WrapsCReferencePayload(
        barcode: String,
        @Suppress("UNUSED_PARAMETER") expected96: String,
        expected128: String,
    ) {
        val epc = Hex.hexToBytes("00400000$expected128")
        val tag = CultuTagCodec.decode(epc)
        assertFalse(tag.security)
        assertEquals(0, tag.reserved)
        assertEquals(0, tag.sorting)
        assertEquals(1, tag.codingType)
        assertEquals(0, tag.modelVersion)
        assertEquals(0, tag.contentIndex)
        assertEquals(barcode, tag.itemIdentifier)
        assertEquals(epc.toList(), CultuTagCodec.encode(tag).toList())
    }

    @Test
    fun decodePreservesNonzeroHeaderFields() {
        val payload = Hex.hexToBytes("1d6f3454114935fe")
        // security=1, reserved=2, sorting=17 → 0b11010001
        // codingType=0, modelVersion=4 → 0x04
        // contentIndex = 0x00A1 (OID 3/12/15 example from the guide)
        val epc = byteArrayOf(0xD1.toByte(), 0x04, 0x00, 0xA1.toByte()) + payload
        val tag = CultuTagCodec.decode(epc)
        assertTrue(tag.security)
        assertEquals(2, tag.reserved)
        assertEquals(17, tag.sorting)
        assertEquals(0, tag.codingType)
        assertEquals(4, tag.modelVersion)
        assertEquals(0x00A1, tag.contentIndex)
        assertEquals("ABC0123456789_", tag.itemIdentifier)
        assertEquals(epc.toList(), CultuTagCodec.encode(tag).toList())
    }

    @Test
    fun decode144RawIdentifierTrimsRightNulPadding() {
        val identifier = "Z20120001%"
        val payload = identifier.toByteArray(Charsets.ISO_8859_1) + ByteArray(4)
        val epc = byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00) + payload
        val tag = CultuTagCodec.decode(epc)
        assertEquals(2, tag.codingType)
        assertEquals(identifier, tag.itemIdentifier)
        assertEquals(18, epc.size)
        assertEquals(epc.toList(), CultuTagCodec.encode(tag).toList())
    }

    @Test
    fun decode144KeepsFourteenCharacterIdentifier() {
        val identifier = "ABCDEFGHIJKLMN"
        val epc = byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00) +
            identifier.toByteArray(Charsets.US_ASCII)
        val tag = CultuTagCodec.decode(epc)
        assertEquals(identifier, tag.itemIdentifier)
        assertEquals(epc.toList(), CultuTagCodec.encode(tag).toList())
    }

    @Test
    fun decode144AcceptsLargerThanEighteenByteEpc() {
        val identifier = "HELLO"
        val payload = identifier.toByteArray(Charsets.US_ASCII) + ByteArray(9)
        val extra = byteArrayOf(0x11, 0x22)
        val epc = byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00) + payload + extra
        val tag = CultuTagCodec.decode(epc)
        assertEquals(identifier, tag.itemIdentifier)
        assertEquals(20, epc.size)
    }

    @Test
    fun decode144DoesNotTrimEmbeddedNul() {
        val payload = byteArrayOf(0x41, 0x00, 0x42, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val epc = byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00) + payload
        val tag = CultuTagCodec.decode(epc)
        assertEquals("A\u0000B", tag.itemIdentifier)
    }

    @Test
    fun encode144RoundTripsEmbeddedNul() {
        val tag = sample(codingType = 2, itemIdentifier = "A\u0000B")
        val epc = CultuTagCodec.encode(tag)
        assertEquals(18, epc.size)
        assertEquals(
            listOf<Byte>(0x41, 0x00, 0x42, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            epc.copyOfRange(4, 18).toList(),
        )
        val decoded = CultuTagCodec.decode(epc)
        assertEquals("A\u0000B", decoded.itemIdentifier)
        assertEquals(2, decoded.codingType)
        assertEquals(epc.toList(), CultuTagCodec.encode(decoded).toList())
    }

    @Test
    fun encode144RejectsTrailingNul() {
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(codingType = 2, itemIdentifier = "AB\u0000"))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(codingType = 2, itemIdentifier = "\u0000"))
        }
    }

    @Test
    fun decode144RejectsHighBitIso646Byte() {
        val payload = ByteArray(14).also {
            it[0] = 0x41
            it[1] = 0x80.toByte()
        }
        val epc = byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00) + payload
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(epc) }
    }

    @Test
    fun decodeRejectsMalformedLengths() {
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(null) }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(ByteArray(0)) }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(ByteArray(3)) }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(ByteArray(11)) }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(ByteArray(13)) }

        val type1Short = ByteArray(15).also { it[1] = 0x40 }
        val type1Long = ByteArray(17).also { it[1] = 0x40 }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(type1Short) }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(type1Long) }

        val type2Short = ByteArray(17).also { it[1] = 0x80.toByte() }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(type2Short) }
    }

    @Test
    fun decodeRejectsReservedCodingType3() {
        val epc = ByteArray(12).also { it[1] = 0xC0.toByte() }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(epc) }
        val longEpc = ByteArray(18).also { it[1] = 0xC0.toByte() }
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(longEpc) }
    }

    @Test
    fun encodeRejectsReservedAndOutOfRangeFields() {
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(codingType = 3))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(reserved = 4))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(sorting = 32))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(modelVersion = 64))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(contentIndex = 0x10000))
        }
        assertFailsWith<CultuFormatException> {
            CultuTagCodec.encode(sample(codingType = 2, itemIdentifier = "ABCDEFGHIJKLMNO"))
        }
    }

    @Test
    fun decodeWrapsMalformedPayloadAsCultuFormatException() {
        val epc = ByteArray(12) // type 0, length nibble 0 is out of range
        assertFailsWith<CultuFormatException> { CultuTagCodec.decode(epc) }
    }

    private fun sample(
        security: Boolean = false,
        reserved: Int = 0,
        sorting: Int = 0,
        codingType: Int = 0,
        modelVersion: Int = 0,
        contentIndex: Int = 0,
        itemIdentifier: String = "A",
    ) = CultuTag(
        security = security,
        reserved = reserved,
        sorting = sorting,
        codingType = codingType,
        modelVersion = modelVersion,
        contentIndex = contentIndex,
        itemIdentifier = itemIdentifier,
    )

    companion object {
        private val CASES = arrayOf(
            "A",
            "AB",
            "ABC",
            "ABCD",
            "ABCDE",
            "ABCDEF",
            "ABCDEFG",
            "ABC01234*",
            "ABC012345?",
            "ABC0123456~",
            "ABC01234567z",
            "ABC012345678*",
            "ABC0123456789?",
            "ABC0123456789_",
        )

        private val C96_HEX = arrayOf(
            "0000000000004101",
            "0000000000424102",
            "0000000043424103",
            "0000004443424104",
            "0000454443424105",
            "0046454443424106",
            "4746454443424107",
            "00001348114932a9",
            "0000c0e4114933fa",
            "00078900114937eb",
            "004b5a1c114937ac",
            "02f18538114932ad",
            "1d6f3454114933fe",
            "1d6f3454114935fe",
        )

        private val C128_HEX = arrayOf(
            "000000000000000000004101",
            "000000000000000000424102",
            "000000000000000043424103",
            "000000000000004443424104",
            "000000000000454443424105",
            "000000000046454443424106",
            "000000004746454443424107",
            "00002a343332313043424109",
            "003f3534333231304342410a",
            "7e363534333231304342410b",
            "11493001020c4146000077ac",
            "11493001020c4146001c82ad",
            "11493001020c4146072093fe",
            "11493001020c4146072095fe",
        )

        @JvmStatic
        fun cases(): List<Arguments> = CASES.indices.map { i ->
            Arguments.of(CASES[i], C96_HEX[i], C128_HEX[i])
        }
    }
}
