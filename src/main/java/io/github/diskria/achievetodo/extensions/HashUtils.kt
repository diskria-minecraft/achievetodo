package io.github.diskria.achievetodo.extensions

import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes

fun Path.calcSha1OrNull(): String? =
    runCatching { MessageDigest.getInstance("SHA-1").digest(readBytes()).toHexString() }.getOrNull()
