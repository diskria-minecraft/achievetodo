package io.github.diskria.achievetodo.networking.s2c;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockBox;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record LandmarksLockedStatusChangedPayload(
    @NotNull Map<LandmarkType, Set<DimensionalBlockBox>> landmarks,
    boolean isLocked
) implements CustomPayload {

    public static final Id<LandmarksLockedStatusChangedPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        LandmarksLockedStatusChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, LandmarksLockedStatusChangedPayload> CODEC = codecOf(
        LandmarksLockedStatusChangedPayload::encode,
        LandmarksLockedStatusChangedPayload::decode
    );

    private static @NotNull LandmarksLockedStatusChangedPayload decode(@NotNull PacketByteBuf buf) {
        int landmarkTypesSize = buf.readInt();
        Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = new HashMap<>(landmarkTypesSize);
        for (int mapIndex = 0; mapIndex < landmarkTypesSize; mapIndex++) {
            LandmarkType landmarkType = buf.readEnumConstant(LandmarkType.class);
            int dimensionalBlockBoxesSize = buf.readInt();
            Set<DimensionalBlockBox> dimensionalBlockBoxes = new HashSet<>(dimensionalBlockBoxesSize);
            for (int listIndex = 0; listIndex < dimensionalBlockBoxesSize; listIndex++) {
                DimensionType dimensionType = buf.readEnumConstant(DimensionType.class);
                int minX = buf.readInt();
                int minY = buf.readInt();
                int minZ = buf.readInt();
                int maxX = buf.readInt();
                int maxY = buf.readInt();
                int maxZ = buf.readInt();
                BlockBox blockBox = new BlockBox(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
                );
                dimensionalBlockBoxes.add(new DimensionalBlockBox(dimensionType, blockBox));
            }
            landmarks.put(landmarkType, dimensionalBlockBoxes);
        }
        boolean isLocked = buf.readBoolean();
        return new LandmarksLockedStatusChangedPayload(landmarks, isLocked);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        int landmarkTypesSize = landmarks.size();
        buf.writeInt(landmarkTypesSize);
        for (var entry : landmarks.entrySet()) {
            LandmarkType landmarkType = entry.getKey();
            buf.writeEnumConstant(landmarkType);
            Set<DimensionalBlockBox> dimensionalBlockBoxes = entry.getValue();
            int dimensionalBlockBoxesSize = dimensionalBlockBoxes.size();
            buf.writeInt(dimensionalBlockBoxesSize);
            for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                DimensionType dimensionType = dimensionalBlockBox.dimensionType();
                buf.writeEnumConstant(dimensionType);
                BlockBox blockBox = dimensionalBlockBox.blockBox();
                int mixX = blockBox.getMinX();
                int mixY = blockBox.getMinY();
                int mixZ = blockBox.getMinZ();
                int maxX = blockBox.getMaxX();
                int maxY = blockBox.getMaxY();
                int maxZ = blockBox.getMaxZ();
                buf.writeInt(mixX);
                buf.writeInt(mixY);
                buf.writeInt(mixZ);
                buf.writeInt(maxX);
                buf.writeInt(maxY);
                buf.writeInt(maxZ);
            }
        }
        buf.writeBoolean(isLocked);
    }
}
