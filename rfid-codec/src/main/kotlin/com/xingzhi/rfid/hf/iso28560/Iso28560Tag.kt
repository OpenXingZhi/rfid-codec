package com.xingzhi.rfid.hf.iso28560

/**
 * Decoded ISO 28560-1 data elements from ISO 28560-2 user memory.
 *
 * Fields follow the ISO 28560-1:2023 Table 1 relative OIDs 1–26. UID, AFI,
 * and DSFID are air-protocol values and are not part of this model — the
 * codec input is user-memory bytes only.
 *
 * [setInformation] is the decoded OID 4 value. When the payload is 2 or 4
 * bytes it is also split into [partsInSet] and [partNumber].
 */
data class Iso28560Tag(
    val primaryItemIdentifier: String? = null,
    val contentParameter: String? = null,
    val ownerInstitution: String? = null,
    val setInformation: String? = null,
    val partsInSet: Int? = null,
    val partNumber: Int? = null,
    val typeOfUsage: String? = null,
    val shelfLocation: String? = null,
    val onixMediaFormat: String? = null,
    val marcMediaFormat: String? = null,
    val supplierIdentifier: String? = null,
    val orderNumber: String? = null,
    val illBorrowingInstitution: String? = null,
    val illBorrowingTransactionNumber: String? = null,
    val gs1ProductIdentifier: String? = null,
    /** OID 14, reserved for future use by ISO 28560-1. */
    val alternativeUniqueItemIdentifier: String? = null,
    val localDataA: String? = null,
    val localDataB: String? = null,
    val title: String? = null,
    val productIdentifierLocal: String? = null,
    val mediaFormatOther: String? = null,
    val supplyChainStage: String? = null,
    val supplierInvoiceNumber: String? = null,
    val alternativeItemIdentifier: String? = null,
    val alternativeOwnerInstitution: String? = null,
    val subsidiaryOfOwnerInstitution: String? = null,
    val alternativeIllBorrowingInstitution: String? = null,
    val localDataC: String? = null,
)
