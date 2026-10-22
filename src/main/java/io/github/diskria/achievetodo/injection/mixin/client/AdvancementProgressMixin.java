package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.injection.extension.main.AdvancementProgressExtension;
import io.github.diskria.achievetodo.server.Constants;
import io.github.diskria.achievetodo.tracking.TrackedNearbyEntitiesType;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import io.github.diskria.achievetodo.tracking.TrackedStatisticsDataType;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementProgress.class)
public abstract class AdvancementProgressMixin implements AdvancementProgressExtension {

    @Unique
    private TrackedScoreType trackedScoreType;

    @Unique
    private TrackedNearbyEntitiesType trackedNearbyEntitiesType;

    @Unique
    private TrackedStatisticsDataType trackedStatisticsDataType;

    @Unique
    private AbilityType abilityType;

    @Override
    public void achievetodo$setAdvancementId(Identifier advancementId) {
        trackedScoreType = TrackedScoreType.findByAdvancement(advancementId);
        if (trackedScoreType == null) {
            trackedNearbyEntitiesType = TrackedNearbyEntitiesType.findByAdvancement(advancementId);
            if (trackedNearbyEntitiesType == null) {
                trackedStatisticsDataType = TrackedStatisticsDataType.findByAdvancement(advancementId);
                if (trackedStatisticsDataType == null) {
                    abilityType = AbilityType.findByAdvancement(advancementId);
                }
            }
        }
    }

    @Shadow
    public abstract boolean isDone();

    @Inject(
        method = "countObtainedRequirements",
        at = @At("HEAD"),
        cancellable = true
    )
    public void overrideObtainedRequirementsCount(CallbackInfoReturnable<Integer> cir) {
        if (trackedScoreType != null) {
            if (isDone()) {
                cir.setReturnValue(trackedScoreType.getFinalValue());
            } else {
                cir.setReturnValue(AchieveToDoClient.getTrackedScore(trackedScoreType));
            }
        } else if (trackedNearbyEntitiesType != null) {
            if (isDone()) {
                cir.setReturnValue(trackedNearbyEntitiesType.getEntitiesCount());
            } else {
                cir.setReturnValue(AchieveToDoClient.getTrackedNearbyEntitiesCount(trackedNearbyEntitiesType));
            }
        } else if (trackedStatisticsDataType != null) {
            if (isDone()) {
                cir.setReturnValue(trackedStatisticsDataType.getFinalValue());
            } else {
                cir.setReturnValue(AchieveToDoClient.getTrackedStatisticsData(trackedStatisticsDataType));
            }
        } else if (abilityType != null) {
            cir.setReturnValue(
                Math.min(
                    AchieveToDoClient.getObtainedAdvancementsCount(),
                    AchieveToDoClient.getRequiredAdvancementsCount(abilityType)
                )
            );
        }
    }

    @WrapOperation(
        method = {
            "getProgressBarPercentage",
            "getProgressBarFraction"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancement/AdvancementRequirements;getLength()I"
        )
    )
    public int overrideRequiredCount(
        AdvancementRequirements requirements,
        Operation<Integer> original
    ) {
        if (trackedScoreType != null) {
            return trackedScoreType.getFinalValue();
        }
        if (trackedNearbyEntitiesType != null) {
            return trackedNearbyEntitiesType.getEntitiesCount();
        }
        if (trackedStatisticsDataType != null) {
            return trackedStatisticsDataType.getFinalValue();
        }
        if (abilityType != null) {
            return AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
        }
        return original.call(requirements);
    }

    @Inject(
        method = "getProgressBarPercentage",
        at = @At("HEAD"),
        cancellable = true
    )
    public void overrideProgressPercentage(CallbackInfoReturnable<Float> cir) {
        if (abilityType != null) {
            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
            if (requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG) {
                cir.setReturnValue(0.0f);
            } else if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG) {
                cir.setReturnValue(1.0f);
            }
        } else if (!isDone()) {
            int completionPercent = 0;
            if (trackedScoreType != null && trackedScoreType.isPercentage()) {
                completionPercent = AchieveToDoClient.getTrackedScore(trackedScoreType);
            } else if (trackedStatisticsDataType != null && trackedStatisticsDataType.isPercentage()) {
                completionPercent = AchieveToDoClient.getTrackedStatisticsData(trackedStatisticsDataType);
            }
            if (completionPercent > 0) {
                cir.setReturnValue(completionPercent / 100.0f);
            }
        }
    }

    @Inject(
        method = "getProgressBarFraction",
        at = @At("HEAD"),
        cancellable = true
    )
    public void overrideProgressText(CallbackInfoReturnable<Text> cir) {
        if (abilityType != null) {
            int requiredCount = AchieveToDoClient.getRequiredAdvancementsCount(abilityType);
            if (requiredCount == Constants.Progression.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == Constants.Progression.PERMANENTLY_LOCKED_FLAG
            ) {
                cir.setReturnValue(null);
                return;
            }
        }
        boolean isScore = trackedScoreType != null && trackedScoreType.isPercentage();
        boolean isStatisticsData = trackedStatisticsDataType != null && trackedStatisticsDataType.isPercentage();
        if (!isScore && !isStatisticsData) {
            return;
        }
        int completionPercent;
        if (isDone()) {
            completionPercent = 100;
        } else if (isScore) {
            completionPercent = AchieveToDoClient.getTrackedScore(trackedScoreType);
        } else {
            completionPercent = AchieveToDoClient.getTrackedStatisticsData(trackedStatisticsDataType);
        }
        cir.setReturnValue(Text.translatable("mco.upload.percent", completionPercent));
    }
}
