package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.injection.extension.main.ChunkExtension;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements ChunkExtension {

    @Shadow
    @Final
    protected ChunkPos pos;
    @Unique
    private Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks;

    @Override
    public void achievetodo$setFeatureLandmarks(
        @NotNull ServerWorld world,
        Map<LandmarkType, Set<DimensionalBlockBox>> featureLandmarks
    ) {
        this.featureLandmarks = featureLandmarks;
        if (featureLandmarks != null) {
            AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(world, pos, featureLandmarks, true);
        }
        markNeedsSaving();
    }

    @Override
    public Map<LandmarkType, Set<DimensionalBlockBox>> achievetodo$getFeatureLandmarks() {
        return featureLandmarks;
    }

    @Override
    public void achievetodo$addFeatureLandmark(
        @NotNull ServerWorld world,
        LandmarkType featureLandmarkType,
        DimensionalBlockBox dimensionalBlockBox
    ) {
        if (featureLandmarks == null) {
            featureLandmarks = new HashMap<>();
        }
        featureLandmarks
            .computeIfAbsent(featureLandmarkType, k -> new HashSet<>())
            .add(dimensionalBlockBox);
        AchieveToDoMod.getServer().onLandmarksLoadedStatusChanged(
            world,
            pos,
            Map.of(featureLandmarkType, Set.of(dimensionalBlockBox)),
            true
        );
        markNeedsSaving();
    }

    @Shadow
    public abstract void markNeedsSaving();
}
