package io.github.diskria.achievetodo.etxensions

import com.mojang.serialization.Codec
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import kotlin.jvm.optionals.getOrNull

fun <T : Any> T.toNbtOrNull(codec: Codec<T>): Tag? =
    codec.encodeStart(NbtOps.INSTANCE, this).result().getOrNull()

fun <T : Any> Tag.decodeOrNull(codec: Codec<T>): T? =
    codec.parse(NbtOps.INSTANCE, this).result().getOrNull()
