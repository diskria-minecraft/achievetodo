package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import io.github.diskria.achievetodo.injection.extension.main.StructureStartExtension;
import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

@Mixin(StructureStart.class)
public class StructureStartMixin implements StructureStartExtension {

    @Unique
    private LandmarkType landmarkType;

    @Unique
    private BlockBox landmarkBlockBox;
    @Shadow
    @Final
    private ChunkPos pos;

    @ModifyReturnValue(
        method = "fromNbt",
        at = @At(
            value = "RETURN",
            ordinal = 2
        )
    )
    private static @NotNull StructureStart readLandmarkBlockBoxFromNbt(
        StructureStart original,
        @Local(argsOnly = true) @NotNull NbtCompound nbt
    ) {
        NbtCompound landmarkNbt = nbt.getCompound(Constants.NbtKey.STRUCTURE_LANDMARK);
        if (landmarkNbt.contains(Constants.NbtKey.LANDMARK_TYPE) &&
            original instanceof StructureStartExtension structureStartExtension
        ) {
            LandmarkType landmarkType = LandmarkType.findByName(landmarkNbt.getString(Constants.NbtKey.LANDMARK_TYPE));
            if (landmarkType != null) {
                structureStartExtension.achievetodo$setLandmarkType(landmarkType);
                if (landmarkNbt.contains(Constants.NbtKey.BLOCK_BOX_MIN_X)) {
                    structureStartExtension.achievetodo$setLandmarkBlockBox(
                        new BlockBox(
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_X),
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_Y),
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MIN_Z),
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_X),
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_Y),
                            landmarkNbt.getInt(Constants.NbtKey.BLOCK_BOX_MAX_Z)
                        )
                    );
                }
            }
        }
        return original;
    }

    @Override
    public void achievetodo$setLandmarkType(LandmarkType landmarkType) {
        this.landmarkType = landmarkType;
    }

    @Override
    public LandmarkType achievetodo$getLandmarkType() {
        return landmarkType;
    }

    @Override
    public void achievetodo$setLandmarkBlockBox(BlockBox landmarkBlockBox) {
        this.landmarkBlockBox = landmarkBlockBox;
    }

    @Override
    public BlockBox achievetodo$getLandmarkBlockBox() {
        return landmarkBlockBox;
    }

    @Inject(
        method = "toNbt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/NbtCompound;putInt(Ljava/lang/String;I)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void writeLandmarkBlockBoxToNbt(
        StructureContext context,
        ChunkPos chunkPos,
        CallbackInfoReturnable<NbtCompound> cir,
        @Local NbtCompound nbtCompound
    ) {
        if (landmarkType != null) {
            NbtCompound landmarkNbt = new NbtCompound();
            landmarkNbt.putString(Constants.NbtKey.LANDMARK_TYPE, landmarkType.getName());
            if (landmarkBlockBox != null) {
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_X, landmarkBlockBox.getMinX());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Y, landmarkBlockBox.getMinY());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MIN_Z, landmarkBlockBox.getMinZ());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_X, landmarkBlockBox.getMaxX());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Y, landmarkBlockBox.getMaxY());
                landmarkNbt.putInt(Constants.NbtKey.BLOCK_BOX_MAX_Z, landmarkBlockBox.getMaxZ());
            }
            nbtCompound.put(Constants.NbtKey.STRUCTURE_LANDMARK, landmarkNbt);
        }
    }

    @Inject(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockBox;getCenter()Lnet/minecraft/util/math/BlockPos;",
            shift = At.Shift.AFTER
        )
    )
    private void startStructureGenerationTracker(
        StructureWorldAccess world,
        StructureAccessor structureAccessor,
        ChunkGenerator chunkGenerator,
        Random random,
        BlockBox chunkBox,
        ChunkPos chunkPos,
        CallbackInfo ci
    ) {
        if (landmarkType != null) {
            ServerWorld serverWorld = world.toServerWorld();
            DimensionType dimensionType = DimensionType.findByWorld(serverWorld.getRegistryKey());
            if (dimensionType != null && world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
                landmarkGenerationTracker.achievetodo$setLandmarkGenerationTrackingEnabled(true);
                if (landmarkBlockBox != null) {
                    landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(new BlockBox(
                        landmarkBlockBox.getMinX(),
                        landmarkBlockBox.getMinY(),
                        landmarkBlockBox.getMinZ(),
                        landmarkBlockBox.getMaxX(),
                        landmarkBlockBox.getMaxY(),
                        landmarkBlockBox.getMaxZ()
                    ));
                } else {
                    landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(null);
                }
            }
        }
    }

    @Inject(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/structure/Structure;postPlace(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/BlockBox;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/structure/StructurePiecesList;)V",
            shift = At.Shift.AFTER
        )
    )
    private void stopStructureGenerationTracker(
        StructureWorldAccess world,
        StructureAccessor structureAccessor,
        ChunkGenerator chunkGenerator,
        Random random,
        BlockBox chunkBox,
        ChunkPos chunkPos,
        CallbackInfo ci
    ) {
        if (landmarkType != null && world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
            BlockBox newLandmarkBlockBox = landmarkGenerationTracker.achievetodo$getLandmarkBlockBox();
            if (newLandmarkBlockBox != null) {
                ServerWorld serverWorld = world.toServerWorld();
                DimensionType dimensionType = DimensionType.findByWorld(serverWorld.getRegistryKey());
                DimensionalBlockBox newDimensionalBlockBox = new DimensionalBlockBox(
                    dimensionType,
                    newLandmarkBlockBox
                );
                if (landmarkBlockBox == null) {
                    AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(
                        serverWorld,
                        pos,
                        Map.of(landmarkType, Set.of(newDimensionalBlockBox)),
                        true
                    );
                } else {
                    DimensionalBlockBox oldDimensionalBlockBox = new DimensionalBlockBox(
                        dimensionType,
                        landmarkBlockBox
                    );
                    if (!newLandmarkBlockBox.equals(landmarkBlockBox)) {
                        AchieveToDoMod.getServer().onLandmarkResized(
                            serverWorld,
                            pos,
                            landmarkType,
                            oldDimensionalBlockBox,
                            newDimensionalBlockBox
                        );
                    }
                }
                landmarkBlockBox = newLandmarkBlockBox;
            }
            landmarkGenerationTracker.achievetodo$setLandmarkGenerationTrackingEnabled(false);
            landmarkGenerationTracker.achievetodo$setLandmarkBlockBox(null);
        }
    }
}
