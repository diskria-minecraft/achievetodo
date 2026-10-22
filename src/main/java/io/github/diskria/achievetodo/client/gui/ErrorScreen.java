package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class ErrorScreen extends NoticeScreen {

    public ErrorScreen(Screen parent, String messageKey) {
        super(
            () -> MinecraftClient.getInstance().setScreen(parent),
            AchieveToDoClient.translate("error.title")
                .formatted(Formatting.RED),
            AchieveToDoClient.translate(messageKey)
        );
    }
}
