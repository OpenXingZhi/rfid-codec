package com.xingzhi.rfid.hf.iso28560

/** Thrown when ISO 28560-2 user-memory bytes cannot be decoded. */
class Iso28560FormatException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
