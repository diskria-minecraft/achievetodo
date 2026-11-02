package io.github.diskria.achievetodo.extensions

typealias DfuPair<F, S> = com.mojang.datafixers.util.Pair<F, S>

fun interpolateColor(from: Int, to: Int, factor: Double): Int {
    val r = (((from shr 16) and 0xFF) + (((to shr 16) and 0xFF) - ((from shr 16) and 0xFF)) * factor).toInt()
    val g = (((from shr 8) and 0xFF) + (((to shr 8) and 0xFF) - ((from shr 8) and 0xFF)) * factor).toInt()
    val b = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * factor).toInt()
    return (r shl 16) or (g shl 8) or b
}
