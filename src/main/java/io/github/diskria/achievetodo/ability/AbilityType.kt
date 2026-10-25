package io.github.diskria.achievetodo.ability

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.ability.AbilitiesHierarchyLayerType.*
import io.github.diskria.achievetodo.client.AchieveToDoClient
import io.github.diskria.achievetodo.client.gui.DesignCodePalette
import io.github.diskria.achievetodo.client.gui.UnlockedToastType
import io.github.diskria.achievetodo.datagen.AbilityAdvancementsTabGenerator
import io.github.diskria.achievetodo.etxensions.armorMaterial
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementNode
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.ByIdMap
import net.minecraft.world.entity.npc.villager.VillagerProfession
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorMaterials
import net.minecraft.world.level.block.*
import kotlin.reflect.KClass

enum class AbilityType(
    val unlockToastType: UnlockedToastType,
    val hierarchyLayerType: AbilitiesHierarchyLayerType,
    val item: Item? = null,
    val block: Block? = null,
    val toolMaterial: ToolMaterial? = null,
    val armorMaterial: ArmorMaterial? = null,
    val portalKlass: KClass<out Portal>? = null,
    val villagerProfession: ResourceKey<VillagerProfession>? = null,
    val landmark: LandmarkType? = null,
) {
    VISION(UnlockedToastType.ACTION, MAIN, item = Items.ENDER_EYE),
    EAT_SALMON(UnlockedToastType.FOOD, FOOD, item = Items.SALMON),
    EAT_COD(UnlockedToastType.FOOD, FOOD, item = Items.COD),
    EAT_TROPICAL_FISH(UnlockedToastType.FOOD, FOOD, item = Items.TROPICAL_FISH),
    JUMP(UnlockedToastType.ACTION, MAIN, Items.SLIME_BLOCK),
    SWIM(UnlockedToastType.ACTION, MAIN, Items.HEART_OF_THE_SEA),
    EAT_ROTTEN_FLESH(UnlockedToastType.FOOD, FOOD, item = Items.ROTTEN_FLESH),
    EAT_SPIDER_EYE(UnlockedToastType.FOOD, FOOD, item = Items.SPIDER_EYE),
    EAT_SWEET_BERRIES(UnlockedToastType.FOOD, FOOD, item = Items.SWEET_BERRIES),
    OPEN_DOOR(UnlockedToastType.ACTION, ACTIONS, Items.PALE_OAK_DOOR),
    EAT_GLOW_BERRIES(UnlockedToastType.FOOD, FOOD, item = Items.GLOW_BERRIES),
    INTERACT_INSIDE_VILLAGE(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.VILLAGE),
    SLEEP(UnlockedToastType.ACTION, MAIN, Items.BED.gray()),
    EAT_POISONOUS_POTATO(UnlockedToastType.FOOD, FOOD, item = Items.POISONOUS_POTATO),
    SNEAK(UnlockedToastType.ACTION, MAIN, Items.CHAINMAIL_LEGGINGS),
    SPRINT(UnlockedToastType.ACTION, MAIN, Items.CHAINMAIL_BOOTS),
    OPEN_INVENTORY(UnlockedToastType.ACTION, MAIN, Items.DYED_BUNDLE.gray()),
    BREAK_BLOCKS(UnlockedToastType.ACTION, MAIN, Items.COBBLESTONE),
    EAT_SUSPICIOUS_STEW(UnlockedToastType.FOOD, FOOD, item = Items.SUSPICIOUS_STEW),
    USE_GOLDEN_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.GOLD),
    OPEN_CHEST(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.CHEST),
    EAT_CHICKEN(UnlockedToastType.FOOD, FOOD, item = Items.CHICKEN),
    EAT_CARROT(UnlockedToastType.FOOD, FOOD, item = Items.CARROT),
    OPEN_CRAFTING_TABLE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.CRAFTING_TABLE),
    EQUIP_GOLDEN_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.GOLD),
    OPEN_BARREL(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.BARREL),
    EAT_BEETROOT(UnlockedToastType.FOOD, FOOD, item = Items.BEETROOT),
    EAT_DRIED_KELP(UnlockedToastType.FOOD, FOOD, item = Items.DRIED_KELP),
    EAT_POTATO(UnlockedToastType.FOOD, FOOD, item = Items.POTATO),
    OPEN_STONECUTTER(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.STONECUTTER),
    GET_INTO_BOAT(UnlockedToastType.ITEM, MAIN, Items.PALE_OAK_BOAT),
    EAT_APPLE(UnlockedToastType.FOOD, FOOD, item = Items.APPLE),
    INTERACT_INSIDE_SHIPWRECK(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.SHIPWRECK),
    USE_SHIELD(UnlockedToastType.EQUIPMENT, UPGRADE, Items.SHIELD),
    EAT_MELON_SLICE(UnlockedToastType.FOOD, FOOD, item = Items.MELON_SLICE),
    INTERACT_INSIDE_RUINED_PORTAL(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.RUINED_PORTAL),
    OPEN_FURNACE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.FURNACE),
    EAT_COOKIE(UnlockedToastType.FOOD, FOOD, item = Items.COOKIE),
    INTERACT_INSIDE_BURIED_TREASURE(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.BURIED_TREASURE),
    EAT_MUSHROOM_STEW(UnlockedToastType.FOOD, FOOD, item = Items.MUSHROOM_STEW),
    EAT_BEETROOT_SOUP(UnlockedToastType.FOOD, FOOD, item = Items.BEETROOT_SOUP),
    PUT_IN_BUNDLE(UnlockedToastType.ITEM, MAIN, Items.BUNDLE),
    EAT_RABBIT_STEW(UnlockedToastType.FOOD, FOOD, item = Items.RABBIT_STEW),
    OPEN_TRAPDOOR(UnlockedToastType.ACTION, ACTIONS, Items.PALE_OAK_TRAPDOOR),
    USE_WOODEN_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.WOOD),
    EAT_HONEY(UnlockedToastType.FOOD, FOOD, item = Items.HONEY_BOTTLE),
    INTERACT_INSIDE_IGLOO(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.IGLOO),
    USE_WATER_BUCKET(UnlockedToastType.ACTION, ACTIONS, Items.WATER_BUCKET),
    EAT_MUTTON(UnlockedToastType.FOOD, FOOD, item = Items.MUTTON),
    THROW_SNOWBALL(UnlockedToastType.ITEM, ACTIONS, Items.SNOWBALL),
    INTERACT_INSIDE_OCEAN_RUIN(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.OCEAN_RUIN),
    EQUIP_LEATHER_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.LEATHER),
    EAT_PUFFERFISH(UnlockedToastType.FOOD, FOOD, item = Items.PUFFERFISH),
    OPEN_FENCE_GATE(UnlockedToastType.ACTION, ACTIONS, Items.PALE_OAK_FENCE_GATE),
    EAT_PUMPKIN_PIE(UnlockedToastType.FOOD, FOOD, item = Items.PUMPKIN_PIE),
    EAT_GOLDEN_APPLE(UnlockedToastType.FOOD, FOOD, item = Items.GOLDEN_APPLE),
    USE_SHEARS(UnlockedToastType.ITEM, ACTIONS, Items.SHEARS),
    INTERACT_INSIDE_DESERT_PYRAMID(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.DESERT_PYRAMID),
    BREAK_BLOCKS_IN_NEGATIVE_Y(UnlockedToastType.ACTION, MAIN, Items.COBBLED_DEEPSLATE),
    EAT_ENCHANTED_GOLDEN_APPLE(UnlockedToastType.FOOD, FOOD, item = Items.ENCHANTED_GOLDEN_APPLE),
    INTERACT_INSIDE_MINESHAFT(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.MINESHAFT),
    THROW_EGG(UnlockedToastType.ITEM, ACTIONS, Items.EGG),
    USE_STONE_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.STONE),
    OPEN_GRINDSTONE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.GRINDSTONE),
    SHOOT_CROSSBOW(UnlockedToastType.WEAPON, UPGRADE, Items.CROSSBOW),
    EAT_RABBIT(UnlockedToastType.FOOD, FOOD, item = Items.RABBIT),
    INTERACT_INSIDE_SWAMP_HUT(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.SWAMP_HUT),
    EQUIP_CHAINMAIL_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.CHAINMAIL),
    OPEN_ANVIL(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.ANVIL),
    USE_FLINT_AND_STEEL(UnlockedToastType.ITEM, ACTIONS, Items.FLINT_AND_STEEL),
    IGNITE_TNT(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.TNT),
    INTERACT_INSIDE_ANCIENT_CITY(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.ANCIENT_CITY),
    ENTER_NETHER(UnlockedToastType.PORTAL, UPGRADE, portalKlass = NetherPortalBlock::class),
    TRADE_WITH_WANDERING_TRADER(UnlockedToastType.TRADING, TRADING, Items.WANDERING_TRADER_SPAWN_EGG),
    GET_INTO_MINECART(UnlockedToastType.ITEM, ACTIONS, Items.MINECART),
    INTERACT_INSIDE_FORTRESS(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.FORTRESS),
    USE_OMINOUS_BOTTLE(UnlockedToastType.ITEM, ACTIONS, Items.OMINOUS_BOTTLE),
    USE_IRON_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.IRON),
    INTERACT_INSIDE_JUNGLE_PYRAMID(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.JUNGLE_PYRAMID),
    ATTACK_WITH_TRIDENT(UnlockedToastType.WEAPON, UPGRADE, Items.TRIDENT),
    EQUIP_IRON_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.IRON),
    SHOOT_BOW(UnlockedToastType.WEAPON, UPGRADE, Items.BOW),
    INTERACT_INSIDE_BASTION_REMNANT(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.BASTION_REMNANT),
    USE_JUKEBOX(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.JUKEBOX),
    THROW_ENDER_PEARL(UnlockedToastType.ITEM, ACTIONS, Items.ENDER_PEARL),
    USE_COMPOSTER(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.COMPOSTER),
    CHARGE_RESPAWN_ANCHOR(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.RESPAWN_ANCHOR),
    EAT_BEEF(UnlockedToastType.FOOD, FOOD, item = Items.BEEF),
    INTERACT_INSIDE_PILLAGER_OUTPOST(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.PILLAGER_OUTPOST),
    TRADE_WITH_MASON(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.MASON),
    USE_FISHING_ROD(UnlockedToastType.ITEM, ACTIONS, Items.FISHING_ROD),
    EAT_PORKCHOP(UnlockedToastType.FOOD, FOOD, item = Items.PORKCHOP),
    TRADE_WITH_CARTOGRAPHER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.CARTOGRAPHER),
    INTERACT_INSIDE_MONUMENT(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.MONUMENT),
    USE_DIAMOND_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.DIAMOND),
    USE_CAULDRON(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.CAULDRON),
    INTERACT_INSIDE_MONSTER_ROOM(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.MONSTER_ROOM),
    EAT_BAKED_POTATO(UnlockedToastType.FOOD, FOOD, item = Items.BAKED_POTATO),
    OPEN_SMOKER(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.SMOKER),
    EQUIP_TURTLE_HELMET(UnlockedToastType.EQUIPMENT, UPGRADE, Items.TURTLE_HELMET),
    INTERACT_INSIDE_TRAIL_RUINS(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.TRAIL_RUINS),
    USE_BRUSH(UnlockedToastType.ITEM, ACTIONS, Items.BRUSH),
    EQUIP_DIAMOND_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.DIAMOND),
    UNLOCK_VAULT(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.VAULT),
    OPEN_BLAST_FURNACE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.BLAST_FURNACE),
    INTERACT_INSIDE_STRONGHOLD(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.STRONGHOLD),
    TRADE_WITH_LEATHERWORKER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.LEATHERWORKER),
    USE_SPYGLASS(UnlockedToastType.ITEM, ACTIONS, Items.SPYGLASS),
    INTERACT_INSIDE_MANSION(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.MANSION),
    OPEN_BEACON(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.BEACON),
    THROW_WIND_CHARGE(UnlockedToastType.ITEM, ACTIONS, Items.WIND_CHARGE),
    ENTER_END(UnlockedToastType.PORTAL, UPGRADE, portalKlass = EndPortalBlock::class),
    OPEN_CARTOGRAPHY_TABLE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.CARTOGRAPHY_TABLE),
    INTERACT_INSIDE_DESERT_WELL(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.DESERT_WELL),
    EAT_COOKED_SALMON(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_SALMON),
    EQUIP_ELYTRA(UnlockedToastType.EQUIPMENT, UPGRADE, Items.ELYTRA),
    TRADE_WITH_SHEPHERD(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.SHEPHERD),
    TRADE_WITH_BUTCHER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.BUTCHER),
    INTERACT_INSIDE_TRIAL_CHAMBERS(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.TRIAL_CHAMBERS),
    OPEN_ENDER_CHEST(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.ENDER_CHEST),
    ATTACK_WITH_MACE(UnlockedToastType.WEAPON, UPGRADE, Items.MACE),
    INTERACT_INSIDE_END_CITY(UnlockedToastType.LANDMARK, LANDMARK, landmark = LandmarkType.END_CITY),
    USE_ENDER_EYE(UnlockedToastType.ITEM, ACTIONS, Items.ENDER_EYE),
    TELEPORT_OUTER_ISLANDS(UnlockedToastType.PORTAL, UPGRADE, portalKlass = EndGatewayBlock::class),
    USE_NETHERITE_TOOLS(UnlockedToastType.TOOL, UPGRADE, toolMaterial = ToolMaterial.NETHERITE),
    EAT_COOKED_COD(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_COD),
    TRADE_WITH_FARMER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.FARMER),
    GLIDE_WITH_FIREWORKS(UnlockedToastType.ACTION, ACTIONS, Items.FIREWORK_ROCKET),
    TRADE_WITH_CLERIC(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.CLERIC),
    EQUIP_NETHERITE_ARMOR(UnlockedToastType.EQUIPMENT, UPGRADE, armorMaterial = ArmorMaterials.NETHERITE),
    OPEN_BREWING_STAND(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.BREWING_STAND),
    EAT_COOKED_RABBIT(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_RABBIT),
    PLACE_END_CRYSTAL(UnlockedToastType.ITEM, ACTIONS, Items.END_CRYSTAL),
    TRADE_WITH_FISHERMAN(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.FISHERMAN),
    OPEN_SMITHING_TABLE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.SMITHING_TABLE),
    EAT_COOKED_CHICKEN(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_CHICKEN),
    EAT_CHORUS_FRUIT(UnlockedToastType.FOOD, FOOD, item = Items.CHORUS_FRUIT),
    TRADE_WITH_FLETCHER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.FLETCHER),
    EAT_COOKED_MUTTON(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_MUTTON),
    TRADE_WITH_ARMORER(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.ARMORER),
    EAT_COOKED_PORKCHOP(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_PORKCHOP),
    EAT_BREAD(UnlockedToastType.FOOD, FOOD, item = Items.BREAD),
    TRADE_WITH_WEAPONSMITH(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.WEAPONSMITH),
    EAT_COOKED_BEEF(UnlockedToastType.FOOD, FOOD, item = Items.COOKED_BEEF),
    OPEN_LOOM(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.LOOM),
    USE_CAMPFIRE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.CAMPFIRE),
    OPEN_SHULKER_BOX(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.DYED_SHULKER_BOX.gray()),
    EAT_GOLDEN_CARROT(UnlockedToastType.FOOD, FOOD, item = Items.GOLDEN_CARROT),
    TRADE_WITH_TOOLSMITH(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.TOOLSMITH),
    TRADE_WITH_LIBRARIAN(UnlockedToastType.TRADING, TRADING, villagerProfession = VillagerProfession.LIBRARIAN),
    OPEN_ENCHANTING_TABLE(UnlockedToastType.BLOCK, BLOCKS, block = Blocks.ENCHANTING_TABLE);

    fun buildUnlockProgressMessage(leftCount: Int): Component =
        buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.left_to_unlock", leftCount))
            .withStyle(DesignCodePalette.TEXT_COLOR)

    fun buildPermanentlyLockedMessage(): Component =
        buildLockedMessagePrefix()
            .append(AchieveToDoClient.translate("ability.permanently_locked"))
            .withStyle(ChatFormatting.RED)

    private fun buildLockedMessagePrefix(): MutableComponent =
        AchieveToDoClient.translate("ability.$id.locked_message")
            .append("." + (if (FabricLoader.getInstance().isModLoaded("multilineactionbar")) "\n" else " "))

    val id: String = name.lowercase()

    val icon: Item?
        get() = when {
            item != null -> item
            block != null -> block.asItem()
            toolMaterial != null -> when (toolMaterial) {
                ToolMaterial.WOOD -> Items.WOODEN_PICKAXE
                ToolMaterial.STONE -> Items.STONE_PICKAXE
                ToolMaterial.COPPER -> Items.COPPER_PICKAXE
                ToolMaterial.IRON -> Items.IRON_PICKAXE
                ToolMaterial.DIAMOND -> Items.DIAMOND_PICKAXE
                ToolMaterial.GOLD -> Items.GOLDEN_PICKAXE
                ToolMaterial.NETHERITE -> Items.NETHERITE_PICKAXE
                else -> null
            }

            armorMaterial != null -> when (armorMaterial) {
                ArmorMaterials.LEATHER -> Items.LEATHER_CHESTPLATE
                ArmorMaterials.COPPER -> Items.COPPER_CHESTPLATE
                ArmorMaterials.CHAINMAIL -> Items.CHAINMAIL_CHESTPLATE
                ArmorMaterials.IRON -> Items.IRON_CHESTPLATE
                ArmorMaterials.GOLD -> Items.GOLDEN_CHESTPLATE
                ArmorMaterials.DIAMOND -> Items.DIAMOND_CHESTPLATE
                ArmorMaterials.NETHERITE -> Items.NETHERITE_CHESTPLATE
                else -> null
            }

            portalKlass != null -> when (this) {
                ENTER_NETHER -> Items.OBSIDIAN
                ENTER_END -> Items.END_PORTAL_FRAME
                TELEPORT_OUTER_ISLANDS -> Items.CHORUS_FLOWER
                else -> null
            }

            villagerProfession != null -> {
                val professionId = villagerProfession.identifier()
                val poiType = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getValue(professionId) ?: return null
                val workstationBlock = poiType.matchingStates.firstOrNull()?.block ?: return null
                return workstationBlock.asItem()
            }

            landmark != null -> when (landmark) {
                LandmarkType.DESERT_PYRAMID -> Items.CHISELED_SANDSTONE
                LandmarkType.DESERT_WELL -> Items.SANDSTONE
                LandmarkType.JUNGLE_PYRAMID -> Items.MOSSY_COBBLESTONE
                LandmarkType.PILLAGER_OUTPOST -> Items.DARK_OAK_PLANKS
                LandmarkType.IGLOO -> Items.SNOW_BLOCK
                LandmarkType.SWAMP_HUT -> Items.CAULDRON
                LandmarkType.MANSION -> Items.DARK_OAK_LOG
                LandmarkType.VILLAGE -> Items.BELL
                LandmarkType.RUINED_PORTAL -> Items.CRYING_OBSIDIAN
                LandmarkType.BURIED_TREASURE -> Items.SAND
                LandmarkType.SHIPWRECK -> Items.OAK_PLANKS
                LandmarkType.OCEAN_RUIN -> Items.SEA_LANTERN
                LandmarkType.MONUMENT -> Items.PRISMARINE
                LandmarkType.MONSTER_ROOM -> Items.SPAWNER
                LandmarkType.MINESHAFT -> Items.RAIL
                LandmarkType.TRAIL_RUINS -> Items.SUSPICIOUS_GRAVEL
                LandmarkType.ANCIENT_CITY -> Items.REINFORCED_DEEPSLATE
                LandmarkType.TRIAL_CHAMBERS -> Items.TRIAL_SPAWNER
                LandmarkType.STRONGHOLD -> Items.INFESTED_CRACKED_STONE_BRICKS
                LandmarkType.FORTRESS -> Items.CHISELED_NETHER_BRICKS
                LandmarkType.BASTION_REMNANT -> Items.GILDED_BLACKSTONE
                LandmarkType.END_CITY -> Items.PURPUR_BLOCK
            }

            else -> null
        }

    val title: MutableComponent get() = AchieveToDoClient.translate("ability.$id.name")
    val description: MutableComponent get() = AchieveToDoClient.translate("ability.$id.description")

    val chaosPriority: Int
        get() = when (this) {
            OPEN_CRAFTING_TABLE -> 100

            INTERACT_INSIDE_FORTRESS,
            INTERACT_INSIDE_STRONGHOLD,
            USE_ENDER_EYE,
            ENTER_NETHER,
            ENTER_END -> 90

            OPEN_BEACON,
            OPEN_ENDER_CHEST,
            OPEN_SMITHING_TABLE,
            OPEN_ANVIL,
            OPEN_STONECUTTER -> 80

            USE_GOLDEN_TOOLS,
            USE_WOODEN_TOOLS,
            USE_STONE_TOOLS,
            USE_IRON_TOOLS,
            USE_SHIELD,
            USE_WATER_BUCKET,
            USE_FLINT_AND_STEEL -> 70

            EQUIP_LEATHER_ARMOR,
            EQUIP_CHAINMAIL_ARMOR,
            EQUIP_IRON_ARMOR,
            EQUIP_ELYTRA -> 60

            THROW_ENDER_PEARL,
            USE_OMINOUS_BOTTLE,
            PLACE_END_CRYSTAL -> 50

            EAT_PUFFERFISH,
            EAT_ROTTEN_FLESH,
            EAT_SPIDER_EYE,
            EAT_SUSPICIOUS_STEW,
            EAT_POISONOUS_POTATO -> 40

            else -> when (hierarchyLayerType) {
                UPGRADE, BLOCKS, ACTIONS -> 30
                TRADING, LANDMARK, FOOD -> 0
                else -> 100
            }
        }

    companion object {
        fun fromAbilityId(id: String): AbilityType? =
            entries.find { it.id == id }

        fun fromAdvancementId(id: Identifier): AbilityType? {
            if (id.namespace != AchieveToDoMod.ID) return null
            val path = id.path
            val prefix = AbilityAdvancementsTabGenerator.ABILITY_PATH_PREFIX
            if (path.startsWith(prefix)) {
                return fromAbilityId(path.removePrefix(prefix))
            }
            return null
        }

        fun fromAdvancement(advancement: AdvancementHolder): AbilityType? =
            fromAdvancementId(advancement.id)

        fun fromAdvancement(advancement: AdvancementNode): AbilityType? =
            fromAdvancement(advancement.holder())

        fun fromFood(stack: ItemStack): AbilityType? {
            val item = stack.item
            return entries.find { it.item == item }
        }

        fun fromToolMaterial(toolMaterial: ToolMaterial): AbilityType? =
            entries.find { it.toolMaterial == toolMaterial }

        fun fromEquipmentItem(item: Item) = when (item) {
            Items.TURTLE_HELMET -> EQUIP_TURTLE_HELMET
            Items.ELYTRA -> EQUIP_ELYTRA
            else -> {
                val expectMaterial = item.armorMaterial
                entries.find { it.armorMaterial == expectMaterial }
            }
        }

        fun fromPortal(portal: Portal) = entries.find { it.portalKlass == portal::class }
        fun fromVillagerProfession(profession: ResourceKey<VillagerProfession>) =
            entries.find { it.villagerProfession == profession }

        fun fromLandmark(landmark: LandmarkType) = entries.find { it.landmark == landmark }

        private val BY_ORDINAL = ByIdMap.continuous(
            AbilityType::ordinal, entries.toTypedArray(), ByIdMap.OutOfBoundsStrategy.ZERO
        )

        val STREAM_CODEC = ByteBufCodecs.idMapper(BY_ORDINAL, AbilityType::ordinal)
    }
}
