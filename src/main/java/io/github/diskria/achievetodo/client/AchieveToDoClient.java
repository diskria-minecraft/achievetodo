package io.github.diskria.achievetodo.client;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.*;
import io.github.diskria.achievetodo.client.gui.LockedLandmarkBox;
import io.github.diskria.achievetodo.payloads.clientbound.*;
import io.github.diskria.achievetodo.payloads.serverbound.DemystifyAbilityPayload;
import io.github.diskria.achievetodo.server.Constants;
import io.github.diskria.achievetodo.tracking.TrackedNearbyEntitiesType;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import kotlin.Unit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AchieveToDoClient implements ClientModInitializer {

    private static final Map<LandmarkType, Set<DimensionalBlockBox>> lockedLandmarkBlockBoxes = new HashMap<>();
    private static final Map<TrackedScoreType, Integer> trackedScores = new Object2IntOpenHashMap<>();
    private static final Map<TrackedStatisticsDataType, Integer> trackedStatisticsData = new Object2IntOpenHashMap<>();
    private static final List<List<AbilityType>> abilityRows = new ArrayList<>();
    private static Map<AbilityType, Integer> abilitiesConfiguration;
    private static int obtainedAdvancementsCount = Integer.MIN_VALUE;
    private static List<LockedLandmarkBox> lockedLandmarkBoxes = new ArrayList<>();

    public static int getRequiredAdvancementsCount(AbilityType ability) {
        return abilitiesConfiguration.get(ability);
    }

    public static boolean isNotReady() {
        return abilitiesConfiguration == null || obtainedAdvancementsCount == Integer.MIN_VALUE;
    }

    public static int getObtainedAdvancementsCount() {
        return obtainedAdvancementsCount;
    }

    public static int getTrackedScore(@NotNull TrackedScoreType progressType) {
        return trackedScores.getOrDefault(progressType, 0);
    }

    public static int getTrackedStatisticsData(@NotNull TrackedStatisticsDataType statType) {
        return trackedStatisticsData.getOrDefault(statType, 0);
    }

    public static List<LockedLandmarkBox> getLockedLandmarkBoxes() {
        return lockedLandmarkBoxes;
    }

    public static int getTrackedNearbyEntitiesCount(TrackedNearbyEntitiesType type) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return 0;
        }
        int radius = type.getRadius();
        Vec3 playerPos = player.position();
        AABB area = new AABB(playerPos, playerPos).inflate(radius);
        Set<EntityType<?>> trackedEntities = type.getEntities();
        boolean isBabySeparated = type.isBabySeparated();
        Map<EntityType<?>, Map<Boolean, Boolean>> trackedMap = new HashMap<>();
        for (EntityType<?> entityType : trackedEntities) {
            Map<Boolean, Boolean> babyAndAdultMap = new HashMap<>();
            babyAndAdultMap.put(false, false);
            if (isBabySeparated) {
                babyAndAdultMap.put(true, false);
            }
            trackedMap.put(entityType, babyAndAdultMap);
        }
        int entitiesCount = 0;
        List<Entity> entities = player.level().getEntitiesOfClass(
            Entity.class,
            area,
            entity -> trackedEntities.contains(entity.getType())
        );
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                if (playerPos.distanceTo(entity.position()) > radius) {
                    continue;
                }
                Map<Boolean, Boolean> stateMap = trackedMap.get(entity.getType());
                if (stateMap != null) {
                    boolean isBaby = isBabySeparated && livingEntity.isBaby();
                    if (!stateMap.getOrDefault(isBaby, false)) {
                        stateMap.put(isBaby, true);
                        entitiesCount++;
                    }
                }
            }
        }
        return entitiesCount;
    }

    public static @NotNull MutableComponent translate(String keySuffix, Object... args) {
        return Component.translatable(AchieveToDoMod.ID + "." + keySuffix, args);
    }

    public static boolean isAbilityLocked(@NotNull AbilityType ability) {
        return isAbilityLocked(ability, false);
    }

    public static boolean isAbilityLocked(@NotNull AbilityType abilityType, boolean checkOnly) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (isNotReady()) {
            return true;
        }
        int requiredCount = abilitiesConfiguration.get(abilityType);
        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
            return false;
        }
        if (requiredCount != Constants.Progression.PERMANENTLY_LOCKED_FLAG &&
            obtainedAdvancementsCount >= requiredCount
        ) {
            return false;
        }
        if (!checkOnly) {
            Component lockedMessageText;
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                lockedMessageText = abilityType.buildPermanentlyLockedMessage();
            } else {
                int leftCount = requiredCount - obtainedAdvancementsCount;
                lockedMessageText = abilityType.buildUnlockProgressMessage(leftCount);
            }
            player.sendOverlayMessage(lockedMessageText);
            DemystifyAbilityPayload.ClientBound.send(abilityType);
        }
        return true;
    }

    public static boolean isTargetInLockedLandmark(@NotNull DimensionType targetDimensionType, @NotNull BlockBox targetBox) {
        for (LockedLandmarkBox lockedLandmarksBox : lockedLandmarkBoxes) {
            if (lockedLandmarksBox.dimensionType() == targetDimensionType &&
                lockedLandmarksBox.box().intersects(targetBox)
            ) {
                AbilityType abilityType = AbilityType.findByLandmarkType(lockedLandmarksBox.landmarkType());
                if (abilityType != null) {
                    return isAbilityLocked(abilityType, false);
                }
            }
        }
        return false;
    }

    public static @Nullable List<List<AbilityType>> getAbilityRows() {
        if (isNotReady()) {
            return null;
        }
        if (abilityRows.isEmpty()) {
            Map<AbilitiesHierarchyLayerType, List<AbilityType>> abilitiesByCategory = Arrays
                .stream(AbilityType.values())
                .sorted(Comparator.comparingInt((AbilityType ability) -> {
                        int requiredCount = abilitiesConfiguration.get(ability);
                        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
                            return 0;
                        }
                        if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                            return 2;
                        }
                        return 1;
                    })
                    .thenComparingInt(abilitiesConfiguration::get)
                    .thenComparing(Enum::ordinal))
                .collect(Collectors.groupingBy(AbilityType::getHierarchyLayerType));
            for (AbilitiesHierarchyLayerType category : AbilitiesHierarchyLayerType.values()) {
                List<AbilityType> categoryAbilities = abilitiesByCategory.get(category);
                int categoryRowsCount = category.getRowsCount();
                if (categoryRowsCount == 1) {
                    abilityRows.add(categoryAbilities);
                } else {
                    if (categoryAbilities.size() % categoryRowsCount != 0) {
                        throw new IllegalStateException(
                            "Abilities in category " + category +
                                " cannot be evenly distributed across " + categoryRowsCount + " rows."
                        );
                    }
                    int rowLength = categoryAbilities.size() / categoryRowsCount;
                    List<List<AbilityType>> categoryRows = new ArrayList<>(categoryRowsCount);
                    for (int i = 0; i < categoryRowsCount; i++) {
                        categoryRows.add(new ArrayList<>(rowLength));
                    }
                    for (int i = 0; i < categoryAbilities.size(); i++) {
                        int rowIndex = i / rowLength;
                        categoryRows.get(rowIndex).add(categoryAbilities.get(i));
                    }
                    int half = categoryRowsCount / 2;
                    abilityRows.addAll(0, categoryRows.subList(0, half));
                    abilityRows.addAll(categoryRows.subList(half, categoryRowsCount));
                }
            }
        }
        return abilityRows;
    }

    @Override
    public void onInitializeClient() {
        registerPayloads();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            abilitiesConfiguration = null;
            obtainedAdvancementsCount = Integer.MIN_VALUE;
            lockedLandmarkBlockBoxes.clear();
            lockedLandmarkBoxes.clear();
            trackedScores.clear();
            trackedStatisticsData.clear();
        });
    }

    private void registerPayloads() {
        SyncAbilitiesConfigurationPayload.ClientBound.listen((context, config) -> {
            abilitiesConfiguration = config;
            abilityRows.clear();
            calculateLockedLandmarkBoxes();
            return Unit.INSTANCE;
        });
        SyncObtainedAdvancementsCountPayload.ClientBound.listen((context, count) -> {
            obtainedAdvancementsCount = count;
            return Unit.INSTANCE;
        });
        LandmarksLockedStatusChangedPayload.ClientBound.listen((context, landmarks, isLocked) -> {
            boolean isChanged = false;
            for (var entry : landmarks.entrySet()) {
                LandmarkType landmarkType = entry.getKey();
                if (isLocked) {
                    if (lockedLandmarkBlockBoxes
                        .computeIfAbsent(landmarkType, k -> new HashSet<>())
                        .addAll(entry.getValue())
                    ) {
                        isChanged = true;
                    }
                } else {
                    Set<DimensionalBlockBox> dimensionalBoxes = lockedLandmarkBlockBoxes.get(landmarkType);
                    if (dimensionalBoxes != null && dimensionalBoxes.removeAll(entry.getValue())) {
                        isChanged = true;
                        if (dimensionalBoxes.isEmpty()) {
                            lockedLandmarkBlockBoxes.remove(landmarkType);
                        }
                    }
                }
            }
            if (isChanged) {
                calculateLockedLandmarkBoxes();
            }
            return Unit.INSTANCE;
        });
        LandmarkTypesUnlockedPayload.ClientBound.listen((context, landmarks) -> {
            boolean isChanged = false;
            for (LandmarkType landmarkType : landmarks) {
                if (lockedLandmarkBlockBoxes.remove(landmarkType) != null) {
                    isChanged = true;
                }
            }
            if (isChanged) {
                calculateLockedLandmarkBoxes();
            }
            return Unit.INSTANCE;
        });
        LockedLandmarkResizedPayload.ClientBound.listen((context, landmark, dimension, oldBox, newBox) -> {
            Set<DimensionalBlockBox> dimensionalBlockBoxes = lockedLandmarkBlockBoxes.get(landmark);
            if (dimensionalBlockBoxes.remove(new DimensionalBlockBox(dimension, oldBox))) {
                dimensionalBlockBoxes.add(new DimensionalBlockBox(dimension, newBox));
                calculateLockedLandmarkBoxes();
            }
            return Unit.INSTANCE;
        });
        ScoreProgressChangedPayload.ClientBound.listen((context, score, progress) -> {
            trackedScores.put(score, progress);
            return Unit.INSTANCE;
        });
        StatisticsDataProgressChangedPayload.ClientBound.listen((context, statisticsData, progress) -> {
            trackedStatisticsData.put(statisticsData, progress);
            return Unit.INSTANCE;
        });
        CheckTargetInLockedLandmarkPayload.ClientBound.listen((context, dimensionType, box) -> {
            isTargetInLockedLandmark(dimensionType, box);
            return Unit.INSTANCE;
        });
    }

    private void calculateLockedLandmarkBoxes() {
        if (abilitiesConfiguration == null) {
            return;
        }
        List<LockedLandmarkBox> result = new ArrayList<>();
        for (var entry : lockedLandmarkBlockBoxes.entrySet()) {
            LandmarkType landmarkType = entry.getKey();
            for (DimensionalBlockBox dimensionalBlockBox : entry.getValue()) {
                result.add(new LockedLandmarkBox(
                    landmarkType,
                    dimensionalBlockBox.dimensionType(),
                    Box.from(dimensionalBlockBox.blockBox)
                ));
            }
        }
        result.sort((lockedLandmarkBox, otherLockedLandmarkBox) -> {
            DimensionType dimensionType = lockedLandmarkBox.dimensionType();
            DimensionType otherDimensionType = otherLockedLandmarkBox.dimensionType();
            int dimensionTypeComparison = dimensionType.compareTo(otherDimensionType);
            if (dimensionTypeComparison != 0) {
                return dimensionTypeComparison;
            }

            LandmarkType landmarkType = lockedLandmarkBox.landmarkType();
            LandmarkType otherLandmarkType = otherLockedLandmarkBox.landmarkType();
            AbilityType abilityType = AbilityType.findByLandmarkType(landmarkType);
            AbilityType otherAbilityType = AbilityType.findByLandmarkType(otherLandmarkType);

            int requiredCount = abilitiesConfiguration.get(abilityType);
            int otherRequiredCount = abilitiesConfiguration.get(otherAbilityType);
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                requiredCount = Integer.MAX_VALUE;
            }
            if (otherRequiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                otherRequiredCount = Integer.MAX_VALUE;
            }
            int requiredCountComparison = Integer.compare(otherRequiredCount, requiredCount);
            if (requiredCountComparison != 0) {
                return requiredCountComparison;
            }
            return Integer.compare(otherLandmarkType.ordinal(), landmarkType.ordinal());
        });
        lockedLandmarkBoxes = result;
    }
}
