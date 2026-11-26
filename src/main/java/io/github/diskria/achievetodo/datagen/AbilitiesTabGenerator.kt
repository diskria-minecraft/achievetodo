package io.github.diskria.achievetodo.datagen

import com.google.common.hash.Hashing
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import io.github.diskria.achievetodo.AchieveToDoMod
import io.github.diskria.achievetodo.abilities.AbilityType
import io.github.diskria.achievetodo.client.gui.DesignCodePalette
import io.github.diskria.achievetodo.extensions.buildTechnicalAdvancement
import io.github.diskria.achievetodo.extensions.withFooterStyle
import io.github.diskria.achievetodo.qol.FlashAdvancementClickEvent
import io.github.diskria.achievetodo.qol.I18N
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.triggers.CriteriaTriggers
import net.minecraft.advancements.triggers.ImpossibleTrigger
import net.minecraft.advancements.triggers.PlayerTrigger
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.network.chat.*
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.util.Util
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class AbilitiesTabGenerator(
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
        val rootAdvancement = buildTechnicalAdvancement(buildAdvancementId("root")) {
            rootDisplay(
                /* icon = */ Items.BARRIER,
                /* title = */ Component.literal(AchieveToDoMod.NAME),
                /* description = */ Component.translatable("achievetodo.description"),
                /* background = */ TextureMapping.getBlockTexture(BACKGROUND).sprite(),
                /* frame = */ AdvancementType.TASK,
                /* showToast = */ false,
                /* announceChat = */ false,
                /* hidden = */ false,
            )
            addCriterion("tick", tickCriterion)
        }
        futures += output.writeText(
            text = rootAdvancement.value().toCompactJson(Advancement.CODEC),
            path = advancementsDir.json(rootAdvancement.id()),
        )

        AbilityType.entries.forEach { ability ->
            val rewardFunctionId = buildAdvancementId(ability.id)
            val advancement = buildTechnicalAdvancement(buildAdvancementId(ability)) {
                parent(rootAdvancement)
                display(
                    /* icon = */ ability.icon ?: error("Unexpected icon for ability '${ability.id}'."),
                    /* title = */ ability.title,
                    /* description = */ ability.description,
                    /* frame = */ AdvancementType.TASK,
                    /* showToast = */ true,
                    /* announceChat = */ false,
                    /* hidden = */ false,
                )
                rewards(AdvancementRewards.Builder.function(rewardFunctionId))
                addCriterion(DEMYSTIFIED_CRITERION, impossibleCriterion)
                addCriterion(UNLOCKED_CRITERION, impossibleCriterion)
            }
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
        val advancementId = buildAdvancementId(ability)
        val flashAdvancement = requireNotNull(FlashAdvancementClickEvent.create(advancementId))
        val color = DesignCodePalette.TEXT_COLOR
        val displayTitle = ability.title.withStyle(color)
        val title = displayTitle.copy().withStyle {
            val tooltip = Component.empty()
                .append(displayTitle)
                .append(CommonComponents.NEW_LINE)
                .append(ability.description.withStyle(color))
                .append(CommonComponents.NEW_LINE)
                .append(I18N.tab(AchieveToDoMod.NAME).withFooterStyle())
                .append(FlashAdvancementClickEvent.clickableFooter)
            val showTooltip = HoverEvent.ShowText(tooltip)
            it.withHoverEvent(showTooltip).withClickEvent(flashAdvancement)
        }
        val atSelf = EntitySelector.COMPILABLE_CODEC.parse(
            JsonOps.INSTANCE, JsonPrimitive(EntitySelectors.SELF)
        ).getOrThrow()
        val message = Component.translatable(
            "${AchieveToDoMod.ID}.ability_unlocked_chat_message",
            Component.selector(atSelf, Optional.empty()),
            ComponentUtils.wrapInSquareBrackets(title).withStyle(color)
        )
        val command = "tellraw ${EntitySelectors.ALL} ${message.toCompactJson(ComponentSerialization.CODEC)}"
        return output.writeText(command, functionsDir.file(rewardFunctionId, "mcfunction"))
    }

    object EntitySelectors {
        const val SELF = "@s"
        const val ALL = "@a"
    }

    companion object {
        const val DEMYSTIFIED_CRITERION: String = "demystified"
        const val UNLOCKED_CRITERION: String = "unlocked"
        const val ABILITY_PATH_PREFIX: String = "ability/"

        val BACKGROUND = Blocks.YELLOW_POPLAR_LEAVES

        fun buildAdvancementId(suffix: String): Identifier = AchieveToDoMod.getIdentifier(ABILITY_PATH_PREFIX + suffix)
        fun buildAdvancementId(ability: AbilityType): Identifier = buildAdvancementId(ability.id)
    }
}

private val tickCriterion = PlayerTrigger.TriggerInstance.tick()
private val impossibleCriterion = CriteriaTriggers.IMPOSSIBLE.createCriterion(ImpossibleTrigger.TriggerInstance())

private val gson = Gson()

context(jsonSerializer: RegistryOps<JsonElement>)
private fun <T> T.toCompactJson(codec: Codec<T>): String =
    gson.toJson(codec.encodeStart(jsonSerializer, this).orThrow.asJsonObject)

private fun CachedOutput.writeText(text: String, path: Path): CompletableFuture<*> =
    CompletableFuture.runAsync({
        val input = text.toByteArray()
        @Suppress("DEPRECATION") val sha1HashFun = Hashing.sha1()
        writeIfNeeded(path, input, sha1HashFun.hashBytes(input))
    }, Util.backgroundExecutor().forName("saveRawString"))
