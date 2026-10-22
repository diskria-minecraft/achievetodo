package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.injection.extension.main.AdvancementProgressExtension;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.network.ClientAdvancementManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientAdvancementManager.class)
public class ClientAdvancementManagerMixin {

    @WrapOperation(
        method = "onAdvancements",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancement/AdvancementProgress;init(Lnet/minecraft/advancement/AdvancementRequirements;)V"
        )
    )
    public void setAdvancementId(
        AdvancementProgress progress,
        AdvancementRequirements requirements,
        @NotNull Operation<Void> original,
        @Local PlacedAdvancement placedAdvancement
    ) {
        original.call(progress, requirements);
        if (progress instanceof AdvancementProgressExtension advancementProgressExtension) {
            advancementProgressExtension.achievetodo$setAdvancementId(placedAdvancement.getAdvancementEntry().id());
        }
    }
}
