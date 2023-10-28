package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.ui.font.Fonts

@ModuleInfo(name = "ChatManager", spacedName = "Chat Manager", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
class GuiChatModule : Module() {
    val fontChatValue = BoolValue("FontChat", false)
    val fontType = FontValue("Font", Fonts.Nunito40).displayable { fontChatValue.get() }
    val chatRectValue = BoolValue("ChatRect", false)
    val chatLimitValue = BoolValue("NoChatLimit", true)
    val chatCombine = BoolValue("ChatCombine", true)
    val chatAnimValue = BoolValue("ChatAnimation", true)
}