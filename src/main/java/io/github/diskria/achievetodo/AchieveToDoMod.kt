package io.github.diskria.achievetodo

import io.github.diskria.achievetodo.abilities.AbilityType
import io.github.diskria.achievetodo.abilities.DimensionType
import io.github.diskria.achievetodo.client.AchieveToDoClient
import io.github.diskria.achievetodo.payloads.clientbound.*
import io.github.diskria.achievetodo.payloads.serverbound.DemystifyAbilityPayload
import io.github.diskria.achievetodo.server.AchieveToDoServer
import net.fabricmc.api.ModInitializer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.abs

class AchieveToDoMod : ModInitializer {

    override fun onInitialize() {
        registerPayloads()
        server.init()
    }

    private fun registerPayloads() {
        DemystifyAbilityPayload.register()
        SyncAbilitiesConfigurationPayload.register()
        SyncObtainedAdvancementsCountPayload.register()
        LandmarkTypesUnlockedPayload.register()
        LockedLandmarkResizedPayload.register()
        TrackedAdvancementProgressChangedPayload.register()
        LandmarksLockedStatusChangedPayload.register()
        CheckTargetInLockedLandmarkPayload.register()
    }

    companion object {
        const val NAME: String = "AchieveToDo"
        const val ID: String = "achievetodo"

        val COLORED_NAME: Component by lazy {
            val colorFrom = 0x07C5FA
            val colorTo = 0xFFFF00
            val mid = NAME.lastIndex / 2.0f
            Component.empty().apply {
                NAME.forEachIndexed { index, char ->
                    val factor = 1.0f - abs(index - mid) / mid
                    val color = ARGB.srgbLerp(factor, colorFrom, colorTo)
                    append(Component.literal(char.toString()).withStyle { it.withColor(color) })
                }
            }
        }

        val server = AchieveToDoServer()
        val logger: Logger = LoggerFactory.getLogger(NAME)

        const val USER_AGENT = "diskria-minecraft/achievetodo/3.0.0 (diskria@proton.me)"

        fun getIdentifier(path: String): Identifier = Identifier.fromNamespaceAndPath(ID, path)

        fun isAbilityLocked(player: Player, abilityType: AbilityType): Boolean =
            isAbilityLocked(player, abilityType, false)

        fun isAbilityLocked(player: Player, abilityType: AbilityType, checkOnly: Boolean): Boolean {
            if (player.level().isClientSide) {
                return AchieveToDoClient.isAbilityLocked(abilityType, checkOnly)
            }
            return player is ServerPlayer && server.isAbilityLocked(player, abilityType)
        }

        fun isTargetInLockedLandmark(actor: Player, target: Entity): Boolean =
            isTargetInLockedLandmark(actor, target.level(), target.boundingBox)

//        fun isTargetInLockedLandmark(context: ItemUsageContext): Boolean {
//            return isTargetInLockedLandmark(context.getPlayer(), context.getWorld(), context.getBlockPos())
//        }

        fun isTargetInLockedLandmark(actor: Player, targetWorld: Level, targetBlockPos: BlockPos): Boolean =
            isTargetInLockedLandmark(actor, targetWorld, AABB(targetBlockPos))

        fun isTargetInLockedLandmark(actor: Player, targetWorld: Level, targetAABB: AABB): Boolean {
            val targetDimensionType = DimensionType.findByLevel(targetWorld.dimension()) ?: return false
            if (actor.level().isClientSide) {
                return AchieveToDoClient.isTargetInLockedLandmark(targetDimensionType, targetAABB)
            }
            return actor is ServerPlayer && server.isTargetInLockedLandmark(actor, targetDimensionType, targetAABB)
        }
    }
}
