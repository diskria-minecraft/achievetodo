package io.github.diskria.achievetodo.ability

import io.github.diskria.achievetodo.server.Constants
import kotlin.math.roundToInt
import kotlin.random.Random

class ChaosProgressionGenerator private constructor(private val random: Random) {

    private val progression = Progressions.hard.toMutableMap()

    private fun shuffle() = apply {
        val shuffledKeys = progression.keys.shuffled(random)
        val shuffledValues = progression.values.shuffled(random)

        progression.clear()
        progression.putAll(shuffledKeys.zip(shuffledValues))
    }

    private fun shift() = apply {
        val updated = progression.mapValues { (ability, oldCount) ->
            val temperature = ability.chaosTemperature
            val newCount = if (temperature <= 0) {
                oldCount
            } else {
                val shiftRange = (temperature * 0.5f).roundToInt().coerceAtLeast(1)
                val shiftMagnitude = random.nextInt(shiftRange)
                val shift = if (random.nextBoolean()) shiftMagnitude else -shiftMagnitude
                oldCount + shift
            }
            newCount.coerceIn(Constants.Progression.MIN_ADVANCEMENTS_COUNT, MAX_ADVANCEMENTS_COUNT)
        }
        progression.putAll(updated)
    }

    private fun fixPriority() = apply {
        val updated = progression.mapValues { (ability, oldCount) ->
            val baseCount = Progressions.hard[ability] ?: return@mapValues oldCount
            val temperature = ability.chaosTemperature
            when {
                temperature <= 0 -> oldCount
                else -> {
                    val ratio = (1.0 / (1 + temperature)) * random.nextDouble()
                    (oldCount + ratio * (baseCount - oldCount)).roundToInt()
                }
            }
        }
        progression.putAll(updated)
    }

    private fun applySpecialFlags() {
        val chance = random.nextDouble() * 100.0
        val updated = progression.mapValues { (ability, oldCount) ->
            when {
                ability == AbilityType.VISION || chance < UNLOCKED_GIFT_CHANCE -> {
                    Constants.Progression.INITIALLY_UNLOCKED_FLAG
                }

                ability.chaosTemperature > 0 && chance < (PERMALOCK_CHANCE * ability.chaosTemperature) -> {
                    Constants.Progression.PERMANENTLY_LOCKED_FLAG
                }

                else -> oldCount
            }
        }
        progression.putAll(updated)
    }

    companion object {
        private const val MAX_ADVANCEMENTS_COUNT = 1000
        private const val UNLOCKED_GIFT_CHANCE = 1.0
        private const val PERMALOCK_CHANCE = 0.2

        fun generate(seed: Long): Map<AbilityType, Int> =
            ChaosProgressionGenerator(Random(seed)).run {
                shuffle()
                shift()
                fixPriority()
                applySpecialFlags()
                progression
            }
    }
}
