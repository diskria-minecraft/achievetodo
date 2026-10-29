//package io.github.diskria.achievetodo.client.gui;
//
//import com.mojang.blaze3d.Blaze3D;
//import io.github.diskria.achievetodo.client.AchieveToDoClient;
//import io.github.diskria.achievetodo.client.ExternalPack;
//import io.github.diskria.achievetodo.client.Utils;
//import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
//import net.fabricmc.api.EnvType;
//import net.fabricmc.api.Environment;
//import net.minecraft.ChatFormatting;
//import net.minecraft.client.gui.components.Button;
//import net.minecraft.client.gui.components.Tooltip;
//import net.minecraft.client.gui.layouts.LinearLayout;
//import net.minecraft.client.gui.screens.ConfirmScreen;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//import net.minecraft.util.Util;
//import org.jetbrains.annotations.NotNull;
//import org.lwjgl.PointerBuffer;
//import org.lwjgl.system.MemoryStack;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URI;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.Enumeration;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;
//
//@Environment(EnvType.CLIENT)
//public class ExternalPackDownloader extends ConfirmScreen {
//
//    private static final int BUTTON_WIDTH = 100;
//    private static final int BUTTON_HEIGHT = 20;
//    private static final int BUTTON_MARGIN = 5;
//    private static final AtomicBoolean isDownloadingCanceled = new AtomicBoolean(true);
//    private final Screen parent;
//    private final ExternalPack externalPack;
//    private final BooleanConsumer exitCallback;
//    private final boolean inGameDownloadSupported;
//    private boolean isFileDownloaded;
//    private Button downloadButton;
//    private Button backButton;
//
//    public ExternalPackDownloader(
//        Screen parent,
//        @NotNull ExternalPack pack,
//        BooleanConsumer exitCallback,
//        boolean isOutdatedVersion
//    ) {
//        super(
//            null,
//            AchieveToDoClient.Companion.translate("downloader.title_prefix")
//                .append(
//                    Component.literal(pack.getTitle())
//                        .withStyle(pack.getColor(), ChatFormatting.ITALIC)
//                ),
//            AchieveToDoClient.Companion.translate(
//                    isOutdatedVersion ? "downloader.reason.outdated" : pack.getReasonKey()
//                )
//                .append("\n")
//                .append("\n")
//                .append("\n")
//                .append("\n")
//                .append("\n")
//                .append(
//                    AchieveToDoClient.Companion.translate(
//                        pack.getInGameDownloadSupported() ? "downloader.automatically_info" : "downloader.manually_info"
//                    ).withStyle(DesignCodePalette.INSTANCE.getTEXT_COLOR())
//                )
//        );
//        this.parent = parent;
//        this.externalPack = pack;
//        this.exitCallback = exitCallback;
//        inGameDownloadSupported = pack.getInGameDownloadSupported();
//    }
//
//
//    @Override
//    public void onClose() {
//        super.onClose();
//        minecraft.setScreenAndShow(parent);
//        exitCallback.accept(isFileDownloaded);
//    }
//
//    @Override
//    protected void addButtons(LinearLayout buttonLayout) {
//        int selectFileButtonX = (width - BUTTON_WIDTH) / 2;
//
//        downloadButton = addWidget(
//            Button.builder(
//                    AchieveToDoClient.Companion.translate("downloader.download"),
//                    button -> {
//                        if (inGameDownloadSupported) {
//                            backButton.setMessage(ScreenTexts.CANCEL);
//                            button.active = false;
//                            startDownload();
//                        } else {
//                            Util.getPlatform().open(externalPack.downloadUrl);
//                        }
//                    }
//                )
//                .tooltip(inGameDownloadSupported ? null :
//                    Tooltip.create(AchieveToDoClient.Companion.translate("downloader.download.tooltip"))
//                )
//                .dimensions(
//                    selectFileButtonX - BUTTON_MARGIN - BUTTON_WIDTH,
//                    y,
//                    inGameDownloadSupported ? BUTTON_WIDTH * 2 + BUTTON_MARGIN : BUTTON_WIDTH,
//                    BUTTON_HEIGHT
//                )
//                .build()
//        );
//
//        if (!inGameDownloadSupported) {
//            addWidget(
//                Button.builder(
//                        AchieveToDoClient.Companion.translate("downloader.select_file"),
//                        button -> {
//                            try (MemoryStack stack = MemoryStack.stackPush()) {
//                                PointerBuffer filters = stack.mallocPointer(1);
//                                filters.put(0, stack.UTF8("*.zip"));
//
//                                String selectedFilePath = TinyFileDialogs.tinyfd_openFileDialog(
//                                    AchieveToDoClient.Companion.translate("downloader.select_file").getString(),
//                                    System.getProperty("user.home"),
//                                    filters,
//                                    null,
//                                    false
//                                );
//                                if (selectedFilePath != null) {
//                                    handleDatapackFile(Paths.get(selectedFilePath));
//                                }
//                            }
//                        }
//                    )
//                    .dimensions(selectFileButtonX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
//                    .build()
//            );
//        }
//
//        backButton = addDrawableChild(
//            ButtonWidget.builder(
//                    ScreenTexts.BACK,
//                    button -> {
//                        if (!isDownloadingCanceled.get()) {
//                            isDownloadingCanceled.set(true);
//                        } else {
//                            close();
//                        }
//                    }
//                )
//                .dimensions(selectFileButtonX + BUTTON_WIDTH + BUTTON_MARGIN, y, BUTTON_WIDTH, BUTTON_HEIGHT)
//                .build()
//        );
//
//        addDrawableChild(
//            ButtonWidget.builder(
//                    AchieveToDoClient.Companion.translate("downloader.learn_more"),
//                    button -> Blaze3D.openUri(URI.create(externalPack.getPageUrl()))
//                )
//                .tooltip(Tooltip.create(AchieveToDoClient.Companion.translate("downloader.learn_more.tooltip")))
//                .dimensions(
//                    selectFileButtonX + BUTTON_WIDTH + BUTTON_MARGIN,
//                    y + BUTTON_HEIGHT + BUTTON_MARGIN,
//                    BUTTON_WIDTH,
//                    BUTTON_HEIGHT
//                )
//                .build()
//        );
//    }
//
//    @Override
//    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//        if (isDownloadingCanceled.get() && keyCode == 256) {
//            close();
//            return true;
//        }
//        return super.keyPressed(keyCode, scanCode, modifiers);
//    }
//
//    @Override
//    public void onFilesDropped(List<Path> paths) {
//        super.onFilesDropped(paths);
//        if (paths == null || paths.size() != 1) {
//            return;
//        }
//        handleDatapackFile(paths.getFirst());
//    }
//
//    private void handleDatapackFile(Path path) {
//        if (client == null) {
//            return;
//        }
//        boolean isWrapper;
//        try {
//            String sha1 = Utils.calculateSHA1(path);
//            if (sha1 == null) {
//                return;
//            }
//            isWrapper = sha1.equals(externalPack.wrapperSha1);
//            if (!isWrapper && !sha1.equalsIgnoreCase(externalPack.sha1)) {
//                client.setScreen(new ErrorScreen(this, "error.wrong_datapack_file"));
//                return;
//            }
//        } catch (Exception e) {
//            return;
//        }
//        Path globalPacksDirectory = new File(client.runDirectory, "datapacks").toPath();
//        try {
//            if (Files.notExists(globalPacksDirectory)) {
//                Files.createDirectory(globalPacksDirectory);
//            }
//            if (isWrapper) {
//                Path extractedArchive = unzip(path, globalPacksDirectory);
//                Files.move(extractedArchive, globalPacksDirectory.resolve(externalPack.getFileName()));
//            } else {
//                Files.copy(path, globalPacksDirectory.resolve(externalPack.getFileName()));
//            }
//        } catch (IOException e) {
//            return;
//        }
//        isFileDownloaded = true;
//        close();
//    }
//
//    private Path unzip(Path source, Path destination) throws IOException {
//        Path firstExtractedFile = null;
//        try (ZipFile zipFile = new ZipFile(source.toFile())) {
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            while (entries.hasMoreElements()) {
//                ZipEntry entry = entries.nextElement();
//                Path outputPath = destination.resolve(entry.getName());
//                if (entry.isDirectory()) {
//                    Files.createDirectories(outputPath);
//                } else {
//                    if (firstExtractedFile == null) {
//                        firstExtractedFile = outputPath;
//                    }
//                    try (InputStream in = zipFile.getInputStream(entry);
//                         OutputStream out = Files.newOutputStream(outputPath)) {
//                        byte[] buffer = new byte[1024];
//                        int len;
//                        while ((len = in.read(buffer)) != -1) {
//                            out.write(buffer, 0, len);
//                        }
//                    }
//                }
//            }
//        }
//        return firstExtractedFile;
//    }
//
//    private void startDownload() {
//        if (client == null) {
//            return;
//        }
//        isDownloadingCanceled.set(false);
//        String url = externalPack.downloadUrl;
//        Path downloadTempDirectory = new File(client.runDirectory, ".datapacks_temp").toPath();
//        if (Files.notExists(downloadTempDirectory)) {
//            try {
//                Files.createDirectory(downloadTempDirectory);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        Path tempFile = downloadTempDirectory.resolve(String.valueOf(UUID.randomUUID()));
//        CompletableFuture.runAsync(() -> {
//            HttpURLConnection connection = null;
//
//            try {
//                URI uri = URI.create(url);
//                connection = (HttpURLConnection) uri.toURL().openConnection();
//                connection.setDoInput(true);
//                connection.setDoOutput(false);
//                connection.connect();
//
//                int responseCode = connection.getResponseCode();
//                if (responseCode / 100 != 2) {
//                    throw new IOException("Failed to open " + url + ", HTTP error code: " + responseCode);
//                }
//
//                int totalBytes = connection.getContentLength();
//                if (totalBytes == -1) {
//                    throw new IOException("Unable to determine file size for " + url);
//                }
//
//                try (InputStream in = connection.getInputStream();
//                     OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
//
//                    byte[] buffer = new byte[8192];
//                    int bytesRead;
//                    int downloadedBytes = 0;
//
//                    while ((bytesRead = in.read(buffer)) != -1) {
//                        if (isDownloadingCanceled.get()) {
//                            hideDownloadingUI();
//                            break;
//                        }
//                        out.write(buffer, 0, bytesRead);
//                        downloadedBytes += bytesRead;
//
//                        int progress = (downloadedBytes * 100) / totalBytes;
//                        client.execute(() -> downloadButton.setMessage(
//                            Component.translatable("mco.download.downloading").append(": " + progress + "%"))
//                        );
//                    }
//
//                    if (isDownloadingCanceled.get()) {
//                        Files.deleteIfExists(tempFile);
//                        hideDownloadingUI();
//                    }
//                    client.execute(() -> {
//                        handleDatapackFile(tempFile);
//                        try {
//                            Files.deleteIfExists(tempFile);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
//                } catch (IOException e) {
//                    Files.deleteIfExists(tempFile);
//                    throw e;
//                }
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
//            } finally {
//                if (connection != null) {
//                    connection.disconnect();
//                }
//            }
//        });
//    }
//
//    private void hideDownloadingUI() {
//        backButton.setMessage(ScreenTexts.BACK);
//        downloadButton.active = true;
//    }
//}
