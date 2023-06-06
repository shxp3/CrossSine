package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "FovChanger", spacedName = "Fov Changer", category = ModuleCategory.VISUAL)
class FovChanger : Module(){
    private val FovChangerValue = FloatValue("Fov", 0F, 0F, 200F)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.gameSettings.fovSetting = FovChangerValue.get()
    }
}