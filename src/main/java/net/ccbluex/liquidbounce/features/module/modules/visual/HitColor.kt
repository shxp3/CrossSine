package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo(name = "HitColor", spacedName = "HitColor", category = ModuleCategory.VISUAL)
class HitColor : Module() {
    val hitColorRValue = IntegerValue("HitRed", 255, 0, 255)
    val hitColorGValue = IntegerValue("HitGreen", 255, 0, 255)
    val hitColorBValue = IntegerValue("HitBlue", 255, 0, 255)
    val hitColorAlphaValue = IntegerValue("HitAlpha", 255, 0, 255)
}
