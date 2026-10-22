package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRegion.class)
public abstract class ChunkRegionMixin implements StructureWorldAccess, LandmarkGenerationTracker {

    @Unique
    private boolean isLandmarkGenerationTrackingEnabled;

    @Unique
    private BlockBox landmarkBlockBox;

    @Override
    public void achievetodo$setLandmarkGenerationTrackingEnabled(boolean isLandmarkGenerationTrackingEnabled) {
        this.isLandmarkGenerationTrackingEnabled = isLandmarkGenerationTrackingEnabled;
    }

    @Override
    public void achievetodo$setLandmarkBlockBox(BlockBox landmarkBlockBox) {
        this.landmarkBlockBox = landmarkBlockBox;
    }

    @Override
    public BlockBox achievetodo$getLandmarkBlockBox() {
        return landmarkBlockBox;
    }

    @SuppressWarnings("deprecation")
    @Inject(
        method = "setBlockState",
        at = @At(value = "TAIL")
    )
    private void trackLandmarkGeneration(
        BlockPos pos,
        BlockState state,
        int flags,
        int maxUpdateDepth,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (isLandmarkGenerationTrackingEnabled) {
            if (landmarkBlockBox == null) {
                landmarkBlockBox = new BlockBox(pos);
            }
            landmarkBlockBox.encompass(pos);
        }
    }
}
