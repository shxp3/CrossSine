package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ColorValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import java.awt.Color

@ModuleInfo(name = "CustomClientColor", category = ModuleCategory.CLIENT)
object CustomClientColor : Module() {
    private val colorValue = ColorValue("Color",Color(255,255,255))
    fun getColor() : Color {
        return colorValue.get()
    }
    fun getColor(alpha : Int) : Color {
        return Color(colorValue.get().red, colorValue.get().green, colorValue.get().blue, alpha)
    }
}