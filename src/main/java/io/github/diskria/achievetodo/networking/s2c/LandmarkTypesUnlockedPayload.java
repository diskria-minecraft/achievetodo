package io.github.diskria.achievetodo.networking.s2c;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record LandmarkTypesUnlockedPayload(
    @NotNull Set<LandmarkType> unlockedLandmarkTypes
) implements CustomPayload {

    public static final Id<LandmarkTypesUnlockedPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        LandmarkTypesUnlockedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, LandmarkTypesUnlockedPayload> CODEC = codecOf(
        LandmarkTypesUnlockedPayload::encode,
        LandmarkTypesUnlockedPayload::decode
    );

    private static @NotNull LandmarkTypesUnlockedPayload decode(@NotNull PacketByteBuf buf) {
        int unlockedLandmarkTypesSize = buf.readInt();
        Set<LandmarkType> landmarks = new HashSet<>(unlockedLandmarkTypesSize);
        for (int i = 0; i < unlockedLandmarkTypesSize; i++) {
            landmarks.add(buf.readEnumConstant(LandmarkType.class));
        }
        return new LandmarkTypesUnlockedPayload(landmarks);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        int unlockedLandmarkTypesSize = unlockedLandmarkTypes.size();
        buf.writeInt(unlockedLandmarkTypesSize);
        for (LandmarkType landmarkType : unlockedLandmarkTypes) {
            buf.writeEnumConstant(landmarkType);
        }
    }
}
