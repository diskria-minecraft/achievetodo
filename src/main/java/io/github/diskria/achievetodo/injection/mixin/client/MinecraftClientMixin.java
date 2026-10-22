package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import io.github.diskria.achievetodo.injection.extension.client.CreateWorldScreenExtension;
import io.github.diskria.achievetodo.injection.extension.client.MovementTutorialStepHandlerExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public @Nullable ClientPlayerEntity player;

    @Inject(
        method = "setScreen",
        at = @At("HEAD"),
        cancellable = true
    )
    public void setScreenInject(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (screen instanceof AdvancementsScreen) {
            if (AchieveToDoClient.isNotReady()) {
                if (player != null) {
                    player.sendMessage(
                        AchieveToDoClient.translate("error.not_ready_yet")
                            .formatted(Formatting.RED),
                        true
                    );
                }
                ci.cancel();
            } else if (client.getTutorialManager().currentHandler
                instanceof MovementTutorialStepHandlerExtension movementTutorialStepHandlerExtension
            ) {
                movementTutorialStepHandlerExtension.achievetodo$onAdvancementsOpened();
            }
        } else if (screen instanceof CreateWorldScreen createWorldScreen &&
            screen instanceof CreateWorldScreenExtension createWorldScreenExtension &&
            createWorldScreenExtension.achievetodo$isWaitingDatapack()
        ) {
            createWorldScreen.createLevel();
            createWorldScreenExtension.achievetodo$setWaitingDatapack(false);
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "handleInputEvents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z",
            ordinal = 4
        )
    )
    public boolean lockInventory(KeyBinding keyBinding, @NotNull Operation<Boolean> original) {
        return original.call(keyBinding) && !AchieveToDoClient.isAbilityLocked(AbilityType.OPEN_INVENTORY);
    }
}
