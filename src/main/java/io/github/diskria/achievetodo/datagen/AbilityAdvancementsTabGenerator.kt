package io.github.diskria.achievetodo.datagen

import com.google.common.hash.HashCode
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.ability.AbilityType
import io.github.diskria.achievetodo.client.AchieveToDoClient
import io.github.diskria.achievetodo.client.gui.DesignCodePalette
import io.github.diskria.achievetodo.etxensions.calcSha1
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.ChatFormatting
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.triggers.CriteriaTriggers
import net.minecraft.advancements.triggers.ImpossibleTrigger
import net.minecraft.advancements.triggers.PlayerTrigger
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.util.Util
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class AbilityAdvancementsTabGenerator(
    output: FabricPackOutput,
    private val registryLookup: CompletableFuture<HolderLookup.Provider>,
) : DataProvider {

    private val advancementsDir = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT)
    private val functionsDir = output.createPathProvider(PackOutput.Target.DATA_PACK, "function")

    override fun getName(): String = javaClass.simpleName

    override fun run(output: CachedOutput): CompletableFuture<*> =
        registryLookup.thenCompose { lookup ->
            val jsonSerializer = lookup.createSerializationContext(JsonOps.INSTANCE)
            val futures = context(output, jsonSerializer) { generateAdvancements() }
            CompletableFuture.allOf(*futures.toTypedArray())
        }

    context(output: CachedOutput, jsonSerializer: RegistryOps<JsonElement>)
    private fun generateAdvancements(): List<CompletableFuture<*>> {
        val futures = mutableListOf<CompletableFuture<*>>()
        val backgroundPath = BuiltInRegistries.BLOCK.getKey(TAB_BACKGROUND).path

        val rootAdvancement = Advancement.Builder
            .recipeAdvancement()
            .rootDisplay(
                /* icon = */ Items.BARRIER,
                /* title = */ Component.literal(AchieveToDoMod.NAME),
                /* description = */ AchieveToDoClient.translate("description"),
                /* background = */ Identifier.withDefaultNamespace("textures/block/$backgroundPath.png"),
                /* frame = */ AdvancementType.TASK,
                /* showToast = */ false,
                /* announceChat = */ false,
                /* hidden = */ false,
            )
            .addCriterion("tick", tickCriterion)
            .build(buildAdvancementId("root"))

        futures += output.writeText(
            text = rootAdvancement.value().toCompactJson(Advancement.CODEC),
            path = advancementsDir.json(rootAdvancement.id()),
        )

        AbilityType.entries.forEach { ability ->
            val rewardFunctionId = buildAdvancementId(ability.id)

            val advancement = Advancement.Builder
                .recipeAdvancement()
                .parent(rootAdvancement)
                .display(
                    /* icon = */ ability.icon ?: error("Unexpected icon for ability '${ability.id}'."),
                    /* title = */ ability.title,
                    /* description = */ ability.description,
                    /* frame = */ AdvancementType.TASK,
                    /* showToast = */ true,
                    /* announceChat = */ false,
                    /* hidden = */ false,
                )
                .rewards(AdvancementRewards.Builder.function(rewardFunctionId))
                .addCriterion(DEMYSTIFIED_CRITERION, impossibleCriterion)
                .addCriterion(UNLOCKED_CRITERION, impossibleCriterion)
                .build(buildAdvancementId(ability))

            futures += output.writeText(
                text = advancement.value().toCompactJson(Advancement.CODEC),
                path = advancementsDir.json(advancement.id()),
            )
            futures += generateRewardFunction(ability, rewardFunctionId)
        }

        return futures
    }

    context(output: CachedOutput, jsonSerializer: RegistryOps<JsonElement>)
    private fun generateRewardFunction(ability: AbilityType, rewardFunctionId: Identifier): CompletableFuture<*> {
        val advancementId = buildAdvancementId(ability).toString()
        val modId = AchieveToDoMod.ID
        val modName = AchieveToDoMod.NAME
        val color = DesignCodePalette.TEXT_COLOR
        val hoverEvent = HoverEvent.ShowText(
            ability.title
                .withStyle { it.withColor(color) }
                .append(Component.literal("\n"))
                .append(ability.description.withStyle { it.withColor(color) })
                .append(Component.literal("\n"))
                .append(
                    Component.translatable(
                        "%s tab",
                        Component.literal(modName)
                    ).withStyle { it.withColor(ChatFormatting.GRAY).withItalic(true) }
                )
        )
        val clickEvent = ClickEvent.RunCommand("advancementssearch highlight $advancementId obtained_status")
        val abilityComponent = ability.title
            .withStyle { it.withColor(color).withHoverEvent(hoverEvent).withClickEvent(clickEvent) }

        val atSelf = EntitySelector.COMPILABLE_CODEC.parse(JsonOps.INSTANCE, JsonPrimitive(Selectors.SELF)).getOrThrow()
        val message = Component.translatable(
            "$modId.ability_unlocked_chat_message",
            Component.selector(atSelf, Optional.empty()),
            Component.literal("[").withStyle { it.withColor(color) },
            abilityComponent,
            Component.literal("]").withStyle { it.withColor(color) },
        )
        val command = "tellraw ${Selectors.EVERYONE} ${message.toCompactJson(ComponentSerialization.CODEC)}"
        return output.writeText(command, functionsDir.file(rewardFunctionId, "mcfunction"))
    }

    object Selectors {
        const val SELF = "@s"
        const val EVERYONE = "@a"
    }

    companion object {
        const val DEMYSTIFIED_CRITERION: String = "demystified"
        const val UNLOCKED_CRITERION: String = "unlocked"
        const val ABILITY_PATH_PREFIX: String = "abilities/"

        val TAB_BACKGROUND: Block = Blocks.PALE_MOSS_BLOCK

        fun buildAdvancementId(suffix: String): Identifier =
            AchieveToDoMod.getIdentifier(ABILITY_PATH_PREFIX + suffix)

        fun buildAdvancementId(ability: AbilityType): Identifier =
            buildAdvancementId(ability.id)
    }
}

private val tickCriterion = PlayerTrigger.TriggerInstance.tick()
private val impossibleCriterion = CriteriaTriggers.IMPOSSIBLE.createCriterion(ImpossibleTrigger.TriggerInstance())

private val gson = Gson()

context(jsonSerializer: RegistryOps<JsonElement>)
fun <T> T.toCompactJson(codec: Codec<T>): String =
    gson.toJson(codec.encodeStart(jsonSerializer, this).orThrow.asJsonObject)

fun CachedOutput.writeText(text: String, path: Path): CompletableFuture<*> =
    CompletableFuture.runAsync({
        val input = text.toByteArray()
        writeIfNeeded(path, input, HashCode.fromBytes(input.calcSha1()))
    }, Util.backgroundExecutor().forName("saveRawString"))
