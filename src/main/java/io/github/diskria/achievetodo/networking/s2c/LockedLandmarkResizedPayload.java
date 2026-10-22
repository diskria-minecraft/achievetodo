package io.github.diskria.achievetodo.networking.s2c;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockBox;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record LockedLandmarkResizedPayload(
    @NotNull LandmarkType landmarkType,
    @NotNull DimensionType dimensionType,
    @NotNull BlockBox oldBlockBox,
    @NotNull BlockBox newBlockBox
) implements CustomPayload {

    public static final Id<LockedLandmarkResizedPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        LockedLandmarkResizedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, LockedLandmarkResizedPayload> CODEC = codecOf(
        LockedLandmarkResizedPayload::encode,
        LockedLandmarkResizedPayload::decode
    );

    private static @NotNull LockedLandmarkResizedPayload decode(@NotNull PacketByteBuf buf) {
        LandmarkType landmarkType = buf.readEnumConstant(LandmarkType.class);
        DimensionType dimensionType = buf.readEnumConstant(DimensionType.class);
        int oldMinX = buf.readInt();
        int oldMinY = buf.readInt();
        int oldMinZ = buf.readInt();
        int oldMaxX = buf.readInt();
        int oldMaxY = buf.readInt();
        int oldMaxZ = buf.readInt();
        BlockBox oldBlockBox = new BlockBox(
            oldMinX,
            oldMinY,
            oldMinZ,
            oldMaxX,
            oldMaxY,
            oldMaxZ
        );

        int newMinX = buf.readInt();
        int newMinY = buf.readInt();
        int newMinZ = buf.readInt();
        int newMaxX = buf.readInt();
        int newMaxY = buf.readInt();
        int newMaxZ = buf.readInt();
        BlockBox newBlockBox = new BlockBox(
            newMinX,
            newMinY,
            newMinZ,
            newMaxX,
            newMaxY,
            newMaxZ
        );
        return new LockedLandmarkResizedPayload(landmarkType, dimensionType, oldBlockBox, newBlockBox);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        buf.writeEnumConstant(landmarkType);
        buf.writeEnumConstant(dimensionType);
        int oldMinX = oldBlockBox.getMinX();
        int oldMinY = oldBlockBox.getMinY();
        int oldMinZ = oldBlockBox.getMinZ();
        int oldMaxX = oldBlockBox.getMaxX();
        int oldMaxY = oldBlockBox.getMaxY();
        int oldMaxZ = oldBlockBox.getMaxZ();
        buf.writeInt(oldMinX);
        buf.writeInt(oldMinY);
        buf.writeInt(oldMinZ);
        buf.writeInt(oldMaxX);
        buf.writeInt(oldMaxY);
        buf.writeInt(oldMaxZ);

        int newMinX = newBlockBox.getMinX();
        int newMinY = newBlockBox.getMinY();
        int newMinZ = newBlockBox.getMinZ();
        int newMaxX = newBlockBox.getMaxX();
        int newMaxY = newBlockBox.getMaxY();
        int newMaxZ = newBlockBox.getMaxZ();
        buf.writeInt(newMinX);
        buf.writeInt(newMinY);
        buf.writeInt(newMinZ);
        buf.writeInt(newMaxX);
        buf.writeInt(newMaxY);
        buf.writeInt(newMaxZ);
    }
}
