package io.github.diskria.achievetodo;

import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.networking.c2s.DemystifyAbilityPayload;
import io.github.diskria.achievetodo.networking.s2c.*;
import io.github.diskria.achievetodo.server.AchieveToDoServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
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
        return Identifier.of(ID, path);
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
        DimensionType targetDimensionType = DimensionType.findByWorld(targetWorld.getRegistryKey());
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
        PayloadTypeRegistry.playC2S().register(
            DemystifyAbilityPayload.ID,
            DemystifyAbilityPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
            SyncAbilitiesConfigurationPayload.ID,
            SyncAbilitiesConfigurationPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            SyncObtainedAdvancementsCountPayload.ID,
            SyncObtainedAdvancementsCountPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            LandmarksLockedStatusChangedPayload.ID,
            LandmarksLockedStatusChangedPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            LandmarkTypesUnlockedPayload.ID,
            LandmarkTypesUnlockedPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            LockedLandmarkResizedPayload.ID,
            LockedLandmarkResizedPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            ScoreProgressChangedPayload.ID,
            ScoreProgressChangedPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            StatisticsDataProgressChangedPayload.ID,
            StatisticsDataProgressChangedPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            CheckTargetInLockedLandmarkPayload.ID,
            CheckTargetInLockedLandmarkPayload.CODEC
        );
    }

    @Override
    public void onInitialize() {
        registerPayloads();

        server = new AchieveToDoServer();
        server.onInitializeServer();
    }
}
