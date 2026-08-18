package com.xingzhi.rfid.uhf.cultu

/**
 * Decoded 高校图书馆UHF-RFID技术联盟标准 第四版 full EPC.
 *
 * Fields follow application-guide §7 / §9 byte layout:
 * - byte 0: [security:1][reserved:2][sorting:5]
 * - byte 1: [codingType:2][modelVersion:6]
 * - bytes 2–3: [contentIndex] unsigned 16-bit big-endian
 * - remaining bytes: item identifier
 *
 * [security] is the EPC security bit (true = bit 7 set = on loan).
 * [codingType] is 0 (96-bit compressed), 1 (128-bit compressed), or
 * 2 (144-bit raw ISO/IEC 646). Type 3 is reserved and never present here.
 *
 * Type of usage / shelf-layer classification is a backend data element,
 * not part of the EPC.
 */
data class CultuTag(
    val security: Boolean,
    val reserved: Int,
    val sorting: Int,
    val codingType: Int,
    val modelVersion: Int,
    val contentIndex: Int,
    val itemIdentifier: String,
)
