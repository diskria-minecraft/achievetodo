package io.github.diskria.achievetodo.injection.extension.main;

import io.github.diskria.achievetodo.ability.DimensionalBlockBox;
import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Set;

public interface ChunkExtension {

    void achievetodo$setFeatureLandmarks(ServerWorld world, Map<LandmarkType, Set<DimensionalBlockBox>> landmarks);

    Map<LandmarkType, Set<DimensionalBlockBox>> achievetodo$getFeatureLandmarks();

    void achievetodo$addFeatureLandmark(
        ServerWorld world,
        LandmarkType landmarkType,
        DimensionalBlockBox dimensionalBlockBox
    );
}
