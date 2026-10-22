package io.github.diskria.achievetodo.injection.mixin.client;

import io.github.diskria.achievetodo.client.ExternalPack;
import io.github.diskria.achievetodo.client.InternalPack;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(PackScreen.class)
public class PackScreenMixin {

    @Redirect(
        method = "updatePackList",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"
        )
    )
    public void hideExternalAndInternalPacks(
        @NotNull Stream<ResourcePackOrganizer.Pack> packs,
        Consumer<ResourcePackOrganizer.Pack> consumer
    ) {
        packs.filter(pack -> {
            String name = pack.getName();
            for (ExternalPack externalPack : ExternalPack.values()) {
                if (name.equals(externalPack.getDatapackName())) {
                    return false;
                }
            }
            for (InternalPack internalPack : InternalPack.values()) {
                if (name.equals(internalPack.getDatapackName())) {
                    return false;
                }
            }
            return true;
        }).forEach(consumer);
    }
}
