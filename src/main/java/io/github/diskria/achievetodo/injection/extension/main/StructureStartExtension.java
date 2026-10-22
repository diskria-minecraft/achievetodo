package io.github.diskria.achievetodo.injection.extension.main;

import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.util.math.BlockBox;

public interface StructureStartExtension {
    void achievetodo$setLandmarkType(LandmarkType landmarkType);

    LandmarkType achievetodo$getLandmarkType();

    void achievetodo$setLandmarkBlockBox(BlockBox blockBox);

    BlockBox achievetodo$getLandmarkBlockBox();
}
