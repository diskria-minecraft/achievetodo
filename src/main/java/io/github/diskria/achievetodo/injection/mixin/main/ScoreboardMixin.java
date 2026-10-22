package io.github.diskria.achievetodo.injection.mixin.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.server.AdvancementsMode;
import io.github.diskria.achievetodo.tracking.TrackedScoreType;
import net.minecraft.scoreboard.*;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {

    @Mixin(targets = "net/minecraft/scoreboard/Scoreboard$1")
    public static class ScoreMixin {

        @WrapOperation(
            method = "update",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/scoreboard/Scoreboard;updateScore(Lnet/minecraft/scoreboard/ScoreHolder;Lnet/minecraft/scoreboard/ScoreboardObjective;Lnet/minecraft/scoreboard/ScoreboardScore;)V"
            )
        )
        private void trackScoreChanges(
            Scoreboard scoreboard,
            @NotNull ScoreHolder scoreHolder,
            @NotNull ScoreboardObjective objective,
            ScoreboardScore scoreboardScore,
            @NotNull Operation<Void> original
        ) {
            original.call(scoreboard, scoreHolder, objective, scoreboardScore);

            if (scoreboard instanceof ServerScoreboard serverScoreboard) {
                int score = scoreboardScore.getScore();
                String objectiveName = objective.getName();
                AdvancementsMode advancementsMode = AchieveToDoMod.getServer().currentAdvancementsMode;
                if (advancementsMode != null &&
                    advancementsMode == AdvancementsMode.findByObjectiveName(objectiveName)
                ) {
                    if (advancementsMode.isTeamsMode()) {
                        Team team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());
                        if (team != null) {
                            PlayerManager playerManager = serverScoreboard.server.getPlayerManager();
                            for (String playerName : team.getPlayerList()) {
                                ServerPlayerEntity serverPlayer = playerManager.getPlayer(playerName);
                                if (serverPlayer != null) {
                                    AchieveToDoMod.getServer().setObtainedCount(serverPlayer, score);
                                }
                            }
                        }
                    } else if (scoreHolder instanceof ServerPlayerEntity serverPlayer) {
                        AchieveToDoMod.getServer().setObtainedCount(serverPlayer, score);
                    }
                } else if (scoreHolder instanceof ServerPlayerEntity serverPlayer) {
                    Set<TrackedScoreType> progressTypes = TrackedScoreType.findByObjectiveName(objectiveName);
                    if (progressTypes != null) {
                        for (TrackedScoreType progressType : progressTypes) {
                            AchieveToDoMod.getServer().setScore(
                                serverPlayer,
                                progressType,
                                progressType.fixScore(scoreboard, scoreHolder, score)
                            );
                        }
                    }
                }
            }
        }
    }
}
