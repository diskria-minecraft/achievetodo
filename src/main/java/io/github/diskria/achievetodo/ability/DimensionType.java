package io.github.diskria.achievetodo.ability;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public enum DimensionType {

    OVERWORLD,
    NETHER,
    END;

    private static final IntFunction<DimensionType> BY_ORDINAL =
        ByIdMap.continuous(DimensionType::ordinal, DimensionType.values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    public static final StreamCodec<ByteBuf, DimensionType> STREAM_CODEC =
        ByteBufCodecs.idMapper(BY_ORDINAL, DimensionType::ordinal);

    public static @Nullable DimensionType findByLevel(ResourceKey<Level> level) {
        if (level == Level.OVERWORLD) {
            return OVERWORLD;
        }
        if (level == Level.NETHER) {
            return NETHER;
        }
        if (level == Level.END) {
            return END;
        }
        return null;
    }
}
