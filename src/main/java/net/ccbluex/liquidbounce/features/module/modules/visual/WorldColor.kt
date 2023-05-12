package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo(name = "WorldColor", spacedName = "World Color", category = ModuleCategory.VISUAL)
class WorldColor : Module() {
    val worldColorRValue = IntegerValue("WorldRed", 255, 0, 255)
    val worldColorGValue = IntegerValue("WorldGreen", 255, 0, 255)
    val worldColorBValue = IntegerValue("WorldBlue", 255, 0, 255)
}