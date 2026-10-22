package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlockMixin {

    @Shadow
    @Final
    protected CauldronBehavior.CauldronBehaviorMap behaviorMap;

    @WrapOperation(
        method = "onUseWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/cauldron/CauldronBehavior;interact(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/ActionResult;"
        )
    )
    public ActionResult lockCauldron(
        CauldronBehavior behavior,
        BlockState blockState,
        World world,
        BlockPos blockPos,
        PlayerEntity player,
        Hand hand,
        ItemStack stack,
        Operation<ActionResult> original
    ) {
        if (behavior != ((Object2ObjectOpenHashMap<?, ?>) behaviorMap.map()).defaultReturnValue()) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, world, blockPos) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_CAULDRON)
            ) {
                return ActionResult.SUCCESS;
            }
        }
        return original.call(behavior, blockState, world, blockPos, player, hand, stack);
    }
}
