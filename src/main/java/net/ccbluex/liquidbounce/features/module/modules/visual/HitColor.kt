package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue

@ModuleInfo(name = "HitColor",category = ModuleCategory.VISUAL)
class HitColor : Module() {
    val hitColorTheme = BoolValue("ColorTheme", false)
    val hitColorRValue = IntegerValue("HitRed", 255, 0, 255).displayable { !hitColorTheme.get() }
    val hitColorGValue = IntegerValue("HitGreen", 255, 0, 255).displayable { !hitColorTheme.get() }
    val hitColorBValue = IntegerValue("HitBlue", 255, 0, 255).displayable { !hitColorTheme.get() }
    val hitColorAlphaValue = IntegerValue("HitAlpha", 255, 0, 255)
}
