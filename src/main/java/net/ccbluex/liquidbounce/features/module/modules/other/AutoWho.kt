package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.ChatEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils

@ModuleInfo(name = "AutoWho", spacedName = "Auto Who", category = ModuleCategory.OTHER)
class AutoWho : Module() {
    private val debug = BoolValue("Debug", false)

    @EventTarget
    fun onChat(event: ChatEvent) {
        val msg = event.chatString
        val strip = (ColorUtils.stripColor(msg).contains("The game starts in 1 second!") || ColorUtils.stripColor(msg).contains("has joined"))
        val noStrip = (msg.contains("The game starts in 1 second!") || msg.contains("has joined"))
        if (strip || noStrip) {
            mc.thePlayer.sendChatMessage("/who")
            if (debug.get())
                ClientUtils.displayChatMessage("[§EAutoWho§F] Send!")
        }
    }
}