package io.github.diskria.achievetodo.client;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.*;
import io.github.diskria.achievetodo.client.gui.LockedLandmarkBox;
import io.github.diskria.achievetodo.networking.c2s.DemystifyAbilityPayload;
import io.github.diskria.achievetodo.networking.s2c.*;
import io.github.diskria.achievetodo.server.Constants;
import io.github.diskria.achievetodo.tracking.TrackedNearbyEntitiesType;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator()) {
            return 0;
        }
        int radius = type.getRadius();
        Vec3d playerPos = player.getPos();
        Box area = new Box(playerPos, playerPos).expand(radius);
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
        List<Entity> entities = player.getWorld().getEntitiesByType(
            TypeFilter.instanceOf(Entity.class),
            area,
            entity -> trackedEntities.contains(entity.getType())
        );
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                if (playerPos.distanceTo(entity.getPos()) > radius) {
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

    public static @NotNull MutableText translate(String keySuffix, Object... args) {
        return Text.translatable(AchieveToDoMod.ID + "." + keySuffix, args);
    }

    public static boolean isAbilityLocked(@NotNull AbilityType ability) {
        return isAbilityLocked(ability, false);
    }

    public static boolean isAbilityLocked(@NotNull AbilityType abilityType, boolean checkOnly) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
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
            Text lockedMessageText;
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                lockedMessageText = abilityType.buildPermanentlyLockedMessage();
            } else {
                int leftCount = requiredCount - obtainedAdvancementsCount;
                lockedMessageText = abilityType.buildUnlockProgressMessage(leftCount);
            }
            player.sendMessage(lockedMessageText, true);
            ClientPlayNetworking.send(new DemystifyAbilityPayload(abilityType));
        }
        return true;
    }

    public static boolean isTargetInLockedLandmark(@NotNull DimensionType targetDimensionType, @NotNull Box targetBox) {
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
        ClientPlayNetworking.registerGlobalReceiver(SyncAbilitiesConfigurationPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                abilitiesConfiguration = payload.abilitiesConfiguration();
                abilityRows.clear();
                calculateLockedLandmarkBoxes();
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(SyncObtainedAdvancementsCountPayload.ID, (payload, context) ->
            context.client().execute(() -> obtainedAdvancementsCount = payload.obtainedAdvancementsCount())
        );
        ClientPlayNetworking.registerGlobalReceiver(LandmarksLockedStatusChangedPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                boolean isChanged = false;
                for (var entry : payload.landmarks().entrySet()) {
                    LandmarkType landmarkType = entry.getKey();
                    if (payload.isLocked()) {
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
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(LandmarkTypesUnlockedPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                boolean isChanged = false;
                for (LandmarkType landmarkType : payload.unlockedLandmarkTypes()) {
                    if (lockedLandmarkBlockBoxes.remove(landmarkType) != null) {
                        isChanged = true;
                    }
                }
                if (isChanged) {
                    calculateLockedLandmarkBoxes();
                }
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(LockedLandmarkResizedPayload.ID, (payload, context) ->
            context.client().execute(() -> {
                Set<DimensionalBlockBox> dimensionalBlockBoxes = lockedLandmarkBlockBoxes.get(payload.landmarkType());
                DimensionType dimensionType = payload.dimensionType();
                if (dimensionalBlockBoxes.remove(new DimensionalBlockBox(dimensionType, payload.oldBlockBox()))) {
                    dimensionalBlockBoxes.add(new DimensionalBlockBox(dimensionType, payload.newBlockBox()));
                    calculateLockedLandmarkBoxes();
                }
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(ScoreProgressChangedPayload.ID, (payload, context) ->
            context.client().execute(() -> trackedScores.put(payload.progressType(), payload.progress()))
        );
        ClientPlayNetworking.registerGlobalReceiver(StatisticsDataProgressChangedPayload.ID, (payload, context) ->
            context.client().execute(() -> trackedStatisticsData.put(
                payload.trackedStatisticsDataType(),
                payload.newProgress()
            ))
        );
        ClientPlayNetworking.registerGlobalReceiver(CheckTargetInLockedLandmarkPayload.ID, (payload, context) ->
            context.client().execute(() -> isTargetInLockedLandmark(payload.targetDimensionType(), payload.targetBox()))
        );
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
                    Box.from(dimensionalBlockBox.blockBox())
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
