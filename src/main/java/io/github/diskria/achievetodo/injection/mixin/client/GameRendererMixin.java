package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private float blackOverlayAlpha = 0.0f;
    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Unique
    private void drawBlackOverlay(@NotNull MatrixStack stack) {
        VertexConsumerProvider.Immediate immediate = buffers.getEntityVertexConsumers();
        Matrix4f matrix = stack.peek().getPositionMatrix();
        VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayer.getGui());
        int blackColor = (int) (blackOverlayAlpha * 255.0f) << 24;
        vertexConsumer.vertex(matrix, -1.0f, -1.0f, -0.1f).color(blackColor);
        vertexConsumer.vertex(matrix, 1.0f, -1.0f, -0.1f).color(blackColor);
        vertexConsumer.vertex(matrix, 1.0f, 1.0f, -0.1f).color(blackColor);
        vertexConsumer.vertex(matrix, -1.0f, 1.0f, -0.1f).color(blackColor);
        immediate.draw();
    }

    @Shadow
    protected abstract void updateWorldIcon();

    @Inject(
        method = "renderHand",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;",
            shift = At.Shift.BEFORE,
            ordinal = 1
        )
    )
    private void lockVision(
        Camera camera,
        float tickDelta,
        Matrix4f matrix4f,
        CallbackInfo ci,
        @Local @NotNull MatrixStack stack
    ) {
        if (AchieveToDoClient.isAbilityLocked(AbilityType.VISION)) {
            blackOverlayAlpha = 1.0f;
            drawBlackOverlay(stack);
        } else if (blackOverlayAlpha > 0) {
            blackOverlayAlpha = Math.max(0, blackOverlayAlpha - 0.02f * tickDelta);
            if (blackOverlayAlpha == 0) {
                updateWorldIcon();
            } else {
                drawBlackOverlay(stack);
            }
        }
    }

    @Inject(
        method = "updateWorldIcon()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void scheduleWorldIconUpdateUntilVisionAbilityUnlocked(CallbackInfo ci) {
        if (blackOverlayAlpha != 0) {
            ci.cancel();
        }
    }
}
