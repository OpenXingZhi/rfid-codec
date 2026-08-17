package com.xingzhi.rfid.uhf.cultu

import com.xingzhi.rfid.Hex
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EpcCodecTest {

    @ParameterizedTest(name = "96-bit {0}")
    @MethodSource("cases")
    fun encode96MatchesCReferenceAndRoundTrips(
        barcode: String,
        expected96: String,
        @Suppress("UNUSED_PARAMETER") expected128: String,
    ) {
        assertEquals(expected96.uppercase(), EpcCodec.encode96Hex(barcode).uppercase())
        assertEquals(barcode, EpcCodec.decode96(EpcCodec.encode96(barcode)))
        assertEquals(barcode, EpcCodec.decode96(Hex.hexToBytes(expected96)))
    }

    @ParameterizedTest(name = "128-bit {0}")
    @MethodSource("cases")
    fun encode128MatchesCReferenceAndRoundTrips(barcode: String, @Suppress("UNUSED_PARAMETER") expected96: String, expected128: String) {
        assertEquals(expected128.uppercase(), EpcCodec.encode128Hex(barcode).uppercase())
        assertEquals(barcode, EpcCodec.decode128(EpcCodec.encode128(barcode)))
        assertEquals(barcode, EpcCodec.decode128(Hex.hexToBytes(expected128)))
    }

    @Test
    fun encode96RejectsNull() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.encode96(null) }
    }

    @Test
    fun encode96RejectsEmpty() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.encode96("") }
    }

    @Test
    fun encode96RejectsLength15() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.encode96("ABC0123456789_X") }
    }

    @Test
    fun encode96RejectsNonDigitAtPosition4() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.encode96("ABCA1234*") }
    }

    @Test
    fun encode96RejectsNonDigitInLongMid() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.encode96("ABC0A234*") }
    }

    @Test
    fun decode96RejectsNullAndShort() {
        assertFailsWith<IllegalArgumentException> { EpcCodec.decode96(null) }
        assertFailsWith<IllegalArgumentException> { EpcCodec.decode96(ByteArray(7)) }
    }

    /**
     * mid = 999999999 pushes the 96-bit high lane above Int.MAX_VALUE,
     * exercising unsigned 32-bit handling in decode96. Not covered by the
     * C reference vectors.
     */
    @Test
    fun roundTripsWhenHighLaneSignBitIsSet() {
        val barcode = "ABC0999999999X"
        assertEquals(barcode, EpcCodec.decode96(EpcCodec.encode96(barcode)))
        assertEquals(barcode, EpcCodec.decode128(EpcCodec.encode128(barcode)))
    }

    /** The C reference vectors only use '0' at position 4; cover the d3 split across lanes. */
    @Test
    fun roundTripsWithNonZeroDigitAtPosition4() {
        val barcode = "ABC9123456789X"
        assertEquals(barcode, EpcCodec.decode96(EpcCodec.encode96(barcode)))
        assertEquals(barcode, EpcCodec.decode128(EpcCodec.encode128(barcode)))
    }

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
