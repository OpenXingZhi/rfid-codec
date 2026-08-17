package com.xingzhi.rfid.hf.iso28560

import com.xingzhi.rfid.Hex
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class Iso28560Test {

    /**
     * RobotDroid `ISO28560Parser.main()` user-memory vector.
     * 6-bit OID 1 payload decodes to YST0450585.
     */
    private val robotDroidParserHex =
        "C10108653530D35C35E358000201A80302CCE96501126701BA00000000000000"

    /**
     * Inventory `ExampleUnitTest.rawValueTest` vector with the trailing `OO`
     * typo corrected to `00`. The first 8 bytes are the ISO 15693 UID and are
     * stripped so the codec sees user memory only.
     */
    private val inventoryRawHex =
        "E0040150F2191FF8A101040014815F000203A80008830203D6593F00"

    @Test
    fun robotDroidParserVectorDecodesPrimaryItemIdentifier() {
        val userMemory = Hex.hexToBytes(robotDroidParserHex)
        assertEquals("YST0450585", Iso28560.decodePrimaryItemIdentifier(userMemory))
        assertEquals("YST0450585", Iso28560.decode(userMemory).primaryItemIdentifier)
    }

    @Test
    fun robotDroidParserVectorDecodesCompanionFields() {
        val tag = Iso28560.decode(Hex.hexToBytes(robotDroidParserHex))
        assertEquals("YST0450585", tag.primaryItemIdentifier)
        assertEquals("3 5 7", tag.contentParameter)
        assertEquals("YST", tag.ownerInstitution)
        assertEquals("12", tag.typeOfUsage)
        assertEquals("\u00ba", tag.onixMediaFormat)
    }

    @Test
    fun inventoryVectorDecodesAfterFixingOoTypo() {
        val frame = Hex.hexToBytes(inventoryRawHex)
        val userMemory = frame.copyOfRange(8, frame.size)
        val tag = Iso28560.decode(userMemory)
        assertEquals("0014815", tag.primaryItemIdentifier)
        assertEquals("0014815", Iso28560.decodePrimaryItemIdentifier(userMemory))
        assertEquals("3 5 7 23", tag.contentParameter)
        assertEquals("ZYLS", tag.ownerInstitution)
    }

    @Test
    fun emptyArrayThrowsFormatException() {
        assertFailsWith<Iso28560FormatException> { Iso28560.decode(ByteArray(0)) }
        assertFailsWith<Iso28560FormatException> { Iso28560.decodePrimaryItemIdentifier(ByteArray(0)) }
        assertFailsWith<Iso28560FormatException> { Iso28560.decode(null) }
    }

    @Test
    fun garbageBytesThrowFormatException() {
        assertFailsWith<Iso28560FormatException> {
            Iso28560.decode(byteArrayOf(0xC1.toByte(), 0x01))
        }
        assertFailsWith<Iso28560FormatException> {
            Iso28560.decodePrimaryItemIdentifier(byteArrayOf(0x01))
        }
    }

    @Test
    fun truncatedDataThrowsFormatException() {
        val truncated = Hex.hexToBytes("C10108653530D35C35")
        assertFailsWith<Iso28560FormatException> { Iso28560.decode(truncated) }
        assertFailsWith<Iso28560FormatException> { Iso28560.decodePrimaryItemIdentifier(truncated) }
    }

    @Test
    fun terminatorOnlyUserMemoryHasNoPrimaryId() {
        val tag = Iso28560.decode(byteArrayOf(0x00))
        assertNull(tag.primaryItemIdentifier)
        assertNull(Iso28560.decodePrimaryItemIdentifier(byteArrayOf(0x00)))
    }
}
