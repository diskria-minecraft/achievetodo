package io.github.diskria.achievetodo.ability;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum LandmarkType {

    DESERT_PYRAMID(
        StructureKeys.DESERT_PYRAMID
    ),
    DESERT_WELL(
        Feature.DESERT_WELL
    ),
    JUNGLE_PYRAMID(
        StructureKeys.JUNGLE_PYRAMID
    ),
    PILLAGER_OUTPOST(
        StructureKeys.PILLAGER_OUTPOST
    ),
    IGLOO(
        StructureKeys.IGLOO
    ),
    SWAMP_HUT(
        StructureKeys.SWAMP_HUT
    ),
    MANSION(
        StructureKeys.MANSION
    ),
    VILLAGE(
        StructureKeys.VILLAGE_PLAINS,
        StructureKeys.VILLAGE_DESERT,
        StructureKeys.VILLAGE_SAVANNA,
        StructureKeys.VILLAGE_SNOWY,
        StructureKeys.VILLAGE_TAIGA
    ),
    RUINED_PORTAL(
        StructureKeys.RUINED_PORTAL,
        StructureKeys.RUINED_PORTAL_DESERT,
        StructureKeys.RUINED_PORTAL_JUNGLE,
        StructureKeys.RUINED_PORTAL_SWAMP,
        StructureKeys.RUINED_PORTAL_MOUNTAIN,
        StructureKeys.RUINED_PORTAL_OCEAN
    ),
    BURIED_TREASURE(
        StructureKeys.BURIED_TREASURE
    ),
    SHIPWRECK(
        StructureKeys.SHIPWRECK,
        StructureKeys.SHIPWRECK_BEACHED
    ),
    OCEAN_RUIN(
        StructureKeys.OCEAN_RUIN_COLD,
        StructureKeys.OCEAN_RUIN_WARM
    ),
    MONUMENT(
        StructureKeys.MONUMENT
    ),
    MONSTER_ROOM(
        Feature.MONSTER_ROOM
    ),
    MINESHAFT(
        StructureKeys.MINESHAFT,
        StructureKeys.MINESHAFT_MESA
    ),
    TRAIL_RUINS(
        StructureKeys.TRAIL_RUINS
    ),
    ANCIENT_CITY(
        StructureKeys.ANCIENT_CITY
    ),
    TRIAL_CHAMBERS(
        StructureKeys.TRIAL_CHAMBERS
    ),
    STRONGHOLD(
        StructureKeys.STRONGHOLD
    ),
    FORTRESS(
        StructureKeys.FORTRESS
    ),
    BASTION_REMNANT(
        StructureKeys.BASTION_REMNANT
    ),
    END_CITY(
        StructureKeys.END_CITY
    );

    public static final HashMap<Feature<?>, LandmarkType> FEATURES = new HashMap<>();

    static {
        for (LandmarkType landmarkType : values()) {
            if (landmarkType.isFeature()) {
                FEATURES.put(landmarkType.getFeature(), landmarkType);
            }
        }
    }

    private final Set<RegistryKey<Structure>> structureRegistryKeys;
    private final Feature<?> feature;

    @SafeVarargs
    LandmarkType(RegistryKey<Structure>... structures) {
        this(null, Arrays.stream(structures).collect(Collectors.toUnmodifiableSet()));
    }

    LandmarkType(Feature<?> feature) {
        this(feature, null);
    }

    LandmarkType(Feature<?> feature, Set<RegistryKey<Structure>> structureRegistryKeys) {
        this.structureRegistryKeys = structureRegistryKeys;
        this.feature = feature;
    }

    public static @Nullable LandmarkType findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (LandmarkType landmarkType : values()) {
            if (landmarkType.name().equalsIgnoreCase(name)) {
                return landmarkType;
            }
        }
        return null;
    }

    public static @Nullable LandmarkType findByStructureRegistryKey(RegistryKey<Structure> structureRegistryKey) {
        if (structureRegistryKey == null) {
            return null;
        }
        for (LandmarkType type : values()) {
            if (type.isStructure() && type.getStructureRegistryKeys().contains(structureRegistryKey)) {
                return type;
            }
        }
        return null;
    }

    public boolean isStructure() {
        return structureRegistryKeys != null;
    }

    public boolean isFeature() {
        return feature != null;
    }

    public Set<RegistryKey<Structure>> getStructureRegistryKeys() {
        return structureRegistryKeys;
    }

    public Feature<?> getFeature() {
        return feature;
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
