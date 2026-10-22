package io.github.diskria.achievetodo.tracking;

import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public enum TrackedScoreType {

    AN_APPLE_A_DAY(
        "blazeandcave:farming/an_apple_a_day",
        30,
        false,
        "bac_apple_days"
    ),
    JUST_KEEPS_GOING(
        "blazeandcave:animal/just_keeps_going",
        3600,
        true,
        "bac_just_keep"
    ),
    CAPTAIN_AMERICA(
        "blazeandcave:biomes/captain_america",
        1200,
        true,
        "bac_captain_america"
    ),
    LET_ME_OUT(
        "blazeandcave:nether/let_me_out",
        24000,
        true,
        "bac_let_me_out"
    ),
    I_YEARNED_FOR_THE_MINES(
        "blazeandcave:mining/i_yearned_for_the_mines",
        6000,
        true,
        "bac_i_yearned_for_the_mines"
    ),
    CASTAWAY(
        "blazeandcave:farming/castaway",
        3600,
        true,
        "bac_castaway"
    ),
    FREE_DIVER(
        "blazeandcave:biomes/free_diver",
        120,
        true,
        "bac_underwater"
    ),
    SLEEP_WITH_THE_FISHES(
        "blazeandcave:biomes/sleep_with_the_fishes",
        1200,
        true,
        "bac_underwater"
    ),
    PASSING_THE_TIME(
        "blazeandcave:statistics/passing_the_time",
        100,
        false,
        "bac_day_count"
    ),
    HAPPY_NEW_YEAR(
        "blazeandcave:statistics/happy_new_year",
        365,
        false,
        "bac_day_count"
    ),
    HALF_HEART_LIFE(
        "blazeandcave:weaponry/half_heart_life",
        60,
        false,
        "bac_hh_life"
    ),
    HOUSE_OF_FREAKS(
        "blazeandcave:monsters/house_of_freaks",
        5,
        false,
        "bac_warden_count"
    ),
    OVERWARDEN(
        "blazeandcave:challenges/overwarden",
        50,
        false,
        "bac_overwarden_count"
    ),
    DIVERS_DOZEN(
        "blazeandcave:end/divers_dozen",
        12,
        false,
        "bac_divers_dozen_count"
    ),
    FACTORIO(
        "blazeandcave:redstone/factorio",
        64,
        false,
        "bac_factorio_count"
    ),
    A_PIGLINS_BEST_FRIEND(
        "blazeandcave:nether/a_piglins_best_friend",
        500,
        false,
        "bac_pigling"
    ),
    THE_WORLD_IS_ENDING(
        "blazeandcave:challenges/the_world_is_ending",
        10,
        false,
        "bac_ten_withers"
    ),
    VAULT_HUNTER(
        "blazeandcave:adventure/vault_hunter",
        25,
        false,
        "bac_vault_hunter_count"
    ),
    WHACK_A_MOLE(
        "blazeandcave:enchanting/whack_a_mole",
        8,
        false,
        "bac_whack_a_mole_count"
    ),
    OM_NOM_NOM(
        "blazeandcave:statistics/om_nom_nom",
        200,
        false,
        "bac_stat_food"
    ),
    YUM_YUM_YUMMO(
        "blazeandcave:statistics/yum_yum_yummo",
        1000,
        false,
        "bac_stat_food"
    ),
    FOOD_GLORIOUS_FOOD(
        "blazeandcave:statistics/food_glorious_food",
        5000,
        false,
        "bac_stat_food"
    ),
    LOOT_EM(
        "blazeandcave:statistics/loot_em",
        10,
        false,
        "bac_stat_loot_chest"
    ),
    MORE_FOR_ME(
        "blazeandcave:statistics/more_for_me",
        100,
        false,
        "bac_stat_loot_chest"
    ),
    I_HEART_CHESTS(
        "blazeandcave:statistics/i_heart_chests",
        500,
        false,
        "bac_stat_loot_chest"
    ),
    ON_A_RAIL(
        "blazeandcave:redstone/on_a_rail",
        1000,
        true,
        "bac_oar_eligible_x", "bac_oar_eligible_z",
        "bac_oar_current_x", "bac_oar_current_z"
    ),
    ARTILLERY(
        "blazeandcave:weaponry/artillery",
        9,
        false,
        "bac_inv_artillery"
    ),
    CHESTFUL_OF_COBBLESTONE(
        "blazeandcave:mining/chestful_of_cobblestone",
        1728,
        true,
        "bac_inv_chestful_of_cobblestone"
    ),
    IMMORTAL(
        "blazeandcave:challenges/immortal",
        37,
        false,
        "bac_inv_immortal"
    ),
    ROCKETMAN(
        "blazeandcave:end/rocketman",
        2368,
        true,
        "bac_inv_rocketman"
    ),
    LOSER(
        "blazeandcave:weaponry/loser",
        2,
        false,
        "bac_loser"
    );

    public static final Map<String, Set<TrackedScoreType>> SCORES = new HashMap<>();

    static {
        for (TrackedScoreType type : values()) {
            for (String objectiveName : type.objectiveNames) {
                SCORES
                    .computeIfAbsent(objectiveName, k -> new HashSet<>())
                    .add(type);
            }
        }
    }

    private final String advancementId;
    private final int finalValue;
    private final boolean isPercentage;
    private final Set<String> objectiveNames;

    TrackedScoreType(String advancementId, int finalValue, boolean isPercentage, String... objectiveNames) {
        this.advancementId = advancementId;
        this.finalValue = finalValue;
        this.isPercentage = isPercentage;
        this.objectiveNames = Arrays.stream(objectiveNames).collect(Collectors.toUnmodifiableSet());
    }

    public static @Nullable TrackedScoreType findByAdvancement(@NotNull Identifier advancementId) {
        for (TrackedScoreType type : TrackedScoreType.values()) {
            if (advancementId.toString().equals(type.advancementId)) {
                return type;
            }
        }
        return null;
    }

    public static @Nullable Set<TrackedScoreType> findByObjectiveName(String objectiveName) {
        Set<TrackedScoreType> progressTypes = null;
        for (TrackedScoreType type : values()) {
            if (type.objectiveNames.contains(objectiveName)) {
                if (progressTypes == null) {
                    progressTypes = new HashSet<>();
                }
                progressTypes.add(type);
            }
        }
        return progressTypes;
    }

    public int getFinalValue() {
        return finalValue;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    public int fixScore(Scoreboard scoreboard, ScoreHolder scoreHolder, int score) {
        if (this == TrackedScoreType.ON_A_RAIL) {
            ReadableScoreboardScore eligibleXScore = scoreboard.getScore(
                scoreHolder, scoreboard.getNullableObjective("bac_oar_eligible_x")
            );
            if (eligibleXScore != null) {
                if (eligibleXScore.getScore() == 1) {
                    ReadableScoreboardScore currentXScore = scoreboard.getScore(
                        scoreHolder, scoreboard.getNullableObjective("bac_oar_current_x")
                    );
                    if (currentXScore != null) {
                        score = Math.abs(currentXScore.getScore());
                    }
                } else {
                    ReadableScoreboardScore eligibleZScore = scoreboard.getScore(
                        scoreHolder, scoreboard.getNullableObjective("bac_oar_eligible_z")
                    );
                    if (eligibleZScore != null) {
                        if (eligibleZScore.getScore() == 1) {
                            ReadableScoreboardScore currentZScore = scoreboard.getScore(
                                scoreHolder, scoreboard.getNullableObjective("bac_oar_current_z")
                            );
                            if (currentZScore == null) {
                                return score;
                            }
                            score = Math.abs(currentZScore.getScore());
                        } else {
                            score = 0;
                        }
                    }
                }
            }
        } else if (this == TrackedScoreType.LOSER) {
            score = score <= 10 ? 1 : 0;
        }
        return score;
    }
}
