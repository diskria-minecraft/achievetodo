package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.ability.DimensionType;
import io.github.diskria.achievetodo.ability.LandmarkType;
import net.minecraft.util.math.Box;

public record LockedLandmarkBox(LandmarkType landmarkType, DimensionType dimensionType, Box box) {
}
