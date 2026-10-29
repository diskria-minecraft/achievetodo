//package io.github.diskria.achievetodo.injection.mixin.other;
//
//import io.github.diskria.achievetodo.injection.extension.main.AdvancementProgressExtension;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(PlayerAdvancementTracker.class)
//public class PlayerAdvancementTrackerMixin {
//
//    @Inject(
//        method = "initProgress",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/advancement/AdvancementProgress;init(Lnet/minecraft/advancement/AdvancementRequirements;)V",
//            shift = At.Shift.AFTER
//        )
//    )
//    public void setAdvancementId(
//        AdvancementEntry advancement,
//        AdvancementProgress progress,
//        CallbackInfo ci
//    ) {
//        if (progress instanceof AdvancementProgressExtension advancementProgressExtension) {
//            advancementProgressExtension.achievetodo$setAdvancementId(advancement.id());
//        }
//    }
//}
