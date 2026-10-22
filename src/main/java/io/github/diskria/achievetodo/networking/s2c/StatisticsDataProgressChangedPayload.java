package io.github.diskria.achievetodo.networking.s2c;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record StatisticsDataProgressChangedPayload(
    @NotNull TrackedStatisticsDataType trackedStatisticsDataType,
    int newProgress
) implements CustomPayload {

    public static final Id<StatisticsDataProgressChangedPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        StatisticsDataProgressChangedPayload.class.getName().toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, StatisticsDataProgressChangedPayload> CODEC = codecOf(
        StatisticsDataProgressChangedPayload::encode,
        StatisticsDataProgressChangedPayload::decode
    );

    private static @NotNull StatisticsDataProgressChangedPayload decode(@NotNull PacketByteBuf buf) {
        TrackedStatisticsDataType trackedStatisticsDataType = buf.readEnumConstant(TrackedStatisticsDataType.class);
        int progress = buf.readInt();
        return new StatisticsDataProgressChangedPayload(trackedStatisticsDataType, progress);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        buf.writeEnumConstant(trackedStatisticsDataType);
        buf.writeInt(newProgress);
    }
}
