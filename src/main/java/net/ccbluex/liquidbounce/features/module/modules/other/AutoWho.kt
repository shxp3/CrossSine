package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.ChatEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils

@ModuleInfo(name = "AutoWho", spacedName = "Auto Who", category = ModuleCategory.OTHER)
class AutoWho : Module() {
    private val debug = BoolValue("Debug", false)
    @EventTarget
    fun onChat(event: ChatEvent) {
            val msg = event.chatString
            if (msg.contains("has joined")) {
                mc.thePlayer.sendChatMessage("/who")
                if (debug.get())
                    ClientUtils.displayChatMessage("[§EAutoWho§F] Send!")
            }
    }
}