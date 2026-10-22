package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.injection.extension.client.MovementTutorialStepHandlerExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.client.tutorial.MovementTutorialStepHandler;
import net.minecraft.client.tutorial.TutorialManager;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementTutorialStepHandler.class)
public class MovementTutorialStepHandlerMixin implements MovementTutorialStepHandlerExtension {

    @Unique
    private static final Text OPEN_ADVANCEMENTS_TITLE =
        AchieveToDoClient.translate("tutorial.open_advancements.title");

    @Unique
    private static final Text OPEN_ADVANCEMENTS_DESCRIPTION =
        AchieveToDoClient.translate(
            "tutorial.open_advancements.description",
            TutorialManager.keyToText("advancements")
        );

    @Unique
    private boolean isAdvancementsOpened;

    @Unique
    private TutorialToast openAdvancementsToast;
    @Shadow
    private int moveAroundCompletionTicks;
    @Shadow
    private int lookAroundCompletionTicks;
    @Shadow
    private int ticks;

    @Override
    public void achievetodo$onAdvancementsOpened() {
        if (openAdvancementsToast != null) {
            openAdvancementsToast.hide();
            openAdvancementsToast = null;
            isAdvancementsOpened = true;
        }
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/TutorialManager;getClient()Lnet/minecraft/client/MinecraftClient;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void scheduleUntilVisionAbilityUnlocked(CallbackInfo ci) {
        if (AchieveToDoClient.isAbilityLocked(AbilityType.VISION, true)) {
            ci.cancel();
        }
    }

    @ModifyConstant(
        method = "tick",
        constant = @Constant(intValue = 100)
    )
    private int showToastFaster(int constant) {
        return 50;
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/TutorialManager;getClient()Lnet/minecraft/client/MinecraftClient;"
        )
    )
    public MinecraftClient showOpenAdvancementsToast(
        TutorialManager manager,
        @NotNull Operation<MinecraftClient> original
    ) {
        MinecraftClient client = original.call(manager);
        if (moveAroundCompletionTicks != -1 &&
            lookAroundCompletionTicks != -1 &&
            ticks - lookAroundCompletionTicks >= 40 &&
            !isAdvancementsOpened &&
            openAdvancementsToast == null
        ) {
            openAdvancementsToast = new TutorialToast(
                client.textRenderer,
                TutorialToast.Type.RECIPE_BOOK,
                OPEN_ADVANCEMENTS_TITLE,
                OPEN_ADVANCEMENTS_DESCRIPTION,
                false
            );
            client.getToastManager().add(openAdvancementsToast);
        }
        return client;
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/tutorial/TutorialManager;setStep(Lnet/minecraft/client/tutorial/TutorialStep;)V"
        )
    )
    public void waitOpenAdvancementsCompletion(TutorialManager manager, TutorialStep step, Operation<Void> original) {
        if (isAdvancementsOpened) {
            original.call(manager, step);
        }
    }

    @Inject(
        method = "destroy",
        at = @At(value = "HEAD")
    )
    public void hideOpenAdvancementsToast(CallbackInfo ci) {
        if (openAdvancementsToast != null) {
            openAdvancementsToast.hide();
            openAdvancementsToast = null;
        }
    }
}
