package com.xingzhi.rfid.uhf.cultu

import com.xingzhi.rfid.Hex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * CULTU UHF EPC barcode codec, from 高校图书馆UHF-RFID技术联盟标准 第四版
 * (China University Library UHF-RFID Technology Union Standard, 4th edition),
 * 应用指南附录A.
 *
 * Used on UHF EPC Gen2 / ISO 18000-6C library tags. The algorithm is 1:1 with
 * `src/C/iRFID_96bit.c` (96-bit) and `src/C/iRFID_128bit.c` (128-bit)
 * big-endian layout.
 *
 * Bit layout (BE, `byte[0]` = MSB of the highest lane):
 *
 * 96-bit (8 bytes):
 * - 1–7 chars (Table A-2, high-aligned): `byte[7]` low nibble = length,
 *   `byte[7-N..6]` = reversed ASCII, rest = 0.
 * - 8–14 chars (Table A-1, long branch): `byte[0..3]` = high uint BE
 *   `[mid:30][d3_high:2]`, `byte[4..7]` = low uint BE
 *   `[v0:6][d3_low:2][v1:6][v2:6][check:8][len:4]`.
 *
 * 128-bit (12 bytes):
 * - 1–11 chars (Table A-4, high-aligned): `byte[11]` low nibble = length,
 *   `byte[11-N..10]` = reversed ASCII, rest = 0.
 * - 12–14 chars (Table A-3, long branch): `byte[0..3]` = high uint BE
 *   (barcode[0..4]), `byte[4..7]` = mid uint BE (barcode[5..9]),
 *   `byte[8..11]` = low uint BE `[barcode[10..N-2]][check:8][len:4]`.
 *
 * `encode` + `decode` are inverses.
 */
object EpcCodec {

    /**
     * Encode a 1–14 character barcode to 8 bytes (MSB-first, BE layout).
     *
     * Short branch (1–7 chars): length at `byte[7]` low nibble, reversed ASCII
     * at `byte[7-N..6]`. Long branch (8–14 chars): BE uint pair.
     */
    fun encode96(barcode: String?): ByteArray {
        if (barcode == null) throw IllegalArgumentException("barcode is null")
        val n = barcode.length
        if (n < 1 || n > 14) {
            throw IllegalArgumentException("barcode length must be 1-14, got $n")
        }
        val a = barcode.toByteArray(StandardCharsets.US_ASCII)
        val out = ByteArray(8)

        if (n < 8) {
            for (i in 0 until n) {
                out[6 - i] = a[i]
            }
            out[7] = n.toByte()
            return out
        }

        val check = a[n - 1].toInt() and 0xFF
        val d3 = (a[3].toInt() and 0xFF) - '0'.code
        if (d3 < 0 || d3 > 9) {
            throw IllegalArgumentException("barcode position 4 must be a digit")
        }
        val v0 = (a[0].toInt() and 0xFF) - '0'.code
        val v1 = (a[1].toInt() and 0xFF) - '0'.code
        val v2 = (a[2].toInt() and 0xFF) - '0'.code
        var mid = 0L
        for (i in 4 until n - 1) {
            val c = (a[i].toInt() and 0xFF) - '0'.code
            if (c < 0 || c > 9) {
                throw IllegalArgumentException("barcode position ${i + 1} must be a digit")
            }
            mid = mid * 10 + c
        }
        val hi = ((mid shl 2) or (((d3 and 0x0C) ushr 2).toLong())).toInt()
        val lo = ((d3 and 0x03) shl 30) or
            ((v0 and 0x3F) shl 24) or
            ((v1 and 0x3F) shl 18) or
            ((v2 and 0x3F) shl 12) or
            ((check and 0xFF) shl 4) or
            n
        ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN).putInt(0, hi).putInt(4, lo)
        return out
    }

    /**
     * Decode 8-byte EPC (BE) to the original barcode string.
     */
    fun decode96(epc: ByteArray?): String {
        if (epc == null || epc.size < 8) {
            throw IllegalArgumentException("EPC must be at least 8 bytes")
        }
        val len = epc[7].toInt() and 0x0F
        if (len < 1 || len > 14) {
            throw IllegalArgumentException("length nibble out of range: $len")
        }

        if (len < 8) {
            val chars = CharArray(len)
            for (i in 0 until len) {
                chars[i] = (epc[6 - i].toInt() and 0xFF).toChar()
            }
            return String(chars)
        }

        val bb = ByteBuffer.wrap(epc).order(ByteOrder.BIG_ENDIAN)
        val hi = bb.getInt(0)
        val lo = bb.getInt(4)

        val check = (lo ushr 4) and 0xFF
        val v2 = (lo ushr 12) and 0x3F
        val v1 = (lo ushr 18) and 0x3F
        val v0 = (lo ushr 24) and 0x3F
        val d3Low = (lo ushr 30) and 0x03
        val d3High = hi and 0x03
        val d3 = (d3High shl 2) or d3Low
        // hi must be shifted as an unsigned 32-bit value; sign-extending to Long
        // first would smear the sign bit into the top of mid for large values.
        val mid = (hi ushr 2).toLong() and 0xFFFFFFFFL

        val work = CharArray(len) { '0' }
        work[len - 1] = check.toChar()
        work[0] = (v0 + '0'.code).toChar()
        work[1] = (v1 + '0'.code).toChar()
        work[2] = (v2 + '0'.code).toChar()
        work[3] = (d3 + '0'.code).toChar()
        val midStr = mid.toString()
        val dlen = midStr.length
        for (i in 0 until dlen) {
            val dst = i + len - dlen - 1
            if (dst in 0 until len) {
                work[dst] = midStr[i]
            }
        }
        return String(work)
    }

    fun encode96Hex(barcode: String?): String = Hex.toHex(encode96(barcode))

    /**
     * Encode a 1–14 character barcode to 12 bytes (MSB-first, BE layout).
     *
     * Short branch (1–11 chars): length at `byte[11]` low nibble, reversed ASCII
     * at `byte[11-N..10]`. Long branch (12–14 chars): BE uint triple.
     */
    fun encode128(barcode: String?): ByteArray {
        if (barcode == null) throw IllegalArgumentException("barcode is null")
        val n = barcode.length
        if (n < 1 || n > 14) {
            throw IllegalArgumentException("barcode length must be 1-14, got $n")
        }
        val a = barcode.toByteArray(StandardCharsets.US_ASCII)
        val out = ByteArray(12)

        if (n < 12) {
            for (i in 0 until n) {
                out[10 - i] = a[i]
            }
            out[11] = n.toByte()
            return out
        }

        val checkVal = a[n - 1].toInt() and 0xFF
        var hi = 0L
        var mid = 0L
        var lo = 0L
        for (i in 0 until 5) {
            hi = (hi shl 6) or ((a[i].toInt() and 0xFF) - '0'.code).toLong()
        }
        for (i in 0 until 5) {
            mid = (mid shl 6) or ((a[5 + i].toInt() and 0xFF) - '0'.code).toLong()
        }
        for (i in 0 until n - 11) {
            lo = (lo shl 6) or ((a[10 + i].toInt() and 0xFF) - '0'.code).toLong()
        }
        lo = (lo shl 8) or checkVal.toLong()
        lo = (lo shl 4) or n.toLong()

        ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN)
            .putInt(0, hi.toInt())
            .putInt(4, mid.toInt())
            .putInt(8, lo.toInt())
        return out
    }

    /**
     * Decode 12-byte EPC (BE) to the original barcode string.
     */
    fun decode128(epc: ByteArray?): String {
        if (epc == null || epc.size < 12) {
            throw IllegalArgumentException("EPC must be at least 12 bytes")
        }
        val len = epc[11].toInt() and 0x0F
        if (len < 1 || len > 14) {
            throw IllegalArgumentException("length nibble out of range: $len")
        }

        if (len < 12) {
            val chars = CharArray(len)
            for (i in 0 until len) {
                chars[i] = (epc[10 - i].toInt() and 0xFF).toChar()
            }
            return String(chars)
        }

        val bb = ByteBuffer.wrap(epc).order(ByteOrder.BIG_ENDIAN)
        var hiInt = bb.getInt(0)
        var midInt = bb.getInt(4)
        var loInt = bb.getInt(8)

        val check = (loInt ushr 4) and 0xFF
        loInt = loInt ushr 12
        val work = CharArray(len) { '0' }
        work[len - 1] = check.toChar()
        for (i in (len - 2) downTo 10) {
            work[i] = ((loInt and 0x3F) + '0'.code).toChar()
            loInt = loInt ushr 6
        }
        for (i in 9 downTo 5) {
            work[i] = ((midInt and 0x3F) + '0'.code).toChar()
            midInt = midInt ushr 6
        }
        for (i in 4 downTo 0) {
            work[i] = ((hiInt and 0x3F) + '0'.code).toChar()
            hiInt = hiInt ushr 6
        }
        return String(work)
    }

    fun encode128Hex(barcode: String?): String = Hex.toHex(encode128(barcode))
}
