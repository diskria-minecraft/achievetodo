package io.github.diskria.achievetodo.payloads

import io.github.diskria.achievetodo.AchieveToDoMod
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

inline fun <reified P : CustomPacketPayload> payloadTypeOf(): CustomPacketPayload.Type<P> =
    CustomPacketPayload.Type(AchieveToDoMod.getIdentifier(P::class.java.simpleName.lowercase()))

fun <E : Any> setCodecOf(elementCodec: StreamCodec<ByteBuf, E>): StreamCodec<ByteBuf, Set<E>> =
    ByteBufCodecs.collection(::HashSet, elementCodec)

fun <E : Any> listCodecOf(elementCodec: StreamCodec<ByteBuf, E>): StreamCodec<ByteBuf, List<E>> =
    ByteBufCodecs.collection(::ArrayList, elementCodec)

fun <K : Any, V : Any> mapCodecOf(
    keyValueCodecs: Pair<StreamCodec<ByteBuf, K>, StreamCodec<ByteBuf, V>>
): StreamCodec<ByteBuf, Map<K, V>> = ByteBufCodecs.map(::HashMap, keyValueCodecs.first, keyValueCodecs.second)
