package io.github.diskria.achievetodo.server

import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.ability.*
import io.github.diskria.achievetodo.client.InternalPack
import io.github.diskria.achievetodo.datagen.AbilityAdvancementsTabGenerator
import io.github.diskria.achievetodo.landmarks.landmarkBB
import io.github.diskria.achievetodo.landmarks.landmarkType
import io.github.diskria.achievetodo.payloads.clientbound.CheckTargetInLockedLandmarkPayload
import io.github.diskria.achievetodo.payloads.clientbound.TrackedAdvancementProgressChangedPayload
import io.github.diskria.achievetodo.payloads.serverbound.DemystifyAbilityPayload
import io.github.diskria.achievetodo.qol.TrackedAdvancement
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.ChatFormatting
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ServerScoreboard
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.AABB
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds

class AchieveToDoServer {

    private val obtainedAdvancementsCountByPlayers = Object2IntOpenHashMap<UUID>()
    private val trackedAdvancementProgresses = mutableMapOf<TrackedAdvancement, MutableMap<UUID, Int>>()
    private val landmarksByChunks = mutableMapOf<ChunkPos, MutableList<Landmark>>()
    private val playersByLockedLandmarkTypes = mutableMapOf<LandmarkType, MutableSet<UUID>>()

    private var currentBacapMode: BacapMode? = null
    private var currentScoreboardObjective: Objective? = null
    private var currentScoreboardDisplaySlot: DisplaySlot? = null
    private var abilitiesConfiguration: MutableMap<AbilityType, Int>? = null

    fun prepareScoreboard(scoreboard: ServerScoreboard) {
        val oldAdvancementsMode = currentBacapMode
        val oldScoreboardObjective = currentScoreboardObjective
        val oldScoreboardDisplaySlot = currentScoreboardDisplaySlot

        currentBacapMode = null
        currentScoreboardObjective = null
        currentScoreboardDisplaySlot = null

        arrayOf(DisplaySlot.SIDEBAR, DisplaySlot.LIST, DisplaySlot.BELOW_NAME).forEach { slot ->
            val objective = scoreboard.getDisplayObjective(slot) ?: return@forEach
            val bacapMode = BacapMode.fromObjective(objective.name) ?: return@forEach
            currentBacapMode = bacapMode
            currentScoreboardObjective = objective
            currentScoreboardDisplaySlot = slot
            return@forEach
        }
        if (currentBacapMode == null ||
            currentScoreboardObjective == null ||
            currentScoreboardDisplaySlot == null
        ) {
            AchieveToDoMod.logger.error(
                "Can't find scoreboard objective with advancements counter! " +
                    "Please check that BACAP datapack is installed " +
                    "and enable advancements counter in the sidebar, tab list or below player names."
            )
            return
        }
        if (currentBacapMode != oldAdvancementsMode ||
            currentScoreboardObjective !== oldScoreboardObjective ||
            currentScoreboardDisplaySlot != oldScoreboardDisplaySlot
        ) {
            AchieveToDoMod.logger.info(
                "Scoreboard objective with advancements counter found: advancements mode = {}, objective name = {}, display slot = {}",
                currentBacapMode,
                currentScoreboardObjective!!.name,
                currentScoreboardDisplaySlot
            )
            scoreboard.server.playerList.players.forEach { player ->
                updateObtainedCount(scoreboard, player)
            }
        }
    }

    fun setObtainedCount(player: ServerPlayer, count: Int) {
//        if (isNotReady) return
//        val playerUuid = player.getUUID()
//        val oldCount = obtainedAdvancementsCountByPlayers.getOrDefault(playerUuid, Int.MIN_VALUE)
//        obtainedAdvancementsCountByPlayers[playerUuid] = count
//        var unlockedLandmarkTypes: MutableSet<LandmarkType?>? = null
//        var lockedLandmarks: MutableMap<LandmarkType, MutableList<LandmarkArea>>? = null
//
//        for (abilityType in AbilityType.entries) {
//            val requiredCount = abilitiesConfiguration!![abilityType]!!
//            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
//                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
//            ) {
//                continue
//            }
//            val isLock = when {
//                requiredCount in (oldCount + 1)..count -> {
//                    unlockAbility(player, abilityType)
//                    false
//                }
//
//                oldCount == Int.MIN_VALUE || requiredCount in (count + 1)..oldCount -> {
//                    lockAbility(player, abilityType)
//                    true
//                }
//
//                else -> continue
//            }
//            val landmarkType = abilityType.landmark
//            if (landmarkType != null) {
//                if (isLock) {
//                    playersByLockedLandmarkTypes
//                        .computeIfAbsent(landmarkType) { HashSet() }
//                        .add(playerUuid)
//                    for (landmarks in landmarksByChunks.values) {
//                        val landmarkAreas = landmarks[landmarkType]
//                        if (landmarkAreas != null) {
//                            if (lockedLandmarks == null) {
//                                lockedLandmarks = HashMap()
//                            }
//                            lockedLandmarks[landmarkType] = landmarkAreas
//                        }
//                    }
//                } else {
//                    val players = playersByLockedLandmarkTypes[landmarkType]
//                    if (players != null && players.remove(playerUuid) && players.isEmpty()) {
//                        playersByLockedLandmarkTypes.remove(landmarkType)
//                    }
//                    if (unlockedLandmarkTypes == null) {
//                        unlockedLandmarkTypes = HashSet()
//                    }
//                    unlockedLandmarkTypes.add(landmarkType)
//                }
//            }
//        }
//        context(player) {
//            SyncObtainedAdvancementsCountPayload.Server.send(count)
//            if (unlockedLandmarkTypes != null) {
//                LandmarkTypesUnlockedPayload.Server.send(unlockedLandmarkTypes)
//            }
//            if (lockedLandmarks != null) {
//                LandmarksLockedStatusChangedPayload.Server.send(lockedLandmarks, true)
//            }
//        }
    }

    fun notifyTrackedAdvancementProgress(player: ServerPlayer, progressType: TrackedAdvancement, progress: Int) {
        var progress = progress
        if (progressType.showAsPercents) {
            progress = Math.clamp(((progress * 100.0) / progressType.total).toInt().toLong(), 0, 100)
        }
        val progressByPlayers = trackedAdvancementProgresses.computeIfAbsent(progressType) { Object2IntOpenHashMap() }
        val currentProgress = progressByPlayers[player.getUUID()]
        if (currentProgress == null || currentProgress != progress) {
            progressByPlayers[player.getUUID()] = progress
            context(player) {
                TrackedAdvancementProgressChangedPayload.Server.send(progressType.advancementId, progress)
            }
        }
    }

    val isNotReady: Boolean
        get() = abilitiesConfiguration == null || currentBacapMode == null || currentScoreboardObjective == null || currentScoreboardDisplaySlot == null

    fun isAbilityLocked(player: ServerPlayer, abilityType: AbilityType): Boolean =
        isAbilityLocked(player, abilityType, false)

    fun isAbilityLocked(player: ServerPlayer, abilityType: AbilityType, checkOnly: Boolean): Boolean {
        if (player.isCreative || player.isSpectator) {
            return false
        }
        if (isNotReady) {
            return true
        }
        val obtainedCount = obtainedAdvancementsCountByPlayers.getOrDefault(player.getUUID(), Int.MIN_VALUE)
        val requiredCount: Int = abilitiesConfiguration!![abilityType]!!
        if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
            requiredCount != Constants.Progression.PERMANENTLY_LOCKED_FLAG && obtainedCount >= requiredCount
        ) {
            return false
        }
        if (!checkOnly) {
            val lockedMessageText: Component?
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                lockedMessageText = abilityType.buildPermanentlyLockedMessage()
            } else {
                val leftCount = requiredCount - obtainedCount
                lockedMessageText = abilityType.buildUnlockProgressMessage(leftCount)
            }
            player.sendOverlayMessage(lockedMessageText)
            demystifyAbility(player, abilityType)
        }
        return true
    }

    fun isTargetInLockedLandmark(actor: ServerPlayer, targetDimensionType: DimensionType, targetBox: AABB): Boolean {
        playersByLockedLandmarkTypes.forEach { (landmarkType, uuids) ->
            if (actor.uuid !in uuids) return@forEach
            landmarksByChunks.values.forEach { landmarks ->
                landmarks.filter { it.type == landmarkType }.forEach { landmark ->
                    if (landmark.area.intersects(targetDimensionType, targetBox)) {
                        val abilityType = AbilityType.fromLandmark(landmarkType)
                        if (abilityType != null) {
                            val isAbilityLocked = isAbilityLocked(actor, abilityType, true)
                            if (isAbilityLocked) {
                                context(actor) {
                                    CheckTargetInLockedLandmarkPayload.Server.send(targetDimensionType, targetBox)
                                }
                            }
                            return isAbilityLocked
                        }
                    }
                }
            }
        }
        return false
    }

    /*
    datapacks/achievetodo-utils/data/achievetodo/function/draw_landmark_wireframe.mcfunction
    $fill $(minX) $(minY) $(minZ) $(maxX) $(minY) $(minZ) $(wireframe_block)
    $fill $(minX) $(minY) $(maxZ) $(maxX) $(minY) $(maxZ) $(wireframe_block)
    $fill $(minX) $(minY) $(minZ) $(minX) $(minY) $(maxZ) $(wireframe_block)
    $fill $(maxX) $(minY) $(minZ) $(maxX) $(minY) $(maxZ) $(wireframe_block)
    $fill $(minX) $(maxY) $(minZ) $(maxX) $(maxY) $(minZ) $(wireframe_block)
    $fill $(minX) $(maxY) $(maxZ) $(maxX) $(maxY) $(maxZ) $(wireframe_block)
    $fill $(minX) $(maxY) $(minZ) $(minX) $(maxY) $(maxZ) $(wireframe_block)
    $fill $(maxX) $(maxY) $(minZ) $(maxX) $(maxY) $(maxZ) $(wireframe_block)
    $fill $(minX) $(minY) $(minZ) $(minX) $(maxY) $(minZ) $(wireframe_block)
    $fill $(maxX) $(minY) $(minZ) $(maxX) $(maxY) $(minZ) $(wireframe_block)
    $fill $(minX) $(minY) $(maxZ) $(minX) $(maxY) $(maxZ) $(wireframe_block)
    $fill $(maxX) $(minY) $(maxZ) $(maxX) $(maxY) $(maxZ) $(wireframe_block)

    $fill $(minX) $(minY) $(minZ) $(minX) $(minY) $(minZ) $(corner_block)
    $fill $(maxX) $(minY) $(minZ) $(maxX) $(minY) $(minZ) $(corner_block)
    $fill $(minX) $(minY) $(maxZ) $(minX) $(minY) $(maxZ) $(corner_block)
    $fill $(maxX) $(minY) $(maxZ) $(maxX) $(minY) $(maxZ) $(corner_block)
    $fill $(minX) $(maxY) $(minZ) $(minX) $(maxY) $(minZ) $(corner_block)
    $fill $(maxX) $(maxY) $(minZ) $(maxX) $(maxY) $(minZ) $(corner_block)
    $fill $(minX) $(maxY) $(maxZ) $(minX) $(maxY) $(maxZ) $(corner_block)
    $fill $(maxX) $(maxY) $(maxZ) $(maxX) $(maxY) $(maxZ) $(corner_block)
    */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val resizeJobs = ConcurrentHashMap<String, Job>()
    private val landmarkResizedNotificationsDelay = 3000L
    private fun MinecraftServer.sendLandmarkDebugMessage(landmark: Landmark, chunkPos: ChunkPos, first: Boolean) {
        fun msg() {
            val bb = landmark.area.boundingBox
            val tpCommand = "/tp @s ${bb.center.x} ${bb.center.y} ${bb.center.z}"
            val (wireframeBlock, cornerBlock) = if (first) {
                "copper_block" to "gold_block"
            } else {
                "diamond_block" to "netherite_block"
            }
            val drawCommand = "/function achievetodo:draw_landmark_wireframe {" +
                "minX:${bb.minX()}," +
                "minY:${bb.minY()}," +
                "minZ:${bb.minZ()}," +
                "maxX:${bb.maxX()}," +
                "maxY:${bb.maxY()}," +
                "maxZ:${bb.maxZ()}," +
                "wireframe_block:\"minecraft:$wireframeBlock\"," +
                "corner_block:\"minecraft:$cornerBlock\"" +
                "}"
            val (prefix, color) = if (first) "Found" to ChatFormatting.GOLD else "Done" to ChatFormatting.LIGHT_PURPLE
            val baseMessage = Component.literal("$prefix ${landmark.type.id} ").withStyle(color)
            val tpButton = Component.literal("[TP] ").withStyle { style ->
                style.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(ClickEvent.RunCommand(tpCommand))
            }
            val drawButton = Component.literal("[Draw]").withStyle { style ->
                style.withColor(ChatFormatting.AQUA).withBold(true).withClickEvent(ClickEvent.RunCommand(drawCommand))
            }
            val finalMessage = baseMessage.append(tpButton).append(drawButton)
            playerList.broadcastSystemMessage(finalMessage, false)
        }
        if (first) {
            msg()
        } else {
            val jobId = "${landmark.area.dimension}_${landmark.type}_${chunkPos.x}_${chunkPos.z}"
            resizeJobs[jobId]?.cancel()
            resizeJobs[jobId] = scope.launch {
                delay(landmarkResizedNotificationsDelay.milliseconds)
                execute(TickTask(tickCount) {
                    msg()
                    resizeJobs.remove(jobId)
                })
            }
        }
    }

    fun onLandmarksLoadedStatusChanged(
        world: WorldGenLevel,
        chunkPos: ChunkPos,
        landmarks: Set<Landmark>,
        isLoaded: Boolean,
    ) {
        landmarks.forEach { world.server?.sendLandmarkDebugMessage(it, chunkPos, first = true) }
//        if (this.isNotReady) {
//            return
//        }
//        var isChanged = false
//        if (isLoaded) {
//            val currentLandmarks = landmarksByChunks.getOrPut(chunkPos) { mutableListOf() }
//            for (entry in landmarks) {
//                val landmarkType: LandmarkType = entry.key
//                val abilityType = AbilityType.fromLandmark(landmarkType)
//                if (abilitiesConfiguration!![abilityType] == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
//                    continue
//                }
//                if (currentLandmarks.computeIfAbsent(landmarkType) { ArrayList() }.addAll(entry.value)) {
//                    isChanged = true
//                }
//            }
//        } else {
//            val chunkMap = landmarksByChunks[chunkPos]
//            if (chunkMap != null) {
//                for (entry in landmarks.entrySet()) {
//                    val landmarkAreas = chunkMap[entry.key]
//                    if (landmarkAreas != null && landmarkAreas.removeAll(entry.value)) {
//                        isChanged = true
//                        if (landmarkAreas.isEmpty()) {
//                            chunkMap.remove(entry.key)
//                            if (chunkMap.isEmpty()) {
//                                landmarksByChunks.remove(chunkPos)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        if (!isChanged) {
//            return
//        }
//        val landmarkTypesByPlayers = HashMap<UUID, MutableSet<LandmarkType?>>()
//        for (type in landmarks.keySet()) {
//            val playerUuids = playersByLockedLandmarkTypes[type] ?: continue
//            for (playerUuid in playerUuids) {
//                landmarkTypesByPlayers.computeIfAbsent(playerUuid) { HashSet() }.add(type)
//            }
//        }
//        val playerList = world.server.playerList
//        for (playerUuid in landmarkTypesByPlayers.keys) {
//            val player = playerList.getPlayer(playerUuid) ?: continue
//            val landmarkTypes: MutableSet<LandmarkType?> = landmarkTypesByPlayers[playerUuid]!!
//            val landmarksToSync: MutableSet<Landmark?> = HashSet()
//            for (landmarkType in landmarkTypes) {
//                landmarksToSync.addAll(landmarks.get(landmarkType))
//            }
//            context(player) { LandmarksLockedStatusChangedPayload.Server.send(landmarksToSync, isLoaded) }
//        }
    }

    fun onLandmarkResized(
        world: WorldGenLevel,
        chunkPos: ChunkPos,
        landmarkType: LandmarkType,
        dimensionType: DimensionType,
        oldBoundingBox: BoundingBox,
        newBoundingBox: BoundingBox,
    ) {
        val landmark = Landmark(landmarkType, LandmarkArea(dimensionType, newBoundingBox))
        world.server?.sendLandmarkDebugMessage(landmark, chunkPos, first = false)

//        val landmarksInChunk = landmarksByChunks[chunkPos] ?: return
//        val currentArea = LandmarkArea(dimensionType, oldBoundingBox)
//        val updatedArea = LandmarkArea(dimensionType, newBoundingBox)
//        landmarksInChunk.removeAll { it.type == landmarkType && it.area == currentArea }
//        landmarksInChunk += Landmark(landmarkType, updatedArea)
//        playersByLockedLandmarkTypes[landmarkType]?.forEach { uuid ->
//            val player = world.server?.playerList?.getPlayer(uuid) ?: return@forEach
//            context(player) { LockedLandmarkResizedPayload.Server.send(landmarkType, currentArea, updatedArea) }
//        }
    }

    fun init() {
//        registerInternalDataPacks()
        registerPayloadListeners()
//        ServerLifecycleEvents.SERVER_STARTING.register { server ->
//            if (server.getSaveProperties().getLevelInfo() is LevelInfoExtension) {
//                abilitiesConfiguration = levelInfoExtension.`achievetodo$getAbilitiesConfiguration`(
//                    server.getSaveProperties().getGeneratorOptions().getSeed()
//                )
//                AchieveToDoMod.logger.info("Abilities configuration loaded")
//            }
//        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
//            abilitiesConfiguration = null
//            obtainedAdvancementsCountByPlayers.clear()
            landmarksByChunks.clear()
//            playersByLockedLandmarkTypes.clear()
//            trackedAdvancementProgresses.clear()
//            currentBacapMode = null
//            currentScoreboardObjective = null
//            currentScoreboardDisplaySlot = null
        }

        ServerChunkEvents.CHUNK_LOAD.register { level, chunk, _ ->
            onChunkLoadedStatusChanged(level, chunk, true)
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            onChunkLoadedStatusChanged(level, chunk, false)
        }

//        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
//            val player = handler.player
//            if (this.isNotReady) {
//                player.connection.disconnect(Component.literal(AchieveToDoMod.NAME + " is not ready yet"))
//                return@register
//            }
//            context(player) { SyncAbilitiesConfigurationPayload.Server.send(abilitiesConfiguration) }
//
//            updateObtainedCount(server.scoreboard, player)
//            val scoreboard: Scoreboard = player.level().scoreboard
//            for ((key, value) in TrackedAdvancement.Objective.BY_OBJECTIVE_NAME) {
//                val progress = scoreboard.getOrCreatePlayerScore(player, scoreboard.getObjective(key)!!).get()
//                for (type in value) {
//                    notifyTrackedAdvancementProgress(player, type, type.resolve.invoke(scoreboard, player, progress))
//                }
//            }
//            val stats = player.stats
//            for ((key, value) in TrackedAdvancement.Statistics.BY_STAT) {
//                val progress = stats.getValue(key)
//                for (trackedStat in value) {
//                    notifyTrackedAdvancementProgress(player, trackedStat, progress)
//                }
//            }
//        }
//        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
//            val playerUuid = handler.player.getUUID()
//            obtainedAdvancementsCountByPlayers.remove(playerUuid)
//            for (players in trackedAdvancementProgresses.values) {
//                players.remove(playerUuid)
//            }
//            for (players in playersByLockedLandmarkTypes.values) {
//                players.remove(playerUuid)
//            }
//        }
    }

    private fun registerPayloadListeners() {
        DemystifyAbilityPayload.Server.listen { ability -> demystifyAbility(player(), ability) }
    }

    private fun onChunkLoadedStatusChanged(world: ServerLevel, chunk: ChunkAccess, isLoaded: Boolean) {
        val dimensionType = DimensionType.findByLevel(world.dimension()) ?: return
        val landmarks = chunk.allStarts.values.mapNotNullTo(mutableSetOf()) { structureStart ->
            val type = structureStart.landmarkType ?: return@mapNotNullTo null
            val bb = structureStart.landmarkBB ?: return@mapNotNullTo null
            Landmark(type, LandmarkArea(dimensionType, bb))
        }
//        if (chunk is ChunkExtension) {
//            val featureLandmarks = chunk.`achievetodo$getFeatureLandmarks`()
//            if (featureLandmarks != null) {
//                if (landmarks == null) {
//                    landmarks = HashMap()
//                }
//                landmarks.putAll(featureLandmarks)
//            }
//        }
//        if (landmarks.isNotEmpty()) {
//            onLandmarksLoadedStatusChanged(world, chunk.pos, landmarks, isLoaded)
//        }
    }

    private fun updateObtainedCount(scoreboard: ServerScoreboard, player: ServerPlayer) {
//        if (this.isNotReady) {
//            return
//        }
//        var count = 0
//        val playerName = player.plainTextName
//        if (currentBacapMode!!.isTeamsMode) {
//            val team = scoreboard.getPlayersTeam(playerName)
//            if (team == null) {
//                AchieveToDoMod.logger.warn("Player [{}] is not a member of any team!", playerName)
//                return
//            }
//            for (teamMemberName in team.players) {
//                val teamMemberScore =
//                    scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(teamMemberName), currentScoreboardObjective!!)
//                if (teamMemberScore != null) {
//                    count += teamMemberScore.value()
//                }
//            }
//        } else {
//            val playerScore =
//                scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly(playerName), currentScoreboardObjective!!)
//            if (playerScore != null) {
//                count = playerScore.value()
//            }
//        }
//        setObtainedCount(player, count)
    }

    private fun demystifyAbility(player: ServerPlayer, ability: AbilityType) {
        val advancement = findAbilityAdvancement(player, ability) ?: return
        player.advancements.award(advancement, AbilityAdvancementsTabGenerator.DEMYSTIFIED_CRITERION)
    }

    private fun unlockAbility(player: ServerPlayer, ability: AbilityType) {
        val advancement = findAbilityAdvancement(player, ability) ?: return
        player.advancements.getOrStartProgress(advancement).getRemainingCriteria().forEach { criterion ->
            player.advancements.award(advancement, criterion)
        }
    }

    private fun lockAbility(player: ServerPlayer, ability: AbilityType) {
        val advancement = findAbilityAdvancement(player, ability) ?: return
        player.advancements.revoke(advancement, AbilityAdvancementsTabGenerator.UNLOCKED_CRITERION)
    }

    private fun findAbilityAdvancement(player: ServerPlayer, ability: AbilityType): AdvancementHolder? =
        player.level().server.advancements.get(AbilityAdvancementsTabGenerator.buildAdvancementId(ability))

    companion object {
        private fun registerInternalDataPacks() {
            FabricLoader.getInstance().getModContainer(AchieveToDoMod.ID)
                .ifPresent(Consumer { modContainer: ModContainer? ->
                    for (internalPack in InternalPack.entries) {
                        ResourceManagerHelper.registerBuiltinResourcePack(
                            AchieveToDoMod.getIdentifier(internalPack.datapackName),
                            modContainer!!,
                            ResourcePackActivationType.NORMAL
                        )
                    }
                })
        }
    }
}
