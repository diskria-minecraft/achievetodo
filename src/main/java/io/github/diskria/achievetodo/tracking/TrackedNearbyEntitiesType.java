package io.github.diskria.achievetodo.tracking;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
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
        EntityType.AXOLOTL,
        EntityType.BAT,
        EntityType.CAT,
        EntityType.CHICKEN,
        EntityType.COD,
        EntityType.COW,
        EntityType.DONKEY,
        EntityType.FOX,
        EntityType.FROG,
        EntityType.GLOW_SQUID,
        EntityType.HORSE,
        EntityType.MOOSHROOM,
        EntityType.MULE,
        EntityType.OCELOT,
        EntityType.PARROT,
        EntityType.PIG,
        EntityType.PUFFERFISH,
        EntityType.RABBIT,
        EntityType.SALMON,
        EntityType.SHEEP,
        EntityType.SQUID,
        EntityType.STRIDER,
        EntityType.TROPICAL_FISH,
        EntityType.TURTLE,
        EntityType.BEE,
        EntityType.DOLPHIN,
        EntityType.GOAT,
        EntityType.LLAMA,
        EntityType.PANDA,
        EntityType.POLAR_BEAR,
        EntityType.WOLF,
        EntityType.CAMEL,
        EntityType.SNIFFER,
        EntityType.SKELETON_HORSE,
        EntityType.ARMADILLO,
        EntityType.TADPOLE,
        EntityType.HOGLIN
    ),
    FAMILY_REUNION(
        "blazeandcave:monsters/family_reunion",
        5,
        true,
        EntityType.HUSK,
        EntityType.ZOMBIE_VILLAGER,
        EntityType.DROWNED,
        EntityType.ZOMBIFIED_PIGLIN,
        EntityType.ZOMBIE
    ),
    BONE_TO_PARTY(
        "blazeandcave:monsters/bone_to_party",
        5,
        false,
        EntityType.SKELETON_HORSE,
        EntityType.WITHER,
        EntityType.STRAY,
        EntityType.BOGGED,
        EntityType.WITHER_SKELETON,
        EntityType.SKELETON
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
