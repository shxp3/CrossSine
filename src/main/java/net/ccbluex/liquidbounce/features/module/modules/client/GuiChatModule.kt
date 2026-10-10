package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.ui.font.Fonts

@ModuleInfo(name = "ChatManager", category = ModuleCategory.CLIENT, array = false, defaultOn = true)
class GuiChatModule : Module() {
    val chatRectValue = BoolValue("Chat-Rect", false)
    val chatLimitValue = BoolValue("No-Chat-Limit", true)
    val chatCombine = BoolValue("Chat-Combine", true)
    val chatAnimValue = BoolValue("Chat-Animation", true)
}