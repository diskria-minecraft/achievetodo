package io.github.diskria.achievetodo.server;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import io.github.diskria.achievetodo.client.InternalPack;
import io.github.diskria.achievetodo.client.Utils;
import io.github.diskria.achievetodo.injection.extension.main.ChunkExtension;
import io.github.diskria.achievetodo.injection.extension.main.LevelInfoExtension;
import io.github.diskria.achievetodo.injection.extension.main.StructureStartExtension;
import io.github.diskria.achievetodo.networking.c2s.DemystifyAbilityPayload;
import io.github.diskria.achievetodo.networking.s2c.*;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.scoreboard.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AchieveToDoServer implements ServerModInitializer {

    private final Map<UUID, Integer> obtainedAdvancementsCountByPlayers = new Object2IntOpenHashMap<>();
    private final Map<ChunkPos, Map<LandmarkType, Set<DimensionalBlockBox>>> landmarksByChunks = new HashMap<>();
    private final Map<LandmarkType, Set<UUID>> playersByLockedLandmarkTypes = new HashMap<>();
    private final Map<TrackedScoreType, Map<UUID, Integer>> trackedScores = new HashMap<>();
    private final Map<TrackedStatisticsDataType, Map<UUID, Integer>> trackedStats = new HashMap<>();
    public AdvancementsMode currentAdvancementsMode;
    public ScoreboardObjective currentScoreboardObjective;
    public ScoreboardDisplaySlot currentScoreboardDisplaySlot;
    private Map<AbilityType, Integer> abilitiesConfiguration;

    private static void registerInternalDataPacks() {
        FabricLoader.getInstance().getModContainer(AchieveToDoMod.ID).ifPresent(modContainer -> {
            for (InternalPack internalPack : InternalPack.values()) {
                ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of(internalPack.getDatapackName()),
                    modContainer,
                    ResourcePackActivationType.NORMAL
                );
            }
        });
    }

    public void prepareScoreboard(ServerScoreboard scoreboard) {
        AdvancementsMode oldAdvancementsMode = currentAdvancementsMode;
        ScoreboardObjective oldScoreboardObjective = currentScoreboardObjective;
        ScoreboardDisplaySlot oldScoreboardDisplaySlot = currentScoreboardDisplaySlot;

        currentAdvancementsMode = null;
        currentScoreboardObjective = null;
        currentScoreboardDisplaySlot = null;

        ScoreboardDisplaySlot[] slotPriority = {
            ScoreboardDisplaySlot.SIDEBAR, ScoreboardDisplaySlot.LIST, ScoreboardDisplaySlot.BELOW_NAME
        };
        for (ScoreboardDisplaySlot displaySlot : slotPriority) {
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(displaySlot);
            if (objective != null) {
                AdvancementsMode advancementsMode = AdvancementsMode.findByObjectiveName(objective.getName());
                if (advancementsMode != null) {
                    currentAdvancementsMode = advancementsMode;
                    currentScoreboardObjective = objective;
                    currentScoreboardDisplaySlot = displaySlot;
                    break;
                }
            }
        }
        if (currentAdvancementsMode == null ||
            currentScoreboardObjective == null ||
            currentScoreboardDisplaySlot == null
        ) {
            AchieveToDoMod.logger.error(
                "Can't find scoreboard objective with advancements counter! " +
                    "Please check that BACAP datapack is installed " +
                    "and enable advancements counter in the sidebar, tab list or below player names."
            );
            return;
        }
        if (currentAdvancementsMode != oldAdvancementsMode ||
            currentScoreboardObjective != oldScoreboardObjective ||
            currentScoreboardDisplaySlot != oldScoreboardDisplaySlot
        ) {
            AchieveToDoMod.logger.info(
                "Scoreboard objective with advancements counter found: advancements mode = {}, objective name = {}, display slot = {}",
                currentAdvancementsMode,
                currentScoreboardObjective.getName(),
                currentScoreboardDisplaySlot
            );
            for (ServerPlayerEntity serverPlayer : scoreboard.server.getPlayerManager().getPlayerList()) {
                updateObtainedCount(scoreboard, serverPlayer);
            }
        }
    }

    public void setObtainedCount(@NotNull ServerPlayerEntity player, int obtainedCount) {
        if (isNotReady()) {
            return;
        }
        UUID playerUuid = player.getUuid();
        int oldCount = obtainedAdvancementsCountByPlayers.getOrDefault(playerUuid, Integer.MIN_VALUE);
        obtainedAdvancementsCountByPlayers.put(playerUuid, obtainedCount);
        Set<LandmarkType> unlockedLandmarkTypes = null;
        Map<LandmarkType, Set<DimensionalBlockBox>> lockedLandmarks = null;

        for (AbilityType abilityType : AbilityType.values()) {
            int requiredCount = abilitiesConfiguration.get(abilityType);
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
            ) {
                continue;
            }
            boolean isLock;
            if (obtainedCount >= requiredCount && oldCount < requiredCount) {
                isLock = false;
            } else if (oldCount == Integer.MIN_VALUE || obtainedCount < requiredCount && oldCount >= requiredCount) {
                isLock = true;
            } else {
                continue;
            }
            setAbilityLocked(player, abilityType, isLock);
            LandmarkType landmarkType = abilityType.getLandmarkType();
            if (landmarkType != null) {
                if (isLock) {
                    playersByLockedLandmarkTypes
                        .computeIfAbsent(landmarkType, k -> new HashSet<>())
                        .add(playerUuid);
                    for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                        Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                        if (dimensionalBlockBoxes != null) {
                            if (lockedLandmarks == null) {
                                lockedLandmarks = new HashMap<>();
                            }
                            lockedLandmarks.put(landmarkType, dimensionalBlockBoxes);
                        }
                    }
                } else {
                    Set<UUID> players = playersByLockedLandmarkTypes.get(landmarkType);
                    if (players != null && players.remove(playerUuid) && players.isEmpty()) {
                        playersByLockedLandmarkTypes.remove(landmarkType);
                    }
                    if (unlockedLandmarkTypes == null) {
                        unlockedLandmarkTypes = new HashSet<>();
                    }
                    unlockedLandmarkTypes.add(landmarkType);
                }
            }
        }
        ServerPlayNetworking.send(player, new SyncObtainedAdvancementsCountPayload(obtainedCount));
        if (unlockedLandmarkTypes != null) {
            ServerPlayNetworking.send(player, new LandmarkTypesUnlockedPayload(unlockedLandmarkTypes));
        }
        if (lockedLandmarks != null) {
            ServerPlayNetworking.send(player, new LandmarksLockedStatusChangedPayload(lockedLandmarks, true));
        }
    }

    public void setScore(@NotNull ServerPlayerEntity player, @NotNull TrackedScoreType progressType, int progress) {
        if (progressType.isPercentage()) {
            progress = Math.max(0, Math.min(100, (int) ((progress * 100.0) / progressType.getFinalValue())));
        }
        var progressByPlayers = trackedScores.computeIfAbsent(progressType, k -> new Object2IntOpenHashMap<>());
        Integer currentProgress = progressByPlayers.get(player.getUuid());
        if (currentProgress == null || !currentProgress.equals(progress)) {
            progressByPlayers.put(player.getUuid(), progress);
            ServerPlayNetworking.send(player, new ScoreProgressChangedPayload(progressType, progress));
        }
    }

    public void setStat(@NotNull ServerPlayerEntity player, @NotNull TrackedStatisticsDataType statType, int progress) {
        if (statType.isPercentage()) {
            progress = Math.max(0, Math.min(100, (int) ((progress * 100.0) / statType.getFinalValue())));
        }
        var progressByPlayers = trackedStats.computeIfAbsent(statType, k -> new Object2IntOpenHashMap<>());
        Integer currentProgress = progressByPlayers.get(player.getUuid());
        if (currentProgress == null || !currentProgress.equals(progress)) {
            progressByPlayers.put(player.getUuid(), progress);
            ServerPlayNetworking.send(player, new StatisticsDataProgressChangedPayload(statType, progress));
        }
    }

    public boolean isNotReady() {
        return abilitiesConfiguration == null ||
            currentAdvancementsMode == null ||
            currentScoreboardObjective == null ||
            currentScoreboardDisplaySlot == null;
    }

    public boolean isAbilityLocked(@NotNull ServerPlayerEntity player, @NotNull AbilityType abilityType) {
        return isAbilityLocked(player, abilityType, false);
    }

    public boolean isAbilityLocked(
        @NotNull ServerPlayerEntity player,
        @NotNull AbilityType abilityType,
        boolean checkOnly
    ) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (isNotReady()) {
            return true;
        }
        int obtainedCount = obtainedAdvancementsCountByPlayers.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
        int requiredCount = abilitiesConfiguration.get(abilityType);
        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
            requiredCount != Constants.Progression.PERMANENTLY_LOCKED_FLAG && obtainedCount >= requiredCount
        ) {
            return false;
        }
        if (!checkOnly) {
            Text lockedMessageText;
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                lockedMessageText = abilityType.buildPermanentlyLockedMessage();
            } else {
                int leftCount = requiredCount - obtainedCount;
                lockedMessageText = abilityType.buildUnlockProgressMessage(leftCount);
            }
            player.sendMessage(lockedMessageText, true);
            demystifyAbility(player, abilityType);
        }
        return true;
    }

    public boolean isTargetInLockedLandmark(
        @NotNull ServerPlayerEntity actor,
        @NotNull DimensionType targetDimensionType,
        @NotNull Box targetBox
    ) {
        BlockBox targetBlockBox = Utils.toBlockBox(targetBox);
        for (var entry : playersByLockedLandmarkTypes.entrySet()) {
            if (entry.getValue().contains(actor.getUuid())) {
                LandmarkType landmarkType = entry.getKey();
                for (Map<LandmarkType, Set<DimensionalBlockBox>> landmarks : landmarksByChunks.values()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = landmarks.get(landmarkType);
                    if (dimensionalBlockBoxes != null) {
                        for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                            if (dimensionalBlockBox.dimensionType() == targetDimensionType &&
                                dimensionalBlockBox.blockBox().intersects(targetBlockBox)
                            ) {
                                AbilityType abilityType = AbilityType.findByLandmarkType(landmarkType);
                                if (abilityType != null) {
                                    boolean isAbilityLocked = isAbilityLocked(actor, abilityType, true);
                                    if (isAbilityLocked) {
                                        ServerPlayNetworking.send(actor, new CheckTargetInLockedLandmarkPayload(
                                            targetDimensionType,
                                            targetBox
                                        ));
                                    }
                                    return isAbilityLocked;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void onLandmarksLoadedStatusChanged(
        @NotNull ServerWorld world,
        @NotNull ChunkPos chunkPos,
        @NotNull Map<LandmarkType, Set<DimensionalBlockBox>> landmarks,
        boolean isLoaded
    ) {
        if (isNotReady()) {
            return;
        }
        boolean isChanged = false;
        if (isLoaded) {
            var chunkMap = landmarksByChunks.computeIfAbsent(chunkPos, k -> new HashMap<>());
            for (var entry : landmarks.entrySet()) {
                LandmarkType landmarkType = entry.getKey();
                AbilityType abilityType = AbilityType.findByLandmarkType(landmarkType);
                if (abilitiesConfiguration.get(abilityType) == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
                    continue;
                }
                if (chunkMap
                    .computeIfAbsent(landmarkType, k -> new HashSet<>())
                    .addAll(entry.getValue())
                ) {
                    isChanged = true;
                }
            }
        } else {
            Map<LandmarkType, Set<DimensionalBlockBox>> chunkMap = landmarksByChunks.get(chunkPos);
            if (chunkMap != null) {
                for (var entry : landmarks.entrySet()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = chunkMap.get(entry.getKey());
                    if (dimensionalBlockBoxes != null && dimensionalBlockBoxes.removeAll(entry.getValue())) {
                        isChanged = true;
                        if (dimensionalBlockBoxes.isEmpty()) {
                            chunkMap.remove(entry.getKey());
                            if (chunkMap.isEmpty()) {
                                landmarksByChunks.remove(chunkPos);
                            }
                        }
                    }
                }
            }
        }
        if (!isChanged) {
            return;
        }
        HashMap<UUID, Set<LandmarkType>> landmarkTypesByPlayers = new HashMap<>();
        for (LandmarkType type : landmarks.keySet()) {
            Set<UUID> playerUuids = playersByLockedLandmarkTypes.get(type);
            if (playerUuids == null) {
                continue;
            }
            for (UUID playerUuid : playerUuids) {
                landmarkTypesByPlayers
                    .computeIfAbsent(playerUuid, k -> new HashSet<>())
                    .add(type);
            }
        }
        PlayerManager playerManager = world.getServer().getPlayerManager();
        for (UUID playerUuid : landmarkTypesByPlayers.keySet()) {
            ServerPlayerEntity player = playerManager.getPlayer(playerUuid);
            if (player == null) {
                continue;
            }
            Set<LandmarkType> landmarkTypes = landmarkTypesByPlayers.get(playerUuid);
            HashMap<LandmarkType, Set<DimensionalBlockBox>> landmarksToSync = new HashMap<>();
            for (LandmarkType landmarkType : landmarkTypes) {
                landmarksToSync
                    .computeIfAbsent(landmarkType, k -> new HashSet<>())
                    .addAll(landmarks.get(landmarkType));
            }
            ServerPlayNetworking.send(player, new LandmarksLockedStatusChangedPayload(landmarksToSync, isLoaded));
        }
    }

    public void onLandmarkResized(
        @NotNull ServerWorld world,
        @NotNull ChunkPos chunkPos,
        LandmarkType landmarkType,
        DimensionalBlockBox oldDimensionalBlockBox,
        DimensionalBlockBox newDimensionalBlockBox
    ) {
        Map<LandmarkType, Set<DimensionalBlockBox>> chunkMap = landmarksByChunks.get(chunkPos);
        if (chunkMap == null) {
            return;
        }
        Set<DimensionalBlockBox> dimensionalBlockBoxes = chunkMap.get(landmarkType);
        if (dimensionalBlockBoxes == null) {
            return;
        }
        if (!dimensionalBlockBoxes.remove(oldDimensionalBlockBox)) {
            return;
        }
        dimensionalBlockBoxes.add(newDimensionalBlockBox);
        Set<UUID> playerUuids = playersByLockedLandmarkTypes.get(landmarkType);
        if (playerUuids == null) {
            return;
        }
        PlayerManager playerManager = world.getServer().getPlayerManager();
        for (UUID playerUuid : playerUuids) {
            ServerPlayerEntity player = playerManager.getPlayer(playerUuid);
            if (player == null) {
                continue;
            }
            ServerPlayNetworking.send(player, new LockedLandmarkResizedPayload(
                landmarkType,
                oldDimensionalBlockBox.dimensionType(),
                oldDimensionalBlockBox.blockBox(),
                newDimensionalBlockBox.blockBox()
            ));
        }
    }

    @Override
    public void onInitializeServer() {
        registerInternalDataPacks();
        registerPayloads();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (server.getSaveProperties().getLevelInfo() instanceof LevelInfoExtension levelInfoExtension) {
                abilitiesConfiguration = levelInfoExtension.achievetodo$getAbilitiesConfiguration(
                    server.getSaveProperties().getGeneratorOptions().getSeed()
                );
                AchieveToDoMod.logger.info("Abilities configuration loaded");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            abilitiesConfiguration = null;
            obtainedAdvancementsCountByPlayers.clear();
            landmarksByChunks.clear();
            playersByLockedLandmarkTypes.clear();
            trackedScores.clear();
            trackedStats.clear();
            currentAdvancementsMode = null;
            currentScoreboardObjective = null;
            currentScoreboardDisplaySlot = null;
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> onChunkLoadedStatusChanged(world, chunk, true));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> onChunkLoadedStatusChanged(world, chunk, false));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            if (isNotReady()) {
                player.networkHandler.disconnect(Text.literal(AchieveToDoMod.NAME + " is not ready yet"));
                return;
            }
            ServerPlayNetworking.send(player, new SyncAbilitiesConfigurationPayload(abilitiesConfiguration));
            updateObtainedCount(server.getScoreboard(), player);
            ScoreHolder scoreHolder = ScoreHolder.fromName(player.getNameForScoreboard());
            Scoreboard scoreboard = player.getScoreboard();
            for (var entry : TrackedScoreType.SCORES.entrySet()) {
                ReadableScoreboardScore scoreboardScore = scoreboard.getScore(
                    scoreHolder, scoreboard.getNullableObjective(entry.getKey())
                );
                if (scoreboardScore != null) {
                    int score = scoreboardScore.getScore();
                    for (TrackedScoreType type : entry.getValue()) {
                        setScore(player, type, type.fixScore(scoreboard, scoreHolder, score));
                    }
                }
            }
            ServerStatHandler serverStatHandler = player.getStatHandler();
            for (var entry : TrackedStatisticsDataType.STATISTICS_DATA.entrySet()) {
                int statValue = serverStatHandler.getStat(entry.getKey());
                for (TrackedStatisticsDataType trackedStatisticsDataType : entry.getValue()) {
                    setStat(player, trackedStatisticsDataType, statValue);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            obtainedAdvancementsCountByPlayers.remove(playerUuid);
            for (Map<UUID, Integer> players : trackedScores.values()) {
                players.remove(playerUuid);
            }
            for (Map<UUID, Integer> players : trackedStats.values()) {
                players.remove(playerUuid);
            }
            for (Set<UUID> players : playersByLockedLandmarkTypes.values()) {
                players.remove(playerUuid);
            }
        });
    }

    private void registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(DemystifyAbilityPayload.ID, (payload, context) ->
            context.player().server.execute(() -> demystifyAbility(context.player(), payload.abilityType()))
        );
    }

    private void onChunkLoadedStatusChanged(@NotNull ServerWorld world, @NotNull Chunk chunk, boolean isLoaded) {
        DimensionType dimensionType = DimensionType.findByWorld(world.getRegistryKey());
        if (dimensionType == null) {
            return;
        }
        Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = null;
        for (StructureStart structureStart : chunk.getStructureStarts().values()) {
            if (structureStart instanceof StructureStartExtension structureStartExtension) {
                LandmarkType landmarkType = structureStartExtension.achievetodo$getLandmarkType();
                BlockBox landmarkBlockBox = structureStartExtension.achievetodo$getLandmarkBlockBox();
                if (landmarkType != null && landmarkBlockBox != null) {
                    if (landmarks == null) {
                        landmarks = new HashMap<>();
                    }
                    landmarks
                        .computeIfAbsent(landmarkType, k -> new HashSet<>())
                        .add(new DimensionalBlockBox(dimensionType, landmarkBlockBox));
                }
            }
        }
        if (chunk instanceof ChunkExtension chunkExtension) {
            Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks =
                chunkExtension.achievetodo$getFeatureLandmarks();
            if (featureLandmarks != null) {
                if (landmarks == null) {
                    landmarks = new HashMap<>();
                }
                landmarks.putAll(featureLandmarks);
            }
        }
        if (landmarks != null) {
            onLandmarksLoadedStatusChanged(world, chunk.getPos(), landmarks, isLoaded);
        }
    }

    private void updateObtainedCount(ServerScoreboard scoreboard, @NotNull ServerPlayerEntity player) {
        if (isNotReady()) {
            return;
        }
        int count = 0;
        String playerName = player.getNameForScoreboard();
        if (currentAdvancementsMode.isTeamsMode()) {
            Team team = scoreboard.getScoreHolderTeam(playerName);
            if (team == null) {
                AchieveToDoMod.logger.warn("Player [{}] is not a member of any team!", playerName);
                return;
            }
            for (String teamMemberName : team.getPlayerList()) {
                ReadableScoreboardScore teamMemberScore = scoreboard.getScore(
                    ScoreHolder.fromName(teamMemberName),
                    currentScoreboardObjective
                );
                if (teamMemberScore != null) {
                    count += teamMemberScore.getScore();
                }
            }
        } else {
            ReadableScoreboardScore playerScore = scoreboard.getScore(
                ScoreHolder.fromName(playerName),
                currentScoreboardObjective
            );
            if (playerScore != null) {
                count = playerScore.getScore();
            }
        }
        setObtainedCount(player, count);
    }

    private void demystifyAbility(@NotNull ServerPlayerEntity player, @NotNull AbilityType ability) {
        AdvancementEntry advancement = player.server.getAdvancementLoader()
            .get(AbilityAdvancementsGenerator.buildAdvancementId(ability));
        player.getAdvancementTracker().grantCriterion(
            advancement,
            AbilityAdvancementsGenerator.DEMYSTIFIED_CRITERION
        );
    }

    private void setAbilityLocked(@NotNull ServerPlayerEntity player, @NotNull AbilityType ability, boolean isLocked) {
        AdvancementEntry advancement = player.server.getAdvancementLoader()
            .get(AbilityAdvancementsGenerator.buildAdvancementId(ability));
        PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
        if (isLocked) {
            advancementTracker.revokeCriterion(advancement, AbilityAdvancementsGenerator.UNLOCKED_CRITERION);
        } else {
            for (String criterion : advancementTracker.getProgress(advancement).getUnobtainedCriteria()) {
                advancementTracker.grantCriterion(advancement, criterion);
            }
        }
    }
}
