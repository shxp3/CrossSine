package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo

@ModuleInfo(name = "ShutDown", category = ModuleCategory.CLIENT, canEnable = false)
class ShutDown: Module() {
    override fun onEnable() {
        mc.shutdown()
    }
}