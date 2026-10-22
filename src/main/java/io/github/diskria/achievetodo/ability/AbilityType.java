package io.github.diskria.achievetodo.ability;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.generation.AbilityAdvancementsGenerator;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.client.gui.AbilityUnlockedToastType;
import io.github.diskria.achievetodo.client.gui.DesignCodePalette;
import io.github.diskria.achievetodo.injection.extension.main.ArmorItemExtension;
import io.github.diskria.achievetodo.injection.extension.main.MiningToolItemExtension;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum AbilityType {

    VISION(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.ENDER_EYE),
    EAT_SALMON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.SALMON),
    EAT_COD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COD),
    EAT_TROPICAL_FISH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.TROPICAL_FISH),
    JUMP(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.SLIME_BLOCK),
    SWIM(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.HEART_OF_THE_SEA),
    EAT_ROTTEN_FLESH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.ROTTEN_FLESH),
    EAT_SPIDER_EYE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.SPIDER_EYE),
    EAT_SWEET_BERRIES(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.SWEET_BERRIES),
    OPEN_DOOR(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_DOOR),
    EAT_GLOW_BERRIES(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.GLOW_BERRIES),
    INTERACT_INSIDE_VILLAGE(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.VILLAGE),
    SLEEP(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.LIGHT_GRAY_BED),
    EAT_POISONOUS_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.POISONOUS_POTATO),
    SNEAK(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.CHAINMAIL_LEGGINGS),
    SPRINT(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.CHAINMAIL_BOOTS),
    OPEN_INVENTORY(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.LIGHT_GRAY_BUNDLE),
    BREAK_BLOCKS(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.COBBLESTONE),
    EAT_SUSPICIOUS_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.SUSPICIOUS_STEW),
    USE_GOLDEN_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.GOLD),
    OPEN_CHEST(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CHEST),
    EAT_CHICKEN(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.CHICKEN),
    EAT_CARROT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.CARROT),
    OPEN_CRAFTING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CRAFTING_TABLE),
    EQUIP_GOLDEN_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.GOLD),
    OPEN_BARREL(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BARREL),
    EAT_BEETROOT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.BEETROOT),
    EAT_DRIED_KELP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.DRIED_KELP),
    EAT_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.POTATO),
    OPEN_STONECUTTER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.STONECUTTER),
    GET_INTO_BOAT(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.MAIN, Items.PALE_OAK_BOAT),
    EAT_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.APPLE),
    INTERACT_INSIDE_SHIPWRECK(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.SHIPWRECK),
    USE_SHIELD(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.SHIELD),
    EAT_MELON_SLICE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.MELON_SLICE),
    INTERACT_INSIDE_RUINED_PORTAL(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.RUINED_PORTAL),
    OPEN_FURNACE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.FURNACE),
    EAT_COOKIE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKIE),
    INTERACT_INSIDE_BURIED_TREASURE(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.BURIED_TREASURE),
    EAT_MUSHROOM_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.MUSHROOM_STEW),
    EAT_BEETROOT_SOUP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.BEETROOT_SOUP),
    PUT_IN_BUNDLE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.MAIN, Items.BUNDLE),
    EAT_RABBIT_STEW(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.RABBIT_STEW),
    OPEN_TRAPDOOR(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_TRAPDOOR),
    USE_WOODEN_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.WOOD),
    EAT_HONEY(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.HONEY_BOTTLE),
    INTERACT_INSIDE_IGLOO(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.IGLOO),
    USE_WATER_BUCKET(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.WATER_BUCKET),
    EAT_MUTTON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.MUTTON),
    THROW_SNOWBALL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SNOWBALL),
    INTERACT_INSIDE_OCEAN_RUIN(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.OCEAN_RUIN),
    EQUIP_LEATHER_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.LEATHER),
    EAT_PUFFERFISH(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.PUFFERFISH),
    OPEN_FENCE_GATE(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.PALE_OAK_FENCE_GATE),
    EAT_PUMPKIN_PIE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.PUMPKIN_PIE),
    EAT_GOLDEN_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.GOLDEN_APPLE),
    USE_SHEARS(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SHEARS),
    INTERACT_INSIDE_DESERT_PYRAMID(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.DESERT_PYRAMID),
    BREAK_BLOCKS_IN_NEGATIVE_Y(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.MAIN, Items.COBBLED_DEEPSLATE),
    EAT_ENCHANTED_GOLDEN_APPLE(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.ENCHANTED_GOLDEN_APPLE),
    INTERACT_INSIDE_MINESHAFT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MINESHAFT),
    THROW_EGG(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.EGG),
    USE_STONE_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.STONE),
    OPEN_GRINDSTONE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.GRINDSTONE),
    SHOOT_CROSSBOW(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.CROSSBOW),
    EAT_RABBIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.RABBIT),
    INTERACT_INSIDE_SWAMP_HUT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.SWAMP_HUT),
    EQUIP_CHAINMAIL_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.CHAIN),
    OPEN_ANVIL(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ANVIL),
    USE_FLINT_AND_STEEL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.FLINT_AND_STEEL),
    IGNITE_TNT(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.TNT),
    INTERACT_INSIDE_ANCIENT_CITY(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.ANCIENT_CITY),
    ENTER_NETHER(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, NetherPortalBlock.class),
    TRADE_WITH_WANDERING_TRADER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, Items.WANDERING_TRADER_SPAWN_EGG),
    GET_INTO_MINECART(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.MINECART),
    INTERACT_INSIDE_FORTRESS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.FORTRESS),
    USE_OMINOUS_BOTTLE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.OMINOUS_BOTTLE),
    USE_IRON_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.IRON),
    INTERACT_INSIDE_JUNGLE_PYRAMID(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.JUNGLE_PYRAMID),
    ATTACK_WITH_TRIDENT(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.TRIDENT),
    EQUIP_IRON_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.IRON),
    SHOOT_BOW(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.BOW),
    INTERACT_INSIDE_BASTION_REMNANT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.BASTION_REMNANT),
    USE_JUKEBOX(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.JUKEBOX),
    THROW_ENDER_PEARL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.ENDER_PEARL),
    USE_COMPOSTER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.COMPOSTER),
    CHARGE_RESPAWN_ANCHOR(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.RESPAWN_ANCHOR),
    EAT_BEEF(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.BEEF),
    INTERACT_INSIDE_PILLAGER_OUTPOST(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.PILLAGER_OUTPOST),
    TRADE_WITH_MASON(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.MASON),
    USE_FISHING_ROD(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.FISHING_ROD),
    EAT_PORKCHOP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.PORKCHOP),
    TRADE_WITH_CARTOGRAPHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.CARTOGRAPHER),
    INTERACT_INSIDE_MONUMENT(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MONUMENT),
    USE_DIAMOND_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.DIAMOND),
    USE_CAULDRON(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CAULDRON),
    INTERACT_INSIDE_MONSTER_ROOM(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MONSTER_ROOM),
    EAT_BAKED_POTATO(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.BAKED_POTATO),
    OPEN_SMOKER(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.SMOKER),
    EQUIP_TURTLE_HELMET(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.TURTLE_HELMET),
    INTERACT_INSIDE_TRAIL_RUINS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.TRAIL_RUINS),
    USE_BRUSH(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.BRUSH),
    EQUIP_DIAMOND_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.DIAMOND),
    UNLOCK_VAULT(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.VAULT),
    OPEN_BLAST_FURNACE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BLAST_FURNACE),
    INTERACT_INSIDE_STRONGHOLD(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.STRONGHOLD),
    TRADE_WITH_LEATHERWORKER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.LEATHERWORKER),
    USE_SPYGLASS(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.SPYGLASS),
    INTERACT_INSIDE_MANSION(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.MANSION),
    OPEN_BEACON(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BEACON),
    THROW_WIND_CHARGE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.WIND_CHARGE),
    ENTER_END(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, EndPortalBlock.class),
    OPEN_CARTOGRAPHY_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CARTOGRAPHY_TABLE),
    INTERACT_INSIDE_DESERT_WELL(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.DESERT_WELL),
    EAT_COOKED_SALMON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_SALMON),
    EQUIP_ELYTRA(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, Items.ELYTRA),
    TRADE_WITH_SHEPHERD(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.SHEPHERD),
    TRADE_WITH_BUTCHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.BUTCHER),
    INTERACT_INSIDE_TRIAL_CHAMBERS(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.TRIAL_CHAMBERS),
    OPEN_ENDER_CHEST(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ENDER_CHEST),
    ATTACK_WITH_MACE(AbilityUnlockedToastType.WEAPON, AbilitiesHierarchyLayerType.UPGRADE, Items.MACE),
    INTERACT_INSIDE_END_CITY(AbilityUnlockedToastType.LANDMARK, AbilitiesHierarchyLayerType.LANDMARK, LandmarkType.END_CITY),
    USE_ENDER_EYE(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.ENDER_EYE),
    TELEPORT_OUTER_ISLANDS(AbilityUnlockedToastType.PORTAL, AbilitiesHierarchyLayerType.UPGRADE, EndGatewayBlock.class),
    USE_NETHERITE_TOOLS(AbilityUnlockedToastType.TOOL, AbilitiesHierarchyLayerType.UPGRADE, ToolMaterial.NETHERITE),
    EAT_COOKED_COD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_COD),
    TRADE_WITH_FARMER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FARMER),
    GLIDE_WITH_FIREWORKS(AbilityUnlockedToastType.ACTION, AbilitiesHierarchyLayerType.ACTIONS, Items.FIREWORK_ROCKET),
    TRADE_WITH_CLERIC(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.CLERIC),
    EQUIP_NETHERITE_ARMOR(AbilityUnlockedToastType.EQUIPMENT, AbilitiesHierarchyLayerType.UPGRADE, ArmorMaterials.NETHERITE),
    OPEN_BREWING_STAND(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.BREWING_STAND),
    EAT_COOKED_RABBIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_RABBIT),
    PLACE_END_CRYSTAL(AbilityUnlockedToastType.ITEM, AbilitiesHierarchyLayerType.ACTIONS, Items.END_CRYSTAL),
    TRADE_WITH_FISHERMAN(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FISHERMAN),
    OPEN_SMITHING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.SMITHING_TABLE),
    EAT_COOKED_CHICKEN(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_CHICKEN),
    EAT_CHORUS_FRUIT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.CHORUS_FRUIT),
    TRADE_WITH_FLETCHER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.FLETCHER),
    EAT_COOKED_MUTTON(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_MUTTON),
    TRADE_WITH_ARMORER(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.ARMORER),
    EAT_COOKED_PORKCHOP(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_PORKCHOP),
    EAT_BREAD(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.BREAD),
    TRADE_WITH_WEAPONSMITH(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.WEAPONSMITH),
    EAT_COOKED_BEEF(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.COOKED_BEEF),
    OPEN_LOOM(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.LOOM),
    USE_CAMPFIRE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.CAMPFIRE),
    OPEN_SHULKER_BOX(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.LIGHT_GRAY_SHULKER_BOX),
    EAT_GOLDEN_CARROT(AbilityUnlockedToastType.FOOD, AbilitiesHierarchyLayerType.FOOD, FoodComponents.GOLDEN_CARROT),
    TRADE_WITH_TOOLSMITH(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.TOOLSMITH),
    TRADE_WITH_LIBRARIAN(AbilityUnlockedToastType.TRADING, AbilitiesHierarchyLayerType.TRADING, VillagerProfession.LIBRARIAN),
    OPEN_ENCHANTING_TABLE(AbilityUnlockedToastType.BLOCK, AbilitiesHierarchyLayerType.BLOCKS, Blocks.ENCHANTING_TABLE);

    private final AbilitiesHierarchyLayerType hierarchyLayerType;
    private final AbilityUnlockedToastType unlockToastType;
    private final Item item;
    private final Block block;
    private final FoodComponent foodComponent;
    private final ToolMaterial toolMaterial;
    private final ArmorMaterial equipmentMaterial;
    private final Class<? extends Portal> portal;
    private final VillagerProfession villager;
    private final LandmarkType landmarkType;

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Item item) {
        this(unlockToastType, hierarchyLayerType, item, null, null, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Block block) {
        this(unlockToastType, hierarchyLayerType, null, null, block, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, FoodComponent foodComponent) {
        this(unlockToastType, hierarchyLayerType, null, foodComponent, null, null, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, ToolMaterial toolMaterial) {
        this(unlockToastType, hierarchyLayerType, null, null, null, toolMaterial, null, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, ArmorMaterial armorMaterial) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, armorMaterial, null, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, Class<? extends Portal> portal) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, portal, null, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, VillagerProfession villager) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, null, villager, null);
    }

    AbilityType(AbilityUnlockedToastType unlockToastType, AbilitiesHierarchyLayerType hierarchyLayerType, LandmarkType landmarkType) {
        this(unlockToastType, hierarchyLayerType, null, null, null, null, null, null, null, landmarkType);
    }

    AbilityType(
        AbilityUnlockedToastType unlockToastType,
        AbilitiesHierarchyLayerType hierarchyLayerType,
        Item item,
        FoodComponent foodComponent,
        Block block,
        ToolMaterial toolMaterial,
        ArmorMaterial equipmentMaterial,
        Class<? extends Portal> portal,
        VillagerProfession villager,
        LandmarkType landmarkType
    ) {
        this.hierarchyLayerType = hierarchyLayerType;
        this.unlockToastType = unlockToastType;
        this.item = item;
        this.foodComponent = foodComponent;
        this.block = block;
        this.toolMaterial = toolMaterial;
        this.equipmentMaterial = equipmentMaterial;
        this.portal = portal;
        this.villager = villager;
        this.landmarkType = landmarkType;
    }

    public static @Nullable AbilityType findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (abilityType.name().equalsIgnoreCase(name)) {
                return abilityType;
            }
        }
        return null;
    }

    public static AbilityType findByAdvancement(@NotNull PlacedAdvancement advancement) {
        return findByAdvancement(advancement.getAdvancementEntry());
    }

    public static AbilityType findByAdvancement(@NotNull AdvancementEntry advancement) {
        return findByAdvancement(advancement.id());
    }

    public static @Nullable AbilityType findByAdvancement(Identifier advancementId) {
        if (advancementId == null || !AchieveToDoMod.ID.equals(advancementId.getNamespace())) {
            return null;
        }
        String path = advancementId.getPath();
        if (path.startsWith(AbilityAdvancementsGenerator.ABILITY_PATH_PREFIX)) {
            return findByName(path.split(AbilityAdvancementsGenerator.ABILITY_PATH_PREFIX)[1]);
        }
        return null;
    }

    public static @Nullable AbilityType findEatFoodAbility(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
        if (foodComponent == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (foodComponent == abilityType.foodComponent) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findToolMaterialUsageAbility(ToolMaterial toolMaterial) {
        if (toolMaterial == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (toolMaterial == abilityType.toolMaterial) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findEquipmentEquipAbility(Item item) {
        if (item == null) {
            return null;
        }
        if (item == Items.TURTLE_HELMET) {
            return EQUIP_TURTLE_HELMET;
        }
        if (item == Items.ELYTRA) {
            return EQUIP_ELYTRA;
        }
        if (item instanceof ArmorItemExtension armorItemExtension) {
            for (AbilityType abilityType : values()) {
                if (armorItemExtension.achievetodo$getMaterial() == abilityType.equipmentMaterial) {
                    return abilityType;
                }
            }
        }
        return null;
    }

    public static @Nullable AbilityType findPortalTeleportAbility(Portal portal) {
        if (portal == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (portal.getClass() == abilityType.portal) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findTradeAbility(VillagerProfession profession) {
        if (profession == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (profession == abilityType.villager) {
                return abilityType;
            }
        }
        return null;
    }

    public static @Nullable AbilityType findByLandmarkType(LandmarkType landmarkType) {
        if (landmarkType == null) {
            return null;
        }
        for (AbilityType abilityType : values()) {
            if (landmarkType == abilityType.landmarkType) {
                return abilityType;
            }
        }
        return null;
    }

    public AbilitiesHierarchyLayerType getHierarchyLayerType() {
        return hierarchyLayerType;
    }

    public AbilityUnlockedToastType getUnlockToastType() {
        return unlockToastType;
    }

    public LandmarkType getLandmarkType() {
        return landmarkType;
    }

    public Text buildUnlockProgressMessage(int leftCount) {
        return buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.left_to_unlock", leftCount))
            .formatted(DesignCodePalette.TEXT_COLOR);
    }

    public Text buildPermanentlyLockedMessage() {
        return buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.permanently_locked"))
            .formatted(Formatting.RED);
    }

    private MutableText buildLockedMessagePrefix() {
        return AchieveToDoClient.translate("ability." + getName() + ".locked_message")
            .append("." + (FabricLoader.getInstance().isModLoaded("multilineactionbar") ? "\n" : " "));
    }

    public @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @Nullable Item getIcon() {
        if (item != null) {
            return item;
        }
        if (block != null) {
            return block.asItem();
        }
        if (foodComponent != null) {
            return Registries.ITEM.stream()
                .filter(item -> item.getComponents().get(DataComponentTypes.FOOD) == foodComponent)
                .findFirst()
                .orElseThrow();
        }
        if (toolMaterial != null) {
            return Registries.ITEM.stream()
                .filter(item -> item instanceof PickaxeItem pickaxeItem &&
                    pickaxeItem instanceof MiningToolItemExtension miningToolItem &&
                    miningToolItem.achievetodo$getMaterial() == toolMaterial
                )
                .findFirst()
                .orElse(null);
        }
        if (equipmentMaterial != null) {
            return Registries.ITEM.stream()
                .filter(item -> item instanceof ArmorItemExtension armorItemExtension &&
                    armorItemExtension.achievetodo$getEquipmentType() == EquipmentType.CHESTPLATE &&
                    armorItemExtension.achievetodo$getMaterial() == equipmentMaterial
                )
                .findFirst()
                .orElse(null);
        }
        if (portal != null) {
            return switch (this) {
                case ENTER_NETHER -> Items.OBSIDIAN;
                case ENTER_END -> Items.END_PORTAL_FRAME;
                case TELEPORT_OUTER_ISLANDS -> Items.CHORUS_FLOWER;
                default -> null;
            };
        }
        if (villager != null) {
            PointOfInterestType poi = Registries.POINT_OF_INTEREST_TYPE.get(Identifier.ofVanilla(villager.id()));
            if (poi != null && poi.blockStates() != null) {
                List<BlockState> blockStates = new ArrayList<>(poi.blockStates());
                BlockState blockState = blockStates.getFirst();
                if (blockState != null) {
                    return blockState.getBlock().asItem();
                }
            }
        }
        if (landmarkType != null) {
            return switch (landmarkType) {
                case DESERT_PYRAMID -> Items.CHISELED_SANDSTONE;
                case DESERT_WELL -> Items.SANDSTONE;
                case JUNGLE_PYRAMID -> Items.MOSSY_COBBLESTONE;
                case PILLAGER_OUTPOST -> Items.DARK_OAK_PLANKS;
                case IGLOO -> Items.SNOW_BLOCK;
                case SWAMP_HUT -> Items.CAULDRON;
                case MANSION -> Items.DARK_OAK_LOG;
                case VILLAGE -> Items.BELL;
                case RUINED_PORTAL -> Items.CRYING_OBSIDIAN;
                case BURIED_TREASURE -> Items.SAND;
                case SHIPWRECK -> Items.OAK_PLANKS;
                case OCEAN_RUIN -> Items.SEA_LANTERN;
                case MONUMENT -> Items.PRISMARINE;
                case MONSTER_ROOM -> Items.SPAWNER;
                case MINESHAFT -> Items.RAIL;
                case TRAIL_RUINS -> Items.SUSPICIOUS_GRAVEL;
                case ANCIENT_CITY -> Items.REINFORCED_DEEPSLATE;
                case TRIAL_CHAMBERS -> Items.TRIAL_SPAWNER;
                case STRONGHOLD -> Items.INFESTED_CRACKED_STONE_BRICKS;
                case FORTRESS -> Items.CHISELED_NETHER_BRICKS;
                case BASTION_REMNANT -> Items.GILDED_BLACKSTONE;
                case END_CITY -> Items.PURPUR_BLOCK;
            };
        }
        throw new IllegalStateException("Ability " + this + " haven't icon!");
    }

    public @NotNull Text getTitle() {
        return AchieveToDoClient.translate("ability." + getName() + ".name");
    }

    public @NotNull Text getDescription() {
        return AchieveToDoClient.translate("ability." + getName() + ".description");
    }

    public int getChaosPriority() {
        if (this == OPEN_CRAFTING_TABLE) {
            return 100;
        }
        if (this == INTERACT_INSIDE_FORTRESS ||
            this == INTERACT_INSIDE_STRONGHOLD ||
            this == USE_ENDER_EYE ||
            this == ENTER_NETHER ||
            this == ENTER_END
        ) {
            return 90;
        }
        if (this == OPEN_BEACON ||
            this == OPEN_ENDER_CHEST ||
            this == OPEN_SMITHING_TABLE ||
            this == OPEN_ANVIL ||
            this == OPEN_STONECUTTER
        ) {
            return 80;
        }
        if (this == USE_GOLDEN_TOOLS ||
            this == USE_WOODEN_TOOLS ||
            this == USE_STONE_TOOLS ||
            this == USE_IRON_TOOLS ||
            this == USE_SHIELD ||
            this == USE_WATER_BUCKET ||
            this == USE_FLINT_AND_STEEL
        ) {
            return 70;
        }
        if (this == EQUIP_LEATHER_ARMOR ||
            this == EQUIP_CHAINMAIL_ARMOR ||
            this == EQUIP_IRON_ARMOR ||
            this == EQUIP_ELYTRA
        ) {
            return 60;
        }
        if (this == THROW_ENDER_PEARL ||
            this == USE_OMINOUS_BOTTLE ||
            this == PLACE_END_CRYSTAL
        ) {
            return 50;
        }
        if (this == EAT_PUFFERFISH ||
            this == EAT_ROTTEN_FLESH ||
            this == EAT_SPIDER_EYE ||
            this == EAT_SUSPICIOUS_STEW ||
            this == EAT_POISONOUS_POTATO
        ) {
            return 40;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.UPGRADE ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.BLOCKS ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.ACTIONS
        ) {
            return 30;
        }
        if (hierarchyLayerType == AbilitiesHierarchyLayerType.TRADING ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.LANDMARK ||
            hierarchyLayerType == AbilitiesHierarchyLayerType.FOOD
        ) {
            return 0;
        }
        return 100;
    }
}
