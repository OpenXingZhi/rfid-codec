package com.xingzhi.rfid.uhf.cultu

/** Thrown when a CULTU full EPC cannot be decoded or encoded. */
class CultuFormatException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
