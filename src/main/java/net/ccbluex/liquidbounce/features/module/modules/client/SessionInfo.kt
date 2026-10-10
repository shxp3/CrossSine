package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.SessionUtils
import net.ccbluex.liquidbounce.utils.StatisticsUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.ShaderUtil
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import java.awt.Color

@ModuleInfo("SessionInfo", ModuleCategory.CLIENT, loadConfig = false)
object SessionInfo : HUDModule() {

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        GlStateManager.pushMatrix()
        ShaderUtil.drawRoundedRect(posX - 5F, posY - 5F, posX + 155F, posY + 70F, 8F, 5F, Color(0,0,0,255))
        RenderUtils.customRoundedinf(posX, posY, posX + 150F, posY + 20F, 8F, 8F, 0F, 0F, Color(40,40,40, 255).rgb)
        RenderUtils.customRoundedinf(posX, posY + 20F, posX + 150F, posY + 65F, 0F, 0F, 8F, 8F, Color(15,15,15, 255).rgb)
        Fonts.SFBold50.drawCenteredString("Session Info", posX + 75, posY + 5, Color.WHITE.rgb)
        Fonts.SFBold35.drawString("Kill: " + StatisticsUtils.getKills(), posX + 5F, posY + 27F, Color(100,100,100).rgb)
        Fonts.SFBold35.drawString("Session Time: " + SessionUtils.getFormatSessionTime(), posX + 5F, posY + 40F, Color(100,100,100).rgb)
        Fonts.SFBold35.drawString("Username: " + mc.thePlayer.name, posX + 5F, posY + 53F, Color(100,100,100).rgb)
        GlStateManager.resetColor()
        GlStateManager.popMatrix()
        draw()
        drag()
        updateBounds(posX, posY, 150F, 65F)
    }
}