package io.github.diskria.achievetodo.extensions

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent

fun MutableComponent.withFooterStyle() = withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
