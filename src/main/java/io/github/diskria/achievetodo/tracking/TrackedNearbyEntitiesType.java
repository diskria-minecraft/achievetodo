package io.github.diskria.achievetodo.tracking;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TrackedNearbyEntitiesType {

    ANIMAL_KINGDOM(
        "blazeandcave:animal/animal_kingdom",
        32,
        false,
        EntityTypes.AXOLOTL,
        EntityTypes.BAT,
        EntityTypes.CAT,
        EntityTypes.CHICKEN,
        EntityTypes.COD,
        EntityTypes.COW,
        EntityTypes.DONKEY,
        EntityTypes.FOX,
        EntityTypes.FROG,
        EntityTypes.GLOW_SQUID,
        EntityTypes.HORSE,
        EntityTypes.MOOSHROOM,
        EntityTypes.MULE,
        EntityTypes.OCELOT,
        EntityTypes.PARROT,
        EntityTypes.PIG,
        EntityTypes.PUFFERFISH,
        EntityTypes.RABBIT,
        EntityTypes.SALMON,
        EntityTypes.SHEEP,
        EntityTypes.SQUID,
        EntityTypes.STRIDER,
        EntityTypes.TROPICAL_FISH,
        EntityTypes.TURTLE,
        EntityTypes.BEE,
        EntityTypes.DOLPHIN,
        EntityTypes.GOAT,
        EntityTypes.LLAMA,
        EntityTypes.PANDA,
        EntityTypes.POLAR_BEAR,
        EntityTypes.WOLF,
        EntityTypes.CAMEL,
        EntityTypes.SNIFFER,
        EntityTypes.SKELETON_HORSE,
        EntityTypes.ARMADILLO,
        EntityTypes.TADPOLE,
        EntityTypes.HOGLIN
    ),
    FAMILY_REUNION(
        "blazeandcave:monsters/family_reunion",
        5,
        true,
        EntityTypes.HUSK,
        EntityTypes.ZOMBIE_VILLAGER,
        EntityTypes.DROWNED,
        EntityTypes.ZOMBIFIED_PIGLIN,
        EntityTypes.ZOMBIE
    ),
    BONE_TO_PARTY(
        "blazeandcave:monsters/bone_to_party",
        5,
        false,
        EntityTypes.SKELETON_HORSE,
        EntityTypes.WITHER,
        EntityTypes.STRAY,
        EntityTypes.BOGGED,
        EntityTypes.WITHER_SKELETON,
        EntityTypes.SKELETON
    );

    private final String advancementId;
    private final int radius;
    private final boolean isBabySeparated;
    private final Set<EntityType<?>> entities;

    TrackedNearbyEntitiesType(
        String advancementId,
        int radius,
        boolean isBabySeparated,
        EntityType<?>... entities
    ) {
        this.advancementId = advancementId;
        this.radius = radius;
        this.isBabySeparated = isBabySeparated;
        this.entities = Arrays.stream(entities).collect(Collectors.toUnmodifiableSet());
    }

    public static @Nullable TrackedNearbyEntitiesType findByAdvancement(@NotNull Identifier advancementId) {
        for (TrackedNearbyEntitiesType type : TrackedNearbyEntitiesType.values()) {
            if (advancementId.toString().equals(type.advancementId)) {
                return type;
            }
        }
        return null;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isBabySeparated() {
        return isBabySeparated;
    }

    public Set<EntityType<?>> getEntities() {
        return entities;
    }

    public int getEntitiesCount() {
        int entitiesCount = entities.size();
        if (isBabySeparated) {
            entitiesCount *= 2;
        }
        return entitiesCount;
    }
}
