# XingZhi RFID Codec

RFID tag codecs for XingZhi library applications. The library is organized by
frequency band and by the standard each codec implements.

| Band | Air protocol | Standard | What this library does |
| --- | --- | --- | --- |
| **HF** | ISO 15693 | [ISO 28560-2](https://www.iso.org/standard/59467.html) (data elements from ISO 28560-1) | Decode compact user-memory objects to `Iso28560Tag` |
| **UHF** | EPC Gen2 / ISO 18000-6C | 高校图书馆UHF-RFID技术联盟标准 第四版 (CULTU) | Decode the full EPC layout to `CultuTag`; encode/decode 96 / 128 / 144-bit item identifiers |

Out of scope (planned or left to applications): reader/device drivers, GS1 TDS
EPC schemes, ISO 28560 encode, CULTU USER-bank elements, and type-of-usage /
shelf-layer classification (those belong in the backend, not the EPC).

## Dependency

Coordinates: `com.xingzhi:rfid-codec:2.1.0`

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/OpenXingZhi/rfid-codec")
    credentials(PasswordCredentials::class)
}

implementation("com.xingzhi:rfid-codec:2.1.0")
```

GitHub Packages requires a GitHub username and a token with `read:packages`
(local `~/.gradle/gradle.properties` `GitHubPackagesUsername` /
`GitHubPackagesPassword`, or `GITHUB_ACTOR` / `GITHUB_TOKEN` in CI).

## HF — ISO 28560-2 user memory

Package `com.xingzhi.rfid.hf.iso28560`. Input is user-memory bytes only; UID,
AFI, and DSFID come from the air protocol and are not decoded here.

```kotlin
import com.xingzhi.rfid.hf.iso28560.Iso28560

val tag = Iso28560.decode(userMemory)
val barcode = tag.primaryItemIdentifier
val fast = Iso28560.decodePrimaryItemIdentifier(userMemory)
```

Malformed or truncated user memory throws `Iso28560FormatException`.

## UHF — CULTU full EPC

Package `com.xingzhi.rfid.uhf.cultu`. Implements 高校图书馆UHF-RFID技术联盟标准
第四版 (China University Library UHF-RFID Technology Union Standard, 4th
edition), application guide §7 / §9 and Annex A.

`CultuTagCodec` decodes the complete EPC data-area layout:

| Bytes | Field |
| --- | --- |
| 0 | security bit 7, reserved bits 6–5, sorting bits 4–0 |
| 1 | coding type bits 7–6, data-model version bits 5–0 |
| 2–3 | content index, unsigned 16-bit big-endian |
| 4… | item identifier |

Coding type `00` is a 12-byte (96-bit) EPC whose 8-byte payload is Annex A
compressed. Type `01` is a 16-byte (128-bit) EPC with a 12-byte Annex A
payload. Type `10` is a 144-bit-or-above EPC: the identifier is 14 raw
ISO/IEC 646 bytes in the first 18 bytes, right-padded with `NUL`; extra
capacity beyond 18 bytes is ignored. Type `11` is reserved and rejected.

Type of usage (document vs shelf/layer) is not an EPC field.

```kotlin
import com.xingzhi.rfid.uhf.cultu.CultuTag
import com.xingzhi.rfid.uhf.cultu.CultuTagCodec

val tag = CultuTagCodec.decode(epc)
val barcode = tag.itemIdentifier
val onLoan = tag.security
val model = tag.modelVersion

val written = CultuTagCodec.encode(
    CultuTag(
        security = false,
        reserved = 0,
        sorting = 0,
        codingType = 0,
        modelVersion = 4,
        contentIndex = 0,
        itemIdentifier = "ABC0123456789_",
    ),
)
```

Malformed or reserved EPCs throw `CultuFormatException`.

`EpcCodec` is unchanged: it still encodes and decodes only the item-identifier
payload (8 bytes for type `00`, 12 bytes for type `01`).

```kotlin
import com.xingzhi.rfid.uhf.cultu.EpcCodec

val epc96 = EpcCodec.encode96("ABC0123456789_")
val hex96 = EpcCodec.encode96Hex("ABC0123456789_")
val barcode = EpcCodec.decode96(epc96)

val epc128 = EpcCodec.encode128("ABC0123456789_")
```
