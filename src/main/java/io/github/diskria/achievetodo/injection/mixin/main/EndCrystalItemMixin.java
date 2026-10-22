package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.entity.Entity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemUsageContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

    @WrapOperation(
        method = "useOnBlock",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    public boolean lockEndCrystal(
        List<Entity> entities,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) ItemUsageContext context
    ) {
        if (!original.call(entities)) {
            return false;
        }
        return !AchieveToDoMod.isTargetInLockedLandmark(context) &&
            !AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.PLACE_END_CRYSTAL);
    }
}
