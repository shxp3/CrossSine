 
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import java.awt.Color

@ModuleInfo(name = "Chams", category = ModuleCategory.VISUAL)
class Chams : Module() {
    val targetsValue = BoolValue("Targets", true)
    val itemsValue = BoolValue("Items", true)

    val localPlayerValue = BoolValue("LocalPlayer", true)
    val legacyMode = BoolValue("Legacy-Mode", false)
    val texturedValue = BoolValue("Textured", false).displayable { !legacyMode.get() }
    val colorModeValue = ListValue("Color", arrayOf("Custom", "Slowly", "Fade"), "Custom").displayable { !legacyMode.get() }
    val behindColorModeValue = ListValue("Behind-Color", arrayOf("Same", "Opposite", "Red"), "Red").displayable { !legacyMode.get() }
    val colorValue = ColorValue("Color", Color(255,255,255),true).displayable { !legacyMode.get() }
    val saturationValue = FloatValue("Saturation", 1F, 0F, 1F).displayable { !legacyMode.get() }
    val brightnessValue = FloatValue("Brightness", 1F, 0F, 1F).displayable { !legacyMode.get() }
}