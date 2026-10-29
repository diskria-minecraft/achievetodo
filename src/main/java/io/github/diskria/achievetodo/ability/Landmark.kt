package io.github.diskria.achievetodo.ability

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.codec.StreamCodec

data class Landmark(val type: LandmarkType, val area: LandmarkArea) {
    companion object {
        val CODEC: Codec<Landmark> = RecordCodecBuilder.create { instance ->
            instance.group(
                Landmark::type.let { LandmarkType.CODEC.fieldOf(it.name).forGetter(it) },
                Landmark::area.let { LandmarkArea.CODEC.fieldOf(it.name).forGetter(it) },
            ).apply(instance, ::Landmark)
        }
        val SET_CODEC: Codec<Set<Landmark>> = CODEC.listOf().xmap(::HashSet, ::ArrayList)

        val STREAM_CODEC = StreamCodec.composite(
            LandmarkType.STREAM_CODEC, Landmark::type,
            LandmarkArea.STREAM_CODEC, Landmark::area,
            ::Landmark
        )
    }
}
