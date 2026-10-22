package io.github.diskria.achievetodo.networking.s2c;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record ScoreProgressChangedPayload(
    @NotNull TrackedScoreType progressType,
    int progress
) implements CustomPayload {

    public static final Id<ScoreProgressChangedPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        ScoreProgressChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, ScoreProgressChangedPayload> CODEC = codecOf(
        ScoreProgressChangedPayload::encode,
        ScoreProgressChangedPayload::decode
    );

    private static @NotNull ScoreProgressChangedPayload decode(@NotNull PacketByteBuf buf) {
        TrackedScoreType trackedScoreType = buf.readEnumConstant(TrackedScoreType.class);
        int progress = buf.readInt();
        return new ScoreProgressChangedPayload(trackedScoreType, progress);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        buf.writeEnumConstant(progressType);
        buf.writeInt(progress);
    }
}
