package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.GuiChat
import org.lwjgl.input.Mouse
import java.awt.Color

@ModuleInfo("Scoreboard", ModuleCategory.CLIENT, defaultOn = true, loadConfig = false)
object Scoreboard : HUDModule() {
    private val alphaValue = IntegerValue("Alpha", 150, 0, 255)
    val textShadow = BoolValue("Text-Shadow", false)
    val showNumber = BoolValue("Show-Number", true)
    val fontValue = FontValue("Font:", Fonts.minecraftFont)
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        draw()
        drag(true)
    }
    fun getAlpha() : Int {
        return alphaValue.get()
    }
}