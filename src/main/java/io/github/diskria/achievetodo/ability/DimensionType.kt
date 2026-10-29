package io.github.diskria.achievetodo.ability

import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ByIdMap
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.Level

enum class DimensionType : StringRepresentable {

    OVERWORLD,
    NETHER,
    END;

    override fun getSerializedName(): String = name.lowercase()

    companion object {
        fun findByLevel(level: ResourceKey<Level>) = when (level) {
            Level.OVERWORLD -> OVERWORLD
            Level.NETHER -> NETHER
            Level.END -> END
            else -> null
        }

        private val BY_ORDINAL = ByIdMap.continuous(
            DimensionType::ordinal, entries.toTypedArray(), ByIdMap.OutOfBoundsStrategy.ZERO
        )

        val STREAM_CODEC = ByteBufCodecs.idMapper(BY_ORDINAL, DimensionType::ordinal)
        val CODEC = StringRepresentable.fromEnum { DimensionType.entries.toTypedArray() }
    }
}
