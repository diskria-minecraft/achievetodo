//package io.github.diskria.achievetodo.injection.mixin.client;
//
//import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//import io.github.diskria.achievetodo.ability.AbilityType;
//import io.github.diskria.achievetodo.client.AchieveToDoClient;
//import io.github.diskria.achievetodo.injection.extension.main.AdvancementProgressExtension;
//import io.github.diskria.achievetodo.server.Constants;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(AdvancementProgress.class)
//public abstract class AdvancementProgressMixin implements AdvancementProgressExtension {
//
//    @Unique
//    private TrackedScore trackedScore;
//
//    @Unique
//    private EntitiesNearbyTrackedAdvancement trackedNearbyEntitiesType;
//
//    @Unique
//    private TrackedStat trackedStat;
//
//    @Unique
//    private AbilityType abilityType;
//
//    @Override
//    public void achievetodo$setAdvancementId(Identifier advancementId) {
//        trackedScore = TrackedScore.findByAdvancement(advancementId);
//        if (trackedScore == null) {
//            trackedNearbyEntitiesType = EntitiesNearbyTrackedAdvancement.findByAdvancement(advancementId);
//            if (trackedNearbyEntitiesType == null) {
//                trackedStat = TrackedStat.findByAdvancement(advancementId);
//                if (trackedStat == null) {
//                    abilityType = AbilityType.findByAdvancement(advancementId);
//                }
//            }
//        }
//    }
//
//    @Shadow
//    public abstract boolean isDone();
//
//    @Inject(
//        method = "countObtainedRequirements",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    public void overrideObtainedRequirementsCount(CallbackInfoReturnable<Integer> cir) {
//        if (trackedScore != null) {
//            if (isDone()) {
//                cir.setReturnValue(trackedScore.finalValue);
//            } else {
//                cir.setReturnValue(AchieveToDoClient.getTrackedAdvancementProgress(trackedScore));
//            }
//        } else if (trackedNearbyEntitiesType != null) {
//            if (isDone()) {
//                cir.setReturnValue(trackedNearbyEntitiesType.getFinalValue());
//            } else {
//                cir.setReturnValue(AchieveToDoClient.getTrackedNearbyEntitiesCount(trackedNearbyEntitiesType));
//            }
//        } else if (trackedStat != null) {
//            if (isDone()) {
//                cir.setReturnValue(trackedStat.finalValue);
//            } else {
//                cir.setReturnValue(AchieveToDoClient.getTrackedStatisticsData(trackedStat));
//            }
//        } else if (abilityType != null) {
//            cir.setReturnValue(
//                Math.min(
//                    AchieveToDoClient.getObtainedAdvancementsCount(),
//                    AchieveToDoClient.getRequiredAdvancementsCount(abilityType)
//                )
//            );
//        }
//    }
//
//    @WrapOperation(
//        method = {
//            "getProgressBarPercentage",
//            "getProgressBarFraction"
//        },
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/advancement/AdvancementRequirements;getLength()I"
//        )
//    )
//    public int overrideRequiredCount(
//        AdvancementRequirements requirements,
//        Operation<Integer> original
//    ) {
//        if (trackedScore != null) {
//            return trackedScore.finalValue;
//        }
//        if (trackedNearbyEntitiesType != null) {
//            return trackedNearbyEntitiesType.getFinalValue();
//        }
//        if (trackedStat != null) {
//            return trackedStat.finalValue;
//        }
//        if (abilityType != null) {
//            return AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
//        }
//        return original.call(requirements);
//    }
//
//    @Inject(
//        method = "getProgressBarPercentage",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    public void overrideProgressPercentage(CallbackInfoReturnable<Float> cir) {
//        if (abilityType != null) {
//            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
//            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
//                cir.setReturnValue(0.0f);
//            } else if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
//                cir.setReturnValue(1.0f);
//            }
//        } else if (!isDone()) {
//            int completionPercent = 0;
//            if (trackedScore != null && trackedScore.percentsMode) {
//                completionPercent = AchieveToDoClient.getTrackedAdvancementProgress(trackedScore);
//            } else if (trackedStat != null && trackedStat.percentsMode) {
//                completionPercent = AchieveToDoClient.getTrackedStatisticsData(trackedStat);
//            }
//            if (completionPercent > 0) {
//                cir.setReturnValue(completionPercent / 100.0f);
//            }
//        }
//    }
//
//    @Inject(
//        method = "getProgressBarFraction",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    public void overrideProgressText(CallbackInfoReturnable<Text> cir) {
//        if (abilityType != null) {
//            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
//            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
//                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
//            ) {
//                cir.setReturnValue(null);
//                return;
//            }
//        }
//        boolean isScore = trackedScore != null && trackedScore.percentsMode;
//        boolean isStatisticsData = trackedStat != null && trackedStat.percentsMode;
//        if (!isScore && !isStatisticsData) {
//            return;
//        }
//        int completionPercent;
//        if (isDone()) {
//            completionPercent = 100;
//        } else if (isScore) {
//            completionPercent = AchieveToDoClient.getTrackedAdvancementProgress(trackedScore);
//        } else {
//            completionPercent = AchieveToDoClient.getTrackedStatisticsData(trackedStat);
//        }
//        cir.setReturnValue(Text.translatable("mco.upload.percent", completionPercent));
//    }
//}
