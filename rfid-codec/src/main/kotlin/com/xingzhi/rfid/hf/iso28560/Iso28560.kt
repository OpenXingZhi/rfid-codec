package com.xingzhi.rfid.hf.iso28560

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * ISO 28560-2 decoder for HF ISO 15693 library-tag user memory.
 *
 * Parses compact data objects (relative OID, encoding, optional offset padding)
 * and decodes payloads including Annex F ISIL, 5/6/7-bit compact character
 * sets, numeric strings, and integers. OID 2 (content parameter) and OID 5
 * (type of usage) use the library-tag special cases from existing XingZhi
 * decoders.
 */
object Iso28560 {

    private const val ENC_APP = 0b000
    private const val ENC_INTEGER = 0b001
    private const val ENC_NUMERIC = 0b010
    private const val ENC_5BIT = 0b011
    private const val ENC_6BIT = 0b100
    private const val ENC_7BIT = 0b101
    private const val ENC_8BIT = 0b110
    private const val ENC_UTF8 = 0b111

    fun decode(userMemory: ByteArray?): Iso28560Tag {
        return try {
            Iso28560TagBuilder().apply {
                for (element in parseElements(requireMemory(userMemory))) {
                    apply(element)
                }
            }.build()
        } catch (e: Iso28560FormatException) {
            throw e
        } catch (e: RuntimeException) {
            throw Iso28560FormatException("malformed ISO 28560-2 user memory", e)
        }
    }

    fun decodePrimaryItemIdentifier(userMemory: ByteArray?): String? {
        return try {
            parseElements(requireMemory(userMemory))
                .firstOrNull { it.oid == 1 }
                ?.let { decodeDataBlock(it.encoding, it.data) }
        } catch (e: Iso28560FormatException) {
            throw e
        } catch (e: RuntimeException) {
            throw Iso28560FormatException("malformed ISO 28560-2 user memory", e)
        }
    }

    private fun requireMemory(userMemory: ByteArray?): ByteArray {
        if (userMemory == null || userMemory.isEmpty()) {
            throw Iso28560FormatException("user memory is empty")
        }
        return userMemory
    }

    private data class DataElement(val oid: Int, val encoding: Int, val data: ByteArray)

    private fun parseElements(userMemory: ByteArray): Sequence<DataElement> = sequence {
        val buffer = ByteBuffer.wrap(userMemory)
        while (buffer.hasRemaining()) {
            val flag = buffer.get().toInt() and 0xFF
            val isShifted = (flag and 0x80) != 0
            val encoding = (flag ushr 4) and 0x07
            var oid = flag and 0x0F
            if (oid == 0 && !isShifted) {
                return@sequence
            }
            if (oid == 15) {
                need(buffer, 1, "extended OID")
                oid += buffer.get().toInt() and 0xFF
            }
            val shiftLength = if (isShifted) {
                need(buffer, 1, "offset length")
                buffer.get().toInt() and 0xFF
            } else {
                0
            }
            need(buffer, 1, "data length")
            val dataLength = buffer.get().toInt() and 0xFF
            need(buffer, dataLength, "data block")
            val data = ByteArray(dataLength)
            buffer.get(data)
            val skip = minOf(shiftLength, buffer.remaining())
            buffer.position(buffer.position() + skip)
            yield(DataElement(oid, encoding, data))
        }
    }

    private fun need(buffer: ByteBuffer, n: Int, what: String) {
        if (buffer.remaining() < n) {
            throw Iso28560FormatException("truncated $what")
        }
    }

    private class Iso28560TagBuilder {
        var primaryItemIdentifier: String? = null
        var contentParameter: String? = null
        var ownerInstitution: String? = null
        var setInformation: String? = null
        var partsInSet: Int? = null
        var partNumber: Int? = null
        var typeOfUsage: String? = null
        var shelfLocation: String? = null
        var onixMediaFormat: String? = null
        var marcMediaFormat: String? = null
        var supplierIdentifier: String? = null
        var orderNumber: String? = null
        var illBorrowingInstitution: String? = null
        var illBorrowingTransactionNumber: String? = null
        var gs1ProductIdentifier: String? = null
        var alternativeUniqueItemIdentifier: String? = null
        var localDataA: String? = null
        var localDataB: String? = null
        var title: String? = null
        var productIdentifierLocal: String? = null
        var mediaFormatOther: String? = null
        var supplyChainStage: String? = null
        var supplierInvoiceNumber: String? = null
        var alternativeItemIdentifier: String? = null
        var alternativeOwnerInstitution: String? = null
        var subsidiaryOfOwnerInstitution: String? = null
        var alternativeIllBorrowingInstitution: String? = null
        var localDataC: String? = null

        fun apply(element: DataElement) {
            val value = decodeElement(element)
            when (element.oid) {
                1 -> primaryItemIdentifier = value
                2 -> contentParameter = value
                3 -> ownerInstitution = value
                4 -> {
                    setInformation = value
                    parseSetParts(element.data)?.let { (parts, part) ->
                        partsInSet = parts
                        partNumber = part
                        setInformation = "$parts/$part"
                    }
                }
                5 -> typeOfUsage = value
                6 -> shelfLocation = value
                7 -> onixMediaFormat = value
                8 -> marcMediaFormat = value
                9 -> supplierIdentifier = value
                10 -> orderNumber = value
                11 -> illBorrowingInstitution = value
                12 -> illBorrowingTransactionNumber = value
                13 -> gs1ProductIdentifier = value
                14 -> alternativeUniqueItemIdentifier = value
                15 -> localDataA = value
                16 -> localDataB = value
                17 -> title = value
                18 -> productIdentifierLocal = value
                19 -> mediaFormatOther = value
                20 -> supplyChainStage = value
                21 -> supplierInvoiceNumber = value
                22 -> alternativeItemIdentifier = value
                23 -> alternativeOwnerInstitution = value
                24 -> subsidiaryOfOwnerInstitution = value
                25 -> alternativeIllBorrowingInstitution = value
                26 -> localDataC = value
            }
        }

        fun build() = Iso28560Tag(
            primaryItemIdentifier = primaryItemIdentifier,
            contentParameter = contentParameter,
            ownerInstitution = ownerInstitution,
            setInformation = setInformation,
            partsInSet = partsInSet,
            partNumber = partNumber,
            typeOfUsage = typeOfUsage,
            shelfLocation = shelfLocation,
            onixMediaFormat = onixMediaFormat,
            marcMediaFormat = marcMediaFormat,
            supplierIdentifier = supplierIdentifier,
            orderNumber = orderNumber,
            illBorrowingInstitution = illBorrowingInstitution,
            illBorrowingTransactionNumber = illBorrowingTransactionNumber,
            gs1ProductIdentifier = gs1ProductIdentifier,
            alternativeUniqueItemIdentifier = alternativeUniqueItemIdentifier,
            localDataA = localDataA,
            localDataB = localDataB,
            title = title,
            productIdentifierLocal = productIdentifierLocal,
            mediaFormatOther = mediaFormatOther,
            supplyChainStage = supplyChainStage,
            supplierInvoiceNumber = supplierInvoiceNumber,
            alternativeItemIdentifier = alternativeItemIdentifier,
            alternativeOwnerInstitution = alternativeOwnerInstitution,
            subsidiaryOfOwnerInstitution = subsidiaryOfOwnerInstitution,
            alternativeIllBorrowingInstitution = alternativeIllBorrowingInstitution,
            localDataC = localDataC,
        )
    }

    private fun decodeElement(element: DataElement): String = when (element.oid) {
        2 -> decodeContentParameter(element.data)
        5 -> decodeTypeOfUsage(element.data)
        else -> decodeDataBlock(element.encoding, element.data)
    }

    private fun decodeDataBlock(encoding: Int, data: ByteArray): String = when (encoding) {
        ENC_APP -> decodeIsil(data)
        ENC_INTEGER -> decodeInteger(data)
        ENC_NUMERIC -> decodeNumericString(data)
        ENC_5BIT -> decode5Bit(data)
        ENC_6BIT -> decode6Bit(data)
        ENC_7BIT -> decode7Bit(data)
        ENC_8BIT -> String(data, Charset.forName("ISO-8859-1"))
        ENC_UTF8 -> String(data, StandardCharsets.UTF_8)
        else -> throw Iso28560FormatException("unknown encoding $encoding")
    }

    private fun decodeContentParameter(data: ByteArray): String {
        val present = mutableListOf<Int>()
        var oid = 3
        for (b in data) {
            val unsigned = b.toInt() and 0xFF
            for (bit in 7 downTo 0) {
                if ((unsigned ushr bit) and 1 == 1) {
                    present.add(oid)
                }
                oid++
            }
        }
        return present.joinToString(" ")
    }

    private fun decodeTypeOfUsage(data: ByteArray): String {
        var value = 0L
        for (b in data) {
            value = (value shl 8) or (b.toLong() and 0xFF)
        }
        return value.toString(16)
    }

    private fun parseSetParts(data: ByteArray): Pair<Int, Int>? = when (data.size) {
        2 -> (data[0].toInt() and 0xFF) to (data[1].toInt() and 0xFF)
        4 -> {
            val parts = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val part = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
            parts to part
        }
        else -> null
    }

    private fun decodeInteger(data: ByteArray): String {
        var value = 0L
        for (b in data) {
            value = (value shl 8) or (b.toLong() and 0xFF)
        }
        return value.toString()
    }

    private fun decodeNumericString(data: ByteArray): String {
        val bits = BitReader(data)
        val sb = StringBuilder()
        while (bits.has(4)) {
            val nibble = bits.read(4)
            if (nibble != 0xF) {
                sb.append(if (nibble in 0..9) ('0'.code + nibble).toChar() else '?')
            }
        }
        return sb.toString()
    }

    private fun decode5Bit(data: ByteArray): String {
        val bits = BitReader(data)
        val sb = StringBuilder()
        while (bits.has(5)) {
            sb.append(uppercaseChar(bits.read(5)))
        }
        return sb.toString()
    }

    private fun decode6Bit(data: ByteArray): String {
        val bits = BitReader(data)
        val sb = StringBuilder()
        while (bits.has(6)) {
            val six = bits.read(6)
            val ascii = if ((six and 0x20) != 0) six else (six or 0x40)
            sb.append(ascii.toChar())
        }
        return sb.toString()
    }

    private fun decode7Bit(data: ByteArray): String {
        val bits = BitReader(data)
        val sb = StringBuilder()
        while (bits.has(7)) {
            sb.append(bits.read(7).toChar())
        }
        return sb.toString()
    }

    private enum class IsilCharset { UPPER, LOWER, NUMBER }

    private fun decodeIsil(data: ByteArray): String {
        val bits = BitReader(data)
        val sb = StringBuilder()
        var charset = IsilCharset.UPPER
        while (true) {
            when (charset) {
                IsilCharset.NUMBER -> {
                    if (!bits.has(4)) break
                    val value = bits.read(4)
                    when (value) {
                        0b1100 -> charset = IsilCharset.UPPER
                        0b1101 -> charset = IsilCharset.UPPER
                        0b1110 -> charset = IsilCharset.LOWER
                        0b1111 -> charset = IsilCharset.LOWER
                        in 0..9 -> sb.append(('0'.code + value).toChar())
                        0b1010 -> sb.append('-')
                        0b1011 -> sb.append(':')
                        else -> sb.append('?')
                    }
                }
                IsilCharset.UPPER, IsilCharset.LOWER -> {
                    if (!bits.has(5)) break
                    val value = bits.read(5)
                    when (value) {
                        0b11100 -> charset = IsilCharset.LOWER
                        0b11101 -> charset = IsilCharset.LOWER
                        0b11110 -> charset = IsilCharset.NUMBER
                        0b11111 -> charset = IsilCharset.NUMBER
                        else -> sb.append(
                            if (charset == IsilCharset.UPPER) uppercaseChar(value)
                            else lowercaseChar(value)
                        )
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun uppercaseChar(value: Int): Char = when (value) {
        0b00000 -> '-'
        in 0b00001..0b11010 -> ('A'.code + value - 1).toChar()
        0b11011 -> ':'
        else -> '?'
    }

    private fun lowercaseChar(value: Int): Char = when (value) {
        0b00000 -> '-'
        in 0b00001..0b11010 -> ('a'.code + value - 1).toChar()
        0b11011 -> '/'
        else -> '?'
    }

    /** Bit 0 is the MSB of byte 0 (ISO 28560-2 / ISO 15962 packing). */
    private class BitReader(private val data: ByteArray) {
        private var position = 0
        private val sizeBits = data.size * 8

        fun has(n: Int): Boolean = sizeBits - position >= n

        fun read(n: Int): Int {
            var value = 0
            repeat(n) {
                val byteIndex = position / 8
                val bitIndex = 7 - (position % 8)
                value = (value shl 1) or ((data[byteIndex].toInt() ushr bitIndex) and 1)
                position++
            }
            return value
        }
    }
}
