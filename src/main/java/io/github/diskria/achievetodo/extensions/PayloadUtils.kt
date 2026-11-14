package io.github.diskria.achievetodo.extensions

import io.github.diskria.achievetodo.AchieveToDoMod
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

inline fun <reified P : CustomPacketPayload> payloadTypeOf(): CustomPacketPayload.Type<P> =
    CustomPacketPayload.Type(AchieveToDoMod.getIdentifier(P::class.java.simpleName.lowercase()))

fun <B : ByteBuf, E : Any> setCodecOf(elementCodec: StreamCodec<in B, E>): StreamCodec<B, Set<E>> =
    ByteBufCodecs.collection(::HashSet, elementCodec)

fun <B : ByteBuf, E : Any> listCodecOf(elementCodec: StreamCodec<in B, E>): StreamCodec<B, List<E>> =
    ByteBufCodecs.collection(::ArrayList, elementCodec)

fun <B : ByteBuf, K : Any, V : Any> mapCodecOf(
    keyValueCodecs: Pair<StreamCodec<in B, K>, StreamCodec<in B, V>>,
): StreamCodec<B, Map<K, V>> = ByteBufCodecs.map(::HashMap, keyValueCodecs.first, keyValueCodecs.second)
