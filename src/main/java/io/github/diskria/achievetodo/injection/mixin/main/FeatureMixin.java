package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.injection.extension.main.ChunkExtension;
import io.github.diskria.achievetodo.injection.extension.main.LandmarkGenerationTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Feature.class)
public class FeatureMixin {

    @WrapOperation(
        method = "generateIfValid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/feature/Feature;generate(Lnet/minecraft/world/gen/feature/util/FeatureContext;)Z"
        )
    )
    public <FC extends FeatureConfig> boolean trackFeatureGeneration(
        Feature<?> feature,
        FeatureContext<FC> featureContext,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) @NotNull StructureWorldAccess world,
        @Local(argsOnly = true) BlockPos pos
    ) {
        LandmarkType landmarkType = LandmarkType.FEATURES.get(feature);
        DimensionType dimensionType = null;
        ServerWorld serverWorld = null;
        LandmarkGenerationTracker tracker = null;
        if (landmarkType != null) {
            serverWorld = world.toServerWorld();
            dimensionType = DimensionType.findByWorld(serverWorld.getRegistryKey());
            if (dimensionType == null) {
                serverWorld = null;
            } else if (world instanceof LandmarkGenerationTracker landmarkGenerationTracker) {
                tracker = landmarkGenerationTracker;
                tracker.achievetodo$setLandmarkBlockBox(null);
                tracker.achievetodo$setLandmarkGenerationTrackingEnabled(true);
            }
        }
        boolean result = original.call(feature, featureContext);
        if (tracker != null && world.getChunk(pos) instanceof ChunkExtension chunkExtension) {
            BlockBox blockBox = tracker.achievetodo$getLandmarkBlockBox();
            if (blockBox != null) {
                chunkExtension.achievetodo$addFeatureLandmark(
                    serverWorld,
                    landmarkType,
                    new DimensionalBlockBox(dimensionType, blockBox)
                );
            }
            tracker.achievetodo$setLandmarkGenerationTrackingEnabled(false);
            tracker.achievetodo$setLandmarkBlockBox(null);
        }
        return result;
    }
}
