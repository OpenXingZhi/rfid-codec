package com.xingzhi.rfid

internal object Hex {
    fun toHex(data: ByteArray): String {
        val sb = StringBuilder(data.size * 2)
        for (b in data) {
            sb.append("%02X".format(b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val normalized = hex.replace(" ", "")
        require(normalized.length % 2 == 0) { "hex string must have even length" }
        val out = ByteArray(normalized.length / 2)
        for (i in out.indices) {
            out[i] = normalized.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
