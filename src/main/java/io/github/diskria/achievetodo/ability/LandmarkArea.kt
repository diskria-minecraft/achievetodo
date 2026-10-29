package io.github.diskria.achievetodo.ability

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import kotlin.math.ceil
import kotlin.math.floor

data class LandmarkArea(val dimension: DimensionType, val boundingBox: BoundingBox) {

    fun intersects(dimension: DimensionType, aabb: AABB): Boolean {
        if (this.dimension != dimension) return false
        val minX = floor(aabb.minX).toInt()
        val minY = floor(aabb.minY).toInt()
        val minZ = floor(aabb.minZ).toInt()
        val maxX = ceil(aabb.maxX).toInt()
        val maxY = ceil(aabb.maxY).toInt()
        val maxZ = ceil(aabb.maxZ).toInt()
        return boundingBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)
    }

    companion object {
        val CODEC: Codec<LandmarkArea> = RecordCodecBuilder.create { instance ->
            instance.group(
                LandmarkArea::dimension.let { DimensionType.CODEC.fieldOf(it.name).forGetter(it) },
                LandmarkArea::boundingBox.let { BoundingBox.CODEC.fieldOf(it.name).forGetter(it) },
            ).apply(instance, ::LandmarkArea)
        }

        val STREAM_CODEC = StreamCodec.composite(
            DimensionType.STREAM_CODEC, LandmarkArea::dimension,
            BoundingBox.STREAM_CODEC, LandmarkArea::boundingBox,
            ::LandmarkArea
        )
    }
}
