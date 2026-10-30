package io.github.diskria.achievetodo.landmarks

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.ability.Landmark
import io.github.diskria.achievetodo.ability.LandmarkType
import io.github.diskria.achievetodo.etxensions.decodeOrNull
import io.github.diskria.achievetodo.etxensions.toNbtOrNull
import io.github.diskria.achievetodo.server.awaitPlayers
import io.github.diskria.achievetodo.server.logToChat
import io.github.diskria.lapis.annotations.*
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.PlaceCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.util.RandomSource
import net.minecraft.world.level.*
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.chunk.storage.SerializableChunkData
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.placement.FeaturePlacer
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart
import org.spongepowered.asm.mixin.injection.At
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.Modifier
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

object ServerLandmarksManager {

    val loadedLandmarks = mutableMapOf<ResourceKey<Level>, MutableMap<ChunkPos, MutableSet<Landmark>>>()

    fun registerLandmark(level: ServerLevel, chunkPos: ChunkPos, landmark: Landmark) {
        loadedLandmarks
            .getOrPut(level.dimension()) { mutableMapOf() }
            .getOrPut(chunkPos) { mutableSetOf() }
            .add(landmark)
        level.server.sendDebugMessage(landmark, chunkPos, resize = false)
    }

    fun resizeLandmark(level: ServerLevel, chunkPos: ChunkPos, current: Landmark, resized: Landmark) {
        val chunkLandmarks = loadedLandmarks[level.dimension()]?.get(chunkPos) ?: return
        if (chunkLandmarks.remove(current)) {
            chunkLandmarks.add(resized)
            level.server.sendDebugMessage(resized, chunkPos, resize = true)
        }
    }

    fun registerHooks() {
        ServerChunkEvents.CHUNK_LOAD.register { level, chunk, _ ->
            val chunkPos = chunk.pos
            level.structureManager().startsForStructure(chunkPos.x, chunkPos.z) { true }.forEach { start ->
                val structureLandmark = start.landmark ?: return@forEach
                registerLandmark(level, start.chunkPos, structureLandmark)
            }
            chunk.landmarks?.forEach { featureLandmark -> registerLandmark(level, chunkPos, featureLandmark) }
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            val unloaded = loadedLandmarks[level.dimension()]?.remove(chunk.pos).orEmpty().ifEmpty { return@register }
            level.server.logToChat("Unloaded landmarks: $unloaded")
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val resizeJobs = ConcurrentHashMap<String, Job>()
    private const val RESIZE_NOTIFICATIONS_DELAY = 3000L

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
    @Suppress("SpellCheckingInspection")
    fun MinecraftServer.sendDebugMessage(landmark: Landmark, chunkPos: ChunkPos, resize: Boolean) {
        val (wireframeBlock, cornerBlock) = if (resize) {
            "diamond_block" to "netherite_block"
        } else {
            "copper_block" to "gold_block"
        }
        val (prefix, color) = if (resize) {
            "Resized" to ChatFormatting.LIGHT_PURPLE
        } else {
            "Generated" to ChatFormatting.GOLD
        }
        val baseMessage = Component.literal("$prefix ${landmark.type.id} ").withStyle(color)
        fun commandButton(name: String, color: ChatFormatting, command: String) =
            Component.literal("[$name]").withStyle { style ->
                style.withColor(color).withBold(true).withClickEvent(ClickEvent.RunCommand(command))
            }

        val bb = landmark.bb
        val tpButton = commandButton(
            "TP", ChatFormatting.GREEN,
            "/tp @s ${bb.center.x} ${bb.center.y} ${bb.center.z}"
        )
        val drawButton = commandButton(
            "Draw", ChatFormatting.AQUA,
            """
            /function achievetodo:draw_landmark_wireframe {
                minX:${bb.minX()},minY:${bb.minY()},minZ:${bb.minZ()},
                maxX:${bb.maxX()},maxY:${bb.maxY()},maxZ:${bb.maxZ()},
                wireframe_block:"minecraft:$wireframeBlock",corner_block:"minecraft:$cornerBlock"
            }
            """.trimIndent().replace(Regex("\\s*\n\\s*"), "")
        )
        val resizeJobId = if (resize) "${landmark.type}_${chunkPos.x}_${chunkPos.z}" else null
        resizeJobId?.let { resizeJobs[it]?.cancel() }
        val job = scope.launch {
            if (resizeJobId != null) delay(RESIZE_NOTIFICATIONS_DELAY.milliseconds)
            awaitPlayers()
            execute(wrapRunnable {
                logToChat(baseMessage.append(tpButton).append(" ").append(drawButton))
                resizeJobId?.let { resizeJobs.remove(it) }
            })
        }
        resizeJobId?.let { resizeJobs[resizeJobId] = job }
    }
}

@KMixin<ChunkAccess>(Side.Common)
abstract class ChunkLandmarksData {
    @Extension
    var landmarks: Set<Landmark>? = null
        set(value) {
            field = value
            markUnsaved()
        }

    @KShadow(Modifier.PUBLIC)
    abstract fun markUnsaved()
}

@KMixin<LevelChunk>(Side.Common)
class LevelChunkLandmarksBridge {
    @WrapOperation(
        method = [$$"<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;setAllReferences(Ljava/util/Map;)V"
        )]
    )
    fun copyLandmarks(
        to: LevelChunk,
        structureReferences: Map<*, *>,
        original: Operation<Unit>,
        @Local(argsOnly = true, name = ["protoChunk"]) from: ProtoChunk,
    ) {
        original.call(to, structureReferences)
        to.landmarks = from.landmarks?.toSet()
    }
}

@KMixin<SerializableChunkData>(Side.Common)
class ChunkLandmarksNbt {

    @Extension
    var landmarksNbt: Tag? = null

    @ModifyReturnValue(method = ["write"], at = [At(value = "TAIL")])
    fun saveToNbt(compound: CompoundTag) = compound.apply {
        landmarksNbt?.let { put(LANDMARKS_TAG_NAME, it) }
    }

    @WrapOperation(
        method = ["read"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setAllReferences(Ljava/util/Map;)V"
        )]
    )
    fun unpackLandmarksNbt(chunk: ChunkAccess, structureReferences: Map<*, *>, original: Operation<Unit>) {
        original.call(chunk, structureReferences)
        chunk.landmarks = landmarksNbt?.decodeOrNull(Landmark.SET_CODEC)
    }

    companion object {
        private const val LANDMARKS_TAG_NAME = "achievetodo_landmarks"

        @ModifyReturnValue(method = ["parse"], at = [At(value = "TAIL")])
        fun readFromNbt(
            result: SerializableChunkData,
            @Local(argsOnly = true, name = ["chunkData"]) compound: CompoundTag,
        ) = result.apply { landmarksNbt = compound.get(LANDMARKS_TAG_NAME) }

        @ModifyReturnValue(method = ["copyOf"], at = [At(value = "TAIL")])
        fun packLandmarksNbt(
            result: SerializableChunkData,
            @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess,
        ) = result.apply { landmarksNbt = chunk.landmarks?.toNbtOrNull(Landmark.SET_CODEC) }
    }
}

@KMixin<Structure>(Side.Common)
class StructureLandmarkDetector {
    @ModifyReturnValue(method = ["generate"], at = [At(value = "RETURN", ordinal = 0)])
    fun detectLandmarkType(
        start: StructureStart,
        @Local(argsOnly = true, name = ["selected"]) holder: Holder<Structure>,
    ) = start.apply {
        val structureKey = holder.unwrapKey().getOrNull() ?: return@apply
        landmarkType = LandmarkType.fromStructure(structureKey)
    }
}

@KMixin<StructureStart>(Side.Common)
class StructureLandmarkNbt {

    @Extension
    var landmarkType: LandmarkType? = null

    @Extension
    var landmark: Landmark? = null

    @ModifyReturnValue(method = ["createTag"], at = [At(value = "RETURN", ordinal = 1)])
    fun saveToNbt(compound: CompoundTag) = compound.apply {
        landmarkType?.toNbtOrNull(LandmarkType.CODEC)?.let { put(LANDMARK_TYPE_TAG_NAME, it) }
        landmark?.toNbtOrNull(Landmark.CODEC)?.let { put(LANDMARK_TAG_NAME, it) }
    }

    companion object {
        private const val LANDMARK_TYPE_TAG_NAME = "achievetodo_landmarkType"
        private const val LANDMARK_TAG_NAME = "achievetodo_landmark"

        @ModifyReturnValue(method = ["loadStaticStart"], at = [At(value = "RETURN", ordinal = 2)])
        fun readFromNbt(
            result: StructureStart,
            @Local(argsOnly = true, name = ["tag"]) compound: CompoundTag,
        ) = result.apply {
            landmarkType = compound.get(LANDMARK_TYPE_TAG_NAME)?.decodeOrNull(LandmarkType.CODEC)
            landmark = compound.get(LANDMARK_TAG_NAME)?.decodeOrNull(Landmark.CODEC)
        }
    }
}

@KMixin<StructureStart>(Side.Common)
class StructureLandmarkGeneratorTracker {

    @WrapMethod(method = ["placeInChunk"])
    fun StructureStart.trackGeneration(
        levelGenerator: WorldGenLevel,
        structureManager: StructureManager,
        chunkGenerator: ChunkGenerator,
        random: RandomSource,
        chunkBB: BoundingBox,
        targetChunkPos: ChunkPos,
        original: Operation<Unit>,
    ) {
        fun generate() {
            original.call(levelGenerator, structureManager, chunkGenerator, random, chunkBB, targetChunkPos)
        }

        val landmarkType = landmarkType ?: return generate()
        val (_, bb) = levelGenerator.trackLandmarkBounds { generate() }
        bb?.let { syncChunkUpdates(landmarkType, bb, levelGenerator.level) }
    }

    private fun StructureStart.syncChunkUpdates(type: LandmarkType, bb: BoundingBox, level: ServerLevel) =
        synchronized(this) {
            val current = landmark
            if (current == null) {
                landmark = Landmark(type, bb).also { initialized ->
                    ServerLandmarksManager.registerLandmark(level, chunkPos, initialized)
                }
            } else {
                val combinedBB = BoundingBox.encapsulating(current.bb, bb)
                if (combinedBB != current.bb) {
                    landmark = current.copy(bb = combinedBB).also { resized ->
                        ServerLandmarksManager.resizeLandmark(level, chunkPos, current, resized)
                    }
                }
            }
        }
}

@KMixin<ChunkGenerator>(Side.Common)
class FeatureLandmarkGeneratorTracker {
    @WrapOperation(
        method = ["applyBiomeDecoration"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/placement/FeaturePlacer;placeWithBiomeCheck(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
        )]
    )
    fun trackFeatureLandmarkGeneration(
        placer: FeaturePlacer,
        featureToPlace: PlacedFeature,
        random: RandomSource,
        originPos: BlockPos,
        original: Operation<Boolean>,
        @Local(argsOnly = true, name = ["level"]) levelGenerator: WorldGenLevel,
        @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess,
    ): Boolean {
        val featureHolder = featureToPlace.feature
        return levelGenerator.resolveFeatureGeneration(featureHolder, chunk, byPlaceCommand = false) {
            original.call(placer, featureToPlace, random, originPos)
        }
    }
}

@KMixin<PlaceCommand>(Side.Common)
class PlaceCommandPatches {
    companion object {
        @ModifyReturnValue(method = ["placeStructure"], at = [At(value = "TAIL")])
        fun rememberPlacedStructureLandmark(
            original: Int,
            @Local(name = ["level"]) level: ServerLevel,
            @Local(name = ["start"]) start: StructureStart,
        ): Int {
            start.landmark?.let { landmark ->
                val startChunkPos = start.chunkPos
                val targetChunk = level.getChunk(startChunkPos.x, startChunkPos.z)
                val updated = targetChunk.landmarks?.toMutableSet() ?: mutableSetOf()
                if (updated.add(landmark)) {
                    targetChunk.landmarks = updated
                }
            }
            return original
        }

        @WrapOperation(
            method = ["placeFeature"],
            at = [At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
            )]
        )
        fun trackFeatureLandmarkGeneration(
            feature: Feature,
            levelGenerator: WorldGenLevel,
            chunkGenerator: ChunkGenerator,
            random: RandomSource,
            originPos: BlockPos,
            original: Operation<Boolean>,
            @Local(argsOnly = true, name = ["featureHolder"]) featureHolder: Holder<Feature>,
        ): Boolean {
            val targetChunk = levelGenerator.getChunk(originPos)
            return levelGenerator.resolveFeatureGeneration(featureHolder, targetChunk, byPlaceCommand = true) {
                original.call(feature, levelGenerator, chunkGenerator, random, originPos)
            }
        }
    }
}

@KMixin<WorldGenRegion>(Side.Common, InitStrategy.Synchronized)
class RegionBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At(value = "RETURN")])
    fun LevelWriter.trackBlockPlacing(
        placed: Boolean,
        @Local(argsOnly = true, name = ["pos"]) pos: BlockPos,
        @Local(argsOnly = true, name = ["blockState"]) blockState: BlockState,
    ): Boolean {
        if (placed) LevelWriterTracker.onBlockPlaced(this, pos, blockState)
        return placed
    }
}

@KMixin<Level>(Side.Common, InitStrategy.Synchronized)
class LevelBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At(value = "RETURN")])
    fun LevelWriter.trackBlockPlacing(
        placed: Boolean,
        @Local(argsOnly = true, name = ["pos"]) pos: BlockPos,
        @Local(argsOnly = true, name = ["blockState"]) blockState: BlockState,
    ): Boolean {
        if (placed) LevelWriterTracker.onBlockPlaced(this, pos, blockState)
        return placed
    }
}

private object LevelWriterTracker {

    private val activeState = ThreadLocal<State?>()

    fun onBlockPlaced(writer: LevelWriter, pos: BlockPos, blockState: BlockState) {
        if (blockState.isAir || blockState.`is`(Blocks.STRUCTURE_VOID)) return
        val state = activeState.get() ?: return
        if (state.targetWriter === writer) {
            state.expand(pos)
        }
    }

    inline fun <R> track(writer: LevelWriter, generate: () -> R): Pair<R, BoundingBox?> {
        start(writer)
        return try {
            generate() to stop()
        } finally {
            activeState.remove()
        }
    }

    private fun start(writer: LevelWriter) {
        activeState.set(State(writer))
    }

    private fun stop(): BoundingBox? {
        val state = activeState.get()
        activeState.remove()
        return state?.bb
    }

    class State(val targetWriter: LevelWriter) {

        private var minX = Int.MAX_VALUE
        private var minY = Int.MAX_VALUE
        private var minZ = Int.MAX_VALUE
        private var maxX = Int.MIN_VALUE
        private var maxY = Int.MIN_VALUE
        private var maxZ = Int.MIN_VALUE

        val bb: BoundingBox?
            get() = if (maxX >= minX && maxY >= minY && maxZ >= minZ) {
                BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
            } else {
                null
            }

        fun expand(pos: BlockPos) {
            val x = pos.x
            val y = pos.y
            val z = pos.z
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
        }
    }
}

private inline fun <R> LevelWriter.trackLandmarkBounds(generate: () -> R): Pair<R, BoundingBox?> =
    LevelWriterTracker.track(this, generate)

private inline fun <R> WorldGenLevel.resolveFeatureGeneration(
    holder: Holder<Feature>,
    targetChunk: ChunkAccess,
    byPlaceCommand: Boolean,
    generate: () -> R
): R {
    val featureKey = holder.unwrapKey().getOrNull() ?: return generate()
    val landmarkType = LandmarkType.fromFeature(featureKey) ?: return generate()
    val (result, bb) = trackLandmarkBounds { generate() }
    if (bb != null) {
        val landmark = Landmark(landmarkType, bb)
        val updated = targetChunk.landmarks?.toMutableSet() ?: mutableSetOf()
        if (updated.add(landmark)) {
            targetChunk.landmarks = updated
            if (byPlaceCommand) {
                ServerLandmarksManager.registerLandmark(level, targetChunk.pos, landmark)
            }
        }
    }
    return result
}
