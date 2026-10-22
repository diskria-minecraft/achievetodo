package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.injection.extension.main.ChunkExtension;
import io.github.diskria.achievetodo.injection.extension.main.SerializedChunkExtension;
import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.SerializedChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.StorageKey;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(SerializedChunk.class)
public class SerializedChunkMixin implements SerializedChunkExtension {

    @Unique
    private NbtCompound featureLandmarksNbt;

    @ModifyReturnValue(
        method = "fromChunk",
        at = @At(value = "TAIL")
    )
    private static SerializedChunk writeFeatureLandmarkNbt(
        SerializedChunk original,
        @Local(argsOnly = true) ServerWorld world,
        @Local(argsOnly = true) Chunk chunk
    ) {
        if (original instanceof SerializedChunkExtension serializedChunkExtension &&
            chunk instanceof ChunkExtension chunkExtension
        ) {
            Map<LandmarkType, Set<DimensionalBlockBox>> landmarks = chunkExtension.achievetodo$getFeatureLandmarks();
            if (landmarks != null && !landmarks.isEmpty()) {
                NbtCompound featureLandmarksNbt = new NbtCompound();
                for (var entry : landmarks.entrySet()) {
                    Set<DimensionalBlockBox> dimensionalBlockBoxes = entry.getValue();
                    NbtList blockBoxesNbt = new NbtList();
                    for (DimensionalBlockBox dimensionalBlockBox : dimensionalBlockBoxes) {
                        BlockBox blockBox = dimensionalBlockBox.blockBox();
                        NbtCompound blockBoxNbt = new NbtCompound();
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_X, blockBox.getMinX());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Y, blockBox.getMinY());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Z, blockBox.getMinZ());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_X, blockBox.getMaxX());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Y, blockBox.getMaxY());
                        blockBoxNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Z, blockBox.getMaxZ());
                        blockBoxesNbt.add(blockBoxNbt);
                    }
                    LandmarkType landmarkType = entry.getKey();
                    featureLandmarksNbt.put(landmarkType.getName(), blockBoxesNbt);
                }
                serializedChunkExtension.achievetodo$setFeatureLandmarksNbt(featureLandmarksNbt);
            }
        }
        return original;
    }

    @ModifyReturnValue(
        method = "fromNbt",
        at = @At(value = "TAIL")
    )
    private static SerializedChunk readFeatureLandmarksNbt(
        SerializedChunk serializedChunk,
        @Local(argsOnly = true) @NotNull NbtCompound nbt
    ) {
        NbtCompound featureLandmarksNbt = nbt.getCompound(Constants.NbtKey.FEATURE_LANDMARKS);
        if (featureLandmarksNbt != null &&
            serializedChunk instanceof SerializedChunkExtension serializedChunkExtension
        ) {
            serializedChunkExtension.achievetodo$setFeatureLandmarksNbt(featureLandmarksNbt);
        }
        return serializedChunk;
    }

    @Override
    public void achievetodo$setFeatureLandmarksNbt(NbtCompound featureLandmarksNbt) {
        this.featureLandmarksNbt = featureLandmarksNbt;
    }

    @ModifyReturnValue(
        method = "serialize",
        at = @At(value = "TAIL")
    )
    private NbtCompound serializeLandmarksNbt(NbtCompound original) {
        if (featureLandmarksNbt != null) {
            original.put(Constants.NbtKey.FEATURE_LANDMARKS, featureLandmarksNbt);
        }
        return original;
    }

    @Inject(
        method = "convert",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;setStructureReferences(Ljava/util/Map;)V",
            shift = At.Shift.AFTER
        )
    )
    private void convertFeatureLandmarksNbt(
        ServerWorld world,
        PointOfInterestStorage poiStorage,
        StorageKey key,
        ChunkPos expectedPos,
        CallbackInfoReturnable<ProtoChunk> cir,
        @Local Chunk chunk
    ) {
        if (featureLandmarksNbt != null && chunk instanceof ChunkExtension chunkExtension) {
            DimensionType dimensionType = DimensionType.findByWorld(world.getRegistryKey());
            if (dimensionType == null) {
                return;
            }
            Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks = null;
            for (String landmarkName : featureLandmarksNbt.getKeys()) {
                LandmarkType landmarkType = LandmarkType.findByName(landmarkName);
                if (landmarkType == null) {
                    continue;
                }
                NbtList blockBoxesNbt = featureLandmarksNbt.getList(landmarkName, NbtElement.COMPOUND_TYPE);
                if (blockBoxesNbt == null) {
                    continue;
                }
                for (int i = 0; i < blockBoxesNbt.size(); i++) {
                    NbtCompound blockBoxNbt = blockBoxesNbt.getCompound(i);
                    if (!blockBoxesNbt.isEmpty()) {
                        BlockBox blockBox = new BlockBox(
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_X),
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_Y),
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_Z),
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_X),
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_Y),
                            blockBoxNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_Z)
                        );
                        if (featureLandmarks == null) {
                            featureLandmarks = new HashMap<>();
                        }
                        featureLandmarks
                            .computeIfAbsent(landmarkType, k -> new HashSet<>())
                            .add(new DimensionalBlockBox(dimensionType, blockBox));
                    }
                }
            }
            chunkExtension.achievetodo$setFeatureLandmarks(world, featureLandmarks);
        }
    }
}