package io.github.diskria.achievetodo.etxensions

import java.security.MessageDigest

fun ByteArray.calcSha1(): ByteArray =
    MessageDigest.getInstance("SHA-1").digest(this)

fun String.calcSha1(): ByteArray =
    toByteArray().calcSha1()
