package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "FovChanger", category = ModuleCategory.VISUAL)
class FovChanger : Module(){
    private val FovChangerValue = FloatValue("Fov", 0F, 0F, 200F)

    override fun onEnable() {
        mc.gameSettings.fovSetting = FovChangerValue.get()
    }

}