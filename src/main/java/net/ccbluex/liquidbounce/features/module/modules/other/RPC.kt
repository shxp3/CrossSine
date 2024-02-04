package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import kotlin.concurrent.thread

@ModuleInfo(name = "RPC", spacedName = "RPC", category = ModuleCategory.OTHER, defaultOn = true)
class RPC : Module(){
    override fun onEnable() {
            thread {
                try {
                    CrossSine.clientRichPresence.run()
                } catch (throwable: Throwable) {
                    ClientUtils.logError("Failed to setup Discord RPC.", throwable)
                }
            }
    }

    override fun onDisable() {
        CrossSine.clientRichPresence.stop()
    }
}