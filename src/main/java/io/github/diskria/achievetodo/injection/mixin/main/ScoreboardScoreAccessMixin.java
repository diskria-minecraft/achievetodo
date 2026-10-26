package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.qol.TrackedAdvancement;
import io.github.diskria.achievetodo.server.AdvancementsMode;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(targets = "net/minecraft/world/scores/Scoreboard$1")
public class ScoreboardScoreAccessMixin {

    @WrapOperation(
        method = "sendScoreToPlayers",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/scores/Scoreboard;onScoreChanged(Lnet/minecraft/world/scores/ScoreHolder;Lnet/minecraft/world/scores/Objective;Lnet/minecraft/world/scores/Score;)V"
        )
    )
    private void trackScoreChanges(Scoreboard scoreboard, ScoreHolder owner, Objective objective, Score score, Operation<Void> original) {
        original.call(scoreboard, owner, objective, score);
        if (scoreboard instanceof ServerScoreboard serverScoreboard) {
            String objectiveName = objective.getName();
            AdvancementsMode advancementsMode = AchieveToDoMod.getServer().currentAdvancementsMode;
            if (advancementsMode != null && advancementsMode == AdvancementsMode.findByObjectiveName(objectiveName)) {
                if (advancementsMode.isTeamsMode()) {
                    Team team = scoreboard.getPlayersTeam(owner.getScoreboardName());
                    if (team != null) {
                        PlayerList playerManager = serverScoreboard.server.getPlayerList();
                        for (String playerName : team.getPlayers()) {
                            ServerPlayer serverPlayer = playerManager.getPlayer(playerName);
                            if (serverPlayer != null) {
                                AchieveToDoMod.getServer().setObtainedCount(serverPlayer, score.value());
                            }
                        }
                    }
                } else if (owner instanceof ServerPlayer serverPlayer) {
                    AchieveToDoMod.getServer().setObtainedCount(serverPlayer, score.value());
                }
            } else if (owner instanceof ServerPlayer serverPlayer) {
                Set<TrackedAdvancement.Objective> progressTypes = TrackedAdvancement.Objective.Companion.fromObjective(objectiveName);
                if (progressTypes != null) {
                    for (TrackedAdvancement.Objective progressType : progressTypes) {
                        AchieveToDoMod.getServer().notifyTrackedAdvancementProgress(
                            serverPlayer,
                            progressType,
                            progressType.getResolve().invoke(scoreboard, owner, score.value())
                        );
                    }
                }
            }
        }
    }
}
