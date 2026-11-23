package io.github.diskria.achievetodo.qol

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.injector.ModifyReturnValue
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.sugar.Local
import io.github.diskria.achievetodo.extensions.Op
import io.github.diskria.achievetodo.extensions.invoke
import io.github.diskria.achievetodo.toBrandGradientComponent
import io.github.diskria.lapis.annotations.Extension
import io.github.diskria.lapis.annotations.KMixin
import io.github.diskria.lapis.annotations.KShadow
import io.github.diskria.lapis.annotations.Side
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.CriterionProgress
import net.minecraft.client.gui.screens.DeathScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.ReadOnlyScoreInfo
import org.spongepowered.asm.mixin.injection.At
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE

object BacapScoreFormat : NumberFormat {

    override fun format(number: Int): MutableComponent {
        val text = number.toString()
        return text.toBrandGradientComponent { index ->
            val rightOffset = text.lastIndex - index
            (1.0f - rightOffset / 3.0f).coerceIn(0.0f, 1.0f)
        }
    }

    override fun type() = throw UnsupportedOperationException("Client-side only number format")
}

@KMixin<Objective>(Side.Client)
abstract class BacapScoreboardObjectiveCustomizer {
    @ModifyReturnValue(method = ["getDisplayName"], at = [At("RETURN")])
    fun Objective.customizeDisplayName(original: Component): Component {
        val mode = BacapScoreboardMode.ALL[name] ?: return original
        return mode.title.withStyle { original.style.withHoverEvent(HoverEvent.ShowText(mode.hoverText)) }
    }

    @ModifyReturnValue(method = ["numberFormatOrDefault"], at = [At("RETURN")])
    fun Objective.customizeNumberFormat(original: NumberFormat) =
        if (name in BacapScoreboardMode.ALL) BacapScoreFormat else original
}

@KMixin<Entity>(Side.Client)
abstract class BacapScoreboardEntityBelowNameCustomizer {

    @ModifyReturnValue(
        method = ["belowNameDisplay"],
        at = [At(value = "RETURN", ordinal = 0)]
    )
    context(
        @Local(name = ["objective"]) objective: Objective,
        @Local(name = ["formattedValue"]) formattedValue: Component,
    )
    fun customizeBelowName(original: Component?): Component? {
        if (objective.name !in BacapScoreboardMode.ALL) return original
        return objective.displayName.copy().append(": ").append(formattedValue)
    }
}

@KMixin<ReadOnlyScoreInfo>(Side.Client)
interface BacapPlayerNameScoreFormatRespecter {
    @WrapMethod(method = ["formatValue"])
    fun ReadOnlyScoreInfo.respectBacapScoreFormat(defaultFormat: NumberFormat, op: Op<MutableComponent>) =
        if (defaultFormat === BacapScoreFormat) defaultFormat.format(value()) else op(defaultFormat)
}

@KMixin<PlayerScoreEntry>(Side.Client)
class BacapSidebarScoreFormatRespecter {
    @WrapMethod(method = ["formatValue"])
    fun PlayerScoreEntry.respectBacapScoreFormat(defaultFormat: NumberFormat, op: Op<MutableComponent>) =
        if (defaultFormat === BacapScoreFormat) BacapScoreFormat.format(value()) else op(defaultFormat)
}

@KMixin<DeathScreen>(Side.Client)
abstract class BacapScoreGameOverCustomizer {

    @ModifyExpressionValue(
        method = ["<init>"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
        )]
    )
    fun customizeScore(original: MutableComponent): MutableComponent {
        if (!hardcore) return original
        val clientAdvancements = player.connection.advancements
        val doneAdvancements = clientAdvancements[ADVANCEMENT_LEGEND_ID]
            ?.let { clientAdvancements.progress()[it] }
            ?.getColoredProgressText()
            ?: return original
        return I18N.ADVANCEMENTS.append(": ").append(doneAdvancements)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val hardcore: Boolean

    @KShadow(PRIVATE, FINAL)
    abstract val player: LocalPlayer

    companion object {
        val ADVANCEMENT_LEGEND_ID = Identifier.fromNamespaceAndPath("blazeandcave", "bacap/advancement_legend")
    }
}

@KMixin<AdvancementProgress>(Side.Client)
abstract class BacapAdvancementLegendProgressCustomizer {

    @Extension
    fun getColoredProgressText(): Component? {
        if (criteria.isEmpty()) return null
        val total = requirements.size().takeIf { it > 1 } ?: return null
        val done = BacapScoreFormat.format(countCompletedRequirements())
        return Component.translatable("advancements.progress", done, total)
    }

    @KShadow(PRIVATE, FINAL)
    abstract val criteria: Map<String, CriterionProgress>

    @KShadow(PRIVATE)
    abstract val requirements: AdvancementRequirements

    @KShadow(PRIVATE)
    abstract fun countCompletedRequirements(): Int
}
