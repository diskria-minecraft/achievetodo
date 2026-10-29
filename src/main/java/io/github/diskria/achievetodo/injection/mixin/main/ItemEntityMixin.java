//package io.github.diskria.achievetodo.injection.mixin.main;
//
//import io.github.diskria.achievetodo.AchieveToDoMod;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(ItemEntity.class)
//public class ItemEntityMixin {
//
//    @Inject(
//        method = "onPlayerCollision",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/player/PlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;",
//            shift = At.Shift.BEFORE
//        ),
//        cancellable = true
//    )
//    public void lockPickUp(PlayerEntity player, CallbackInfo ci) {
//        ItemEntity itemEntity = (ItemEntity) (Object) this;
//        if (AchieveToDoMod.isTargetInLockedLandmark(player, itemEntity)) {
//            ci.cancel();
//        }
//    }
//}
