package io.github.diskria.achievetodo.etxensions

import net.minecraft.world.scores.ScoreHolder
import net.minecraft.world.scores.Scoreboard

fun Scoreboard.getScore(objectiveName: String, scoreHolder: ScoreHolder): Int =
    getObjective(objectiveName)?.let { getOrCreatePlayerScore(scoreHolder, it).get() } ?: 0
