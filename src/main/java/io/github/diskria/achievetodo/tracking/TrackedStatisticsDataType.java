package io.github.diskria.achievetodo.tracking;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum TrackedStatisticsDataType {

    ICEOLOGER_SHOULDVE_WON(
        "blazeandcave:animal/iceologer_shouldve_won",
        Stats.KILLED,
        EntityType.GLOW_SQUID,
        100,
        false
    ),
    WHERES_THE_HONEY_LEBOWSKI(
        "blazeandcave:animal/wheres_the_honey_lebowski",
        Stats.USED,
        Items.HONEY_BOTTLE,
        200,
        false
    ),
    INSOMNIAC(
        "blazeandcave:building/insomniac",
        Stats.CUSTOM,
        Stats.TIME_SINCE_REST,
        720_000,
        true
    ),
    RING_OF_THE_END(
        "blazeandcave:end/ring_of_the_end",
        Stats.KILLED,
        EntityType.ENDER_DRAGON,
        20,
        false
    ),
    MUST_BE_YOUR_BIRTHDAY(
        "blazeandcave:farming/must_be_your_birthday",
        Stats.CUSTOM,
        Stats.EAT_CAKE_SLICE,
        100,
        false
    ),
    BULLDOZER(
        "blazeandcave:mining/bulldozer",
        Stats.MINED,
        Blocks.STONE,
        10_000,
        true
    ),
    PUPIL_POPPERS(
        "blazeandcave:monsters/pupil_poppers",
        Stats.USED,
        Items.SPIDER_EYE,
        1_000,
        false
    ),
    OUT_FOR_A_STROLL(
        "blazeandcave:statistics/out_for_a_stroll",
        Stats.CUSTOM,
        Stats.WALK_ONE_CM,
        1_000_000,
        true
    ),
    WHO_NEEDS_CARS(
        "blazeandcave:statistics/who_needs_cars",
        Stats.CUSTOM,
        Stats.WALK_ONE_CM,
        5_000_000,
        true
    ),
    I_ENJOY_LONG_WALKS_AND_PLAYING_MINECRAFT(
        "blazeandcave:statistics/i_enjoy_long_walks_and_playing_minecraft",
        Stats.CUSTOM,
        Stats.WALK_ONE_CM,
        25_000_000,
        true
    ),
    NATURAL_SPRINTER(
        "blazeandcave:statistics/natural_sprinter",
        Stats.CUSTOM,
        Stats.SPRINT_ONE_CM,
        1_000_000,
        true
    ),
    MARATHON(
        "blazeandcave:statistics/marathon",
        Stats.CUSTOM,
        Stats.SPRINT_ONE_CM,
        4_219_500,
        true
    ),
    YOUR_LEGS_MUST_BE_TIRED(
        "blazeandcave:statistics/your_legs_must_be_tired",
        Stats.CUSTOM,
        Stats.SPRINT_ONE_CM,
        25_000_000,
        true
    ),
    SNEAKY_SNITCH(
        "blazeandcave:statistics/sneaky_snitch",
        Stats.CUSTOM,
        Stats.CROUCH_ONE_CM,
        10_000,
        true
    ),
    BLACK_BELT_NINJA(
        "blazeandcave:statistics/black_belt_ninja",
        Stats.CUSTOM,
        Stats.CROUCH_ONE_CM,
        100_000,
        true
    ),
    ANCIENT_KUNG_FU_MASTER(
        "blazeandcave:statistics/ancient_kung_fu_master",
        Stats.CUSTOM,
        Stats.CROUCH_ONE_CM,
        1_000_000,
        true
    ),
    LAPS_IN_THE_POOL(
        "blazeandcave:statistics/laps_in_the_pool",
        Stats.CUSTOM,
        Stats.SWIM_ONE_CM,
        100_000,
        true
    ),
    OLYMPIC_ATHLETE(
        "blazeandcave:statistics/olympic_athlete",
        Stats.CUSTOM,
        Stats.SWIM_ONE_CM,
        1_000_000,
        true
    ),
    OLYMPIC_GOLD_MEDALLIST(
        "blazeandcave:statistics/olympic_gold_medallist",
        Stats.CUSTOM,
        Stats.SWIM_ONE_CM,
        5_000_000,
        true
    ),
    LUXURY_CRUISE(
        "blazeandcave:statistics/luxury_cruise",
        Stats.CUSTOM,
        Stats.BOAT_ONE_CM,
        100_000,
        true
    ),
    PIRATE_CAPTAIN(
        "blazeandcave:statistics/pirate_captain",
        Stats.CUSTOM,
        Stats.BOAT_ONE_CM,
        1_000_000,
        true
    ),
    SAILOR_OF_THE_SEVEN_SEAS(
        "blazeandcave:statistics/sailor_of_the_seven_seas",
        Stats.CUSTOM,
        Stats.BOAT_ONE_CM,
        5_000_000,
        true
    ),
    MINECART_RIDER(
        "blazeandcave:statistics/minecart_rider",
        Stats.CUSTOM,
        Stats.MINECART_ONE_CM,
        100_000,
        true
    ),
    I_LIKE_TRAINS(
        "blazeandcave:statistics/i_like_trains",
        Stats.CUSTOM,
        Stats.MINECART_ONE_CM,
        1_000_000,
        true
    ),
    GLOBAL_RAILWAY_NETWORK(
        "blazeandcave:statistics/global_railway_network",
        Stats.CUSTOM,
        Stats.MINECART_ONE_CM,
        5_000_000,
        true
    ),
    PIG_TRAINING(
        "blazeandcave:statistics/pig_training",
        Stats.CUSTOM,
        Stats.PIG_ONE_CM,
        10_000,
        true
    ),
    SNOUT_500(
        "blazeandcave:statistics/snout_500",
        Stats.CUSTOM,
        Stats.PIG_ONE_CM,
        100_000,
        true
    ),
    LIGHTNING_MCPIG(
        "blazeandcave:statistics/lightning_mcpig",
        Stats.CUSTOM,
        Stats.PIG_ONE_CM,
        1_000_000,
        true
    ),
    TAKING_IT_IN_STRIDE(
        "blazeandcave:statistics/taking_it_in_stride",
        Stats.CUSTOM,
        Stats.STRIDER_ONE_CM,
        10_000,
        true
    ),
    WHO_NEEDS_BOATS(
        "blazeandcave:statistics/who_needs_boats",
        Stats.CUSTOM,
        Stats.STRIDER_ONE_CM,
        100_000,
        true
    ),
    THIS_RIDE_IS_LIT_YO(
        "blazeandcave:statistics/this_ride_is_lit_yo",
        Stats.CUSTOM,
        Stats.STRIDER_ONE_CM,
        1_000_000,
        true
    ),
    HORSE_TRAINING(
        "blazeandcave:statistics/horse_training",
        Stats.CUSTOM,
        Stats.HORSE_ONE_CM,
        100_000,
        true
    ),
    SHERIFF_FIREARM(
        "blazeandcave:statistics/sheriff_firearm",
        Stats.CUSTOM,
        Stats.HORSE_ONE_CM,
        1_000_000,
        true
    ),
    SCOURGE_OF_THE_WEST(
        "blazeandcave:statistics/scourge_of_the_west",
        Stats.CUSTOM,
        Stats.HORSE_ONE_CM,
        5_000_000,
        true
    ),
    TAKE_TO_THE_SKIES(
        "blazeandcave:statistics/take_to_the_skies",
        Stats.CUSTOM,
        Stats.AVIATE_ONE_CM,
        1_000_000,
        true
    ),
    SUPERSONIC(
        "blazeandcave:statistics/supersonic",
        Stats.CUSTOM,
        Stats.AVIATE_ONE_CM,
        10_000_000,
        true
    ),
    FREQUENT_FLYER(
        "blazeandcave:statistics/frequent_flyer",
        Stats.CUSTOM,
        Stats.AVIATE_ONE_CM,
        100_000_000,
        true
    ),
    SPRING_IN_YOUR_STEP(
        "blazeandcave:statistics/spring_in_your_step",
        Stats.CUSTOM,
        Stats.JUMP,
        1_000,
        true
    ),
    BOING_BOING(
        "blazeandcave:statistics/boing_boing",
        Stats.CUSTOM,
        Stats.JUMP,
        10_000,
        true
    ),
    JUMPING_JACKS(
        "blazeandcave:statistics/jumping_jacks",
        Stats.CUSTOM,
        Stats.JUMP,
        100_000,
        true
    ),
    MOB_HUNTER(
        "blazeandcave:statistics/mob_hunter",
        Stats.CUSTOM,
        Stats.MOB_KILLS,
        250,
        false
    ),
    BLOODTHIRSTY(
        "blazeandcave:statistics/bloodthirsty",
        Stats.CUSTOM,
        Stats.MOB_KILLS,
        2_500,
        true
    ),
    KILL_OR_BE_KILLED(
        "blazeandcave:statistics/kill_or_be_killed",
        Stats.CUSTOM,
        Stats.MOB_KILLS,
        25_000,
        true
    ),
    THE_PARROTS_AND_THE_BATS(
        "blazeandcave:statistics/the_parrots_and_the_bats",
        Stats.CUSTOM,
        Stats.ANIMALS_BRED,
        100,
        false
    ),
    CUPID(
        "blazeandcave:statistics/cupid",
        Stats.CUSTOM,
        Stats.ANIMALS_BRED,
        500,
        false
    ),
    TWO_BY_TWO(
        "blazeandcave:statistics/two_by_two",
        Stats.CUSTOM,
        Stats.ANIMALS_BRED,
        2_500,
        false
    ),
    NOVICE_ENCHANTER(
        "blazeandcave:statistics/novice_enchanter",
        Stats.CUSTOM,
        Stats.ENCHANT_ITEM,
        10,
        false
    ),
    JOURNEYMAN_ENCHANTER(
        "blazeandcave:statistics/journeyman_enchanter",
        Stats.CUSTOM,
        Stats.ENCHANT_ITEM,
        50,
        false
    ),
    ELDERLY_ENCHANTER(
        "blazeandcave:statistics/elderly_enchanter",
        Stats.CUSTOM,
        Stats.ENCHANT_ITEM,
        250,
        false
    ),
    THE_HAGGLER(
        "blazeandcave:statistics/the_haggler",
        Stats.CUSTOM,
        Stats.TRADED_WITH_VILLAGER,
        100,
        false
    ),
    BIG_SPENDER(
        "blazeandcave:statistics/big_spender",
        Stats.CUSTOM,
        Stats.TRADED_WITH_VILLAGER,
        500,
        false
    ),
    STONKS(
        "blazeandcave:statistics/stonks",
        Stats.CUSTOM,
        Stats.TRADED_WITH_VILLAGER,
        2_500,
        false
    );

    public static final Map<Stat<?>, Set<TrackedStatisticsDataType>> STATISTICS_DATA = new HashMap<>();

    static {
        for (TrackedStatisticsDataType type : values()) {
            STATISTICS_DATA
                .computeIfAbsent(type.statisticsData, k -> new HashSet<>())
                .add(type);
        }
    }

    private final String advancementId;
    private final Stat<?> statisticsData;
    private final int finalValue;
    private final boolean isPercentage;

    <T> TrackedStatisticsDataType(
        String advancementId,
        @NotNull StatType<T> type,
        T key,
        int finalValue,
        boolean isPercentage
    ) {
        this.advancementId = advancementId;
        this.statisticsData = type.getOrCreateStat(key);
        this.finalValue = finalValue;
        this.isPercentage = isPercentage;
    }

    public static @Nullable Set<TrackedStatisticsDataType> findByStat(Stat<?> stat) {
        return STATISTICS_DATA.get(stat);
    }

    public static @Nullable TrackedStatisticsDataType findByAdvancement(@NotNull Identifier advancementId) {
        for (TrackedStatisticsDataType type : TrackedStatisticsDataType.values()) {
            if (advancementId.toString().equals(type.advancementId)) {
                return type;
            }
        }
        return null;
    }

    public int getFinalValue() {
        return finalValue;
    }

    public boolean isPercentage() {
        return isPercentage;
    }
}
