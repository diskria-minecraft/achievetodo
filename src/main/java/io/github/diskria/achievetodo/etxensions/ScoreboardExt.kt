package io.github.diskria.achievetodo.etxensions

import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard

fun Scoreboard.getScore(objectiveName: String, owner: ScoreHolder): Int =
    getObjective(objectiveName)?.let { getPlayerScoreInfo(owner, it)?.value() } ?: 0
