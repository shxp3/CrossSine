package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.CrossSine.clientRPC
import net.ccbluex.liquidbounce.discordrpc.CrossSineRPC
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import kotlin.concurrent.thread

@ModuleInfo("DiscordRichPresence", ModuleCategory.CLIENT, defaultOn = true)
object DiscordRPC : Module() {
    val serverValue = BoolValue("Show-Server", true)
    override fun onEnable() {
        thread {
            try {
                clientRPC.run()
            } catch (throwable: Throwable) {
                ClientUtils.logError("Failed to run DiscordRPC", throwable)
            }
        }
    }

    override fun onDisable() {
        clientRPC.stop()
    }
}