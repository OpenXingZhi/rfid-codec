# XingZhi RFID Codec

RFID tag codecs for XingZhi library applications. The library is organized by
frequency band and by the standard each codec implements.

| Band | Air protocol | Standard | What this library does |
| --- | --- | --- | --- |
| **HF** | ISO 15693 | [ISO 28560-2](https://www.iso.org/standard/59467.html) (data elements from ISO 28560-1) | Decode compact user-memory objects to `Iso28560Tag` |
| **UHF** | EPC Gen2 / ISO 18000-6C | 高校图书馆UHF-RFID技术联盟标准 第四版, 应用指南附录A (CULTU) | Encode and decode 1–14 character barcodes to 96-bit / 128-bit EPC |

Out of scope (planned or left to applications): reader/device drivers, GS1 TDS
EPC schemes, and ISO 28560 encode.

## Dependency

Coordinates: `com.xingzhi:rfid-codec:2.0.0`

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/OpenXingZhi/rfid-codec")
    credentials(PasswordCredentials::class)
}

implementation("com.xingzhi:rfid-codec:2.0.0")
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

## UHF — CULTU EPC barcode codec

Package `com.xingzhi.rfid.uhf.cultu`. Implements 高校图书馆UHF-RFID技术联盟标准
第四版 (China University Library UHF-RFID Technology Union Standard, 4th
edition), application guide Annex A: barcode ↔ 96-bit / 128-bit EPC.

```kotlin
import com.xingzhi.rfid.uhf.cultu.EpcCodec

val epc96 = EpcCodec.encode96("ABC0123456789_")
val hex96 = EpcCodec.encode96Hex("ABC0123456789_")
val barcode = EpcCodec.decode96(epc96)

val epc128 = EpcCodec.encode128("ABC0123456789_")
```
