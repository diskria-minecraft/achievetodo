package io.github.diskria.achievetodo.landmarks

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.config.ModServerConfig
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.decodeOrNull
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.extensions.toNbtOrNull
import io.github.diskria.lapis.annotations.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
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
import javax.lang.model.element.Modifier.PUBLIC
import kotlin.jvm.optionals.getOrNull

object ServerLandmarksManager {

    val loadedLandmarks = ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<ChunkPos, MutableSet<Landmark>>>()

    fun init() {
        ServerChunkEvents.CHUNK_LOAD.register { level, chunk, _ ->
            if (ModServerConfig.current == null) return@register
            val chunkPos = chunk.pos
            level.structureManager().startsForStructure(chunkPos.x, chunkPos.z) { true }.forEach { start ->
                val structureLandmark = start.landmark ?: return@forEach
                addLandmark(level, start.chunkPos, structureLandmark)
            }
            chunk.landmarks?.forEach { featureLandmark -> addLandmark(level, chunkPos, featureLandmark) }
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            if (ModServerConfig.current == null) return@register
            val inDimension = loadedLandmarks[level.dimension()] ?: return@register
            if (chunk.allStarts.any { it.value.landmark != null }) {
                chunk.landmarks?.let { inDimension[chunk.pos]?.removeAll(it) }
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            loadedLandmarks.clear()
        }
    }

    fun addLandmark(level: ServerLevel, chunkPos: ChunkPos, landmark: Landmark) {
        if (ModServerConfig.current == null) return
        loadedLandmarks
            .getOrPut(level.dimension()) { ConcurrentHashMap() }
            .getOrPut(chunkPos) { ConcurrentHashMap.newKeySet() }
            .add(landmark)
    }

    fun resizeLandmark(level: ServerLevel, chunkPos: ChunkPos, current: Landmark, resized: Landmark) {
        if (ModServerConfig.current == null) return
        val inChunk = loadedLandmarks[level.dimension()]?.get(chunkPos) ?: return
        inChunk.remove(current)
        inChunk.add(resized)
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

    @KShadow(PUBLIC)
    abstract fun markUnsaved()
}

@KMixin<LevelChunk>(Side.Common)
class LandmarksBridgeToLevelChunk {
    @WrapOperation(
        method = [$$"<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V"],
        at = [At("INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setAllReferences(Ljava/util/Map;)V")]
    )
    context(
        @Local(argsOnly = true, name = ["protoChunk"]) from: ProtoChunk,
    )
    fun copyLandmarks(to: LevelChunk, structureReferences: Map<*, *>, op: Op<Unit>) {
        op(to, structureReferences)
        to.landmarks = from.landmarks?.toSet()
    }
}

@KMixin<SerializableChunkData>(Side.Common)
class ChunkLandmarksNbt {

    @Extension
    var landmarksNbt: Tag? = null

    @ModifyReturnValue(method = ["write"], at = [At("TAIL")])
    fun saveToNbt(compound: CompoundTag) = compound.apply {
        landmarksNbt?.let { put(LANDMARKS_TAG_NAME, it) }
    }

    @WrapOperation(
        method = ["read"],
        at = [At("INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setAllReferences(Ljava/util/Map;)V")]
    )
    fun saveToChunk(chunk: ChunkAccess, structureReferences: Map<*, *>, op: Op<Unit>) {
        op(chunk, structureReferences)
        chunk.landmarks = landmarksNbt?.decodeOrNull(Landmark.SET_CODEC)
    }

    companion object {
        private const val LANDMARKS_TAG_NAME = "achievetodo_landmarks"

        @ModifyReturnValue(method = ["parse"], at = [At("TAIL")])
        context(
            @Local(argsOnly = true, name = ["chunkData"]) compound: CompoundTag,
        )
        fun readFromNbt(result: SerializableChunkData) = result.apply {
            landmarksNbt = compound[LANDMARKS_TAG_NAME]
        }

        @ModifyReturnValue(method = ["copyOf"], at = [At("TAIL")])
        context(
            @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess,
        )
        fun readFromChunk(result: SerializableChunkData) = result.apply {
            landmarksNbt = chunk.landmarks?.toNbtOrNull(Landmark.SET_CODEC)
        }
    }
}

@KMixin<Structure>(Side.Common)
class StructureLandmarkDetector {
    @ModifyReturnValue(method = ["generate"], at = [At("RETURN", ordinal = 0)])
    context(
        @Local(argsOnly = true, name = ["selected"]) holder: Holder<Structure>,
    )
    fun detectLandmarkType(start: StructureStart) = start.apply {
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

    @ModifyReturnValue(method = ["createTag"], at = [At("RETURN", ordinal = 1)])
    fun saveToNbt(compound: CompoundTag) = compound.apply {
        landmarkType?.toNbtOrNull(LandmarkType.CODEC)?.let { put(LANDMARK_TYPE_TAG_NAME, it) }
        landmark?.toNbtOrNull(Landmark.CODEC)?.let { put(LANDMARK_TAG_NAME, it) }
    }

    companion object {
        private const val LANDMARK_TYPE_TAG_NAME = "achievetodo_landmarkType"
        private const val LANDMARK_TAG_NAME = "achievetodo_landmark"

        @ModifyReturnValue(method = ["loadStaticStart"], at = [At("RETURN", ordinal = 2)])
        context(
            @Local(argsOnly = true, name = ["tag"]) compound: CompoundTag,
        )
        fun readFromNbt(result: StructureStart) = result.apply {
            landmarkType = compound[LANDMARK_TYPE_TAG_NAME]?.decodeOrNull(LandmarkType.CODEC)
            landmark = compound[LANDMARK_TAG_NAME]?.decodeOrNull(Landmark.CODEC)
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
        op: Op<Unit>
    ) {
        fun generate() = op(levelGenerator, structureManager, chunkGenerator, random, chunkBB, targetChunkPos)

        val landmarkType = landmarkType ?: return generate()
        val (_, bb) = levelGenerator.trackLandmarkBounds { generate() }
        bb?.let { syncChunkUpdates(landmarkType, bb, levelGenerator.level) }
    }

    private fun StructureStart.syncChunkUpdates(type: LandmarkType, bb: BoundingBox, level: ServerLevel) =
        synchronized(this) {
            val current = landmark
            if (current == null) {
                landmark = Landmark(type, bb).also { initialized ->
                    ServerLandmarksManager.addLandmark(level, chunkPos, initialized)
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
    context(
        @Local(argsOnly = true, name = ["level"]) levelGenerator: WorldGenLevel,
        @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess,
    )
    fun trackFeatureLandmarkGeneration(
        placer: FeaturePlacer,
        featureToPlace: PlacedFeature,
        random: RandomSource,
        originPos: BlockPos,
        op: Op<Boolean>
    ): Boolean {
        val featureHolder = featureToPlace.feature
        return levelGenerator.resolveFeatureGeneration(featureHolder, chunk, byPlaceCommand = false) {
            op(placer, featureToPlace, random, originPos)
        }
    }
}

@KMixin<PlaceCommand>(Side.Common)
class PlaceCommandPatches {
    companion object {
        @ModifyReturnValue(method = ["placeStructure"], at = [At("TAIL")])
        context(
            @Local(name = ["level"]) level: ServerLevel,
            @Local(name = ["start"]) start: StructureStart,
        )
        fun rememberPlacedStructureLandmark(original: Int): Int {
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
        context(
            @Local(argsOnly = true, name = ["featureHolder"]) featureHolder: Holder<Feature>,
        )
        fun trackFeatureLandmarkGeneration(
            feature: Feature,
            levelGenerator: WorldGenLevel,
            chunkGenerator: ChunkGenerator,
            random: RandomSource,
            originPos: BlockPos,
            op: Op<Boolean>
        ): Boolean {
            val targetChunk = levelGenerator.getChunk(originPos)
            return levelGenerator.resolveFeatureGeneration(featureHolder, targetChunk, byPlaceCommand = true) {
                op(feature, levelGenerator, chunkGenerator, random, originPos)
            }
        }
    }
}

@KMixin<WorldGenRegion>(Side.Common, InitStrategy.Synchronized)
class RegionBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At("RETURN")])
    context(
        @Local(argsOnly = true, name = ["pos"]) pos: BlockPos,
        @Local(argsOnly = true, name = ["blockState"]) blockState: BlockState,
    )
    fun LevelWriter.trackBlockPlacing(placed: Boolean): Boolean {
        if (placed) LevelWriterTracker.onBlockPlaced(this, pos, blockState)
        return placed
    }
}

@KMixin<Level>(Side.Common, InitStrategy.Synchronized)
class LevelBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At("RETURN")])
    context(
        @Local(argsOnly = true, name = ["pos"]) pos: BlockPos,
        @Local(argsOnly = true, name = ["blockState"]) blockState: BlockState,
    )
    fun LevelWriter.trackBlockPlacing(placed: Boolean): Boolean {
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
    generate: () -> R,
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
                ServerLandmarksManager.addLandmark(level, targetChunk.pos, landmark)
            }
        }
    }
    return result
}
