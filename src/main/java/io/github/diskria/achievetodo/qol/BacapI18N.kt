package io.github.diskria.achievetodo.qol

import net.minecraft.network.chat.Component

object BacapI18N {
    val CLICK_TO_VIEW = Component.translatable("Click to view")
    val TAB_UNLOCKED_REGEX = Regex("""^You have unlocked the root of the (.+?) tab$""")

    fun tabComponent(name: String) = Component.translatable($$"%1$s tab", name)
}
