package io.github.diskria.achievetodo.client

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.ability.AbilitiesHierarchyLayerType
import io.github.diskria.achievetodo.ability.AbilityType
import io.github.diskria.achievetodo.ability.DimensionType
import io.github.diskria.achievetodo.ability.Landmark
import io.github.diskria.achievetodo.payloads.clientbound.*
import io.github.diskria.achievetodo.payloads.serverbound.DemystifyAbilityPayload.Client.send
import io.github.diskria.achievetodo.server.Constants
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.AABB

class AchieveToDoClient : ClientModInitializer {

    override fun onInitializeClient() {
        startPayloadListeners()
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            abilitiesConfiguration.clear()
            lockedLandmarksSet.clear()
            lockedLandmarks = emptyList()
            obtainedAdvancementsCount = Int.MIN_VALUE
            trackedAdvancementProgresses.clear()
        }
    }

    private fun startPayloadListeners() {
        SyncAbilitiesConfigurationPayload.Client.listen { config ->
            abilitiesConfiguration.clear()
            abilitiesConfiguration.putAll(config)
            abilityRows.clear()
            sortLockedLandmarks()
        }
        SyncObtainedAdvancementsCountPayload.Client.listen { count ->
            obtainedAdvancementsCount = count
        }
        LandmarksLockedStatusChangedPayload.Client.listen { landmarks, isLocked ->
            if (isLocked) {
                lockedLandmarksSet += landmarks
            } else {
                lockedLandmarksSet -= landmarks
            }
            sortLockedLandmarks()
        }
        LandmarkTypesUnlockedPayload.Client.listen { landmarks ->
            lockedLandmarksSet.removeAll { it.type in landmarks }
            sortLockedLandmarks()
        }
        LockedLandmarkResizedPayload.Client.listen { type, oldArea, newArea ->
            val oldLandmark = lockedLandmarksSet.find { it.type == type && it.area == oldArea } ?: return@listen
            lockedLandmarksSet -= oldLandmark
            lockedLandmarksSet += oldLandmark.copy(area = newArea)
            sortLockedLandmarks()
        }
        TrackedAdvancementProgressChangedPayload.Client.listen { advancementId, progress ->
            trackedAdvancementProgresses[advancementId] = progress
        }
        CheckTargetInLockedLandmarkPayload.Client.listen { dimensionType, aabb ->
            isTargetInLockedLandmark(dimensionType, aabb)
        }
    }

    private fun sortLockedLandmarks() {
        lockedLandmarks = lockedLandmarksSet.sortedWith(
            compareBy<Landmark> { it.area.dimension }
                .thenByDescending {
                    val count = abilitiesConfiguration[AbilityType.fromLandmark(it.type)] ?: 0
                    if (count == Constants.Progression.PERMANENTLY_LOCKED_FLAG) Int.MAX_VALUE else count
                }
                .thenByDescending { it.type.ordinal }
        )
    }

    companion object {
        private val lockedLandmarksSet = mutableSetOf<Landmark>()
        var lockedLandmarks = emptyList<Landmark>()

        private val trackedAdvancementProgresses = Object2IntOpenHashMap<String>()
        private val abilityRows = mutableListOf<List<AbilityType>>()
        private val abilitiesConfiguration = mutableMapOf<AbilityType, Int>()

        private var obtainedAdvancementsCount = Int.MIN_VALUE

        fun getRequiredAdvancementsCount(ability: AbilityType): Int =
            abilitiesConfiguration[ability] ?: 0

        val isNotReady: Boolean
            get() = abilitiesConfiguration.isEmpty() || obtainedAdvancementsCount == Int.MIN_VALUE

        fun getTrackedAdvancementProgress(advancementId: String): Int =
            trackedAdvancementProgresses.getOrDefault(advancementId, 0)

        fun translate(keySuffix: String, vararg args: Any): MutableComponent =
            Component.translatable("${AchieveToDoMod.ID}.$keySuffix", *args)

        fun isAbilityLocked(ability: AbilityType): Boolean =
            isAbilityLocked(ability, false)

        fun isAbilityLocked(abilityType: AbilityType, checkOnly: Boolean = false): Boolean {
            val player = Minecraft.getInstance().player?.takeIf { !it.isCreative && !it.isSpectator } ?: return false
            if (isNotReady) return true
            val requiredCount = abilitiesConfiguration[abilityType] ?: return false
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG) return false
            if (requiredCount != Constants.Progression.PERMANENTLY_LOCKED_FLAG &&
                obtainedAdvancementsCount >= requiredCount
            ) {
                return false
            }
            if (!checkOnly) {
                player.sendOverlayMessage(
                    when (requiredCount) {
                        Constants.Progression.PERMANENTLY_LOCKED_FLAG -> abilityType.buildPermanentlyLockedMessage()
                        else -> abilityType.buildUnlockProgressMessage(requiredCount - obtainedAdvancementsCount)
                    }
                )
                send(abilityType)
            }
            return true
        }

        fun isTargetInLockedLandmark(dimension: DimensionType, aabb: AABB): Boolean =
            lockedLandmarks.any { (type, area) ->
                area.intersects(dimension, aabb) && AbilityType.fromLandmark(type)?.let { isAbilityLocked(it) } == true
            }

        fun getAbilityRows(): List<List<AbilityType>>? {
            if (isNotReady) return null

            if (abilityRows.isEmpty()) {
                val abilitiesByCategory = AbilityType.entries
                    .sortedWith(
                        compareBy<AbilityType> { ability ->
                            when (abilitiesConfiguration[ability]) {
                                Constants.Progression.INITIALLY_UNLOCKED_FLAG -> 0
                                Constants.Progression.PERMANENTLY_LOCKED_FLAG -> 2
                                else -> 1
                            }
                        }
                            .thenBy { abilitiesConfiguration[it] ?: 0 }
                            .thenBy { it.ordinal }
                    )
                    .groupBy { it.hierarchyLayerType }

                AbilitiesHierarchyLayerType.entries.forEach { category ->
                    val categoryAbilities = abilitiesByCategory[category] ?: return@forEach
                    val categoryRowsCount = category.rowsCount
                    if (categoryRowsCount == 1) {
                        abilityRows.add(categoryAbilities)
                    } else {
                        check(categoryAbilities.size % categoryRowsCount == 0) {
                            "Abilities in category $category cannot be evenly distributed across $categoryRowsCount rows."
                        }
                        val rowLength = categoryAbilities.size / categoryRowsCount
                        val categoryRows = categoryAbilities.chunked(rowLength)

                        val half = categoryRowsCount / 2
                        abilityRows.addAll(0, categoryRows.subList(0, half))
                        abilityRows.addAll(categoryRows.subList(half, categoryRowsCount))
                    }
                }
            }
            return abilityRows
        }
    }
}
