package io.github.diskria.achievetodo;

import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.payloads.clientbound.*;
import io.github.diskria.achievetodo.payloads.serverbound.DemystifyAbilityPayload;
import io.github.diskria.achievetodo.server.AchieveToDoServer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchieveToDoMod implements ModInitializer {

    public static final String NAME = "AchieveToDo";
    public static final String ID = "achievetodo";

    public static Logger logger = LoggerFactory.getLogger(NAME);

    private static AchieveToDoServer server;

    public static AchieveToDoServer getServer() {
        return server;
    }

    public static @NotNull Identifier getIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static boolean isAbilityLocked(@Nullable PlayerEntity player, @Nullable AbilityType abilityType) {
        return isAbilityLocked(player, abilityType, false);
    }

    public static boolean isAbilityLocked(
        @Nullable PlayerEntity player,
        @Nullable AbilityType abilityType,
        boolean checkOnly
    ) {
        if (player == null || abilityType == null) {
            return false;
        }
        if (player.getWorld().isClient) {
            return AchieveToDoClient.isAbilityLocked(abilityType, checkOnly);
        }
        return player instanceof ServerPlayerEntity serverPlayer &&
            server != null &&
            server.isAbilityLocked(serverPlayer, abilityType);
    }

    public static boolean isTargetInLockedLandmark(@Nullable PlayerEntity actor, @NotNull Entity target) {
        return isTargetInLockedLandmark(actor, target.getWorld(), target.getBoundingBox());
    }

    public static boolean isTargetInLockedLandmark(@NotNull ItemUsageContext context) {
        return isTargetInLockedLandmark(context.getPlayer(), context.getWorld(), context.getBlockPos());
    }

    public static boolean isTargetInLockedLandmark(
        @Nullable PlayerEntity actor,
        @NotNull World targetWorld,
        @NotNull BlockPos targetBlockPos
    ) {
        return isTargetInLockedLandmark(actor, targetWorld, new Box(targetBlockPos));
    }

    public static boolean isTargetInLockedLandmark(
        @Nullable PlayerEntity actor,
        @NotNull World targetWorld,
        @NotNull Box targetBox
    ) {
        if (actor == null) {
            return false;
        }
        DimensionType targetDimensionType = DimensionType.findByLevel(targetWorld.getRegistryKey());
        if (targetDimensionType == null) {
            return false;
        }
        if (actor.getWorld().isClient) {
            return AchieveToDoClient.isTargetInLockedLandmark(targetDimensionType, targetBox);
        }
        if (server != null && actor instanceof ServerPlayerEntity serverPlayer) {
            return server.isTargetInLockedLandmark(serverPlayer, targetDimensionType, targetBox);
        }
        return false;
    }

    private static void registerPayloads() {
        DemystifyAbilityPayload.register();
        SyncAbilitiesConfigurationPayload.register();
        SyncObtainedAdvancementsCountPayload.register();
        LandmarkTypesUnlockedPayload.register();
        LockedLandmarkResizedPayload.register();
        TrackedAdvancementProgressChangedPayload.register();
        LandmarksLockedStatusChangedPayload.register();
        CheckTargetInLockedLandmarkPayload.register();
    }

    @Override
    public void onInitialize() {
        registerPayloads();

        server = new AchieveToDoServer();
        server.onInitializeServer();
    }
}
