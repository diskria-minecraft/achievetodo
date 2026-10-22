package io.github.diskria.achievetodo.ability;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.diskria.achievetodo.ability.AbilityType.*;

public class Progressions {

    public static @NotNull Map<AbilityType, Integer> getEasyProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        progression.put(VISION, 0);
        progression.put(JUMP, 0);
        addCycle(progression, 2,
            EAT_SALMON, SWIM, BREAK_BLOCKS, EAT_COD, SNEAK, EAT_TROPICAL_FISH, SPRINT, EAT_SWEET_BERRIES, OPEN_DOOR,
            EAT_ROTTEN_FLESH, SLEEP, EAT_SPIDER_EYE, OPEN_INVENTORY, PUT_IN_BUNDLE, EAT_GLOW_BERRIES,
            BREAK_BLOCKS_IN_NEGATIVE_Y, EAT_POISONOUS_POTATO, EAT_SUSPICIOUS_STEW, USE_GOLDEN_TOOLS, OPEN_CHEST,
            GET_INTO_BOAT
        );
        addFixedProgressionCycles(progression, 0);
        return progression;
    }

    public static @NotNull Map<AbilityType, Integer> getNormalProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        progression.put(VISION, 0);
        addCycle(progression, 2,
            EAT_SALMON, JUMP, EAT_COD, EAT_TROPICAL_FISH, SWIM, SNEAK, EAT_SWEET_BERRIES, SPRINT, EAT_ROTTEN_FLESH,
            OPEN_DOOR, EAT_SPIDER_EYE, SLEEP, EAT_GLOW_BERRIES, EAT_POISONOUS_POTATO, OPEN_INVENTORY, BREAK_BLOCKS,
            EAT_SUSPICIOUS_STEW, PUT_IN_BUNDLE, BREAK_BLOCKS_IN_NEGATIVE_Y, USE_GOLDEN_TOOLS, OPEN_CHEST, GET_INTO_BOAT
        );
        addFixedProgressionCycles(progression, 1);
        return progression;
    }

    public static @NotNull Map<AbilityType, Integer> getHardProgression() {
        Map<AbilityType, Integer> progression = new HashMap<>();
        addCycle(progression, 2,
            VISION, EAT_SALMON, EAT_COD, JUMP, EAT_TROPICAL_FISH, EAT_SWEET_BERRIES, SWIM, EAT_ROTTEN_FLESH,
            EAT_SPIDER_EYE, OPEN_DOOR, EAT_GLOW_BERRIES, SLEEP, EAT_POISONOUS_POTATO, SNEAK, SPRINT, OPEN_INVENTORY,
            BREAK_BLOCKS, EAT_SUSPICIOUS_STEW, USE_GOLDEN_TOOLS, OPEN_CHEST, BREAK_BLOCKS_IN_NEGATIVE_Y, GET_INTO_BOAT,
            PUT_IN_BUNDLE
        );
        addFixedProgressionCycles(progression, 2);
        return progression;
    }

    private static void addFixedProgressionCycles(Map<AbilityType, Integer> progression, int extraSpacing) {
        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            EAT_CHICKEN, OPEN_CRAFTING_TABLE, EAT_CARROT, EQUIP_GOLDEN_ARMOR, OPEN_BARREL, EAT_BEETROOT,
            EQUIP_LEATHER_ARMOR, EAT_POTATO, USE_WOODEN_TOOLS, EAT_DRIED_KELP, OPEN_STONECUTTER, EAT_RABBIT_STEW,
            USE_CAMPFIRE, INTERACT_INSIDE_SHIPWRECK, EAT_APPLE, USE_SHIELD, INTERACT_INSIDE_RUINED_PORTAL,
            EAT_MELON_SLICE, EQUIP_CHAINMAIL_ARMOR, OPEN_FURNACE, EAT_COOKIE, INTERACT_INSIDE_BURIED_TREASURE,
            EAT_MUSHROOM_STEW, USE_STONE_TOOLS, EAT_BEETROOT_SOUP, OPEN_TRAPDOOR, TRADE_WITH_WANDERING_TRADER,
            EAT_HONEY, INTERACT_INSIDE_IGLOO, USE_WATER_BUCKET, EAT_MUTTON, USE_IRON_TOOLS, THROW_SNOWBALL,
            INTERACT_INSIDE_OCEAN_RUIN, GET_INTO_MINECART
        );

        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            EAT_PUFFERFISH, OPEN_FENCE_GATE, EAT_GOLDEN_APPLE, USE_SHEARS, USE_FLINT_AND_STEEL, IGNITE_TNT,
            INTERACT_INSIDE_DESERT_PYRAMID, EAT_PUMPKIN_PIE, USE_FISHING_ROD, INTERACT_INSIDE_VILLAGE, TRADE_WITH_MASON,
            OPEN_BLAST_FURNACE, THROW_ENDER_PEARL, EQUIP_IRON_ARMOR, ENTER_NETHER, EAT_ENCHANTED_GOLDEN_APPLE,
            OPEN_ANVIL, OPEN_LOOM, ATTACK_WITH_TRIDENT, INTERACT_INSIDE_MINESHAFT, THROW_EGG, OPEN_GRINDSTONE,
            USE_BRUSH, SHOOT_CROSSBOW, EAT_RABBIT, INTERACT_INSIDE_SWAMP_HUT, USE_JUKEBOX, TRADE_WITH_CARTOGRAPHER,
            OPEN_SMITHING_TABLE, OPEN_CARTOGRAPHY_TABLE, USE_COMPOSTER, INTERACT_INSIDE_FORTRESS, USE_SPYGLASS,
            THROW_WIND_CHARGE, INTERACT_INSIDE_BASTION_REMNANT
        );

        extraSpacing += 2;
        addCycle(progression, extraSpacing,
            INTERACT_INSIDE_ANCIENT_CITY, EAT_BEEF, SHOOT_BOW, TRADE_WITH_LEATHERWORKER, USE_OMINOUS_BOTTLE,
            EQUIP_DIAMOND_ARMOR, EAT_COOKED_RABBIT, INTERACT_INSIDE_JUNGLE_PYRAMID, OPEN_ENDER_CHEST,
            TRADE_WITH_SHEPHERD, USE_DIAMOND_TOOLS, EAT_PORKCHOP, ATTACK_WITH_MACE, USE_NETHERITE_TOOLS, USE_ENDER_EYE,
            EAT_BREAD, TRADE_WITH_BUTCHER, CHARGE_RESPAWN_ANCHOR, EAT_COOKED_SALMON, INTERACT_INSIDE_PILLAGER_OUTPOST,
            ENTER_END, EAT_BAKED_POTATO, INTERACT_INSIDE_MONUMENT, TRADE_WITH_FARMER, OPEN_BEACON,
            INTERACT_INSIDE_STRONGHOLD, USE_CAULDRON, INTERACT_INSIDE_MONSTER_ROOM, OPEN_SMOKER, EQUIP_TURTLE_HELMET,
            INTERACT_INSIDE_TRAIL_RUINS, EAT_COOKED_CHICKEN, UNLOCK_VAULT, TRADE_WITH_CLERIC, EQUIP_NETHERITE_ARMOR,
            EAT_COOKED_MUTTON, TRADE_WITH_FISHERMAN, INTERACT_INSIDE_MANSION, EAT_COOKED_COD,
            INTERACT_INSIDE_DESERT_WELL, EQUIP_ELYTRA, OPEN_SHULKER_BOX, TRADE_WITH_FLETCHER, INTERACT_INSIDE_END_CITY,
            PLACE_END_CRYSTAL, INTERACT_INSIDE_TRIAL_CHAMBERS, TRADE_WITH_ARMORER, TELEPORT_OUTER_ISLANDS,
            GLIDE_WITH_FIREWORKS, OPEN_BREWING_STAND, EAT_CHORUS_FRUIT, TRADE_WITH_WEAPONSMITH, EAT_COOKED_PORKCHOP,
            EAT_COOKED_BEEF, TRADE_WITH_TOOLSMITH, EAT_GOLDEN_CARROT, TRADE_WITH_LIBRARIAN, OPEN_ENCHANTING_TABLE
        );
    }

    private static void addCycle(
        @NotNull Map<AbilityType, Integer> config,
        int spacing,
        AbilityType @NotNull ... abilityTypes
    ) {
        int requiredCount = config.isEmpty() ? 0 : Collections.max(config.values());
        for (AbilityType abilityType : abilityTypes) {
            requiredCount += spacing;
            config.put(abilityType, requiredCount);
        }
    }
}
