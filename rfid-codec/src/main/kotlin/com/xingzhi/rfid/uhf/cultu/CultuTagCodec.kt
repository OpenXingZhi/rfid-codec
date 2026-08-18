package com.xingzhi.rfid.uhf.cultu

import java.nio.charset.StandardCharsets

/**
 * Full-EPC codec for 高校图书馆UHF-RFID技术联盟标准 第四版
 * (China University Library UHF-RFID Technology Union Standard, 4th edition).
 *
 * Decodes the complete EPC data-area layout from application guide §7 / §9.
 * Item-identifier payloads use [EpcCodec] for coding types 0 and 1, and
 * raw ISO/IEC 646 bytes (right-NUL-padded) for coding type 2.
 *
 * Coding type 2 is defined for tags whose EPC *capacity* is 144-bit or
 * above (`≥18` bytes, §7.1.3 / §9.2). The CULTU fields occupy the first
 * 18 bytes; extra capacity is ignored. Types 0 and 1 require exact
 * 12-byte and 16-byte EPCs.
 */
object CultuTagCodec {

    private const val HEADER_SIZE = 4
    private const val EPC_96_SIZE = 12
    private const val EPC_128_SIZE = 16
    private const val EPC_144_MIN_SIZE = 18
    private const val PAYLOAD_144_SIZE = 14

    /**
     * Decode a full CULTU EPC. Malformed or reserved encodings throw
     * [CultuFormatException].
     */
    fun decode(epc: ByteArray?): CultuTag {
        if (epc == null) {
            throw CultuFormatException("EPC is null")
        }
        if (epc.size < HEADER_SIZE) {
            throw CultuFormatException("EPC is too short: ${epc.size} bytes")
        }

        val b0 = epc[0].toInt() and 0xFF
        val b1 = epc[1].toInt() and 0xFF
        val security = (b0 and 0x80) != 0
        val reserved = (b0 ushr 5) and 0x03
        val sorting = b0 and 0x1F
        val codingType = (b1 ushr 6) and 0x03
        val modelVersion = b1 and 0x3F
        val contentIndex = ((epc[2].toInt() and 0xFF) shl 8) or (epc[3].toInt() and 0xFF)

        val payload = payloadFor(codingType, epc)
        val itemIdentifier = decodeItemIdentifier(codingType, payload)

        return CultuTag(
            security = security,
            reserved = reserved,
            sorting = sorting,
            codingType = codingType,
            modelVersion = modelVersion,
            contentIndex = contentIndex,
            itemIdentifier = itemIdentifier,
        )
    }

    /**
     * Encode [tag] to a full CULTU EPC. Type 0/1 produce 12/16 bytes;
     * type 2 produces 18 bytes (14-byte identifier, right-padded with NUL).
     */
    fun encode(tag: CultuTag): ByteArray {
        requireField(tag.reserved, 0..3, "reserved")
        requireField(tag.sorting, 0..31, "sorting")
        requireField(tag.codingType, 0..2, "coding type")
        requireField(tag.modelVersion, 0..63, "model version")
        requireField(tag.contentIndex, 0..0xFFFF, "content index")

        val payload = encodeItemIdentifier(tag.codingType, tag.itemIdentifier)
        val out = ByteArray(HEADER_SIZE + payload.size)
        val securityBit = if (tag.security) 0x80 else 0
        out[0] = (securityBit or (tag.reserved shl 5) or tag.sorting).toByte()
        out[1] = ((tag.codingType shl 6) or tag.modelVersion).toByte()
        out[2] = ((tag.contentIndex ushr 8) and 0xFF).toByte()
        out[3] = (tag.contentIndex and 0xFF).toByte()
        payload.copyInto(out, HEADER_SIZE)
        return out
    }

    private fun payloadFor(codingType: Int, epc: ByteArray): ByteArray {
        val expected = when (codingType) {
            0 -> EPC_96_SIZE
            1 -> EPC_128_SIZE
            2 -> {
                if (epc.size < EPC_144_MIN_SIZE) {
                    throw CultuFormatException(
                        "coding type 2 requires at least $EPC_144_MIN_SIZE-byte EPC, got ${epc.size}",
                    )
                }
                return epc.copyOfRange(HEADER_SIZE, EPC_144_MIN_SIZE)
            }
            else -> throw CultuFormatException("reserved coding type $codingType")
        }
        if (epc.size != expected) {
            throw CultuFormatException(
                "coding type $codingType requires $expected-byte EPC, got ${epc.size}",
            )
        }
        return epc.copyOfRange(HEADER_SIZE, expected)
    }

    private fun decodeItemIdentifier(codingType: Int, payload: ByteArray): String = try {
        when (codingType) {
            0 -> EpcCodec.decode96(payload)
            1 -> EpcCodec.decode128(payload)
            2 -> decodeRawIso646(payload)
            else -> throw CultuFormatException("reserved coding type $codingType")
        }
    } catch (e: CultuFormatException) {
        throw e
    } catch (e: RuntimeException) {
        throw CultuFormatException("malformed item identifier", e)
    }

    private fun encodeItemIdentifier(codingType: Int, itemIdentifier: String): ByteArray = try {
        when (codingType) {
            0 -> EpcCodec.encode96(itemIdentifier)
            1 -> EpcCodec.encode128(itemIdentifier)
            2 -> encodeRawIso646(itemIdentifier)
            else -> throw CultuFormatException("reserved coding type $codingType")
        }
    } catch (e: CultuFormatException) {
        throw e
    } catch (e: RuntimeException) {
        throw CultuFormatException("malformed item identifier", e)
    }

    /** Fixed 14-byte ISO/IEC 646 field; only trailing NULs are padding. */
    private fun decodeRawIso646(payload: ByteArray): String {
        var end = payload.size
        while (end > 0 && payload[end - 1] == 0.toByte()) {
            end--
        }
        for (i in 0 until end) {
            if (payload[i].toInt() and 0xFF > 0x7F) {
                throw CultuFormatException("item identifier must be ISO/IEC 646")
            }
        }
        return String(payload, 0, end, StandardCharsets.ISO_8859_1)
    }

    /**
     * Right-pad with NUL. Embedded NULs are data; a trailing NUL cannot
     * round-trip through [decodeRawIso646] and is rejected.
     */
    private fun encodeRawIso646(identifier: String): ByteArray {
        if (identifier.length > PAYLOAD_144_SIZE) {
            throw CultuFormatException(
                "item identifier length must be at most $PAYLOAD_144_SIZE, got ${identifier.length}",
            )
        }
        if (identifier.endsWith('\u0000')) {
            throw CultuFormatException("item identifier must not end with NUL")
        }
        val out = ByteArray(PAYLOAD_144_SIZE)
        for (i in identifier.indices) {
            val code = identifier[i].code
            if (code > 0x7F) {
                throw CultuFormatException("item identifier must be ISO/IEC 646")
            }
            out[i] = code.toByte()
        }
        return out
    }

    private fun requireField(value: Int, range: IntRange, name: String) {
        if (value !in range) {
            throw CultuFormatException("$name out of range: $value")
        }
    }
}
