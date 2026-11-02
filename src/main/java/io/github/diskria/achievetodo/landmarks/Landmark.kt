package io.github.diskria.achievetodo.landmarks

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import kotlin.math.ceil
import kotlin.math.floor

data class Landmark(val type: LandmarkType, val bb: BoundingBox) {

    fun intersects(aabb: AABB): Boolean {
        val minX = floor(aabb.minX).toInt()
        val minY = floor(aabb.minY).toInt()
        val minZ = floor(aabb.minZ).toInt()
        val maxX = ceil(aabb.maxX).toInt()
        val maxY = ceil(aabb.maxY).toInt()
        val maxZ = ceil(aabb.maxZ).toInt()
        return bb.intersects(minX, minY, minZ, maxX, maxY, maxZ)
    }

    companion object {
        val CODEC: Codec<Landmark> = RecordCodecBuilder.create { instance ->
            instance.group(
                Landmark::type.let { LandmarkType.CODEC.fieldOf(it.name).forGetter(it) },
                Landmark::bb.let { BoundingBox.CODEC.fieldOf(it.name).forGetter(it) },
            ).apply(instance, ::Landmark)
        }
        val SET_CODEC: Codec<Set<Landmark>> = CODEC.listOf().xmap(::HashSet, ::ArrayList)

        val STREAM_CODEC = StreamCodec.composite(
            LandmarkType.STREAM_CODEC, Landmark::type,
            BoundingBox.STREAM_CODEC, Landmark::bb,
            ::Landmark
        )
    }
}
