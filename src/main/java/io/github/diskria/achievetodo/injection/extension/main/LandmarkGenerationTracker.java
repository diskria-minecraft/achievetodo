package io.github.diskria.achievetodo.injection.extension.main;

import net.minecraft.util.math.BlockBox;

public interface LandmarkGenerationTracker {
    void achievetodo$setLandmarkGenerationTrackingEnabled(boolean isLandmarkGenerationTrackingEnabled);

    void achievetodo$setLandmarkBlockBox(BlockBox landmarkBlockBox);

    BlockBox achievetodo$getLandmarkBlockBox();
}
