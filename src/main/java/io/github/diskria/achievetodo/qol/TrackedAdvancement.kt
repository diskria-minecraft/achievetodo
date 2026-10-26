package io.github.diskria.achievetodo.qol

import io.github.diskria.achievetodo.etxensions.getScore
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.stats.Stat
import net.minecraft.stats.StatType
import net.minecraft.stats.Stats.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes.*
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks.STONE
import net.minecraft.world.phys.AABB
import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard
import kotlin.math.abs
import kotlin.math.max

sealed class TrackedAdvancement(val advancementId: String) {

    abstract val total: Int

    val showAsPercents: Boolean get() = total >= 100

    class Objective(
        advancementId: String,
        private val names: Set<String>,
        override val total: Int,
        val resolve: (scoreboard: Scoreboard, owner: ScoreHolder, score: Int) -> Int,
    ) : TrackedAdvancement(advancementId) {
        companion object {
            val BY_OBJECTIVE_NAME = trackedAdvancements.values.filterIsInstance<Objective>()
                .flatMap { objective -> objective.names.map { it to objective } }
                .groupBy({ it.first }, { it.second })
                .mapValues { it.value.toMutableSet() }

            fun fromObjective(name: String) = BY_OBJECTIVE_NAME[name]
        }
    }

    class Statistics<T : Any>(
        advancementId: String,
        private val type: StatType<T>,
        private val argument: T,
        override val total: Int,
    ) : TrackedAdvancement(advancementId) {
        companion object {
            val BY_STAT = trackedAdvancements.values
                .filterIsInstance<Statistics<*>>()
                .groupBy { it.type.get(it.argument) }

            fun fromStat(stat: Stat<*>) = BY_STAT[stat]
        }
    }

    @Environment(EnvType.CLIENT)
    class NearbyEntities(
        advancementId: String,
        val radius: Int,
        val isBabyVariantsRequired: Boolean,
        val entityTypes: Set<EntityType<*>>,
    ) : TrackedAdvancement(advancementId) {

        override val total: Int get() = if (isBabyVariantsRequired) entityTypes.size * 2 else entityTypes.size

        fun getProgress(): Int {
            val player = Minecraft.getInstance().player?.takeIf { !it.isSpectator } ?: return 0
            val playerPosition = player.position()
            val area = AABB(playerPosition, playerPosition).inflate(radius.toDouble())
            val entities = player.level().getEntitiesOfClass(LivingEntity::class.java, area) { entity ->
                entityTypes.contains(entity.type) && playerPosition.distanceTo(entity.position()) <= radius
            }
            val uniqueVariants = entities.mapTo(mutableSetOf()) { entity ->
                val isBaby = isBabyVariantsRequired && entity.isBaby
                entity.type to isBaby
            }
            return uniqueVariants.size
        }
    }

    companion object {
        fun fromAdvancement(id: Identifier) = trackedAdvancements[id.toString()]
    }
}

private fun String.trackObjective(
    names: Set<String>,
    total: Int,
    resolver: (scoreboard: Scoreboard, owner: ScoreHolder, score: Int) -> Int = { _, _, i -> i },
) = this to TrackedAdvancement.Objective(this, names, total, resolver)

private fun String.trackObjective(
    name: String,
    total: Int,
    resolver: (scoreboard: Scoreboard, owner: ScoreHolder, score: Int) -> Int = { _, _, i -> i },
) = trackObjective(setOf(name), total, resolver)

private fun <T : Any> String.trackStat(total: Int, key: T, type: StatType<T>) =
    this to TrackedAdvancement.Statistics(this, type, key, total)

private fun String.trackStat(total: Int, key: Identifier) =
    trackStat(total, key, CUSTOM)

private fun String.trackNearbyEntities(radius: Int, hasBabyVariants: Boolean, vararg entities: EntityType<*>) =
    this to TrackedAdvancement.NearbyEntities(this, radius, hasBabyVariants, entities.toSet())

private val trackedAdvancements = mapOf(
    "blazeandcave:farming/an_apple_a_day".trackObjective("bac_apple_days", 30),
    "blazeandcave:animal/just_keeps_going".trackObjective("bac_just_keep", 3600),
    "blazeandcave:biomes/captain_america".trackObjective("bac_captain_america", 1200),
    "blazeandcave:nether/let_me_out".trackObjective("bac_let_me_out", 24000),
    "blazeandcave:mining/i_yearned_for_the_mines".trackObjective("bac_i_yearned_for_the_mines", 6000),
    "blazeandcave:farming/castaway".trackObjective("bac_castaway", 3600),
    "blazeandcave:biomes/free_diver".trackObjective("bac_underwater", 120),
    "blazeandcave:biomes/sleep_with_the_fishes".trackObjective("bac_underwater", 1200),
    "blazeandcave:statistics/passing_the_time".trackObjective("bac_day_count", 100),
    "blazeandcave:statistics/happy_new_year".trackObjective("bac_day_count", 365),
    "blazeandcave:weaponry/half_heart_life".trackObjective("bac_hh_life", 60),
    "blazeandcave:monsters/house_of_freaks".trackObjective("bac_warden_count", 5),
    "blazeandcave:challenges/overwarden".trackObjective("bac_overwarden_count", 50),
    "blazeandcave:end/divers_dozen".trackObjective("bac_divers_dozen_count", 12),
    "blazeandcave:redstone/factorio".trackObjective("bac_factorio_count", 64),
    "blazeandcave:nether/a_piglins_best_friend".trackObjective("bac_pigling", 500),
    "blazeandcave:challenges/the_world_is_ending".trackObjective("bac_ten_withers", 10),
    "blazeandcave:adventure/vault_hunter".trackObjective("bac_vault_hunter_count", 25),
    "blazeandcave:enchanting/whack_a_mole".trackObjective("bac_whack_a_mole_count", 8),
    "blazeandcave:statistics/om_nom_nom".trackObjective("bac_stat_food", 200),
    "blazeandcave:statistics/yum_yum_yummo".trackObjective("bac_stat_food", 1000),
    "blazeandcave:statistics/food_glorious_food".trackObjective("bac_stat_food", 5000),
    "blazeandcave:statistics/loot_em".trackObjective("bac_stat_loot_chest", 10),
    "blazeandcave:statistics/more_for_me".trackObjective("bac_stat_loot_chest", 100),
    "blazeandcave:statistics/i_heart_chests".trackObjective("bac_stat_loot_chest", 500),
    "blazeandcave:weaponry/artillery".trackObjective("bac_inv_artillery", 9),
    "blazeandcave:mining/chestful_of_cobblestone".trackObjective("bac_inv_chestful_of_cobblestone", 728),
    "blazeandcave:challenges/immortal".trackObjective("bac_inv_immortal", 37),
    "blazeandcave:end/rocketman".trackObjective("bac_inv_rocketman", 2368),
    "blazeandcave:redstone/on_a_rail".trackObjective(OnARail.OBJECTIVES, 1000) { scoreboard, owner, _ ->
        val isLegalX = scoreboard.getScore(OnARail.ELIGIBLE_X_FLAG, owner) == 1
        val isLegalZ = scoreboard.getScore(OnARail.ELIGIBLE_Z_FLAG, owner) == 1
        val currentX = abs(scoreboard.getScore(OnARail.CURRENT_X, owner))
        val currentZ = abs(scoreboard.getScore(OnARail.CURRENT_Z, owner))
        when {
            isLegalX && isLegalZ -> max(currentX, currentZ)
            isLegalX -> currentX
            isLegalZ -> currentZ
            else -> 0
        }
    },
    "blazeandcave:weaponry/loser".trackObjective("bac_loser", 2) { _, _, score -> if (score <= 10) 1 else 0 },

    "blazeandcave:animal/iceologer_shouldve_won".trackStat(100, GLOW_SQUID, ENTITY_KILLED),
    "blazeandcave:animal/wheres_the_honey_lebowski".trackStat(200, Items.HONEY_BOTTLE, ITEM_USED),
    "blazeandcave:end/ring_of_the_end".trackStat(20, ENDER_DRAGON, ENTITY_KILLED),
    "blazeandcave:mining/bulldozer".trackStat(10000, STONE, BLOCK_MINED),
    "blazeandcave:monsters/pupil_poppers".trackStat(1000, Items.SPIDER_EYE, ITEM_USED),
    "blazeandcave:building/insomniac".trackStat(720000, TIME_SINCE_REST),
    "blazeandcave:farming/must_be_your_birthday".trackStat(100, EAT_CAKE_SLICE),
    "blazeandcave:statistics/out_for_a_stroll".trackStat(1000000, WALK_ONE_CM),
    "blazeandcave:statistics/who_needs_cars".trackStat(5000000, WALK_ONE_CM),
    "blazeandcave:statistics/i_enjoy_long_walks_and_playing_minecraft".trackStat(25000000, WALK_ONE_CM),
    "blazeandcave:statistics/mob_hunter".trackStat(250, MOB_KILLS),
    "blazeandcave:statistics/the_parrots_and_the_bats".trackStat(100, ANIMALS_BRED),
    "blazeandcave:statistics/cupid".trackStat(500, ANIMALS_BRED),
    "blazeandcave:statistics/two_by_two".trackStat(2500, ANIMALS_BRED),
    "blazeandcave:statistics/novice_enchanter".trackStat(10, ENCHANT_ITEM),
    "blazeandcave:statistics/journeyman_enchanter".trackStat(50, ENCHANT_ITEM),
    "blazeandcave:statistics/elderly_enchanter".trackStat(250, ENCHANT_ITEM),
    "blazeandcave:statistics/the_haggler".trackStat(100, TRADED_WITH_VILLAGER),
    "blazeandcave:statistics/big_spender".trackStat(500, TRADED_WITH_VILLAGER),
    "blazeandcave:statistics/stonks".trackStat(2500, TRADED_WITH_VILLAGER),

    "blazeandcave:statistics/natural_sprinter".trackStat(1000000, SPRINT_ONE_CM),
    "blazeandcave:statistics/marathon".trackStat(4219500, SPRINT_ONE_CM),
    "blazeandcave:statistics/your_legs_must_be_tired".trackStat(25000000, SPRINT_ONE_CM),
    "blazeandcave:statistics/sneaky_snitch".trackStat(10000, CROUCH_ONE_CM),
    "blazeandcave:statistics/black_belt_ninja".trackStat(100000, CROUCH_ONE_CM),
    "blazeandcave:statistics/ancient_kung_fu_master".trackStat(1000000, CROUCH_ONE_CM),
    "blazeandcave:statistics/laps_in_the_pool".trackStat(100000, SWIM_ONE_CM),
    "blazeandcave:statistics/olympic_athlete".trackStat(1000000, SWIM_ONE_CM),
    "blazeandcave:statistics/olympic_gold_medallist".trackStat(5000000, SWIM_ONE_CM),
    "blazeandcave:statistics/luxury_cruise".trackStat(100000, BOAT_ONE_CM),
    "blazeandcave:statistics/pirate_captain".trackStat(1000000, BOAT_ONE_CM),
    "blazeandcave:statistics/sailor_of_the_seven_seas".trackStat(5000000, BOAT_ONE_CM),
    "blazeandcave:statistics/minecart_rider".trackStat(100000, MINECART_ONE_CM),
    "blazeandcave:statistics/i_like_trains".trackStat(1000000, MINECART_ONE_CM),
    "blazeandcave:statistics/global_railway_network".trackStat(5000000, MINECART_ONE_CM),
    "blazeandcave:statistics/pig_training".trackStat(10000, PIG_ONE_CM),
    "blazeandcave:statistics/snout_500".trackStat(100000, PIG_ONE_CM),
    "blazeandcave:statistics/lightning_mcpig".trackStat(1000000, PIG_ONE_CM),
    "blazeandcave:statistics/taking_it_in_stride".trackStat(10000, STRIDER_ONE_CM),
    "blazeandcave:statistics/who_needs_boats".trackStat(100000, STRIDER_ONE_CM),
    "blazeandcave:statistics/this_ride_is_lit_yo".trackStat(1000000, STRIDER_ONE_CM),
    "blazeandcave:statistics/horse_training".trackStat(100000, HORSE_ONE_CM),
    "blazeandcave:statistics/sheriff_firearm".trackStat(1000000, HORSE_ONE_CM),
    "blazeandcave:statistics/scourge_of_the_west".trackStat(5000000, HORSE_ONE_CM),
    "blazeandcave:statistics/take_to_the_skies".trackStat(1000000, AVIATE_ONE_CM),
    "blazeandcave:statistics/supersonic".trackStat(10000000, AVIATE_ONE_CM),
    "blazeandcave:statistics/frequent_flyer".trackStat(100000000, AVIATE_ONE_CM),
    "blazeandcave:statistics/spring_in_your_step".trackStat(1000, JUMP),
    "blazeandcave:statistics/boing_boing".trackStat(10000, JUMP),
    "blazeandcave:statistics/jumping_jacks".trackStat(100000, JUMP),
    "blazeandcave:statistics/bloodthirsty".trackStat(2500, MOB_KILLS),
    "blazeandcave:statistics/kill_or_be_killed".trackStat(25000, MOB_KILLS),

    "blazeandcave:animal/animal_kingdom".trackNearbyEntities(
        radius = 32, hasBabyVariants = false,
        CHICKEN, SHEEP, PIG, COW, MOOSHROOM, HORSE, DONKEY, MULE, SKELETON_HORSE, WOLF, CAT, OCELOT, PARROT, LLAMA,
        TADPOLE, COD, SALMON, TROPICAL_FISH, PUFFERFISH, FROG, AXOLOTL, SQUID, GLOW_SQUID, TURTLE, DOLPHIN,
        BAT, FOX, RABBIT, BEE, PANDA, POLAR_BEAR, CAMEL, SNIFFER, ARMADILLO, GOAT,
        STRIDER, HOGLIN,
    ),
    "blazeandcave:monsters/family_reunion".trackNearbyEntities(
        radius = 5, hasBabyVariants = true,
        ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN,
    ),
    "blazeandcave:monsters/bone_to_party".trackNearbyEntities(
        radius = 5, hasBabyVariants = false,
        SKELETON, STRAY, BOGGED, WITHER_SKELETON, SKELETON_HORSE, WITHER,
    ),
)

object OnARail {
    const val ELIGIBLE_X_FLAG = "bac_oar_eligible_x"
    const val ELIGIBLE_Z_FLAG = "bac_oar_eligible_z"
    const val CURRENT_X = "bac_oar_current_x"
    const val CURRENT_Z = "bac_oar_current_z"
    val OBJECTIVES = setOf(ELIGIBLE_X_FLAG, ELIGIBLE_Z_FLAG, CURRENT_X, CURRENT_Z)
}
