package net.ccbluex.liquidbounce.ui.client.hud.element.elements


import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.SessionUtils
import net.ccbluex.liquidbounce.utils.StatisticsUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.ResourceLocation
import java.awt.Color

@ElementInfo(name = "Session", blur = true)
class Session(
    x: Double = 3.39,
    y: Double = 24.48,
    scale: Float = 1F,
    side: Side = Side.default()
) : Element(x, y, scale, side) {
    private val image = BoolValue("Image", false)
    override fun drawElement(partialTicks: Float): Border {
        /**
         * Visual
         */

        RenderUtils.drawRoundedRect(0F, 0F, 180F, 50F, 10F,Color(0,0,0,100).rgb, 2.5F,ClientTheme.getColor().rgb )
        Fonts.fontTenacityBold40.drawCenteredString("Session Stats", 90F, 5F, Color(255,255,255).rgb, true)
        Fonts.fontTenacity35.drawString("Kill: " + StatisticsUtils.getKills(), 5F, 20F, Color.WHITE.rgb, true)
        Fonts.fontTenacity35.drawString("Session Time: " + SessionUtils.getFormatSessionTime(), 5F, 29F, Color.WHITE.rgb, true)
        Fonts.fontTenacity35.drawString("Username: " + mc.thePlayer.name, 5F, 38F, Color.WHITE.rgb, true)
        if (image.get()) {
            RenderUtils.drawImage(ResourceLocation("crosssine/ui/session/session.png"), 130, 8, 40, 40)
        }
        return Border(0f, 0f, 180f, 50F)
    }
}
