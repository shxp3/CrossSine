package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import java.awt.Color

@ModuleInfo(name = "HitColor",category = ModuleCategory.VISUAL)
class HitColor : Module() {
    val hitColorTheme = BoolValue("ColorTheme", false)
    val hitColorValue = ColorValue("Hit-Color", Color(255,255,255))
    val hitColorAlphaValue = IntegerValue("Hit-Alpha", 255, 0, 255)
}
