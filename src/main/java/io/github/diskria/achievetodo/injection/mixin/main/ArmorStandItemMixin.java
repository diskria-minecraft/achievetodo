package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.AchieveToDoMod;
import net.minecraft.entity.Entity;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    @WrapOperation(
        method = "useOnBlock",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    public boolean lockArmorStandPlace(
        List<Entity> entities,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) ItemUsageContext context,
        @Local Box boundingBox
    ) {
        return original.call(entities) &&
            !AchieveToDoMod.isTargetInLockedLandmark(context.getPlayer(), context.getWorld(), boundingBox);
    }
}
