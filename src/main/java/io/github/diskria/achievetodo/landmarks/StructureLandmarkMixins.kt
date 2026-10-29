package io.github.diskria.achievetodo.landmarks

import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.ability.DimensionType
import io.github.diskria.achievetodo.ability.Landmark
import io.github.diskria.achievetodo.ability.LandmarkArea
import io.github.diskria.achievetodo.ability.LandmarkType
import io.github.diskria.achievetodo.etxensions.decodeOrNull
import io.github.diskria.achievetodo.etxensions.toNbtOrNull
import io.github.diskria.lapis.annotations.*
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.util.RandomSource
import net.minecraft.world.level.*
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.chunk.storage.SerializableChunkData
import net.minecraft.world.level.levelgen.placement.FeaturePlacer
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier
import kotlin.jvm.optionals.getOrNull

@KMixin<Structure>(Side.Common)
class StructureLandmarkDetector {
    @ModifyReturnValue(method = ["generate"], at = [At(value = "RETURN", ordinal = 0)])
    fun detect(
        original: StructureStart,
        @Local(argsOnly = true, name = ["selected"]) structureHolder: Holder<Structure>,
    ) = original.apply {
        landmarkType = structureHolder.unwrapKey().getOrNull()?.let { LandmarkType.fromStructure(it) }
    }
}

@KMixin<StructureStart>(Side.Common)
class StructureLandmarkData {

    @Extension
    var landmarkType: LandmarkType? = null

    @Extension
    var landmarkBB: BoundingBox? = null

    @ModifyReturnValue(method = ["createTag"], at = [At(value = "TAIL")])
    fun toNbt(tag: CompoundTag) = tag.apply {
        landmarkType?.toNbtOrNull(LandmarkType.CODEC)?.let { put(TYPE_TAG_NAME, it) }
        landmarkBB?.toNbtOrNull(BoundingBox.CODEC)?.let { put(BB_TAG_NAME, it) }
    }

    companion object {
        private const val TYPE_TAG_NAME = "achievetodo_landmarkType"
        private const val BB_TAG_NAME = "achievetodo_landmarkBB"

        @ModifyReturnValue(method = ["loadStaticStart"], at = [At(value = "RETURN")])
        fun fromNbt(
            original: StructureStart,
            @Local(argsOnly = true, name = ["tag"]) tag: CompoundTag,
        ) = original.apply {
            landmarkType = tag.get(TYPE_TAG_NAME)?.decodeOrNull(LandmarkType.CODEC)
            landmarkBB = tag.get(BB_TAG_NAME)?.decodeOrNull(BoundingBox.CODEC)
        }
    }
}

@KMixin<StructureStart>(Side.Common)
abstract class StructureLandmarkGenTracker {
    @WrapMethod(method = ["placeInChunk"])
    fun StructureStart.trackGen(
        worldGen: WorldGenLevel,
        structureManager: StructureManager,
        generator: ChunkGenerator,
        random: RandomSource,
        chunkBB: BoundingBox,
        chunkPos: ChunkPos,
        original: Operation<Void>,
    ) {
        fun gen() = original.call(worldGen, structureManager, generator, random, chunkBB, chunkPos)

        val landmarkType = landmarkType ?: run { gen(); return }
        val dimensionType = DimensionType.findByLevel(worldGen.level.dimension()) ?: run { gen(); return }
        val (_, bb) = worldGen.trackLandmarkBounds { gen() }
        bb ?: return
        synchronized(this) {
            val currentBB = landmarkBB
            if (currentBB == null) {
                landmarkBB = bb
                AchieveToDoMod.server.onLandmarksLoadedStatusChanged(
                    world = worldGen,
                    chunkPos = startChunkPos,
                    landmarks = setOf(Landmark(landmarkType, LandmarkArea(dimensionType, bb))),
                    isLoaded = true,
                )
                return
            }
            val combinedBB = BoundingBox.encapsulating(currentBB, bb)
            if (combinedBB != currentBB) {
                landmarkBB = combinedBB
                AchieveToDoMod.server.onLandmarkResized(
                    world = worldGen,
                    chunkPos = startChunkPos,
                    landmarkType = landmarkType,
                    dimensionType = dimensionType,
                    oldBoundingBox = currentBB,
                    newBoundingBox = combinedBB,
                )
            }
        }
    }

    @MappingName("chunkPos")
    @KShadow(Modifier.PRIVATE, Modifier.FINAL)
    abstract val startChunkPos: ChunkPos
}

@KMixin<ChunkAccess>(Side.Common)
abstract class ChunkFeatureLandmarksCollector(@Origin private val chunk: ChunkAccess) {

    @Extension
    var featureLandmarks: MutableSet<Landmark> = mutableSetOf()
        private set

    @Extension
    fun setInitialFeatureLandmarks(worldGen: WorldGenLevel, landmarks: Set<Landmark>) {
        featureLandmarks = landmarks.toMutableSet()
        notifyServer(worldGen, landmarks)
    }

    @Extension
    fun addFeatureLandmark(worldGen: WorldGenLevel, landmark: Landmark) {
        if (!featureLandmarks.add(landmark)) return
        notifyServer(worldGen, setOf(landmark))
    }

    private fun notifyServer(worldGen: WorldGenLevel, landmarks: Set<Landmark>) {
        if (chunk is ProtoChunk) return
        AchieveToDoMod.server.onLandmarksLoadedStatusChanged(worldGen, chunkPos, landmarks, isLoaded = true)
    }

    @KShadow(Modifier.PROTECTED, Modifier.FINAL)
    abstract val chunkPos: ChunkPos
}

@KMixin<LevelChunk>(Side.Common)
class FeatureLandmarksProto2Level {
    @WrapOperation(
        method = [$$"<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;setAllReferences(Ljava/util/Map;)V"
        )]
    )
    fun toLevelChunk(
        levelChunk: LevelChunk,
        structureRefs: MutableMap<*, *>,
        original: Operation<Void>,
        @Local(argsOnly = true, name = ["level"]) level: ServerLevel,
        @Local(argsOnly = true, name = ["protoChunk"]) protoChunk: ProtoChunk,
    ) {
        original.call(levelChunk, structureRefs)
        levelChunk.setInitialFeatureLandmarks(level, protoChunk.featureLandmarks)
    }
}

@KMixin<SerializableChunkData>(Side.Common)
class ChunkFeatureLandmarksData {

    @Extension
    var featureLandmarksTag: Tag? = null

    @ModifyReturnValue(method = ["write"], at = [At(value = "TAIL")])
    fun toNbt(tag: CompoundTag) = tag.apply {
        featureLandmarksTag?.let { put(TAG_NAME, it) }
    }

    @WrapOperation(
        method = ["read"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setAllReferences(Ljava/util/Map;)V"
        )]
    )
    fun toProtoChunk(
        chunk: ChunkAccess,
        data: Map<Structure, LongSet>,
        original: Operation<Void>,
        @Local(argsOnly = true, name = ["level"]) level: ServerLevel,
    ) {
        original.call(chunk, data)
        val featureLandmarks = featureLandmarksTag?.decodeOrNull(Landmark.SET_CODEC).orEmpty().ifEmpty { return }
        chunk.setInitialFeatureLandmarks(level, featureLandmarks)
        chunk.markUnsaved()
    }

    companion object {
        private const val TAG_NAME = "achievetodo_featureLandmarks"

        @ModifyReturnValue(method = ["parse"], at = [At(value = "TAIL")])
        fun fromNbt(
            original: SerializableChunkData,
            @Local(argsOnly = true, name = ["chunkData"]) tag: CompoundTag
        ) = original.apply {
            featureLandmarksTag = tag.get(TAG_NAME)
        }

        @ModifyReturnValue(method = ["copyOf"], at = [At(value = "TAIL")])
        fun fromChunk(
            original: SerializableChunkData,
            @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess
        ) = original.apply {
            featureLandmarksTag = chunk.featureLandmarks.toNbtOrNull(Landmark.SET_CODEC)
        }
    }
}

@KMixin<ChunkGenerator>(Side.Common)
class FeatureLandmarkGenTracker {
    @WrapOperation(
        method = ["applyBiomeDecoration"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/placement/FeaturePlacer;placeWithBiomeCheck(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
        )]
    )
    fun trackGen(
        placer: FeaturePlacer,
        featureToPlace: PlacedFeature,
        random: RandomSource,
        origin: BlockPos,
        original: Operation<Boolean>,
        @Local(argsOnly = true, name = ["level"]) worldGen: WorldGenLevel,
        @Local(argsOnly = true, name = ["chunk"]) chunk: ChunkAccess,
    ): Boolean {
        fun gen() = original.call(placer, featureToPlace, random, origin)

        val featureHolder = featureToPlace.feature
        val landmarkType = featureHolder.unwrapKey().getOrNull()?.let { LandmarkType.fromFeature(it) } ?: return gen()
        val dimensionType = DimensionType.findByLevel(worldGen.level.dimension()) ?: return gen()
        val (generated, bb) = worldGen.trackLandmarkBounds { gen() }
        if (bb != null) {
            chunk.addFeatureLandmark(worldGen, Landmark(landmarkType, LandmarkArea(dimensionType, bb)))
        }
        return generated
    }
}

@KMixin<WorldGenRegion>(Side.Common, InitStrategy.Synchronized)
class RegionBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At(value = "RETURN")])
    fun LevelWriter.trackBlockPlacing(placed: Boolean, @Local(argsOnly = true, name = ["pos"]) pos: BlockPos): Boolean {
        LandmarkGenHelper.onBlockPlaced(this, pos, placed)
        return placed
    }
}

@KMixin<Level>(Side.Common, InitStrategy.Synchronized)
class LevelBlockTracker {
    @ModifyReturnValue(method = ["setBlock"], at = [At(value = "RETURN")])
    fun LevelWriter.trackBlockPlacing(placed: Boolean, @Local(argsOnly = true, name = ["pos"]) pos: BlockPos): Boolean {
        LandmarkGenHelper.onBlockPlaced(this, pos, placed)
        return placed
    }
}

private object LandmarkGenHelper {

    private val activeState = ThreadLocal<State?>()

    fun <R> track(level: LevelWriter, gen: () -> R): Pair<R, BoundingBox?> {
        start(level)
        return try {
            gen() to stop()
        } finally {
            activeState.remove()
        }
    }

    private fun start(level: LevelWriter) {
        activeState.set(State(level))
    }

    private fun stop(): BoundingBox? {
        val state = activeState.get()
        activeState.remove()
        return state?.bb
    }

    fun onBlockPlaced(level: LevelWriter, pos: BlockPos, success: Boolean) {
        if (!success) return
        val state = activeState.get() ?: return
        if (state.targetLevel === level) {
            state.expand(pos)
        }
    }

    class State(val targetLevel: LevelWriter) {

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

private fun <R> WorldGenLevel.trackLandmarkBounds(gen: () -> R): Pair<R, BoundingBox?> =
    LandmarkGenHelper.track(this, gen)
