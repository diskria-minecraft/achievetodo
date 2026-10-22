package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.ability.LandmarkType;
import io.github.diskria.achievetodo.injection.extension.main.StructureStartExtension;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Structure.class)
public abstract class StructureMixin {

    @Shadow
    public abstract StructureType<?> getType();

    @ModifyReturnValue(
        method = "createStructureStart",
        at = @At(
            value = "RETURN",
            ordinal = 0
        )
    )
    private StructureStart setStructureLandmarkType(
        StructureStart structureStart,
        @Local(argsOnly = true) RegistryEntry<Structure> structure
    ) {
        if (structureStart instanceof StructureStartExtension structureStartExtension) {
            LandmarkType landmarkType = LandmarkType.findByStructureRegistryKey(structure.getKey().orElse(null));
            if (landmarkType != null) {
                structureStartExtension.achievetodo$setLandmarkType(landmarkType);
            }
        }
        return structureStart;
    }
}
