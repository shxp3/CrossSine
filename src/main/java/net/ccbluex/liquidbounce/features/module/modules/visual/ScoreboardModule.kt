package net.ccbluex.liquidbounce.features.module.modules.visual

import com.sun.org.apache.xpath.internal.operations.Bool
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.FontRenderer

@ModuleInfo("Scoreboard", ModuleCategory.VISUAL, defaultOn = true)
object ScoreboardModule : Module() {
    private val textShodaw = BoolValue("Text-Shadow", false)
    private val showNumber = BoolValue("Show-Number", true)
    private val bgalpha = IntegerValue("Background-Alpha", 80, 0, 255)
    private val posY = FloatValue("Pos-Y", 0F, 0F, 200F)

    fun getAlpha() : Int {
        return bgalpha.get()
    }
    fun getShadow() : Boolean {
        return textShodaw.get()
    }
    fun getShowNumber() : Boolean {
        return showNumber.get()
    }
    fun getPosY() : Float {
        return posY.get()
    }
}