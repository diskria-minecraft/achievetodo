package io.github.diskria.achievetodo.injection.mixin.client;

import io.github.diskria.achievetodo.client.ExternalPack;
import io.github.diskria.achievetodo.client.Utils;
import io.github.diskria.achievetodo.client.gui.ErrorScreen;
import io.github.diskria.achievetodo.client.gui.ExternalPackDownloader;
import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListWidgetMixin {

    @Shadow
    @Final
    LevelSummary level;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private SelectWorldScreen screen;

    @Unique
    private void showUnknownError() {
        client.setScreen(new ErrorScreen(screen, "error.unknown"));
    }

    @Unique
    private void showIntegrityCheckFailed() {
        client.setScreen(new ErrorScreen(screen, "error.integrity_check_failed"));
    }

    @Shadow
    public abstract void play();

    @Inject(
        method = "play",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;createIntegratedServerLoader()Lnet/minecraft/server/integrated/IntegratedServerLoader;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void checkPacks(@NotNull CallbackInfo ci) {
        Path worldPacksDirectory;
        try (LevelStorage.Session session = client.getLevelStorage().createSession(level.getName())) {
            worldPacksDirectory = session.getDirectory(WorldSavePath.DATAPACKS);
        } catch (Exception e) {
            showUnknownError();
            ci.cancel();
            return;
        }
        if (worldPacksDirectory == null || Files.notExists(worldPacksDirectory)) {
            showIntegrityCheckFailed();
            ci.cancel();
            return;
        }
        List<String> worldPackFileNames;
        try (Stream<Path> stream = Files.list(worldPacksDirectory)) {
            worldPackFileNames = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(Constants.FileExtension.ZIP))
                .map(path -> path.getFileName().toString())
                .toList();
        } catch (IOException e) {
            showUnknownError();
            ci.cancel();
            return;
        }
        if (worldPackFileNames.isEmpty()) {
            showIntegrityCheckFailed();
            ci.cancel();
            return;
        }
        List<String> installedPackFileNames = new ArrayList<>(worldPackFileNames);
        List<String> allPackFileNames = Arrays.stream(ExternalPack.values()).map(ExternalPack::getFileName).toList();
        installedPackFileNames.retainAll(allPackFileNames);
        List<ExternalPack> externalPacksToCheck = installedPackFileNames.stream()
            .map(ExternalPack::mapFromFileName)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(Enum::ordinal))
            .toList();
        if (externalPacksToCheck.isEmpty()) {
            showIntegrityCheckFailed();
            ci.cancel();
            return;
        }
        Path globalPacksDirectory = new File(client.runDirectory, "datapacks").toPath();
        if (Files.notExists(globalPacksDirectory)) {
            try {
                Files.createDirectory(globalPacksDirectory);
            } catch (IOException e) {
                showUnknownError();
                ci.cancel();
                return;
            }
        }
        for (ExternalPack externalPack : externalPacksToCheck) {
            Path worldPack = worldPacksDirectory.resolve(externalPack.getFileName());
            if (Utils.calculateSHA1(worldPack).equals(externalPack.getSha1())) {
                continue;
            }
            Path globalPack = globalPacksDirectory.resolve(externalPack.getFileName());
            if (Files.exists(globalPack) && Utils.calculateSHA1(globalPack).equals(externalPack.getSha1())) {
                try {
                    Files.copy(globalPack, worldPack, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    showUnknownError();
                    ci.cancel();
                    return;
                }
                play();
                ci.cancel();
                continue;
            }
            client.setScreen(new ExternalPackDownloader(screen, externalPack, isFileDownloaded -> {
                if (isFileDownloaded) {
                    try {
                        Files.copy(globalPack, worldPack, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        showUnknownError();
                    }
                    play();
                }
            }, true));
            ci.cancel();
        }
    }
}
