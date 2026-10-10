package net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.components

import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class StateBox {
    var name = ""
    var state = false
    var width = 0F
    var radius = 0F

    fun onDraw(x: Float, y: Float) {
        this.width = if (ClickGUIModule.fastRenderValue.get()) x + if (state) Fonts.Nova40.getStringWidth(name) + 13F else 7F else AnimationUtils.animate(x + if (state) Fonts.Nova40.getStringWidth(name) + 13F else 7F, this.width, 0.5f * RenderUtils.deltaTime * 0.025f)
        this.radius = if (ClickGUIModule.fastRenderValue.get()) if (state) 4F else 0F else AnimationUtils.animate(if (state) 4F else 0F, this.radius, 0.5f * RenderUtils.deltaTime * 0.025f)
        RenderUtils.drawBloomRoundedRect(x + 7F, y + 4F, this.width, y + 6F + Fonts.Nova40.FONT_HEIGHT, this.radius, 1.5F, ClientTheme.getColorWithAlpha(0, 150), RenderUtils.ShaderBloom.BOTH)
    }
}