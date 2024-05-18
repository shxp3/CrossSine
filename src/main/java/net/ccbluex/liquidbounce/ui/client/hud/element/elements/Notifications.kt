package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.math.BigDecimal
import kotlin.math.max

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true)
class Notifications(
        x: Double = 0.0, y: Double = 30.0, scale: Float = 1F,
        side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    /**
     * Example notification for CustomHUD designer
     */
    private val exampleNotification = Notification("Notification", "This is an example notification. test test test test testt testteststestetsetstest", NotifyType.INFO)

    /**
     * Draw element
     */
    override fun drawElement(partialTicks: Float): Border? {
        val notifications = mutableListOf<Notification>()
        for ((index, notify) in CrossSine.hud.notifications.withIndex()) {
            GL11.glPushMatrix()

            if (notify.drawNotification(index)) {
                notifications.add(notify)
            }

            GL11.glPopMatrix()
        }
        for (notify in notifications) {
            CrossSine.hud.notifications.remove(notify)
        }

        if (mc.currentScreen is GuiHudDesigner) {
            if (!CrossSine.hud.notifications.contains(exampleNotification))
                CrossSine.hud.addNotification(exampleNotification)

            exampleNotification.fadeState = FadeState.STAY
            exampleNotification.displayTime = System.currentTimeMillis()

            return Border(-150F, -20F, 0F, 0F)
        }
        return null
    }

}

class Notification(
        val title: String,
        val content: String,
        val type: NotifyType,
        val time: Int = 1500,
        val animeTime: Int = 500,
) {
    val width = 100.coerceAtLeast(
            Fonts.fontSFUI35.getStringWidth(this.title)
                    .coerceAtLeast(Fonts.fontSFUI35.getStringWidth(this.content)) + 12
    )
    val height = 30
    var x = 0f

    var fadeState = FadeState.IN
    var nowY = -height
    var displayTime = System.currentTimeMillis()
    var animeXTime = System.currentTimeMillis()
    var animeYTime = System.currentTimeMillis()

    /**
     * Draw notification
     */
    fun drawNotification(index: Int): Boolean {
        val nowTime = System.currentTimeMillis()
        val realY = -(index + 1) * height
        var transY = nowY.toDouble()
        // Y-Axis Animation
        if (nowY != realY) {
            var pct = (nowTime - animeYTime) / animeTime.toDouble()
            if (pct > 1) {
                nowY = realY
                pct = 1.0
            } else {
                pct = EaseUtils.easeOutExpo(pct)
            }
            transY += (realY - nowY) * pct
        } else {
            animeYTime = nowTime
        }

        // X-Axis Animation
        var pct = (nowTime - animeXTime) / animeTime.toDouble()
        when (fadeState) {
            FadeState.IN -> {
                if (pct > 1) {
                    fadeState = FadeState.STAY
                    animeXTime = nowTime
                    pct = 1.0
                }
                pct = EaseUtils.easeOutExpo(pct)
            }

            FadeState.STAY -> {
                pct = 1.0
                if ((nowTime - animeXTime) > time) {
                    fadeState = FadeState.OUT
                    animeXTime = nowTime
                }
            }

            FadeState.OUT -> {
                if (pct > 1) {
                    fadeState = FadeState.END
                    animeXTime = nowTime
                    pct = 1.0
                }
                pct = 1 - EaseUtils.easeInExpo(pct)
            }

            FadeState.END -> {
                return true
            }
        }
        val transX = width - (width * pct) - width
        GL11.glTranslated(transX, transY, 0.0)
        RenderUtils.drawRect(0F, 10F, 80F + Fonts.SFApple35.getStringWidth(content), 30F, Color(0, 0, 0, 200).rgb)
        Fonts.SFApple35.drawString(content, 24F, 17F, Color(255, 255, 255).rgb, true)
        RenderUtils.drawImage(
                ResourceLocation("crosssine/ui/notifications/icons/crosssine/" + type.name + ".png"),
                -3,
                7,
                26,
                26
        )
        GlStateManager.resetColor()
        return false
    }
}


enum class NotifyType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO;
}


enum class FadeState { IN, STAY, OUT, END }