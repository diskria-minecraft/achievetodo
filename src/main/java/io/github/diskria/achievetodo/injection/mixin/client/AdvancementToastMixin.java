package io.github.diskria.achievetodo.injection.mixin.client;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Unique
    private static final Identifier ABILITY_UNLOCKED_NOTIFICATION_BACKGROUND_TEXTURE =
        AchieveToDoMod.getIdentifier("ability_unlocked_notification_background");

    @Unique
    private static final int ABILITY_UNLOCKED_NOTIFICATION_TITLE_COLOR = Colors.BLACK;

    @Unique
    private static final int ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR = 0x725e3c;

    @Unique
    private @Nullable AbilityType abilityType;

    @Inject(
        method = "<init>",
        at = @At(value = "TAIL")
    )
    private void findAbility(AdvancementEntry advancement, CallbackInfo ci) {
        abilityType = AbilityType.findByAdvancement(advancement);
    }

    @ModifyArg(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V",
            ordinal = 0
        ),
        index = 1
    )
    private Identifier setCustomBackgroundTextureForAbilityUnlockedNotification(Identifier original) {
        return abilityType != null ? ABILITY_UNLOCKED_NOTIFICATION_BACKGROUND_TEXTURE : original;
    }

    @ModifyArg(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"
        ),
        index = 1
    )
    private Text setCustomTitleForAbilityUnlockedNotification(Text original) {
        return abilityType != null ? abilityType.getUnlockToastType().getToastTitle() : original;
    }

    @ModifyVariable(
        method = "draw",
        at = @At(value = "STORE"),
        ordinal = 0
    )
    private int setCustomTitleColorForAbilityUnlockedNotification(int original) {
        return abilityType != null ? ABILITY_UNLOCKED_NOTIFICATION_TITLE_COLOR : original;
    }

    @ModifyArg(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I",
            ordinal = 0
        ),
        index = 4
    )
    private int setCustomSubtitleColorForAbilityUnlockedNotification(int original) {
        return abilityType != null ? ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR : original;
    }

    @ModifyConstant(
        method = "draw",
        constant = @Constant(intValue = 16777215)
    )
    private int setCustomTwoLineSubtitleColorForAbilityUnlockedNotification(int original) {
        return abilityType != null ? ABILITY_UNLOCKED_NOTIFICATION_SUBTITLE_COLOR : original;
    }

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancement/AdvancementDisplay;getFrame()Lnet/minecraft/advancement/AdvancementFrame;",
            ordinal = 0
        )
    )
    private void playAbilityUnlockedSound(ToastManager manager, long time, CallbackInfo ci) {
        if (abilityType != null) {
            manager.getClient().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 0.2f)
            );
        }
    }
}
