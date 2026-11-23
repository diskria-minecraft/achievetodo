package io.github.diskria.achievetodo.qol

import net.minecraft.network.chat.Component

object I18N {
    val CLICK_TO_VIEW get() = Component.translatable("Click to view")
    val ADVANCEMENT_POINTS get() = Component.translatable("Advancement Points")

    val TAB_UNLOCKED_REGEX = Regex("""^You have unlocked the root of the (.+?) tab$""")
    fun tab(name: String) = Component.translatable($$"%1$s tab", name)

    val ADVANCEMENTS get() = Component.translatable("gui.advancements")
}
