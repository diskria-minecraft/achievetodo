package io.github.diskria.achievetodo.injection.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.github.diskria.achievetodo.client.ExternalPack;
import io.github.diskria.achievetodo.client.InternalPack;
import io.github.diskria.achievetodo.client.gui.ExternalPackDownloader;
import io.github.diskria.achievetodo.client.gui.WorldCreationTab;
import io.github.diskria.achievetodo.injection.extension.client.CreateWorldScreenExtension;
import io.github.diskria.achievetodo.injection.extension.client.WorldCreatorExtension;
import io.github.diskria.achievetodo.injection.extension.main.LevelInfoExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.world.level.LevelInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = CreateWorldScreen.class, priority = 500)
public abstract class CreateWorldScreenMixin extends Screen implements CreateWorldScreenExtension {

    @Shadow
    @Final
    WorldCreator worldCreator;
    @Unique
    private boolean isWaitingDatapack;
    @Unique
    private WorldCreationTab worldCreationTab;
    @Shadow
    private @Nullable ResourcePackManager packManager;
    @Shadow
    private @Nullable TabNavigationWidget tabNavigation;

    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }

    @Inject(
        method = "create(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/client/world/GeneratorOptionsHolder;Ljava/nio/file/Path;)Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;",
        at = @At(value = "TAIL")
    )
    private static void parseWorldOptionsOnRecreate(
        MinecraftClient client,
        Screen parent,
        @NotNull LevelInfo levelInfo,
        GeneratorOptionsHolder generatorOptionsHolder,
        Path dataPackTempDir,
        CallbackInfoReturnable<CreateWorldScreen> cir,
        @Local @NotNull CreateWorldScreen createWorldScreen
    ) {
        DataConfiguration dataConfiguration = levelInfo.getDataConfiguration();
        if (dataConfiguration != null) {
            DataPackSettings dataPackSettings = dataConfiguration.dataPacks();
            if (dataPackSettings != null) {
                List<String> enabledPacks = dataPackSettings.getEnabled();
                if (enabledPacks != null) {
                    WorldCreator worldCreator = createWorldScreen.getWorldCreator();
                    if (worldCreator instanceof WorldCreatorExtension worldCreatorExtension) {
                        if (levelInfo instanceof LevelInfoExtension levelInfoExtension) {
                            worldCreatorExtension.achievetodo$setConfigName(
                                levelInfoExtension.achievetodo$getConfigName()
                            );
                        }

                        worldCreatorExtension.achievetodo$setTerralithEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_TERRALITH.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setAmplifiedNetherEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_AMPLIFIED_NETHER.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setNullscapeEnabled(
                            enabledPacks.contains(ExternalPack.BACAP_NULLSCAPE.getDatapackName())
                        );

                        worldCreatorExtension.achievetodo$setItemRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_ITEM.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setExperienceRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_EXPERIENCE.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setTrophyRewardsEnabled(
                            enabledPacks.contains(InternalPack.BACAP_REWARDS_TROPHY.getDatapackName())
                        );
                        worldCreatorExtension.achievetodo$setCooperativeModeEnabled(
                            enabledPacks.contains(InternalPack.BACAP_COOPERATIVE_MODE.getDatapackName())
                        );
                    }
                }
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        if (worldCreationTab != null &&
            tabNavigation != null &&
            tabNavigation.tabManager.getCurrentTab() == worldCreationTab
        ) {
            worldCreationTab.render(context);
        }
    }

    @Override
    public boolean achievetodo$isWaitingDatapack() {
        return isWaitingDatapack;
    }

    @Override
    public void achievetodo$setWaitingDatapack(boolean isWaitingDatapack) {
        this.isWaitingDatapack = isWaitingDatapack;
    }

    @Shadow
    public abstract void createLevel();

    @Shadow
    protected abstract @Nullable Path getOrCreateDataPackTempDir();

    @Shadow
    protected abstract @Nullable Pair<Path, ResourcePackManager> getScannedPack(DataConfiguration settings);

    @Shadow
    protected abstract void applyDataPacks(
        ResourcePackManager dataPackManager,
        boolean warnForExperimentsIfApplicable,
        Consumer<DataConfiguration> consumer
    );

    @ModifyArgs(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TabNavigationWidget$Builder;tabs([Lnet/minecraft/client/gui/tab/Tab;)Lnet/minecraft/client/gui/widget/TabNavigationWidget$Builder;"
        )
    )
    private void addTab(@NotNull Args args) {
        CreateWorldScreen createWorldScreen = (CreateWorldScreen) (Object) this;
        Tab[] originalTabs = args.get(0);
        Tab[] newTabs = new Tab[originalTabs.length + 1];
        if (originalTabs.length >= 0) {
            System.arraycopy(originalTabs, 0, newTabs, 0, originalTabs.length);
        }
        newTabs[originalTabs.length] = worldCreationTab = new WorldCreationTab(createWorldScreen);
        args.set(0, newTabs);
    }

    @Inject(
        method = "createLevel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void prepareDatapacks(CallbackInfo ci) {
        CreateWorldScreen createWorldScreen = (CreateWorldScreen) (Object) this;
        WorldCreatorExtension worldCreatorExtension = (WorldCreatorExtension) worldCreator;
        MinecraftClient client = createWorldScreen.client;
        if (client == null) {
            ci.cancel();
            return;
        }

        boolean isHardcoreEnabled = worldCreator.isHardcore();
        boolean isTerralithEnabled = worldCreatorExtension.achievetodo$isTerralithEnabled();
        boolean isAmplifiedNetherEnabled = worldCreatorExtension.achievetodo$isAmplifiedNetherEnabled();
        boolean isNullscapeEnabled = worldCreatorExtension.achievetodo$isNullscapeEnabled();

        Path globalPacksDirectory = new File(client.runDirectory, "datapacks").toPath();
        List<ExternalPack> requiredPacks = new ArrayList<>();
        requiredPacks.add(ExternalPack.BACAP);
        if (isHardcoreEnabled) {
            requiredPacks.add(ExternalPack.BACAP_HARDCORE);
        }
        if (isTerralithEnabled) {
            requiredPacks.add(ExternalPack.TERRALITH);
            requiredPacks.add(ExternalPack.BACAP_TERRALITH);
        }
        if (isAmplifiedNetherEnabled) {
            requiredPacks.add(ExternalPack.AMPLIFIED_NETHER);
            requiredPacks.add(ExternalPack.BACAP_AMPLIFIED_NETHER);
        }
        if (isNullscapeEnabled) {
            requiredPacks.add(ExternalPack.NULLSCAPE);
            requiredPacks.add(ExternalPack.BACAP_NULLSCAPE);
        }
        for (ExternalPack requiredPack : requiredPacks) {
            if (Files.exists(globalPacksDirectory.resolve(requiredPack.getFileName()))) {
                continue;
            }
            client.setScreen(new ExternalPackDownloader(createWorldScreen, requiredPack, isFileDownloaded -> {
                if (isFileDownloaded) {
                    createLevel();
                }
            }, false));
            ci.cancel();
            return;
        }

        if (!isWaitingDatapack) {
            Path worldPacksTempDirectory = getOrCreateDataPackTempDir();
            if (worldPacksTempDirectory == null) {
                ci.cancel();
                return;
            }
            try {
                for (ExternalPack pack : requiredPacks) {
                    Path globalPack = globalPacksDirectory.resolve(pack.getFileName());
                    Path worldPack = worldPacksTempDirectory.resolve(globalPack.getFileName());
                    Files.copy(globalPack, worldPack, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
            }

            if (packManager != null) {
                packManager.scanPacks();
            }
            getScannedPack(worldCreator.getGeneratorOptionsHolder().dataConfiguration());
            if (packManager != null) {
                for (ExternalPack externalPack : ExternalPack.values()) {
                    packManager.disable(externalPack.getDatapackName());
                }
                for (InternalPack internalPack : InternalPack.values()) {
                    packManager.disable(internalPack.getDatapackName());
                }

                packManager.enable(ExternalPack.BACAP.getDatapackName());
                packManager.enable(InternalPack.BACAP_OVERRIDE.getDatapackName());
                if (isHardcoreEnabled) {
                    packManager.enable(ExternalPack.BACAP_HARDCORE.getDatapackName());
                    packManager.enable(InternalPack.BACAP_HARDCORE_OVERRIDE.getDatapackName());
                }
                if (isTerralithEnabled) {
                    packManager.enable(ExternalPack.TERRALITH.getDatapackName());
                    packManager.enable(ExternalPack.BACAP_TERRALITH.getDatapackName());
                    packManager.enable(InternalPack.BACAP_TERRALITH_OVERRIDE.getDatapackName());
                }
                if (isAmplifiedNetherEnabled) {
                    packManager.enable(ExternalPack.AMPLIFIED_NETHER.getDatapackName());
                    packManager.enable(ExternalPack.BACAP_AMPLIFIED_NETHER.getDatapackName());
                    packManager.enable(InternalPack.BACAP_AMPLIFIED_NETHER_OVERRIDE.getDatapackName());
                }
                if (isNullscapeEnabled) {
                    packManager.enable(ExternalPack.NULLSCAPE.getDatapackName());
                    packManager.enable(ExternalPack.BACAP_NULLSCAPE.getDatapackName());
                    packManager.enable(InternalPack.BACAP_NULLSCAPE_OVERRIDE.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isItemRewardsEnabled()) {
                    packManager.enable(InternalPack.BACAP_REWARDS_ITEM.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isExperienceRewardsEnabled()) {
                    packManager.enable(InternalPack.BACAP_REWARDS_EXPERIENCE.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isTrophyRewardsEnabled()) {
                    packManager.enable(InternalPack.BACAP_REWARDS_TROPHY.getDatapackName());
                }
                if (worldCreatorExtension.achievetodo$isCooperativeModeEnabled()) {
                    packManager.enable(InternalPack.BACAP_COOPERATIVE_MODE.getDatapackName());
                }

                isWaitingDatapack = true;
                applyDataPacks(packManager, false, (dataConfiguration) -> client.setScreen(createWorldScreen));

                ci.cancel();
            }
        }
    }

    @ModifyReturnValue(
        method = "createLevelInfo",
        at = @At(
            value = "RETURN",
            ordinal = 1
        )
    )
    private LevelInfo setConfigName(LevelInfo levelInfo) {
        if (worldCreator instanceof WorldCreatorExtension worldCreatorExtension &&
            levelInfo instanceof LevelInfoExtension levelInfoExtension
        ) {
            levelInfoExtension.achievetodo$setConfigName(worldCreatorExtension.achievetodo$getConfigName());
        }
        return levelInfo;
    }
}
