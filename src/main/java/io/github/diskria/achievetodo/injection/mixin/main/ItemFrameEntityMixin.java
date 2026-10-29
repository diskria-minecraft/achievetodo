//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.jetbrains.annotations.NotNull;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(ItemFrameEntity.class)
//public class ItemFrameEntityMixin {
//
//    @Inject(
//        method = "damage",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;dropHeldStack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Z)V",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockDrop(
//        ServerWorld world,
//        @NotNull DamageSource source,
//        float amount,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        ItemFrameEntity itemFrameEntity = (ItemFrameEntity) (Object) this;
//        if (source.getAttacker() instanceof PlayerEntity player &&
//            AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)
//        ) {
//            cir.setReturnValue(false);
//        }
//    }
//}
