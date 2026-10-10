 
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import java.awt.Color

@ModuleInfo(name = "Glint", category = ModuleCategory.VISUAL)
class Glint : Module() {

    private val modeValue = ListValue("Mode", arrayOf("Theme", "AnotherRainbow", "Custom"), "Custom")
    private val colorValue = ColorValue("Color", Color(255,255,255)).displayable { modeValue.equals("Custom") }
    fun getColor(): Color {
        return when (modeValue.get().lowercase()) {
            "theme" -> ClientTheme.getColor(1)
            "anotherrainbow" -> ColorUtils.skyRainbow(10, 0.9F, 1F, 1.0)
            else -> colorValue.get()
        }
    }
}