package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.client.gui.AdvancementsTabType;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementDisplays;
import net.minecraft.advancement.PlacedAdvancement;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.function.Predicate;

@Mixin(AdvancementDisplays.class)
public class AdvancementDisplaysMixin {

    @ModifyConstant(
        method = "shouldDisplay(Lit/unimi/dsi/fastutil/Stack;)Z",
        constant = @Constant(intValue = 2)
    )
    private static int forceShowAllAdvancements(
        int displayDepth,
        @Local(argsOnly = true) Stack<AdvancementDisplays.Status> statuses
    ) {
        return ((ObjectArrayList<AdvancementDisplays.Status>) statuses).size() - 1;
    }

    @WrapOperation(
        method = "shouldDisplay(Lnet/minecraft/advancement/PlacedAdvancement;Lit/unimi/dsi/fastutil/Stack;Ljava/util/function/Predicate;Lnet/minecraft/advancement/AdvancementDisplays$ResultConsumer;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"
        )
    )
    private static boolean wrapTestOperation(
        Predicate<PlacedAdvancement> donePredicate,
        Object advancement,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) @NotNull PlacedAdvancement placedAdvancement
    ) {
        if (!placedAdvancement.getAdvancement().isRoot() &&
            AdvancementsTabType.findByAdvancement(placedAdvancement.getRoot()) != AdvancementsTabType.CHALLENGES
        ) {
            AdvancementDisplay display = placedAdvancement.getAdvancement().display().orElse(null);
            if (display != null && !display.isHidden()) {
                return true;
            }
        }
        return original.call(donePredicate, advancement);
    }
}
