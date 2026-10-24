package io.github.diskria.achievetodo.ability

import net.minecraft.core.BlockBox
import net.minecraft.network.codec.StreamCodec

@JvmRecord
data class DimensionalBlockBox(val dimensionType: DimensionType, val blockBox: BlockBox) {

    companion object {
        val STREAM_CODEC = StreamCodec.composite(
            DimensionType.STREAM_CODEC, DimensionalBlockBox::dimensionType,
            BlockBox.STREAM_CODEC, DimensionalBlockBox::blockBox,
            ::DimensionalBlockBox
        )
    }
}
