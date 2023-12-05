package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@ModuleInfo(name = "AutoWho", spacedName = "Auto Who", category = ModuleCategory.OTHER)
class AutoWho : Module() {
    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
            val msg = event.message.unformattedText
            if (msg.contains("has joined")) {
                mc.thePlayer.sendChatMessage("/who")
            }
    }
}