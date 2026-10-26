package io.github.diskria.achievetodo.injection.mixin.main;

import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.qol.TrackedAdvancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(StatsCounter.class)
public class StatHandlerMixin {

    @Inject(
        method = "setValue",
        at = @At("RETURN")
    )
    private void onStatValueChanged(Player player, Stat<?> stat, int count, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<TrackedAdvancement.Statistics<?>> trackedStats = TrackedAdvancement.Statistics.Companion.fromStat(stat);
            if (trackedStats != null) {
                for (TrackedAdvancement.Statistics<?> trackedStat : trackedStats) {
                    AchieveToDoMod.getServer().notifyTrackedAdvancementProgress(serverPlayer, trackedStat, count);
                }
            }
        }
    }
}
