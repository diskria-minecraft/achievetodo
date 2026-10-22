package io.github.diskria.achievetodo.networking.c2s;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.minecraft.network.packet.CustomPayload.codecOf;

public record DemystifyAbilityPayload(
    @NotNull AbilityType abilityType
) implements CustomPayload {

    public static final Id<DemystifyAbilityPayload> ID = new Id<>(AchieveToDoMod.getIdentifier(
        DemystifyAbilityPayload.class.getName().toLowerCase(Locale.ROOT).toLowerCase(Locale.ROOT)
    ));

    public static final PacketCodec<PacketByteBuf, DemystifyAbilityPayload> CODEC = codecOf(
        DemystifyAbilityPayload::encode,
        DemystifyAbilityPayload::decode
    );

    private static @NotNull DemystifyAbilityPayload decode(@NotNull PacketByteBuf buf) {
        AbilityType abilityType = buf.readEnumConstant(AbilityType.class);
        return new DemystifyAbilityPayload(abilityType);
    }

    @Override
    public Id<?> getId() {
        return ID;
    }

    private void encode(@NotNull PacketByteBuf buf) {
        buf.writeEnumConstant(abilityType);
    }
}
