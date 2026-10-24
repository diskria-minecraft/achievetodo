package io.github.diskria.achievetodo.client.gui;

import io.github.diskria.achievetodo.client.AchieveToDoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public class ErrorScreen extends NoticeWithLinkScreen {

    public ErrorScreen(Screen parent, String messageKey) {
        super(
            () -> Minecraft.getInstance().setScreenAndShow(parent),
            AchieveToDoClient.translate("error.title")
                .formatted(Formatting.RED),
            AchieveToDoClient.translate(messageKey)
        );
    }
}
