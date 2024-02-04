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
    private val exampleNotification = Notification("Notification", "This is an example notification.", NotifyType.INFO)

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
//            exampleNotification.x = exampleNotification.textLength + 8F

            return Border(-150F, -30F, 0F, 0F)
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
        var pct = (nowTime - animeXTime) / animeTime.toDouble()
        val image = ResourceLocation("liquidbounce+/ui/" + type.name + ".png")
        //Y-Axis Animation
        if (nowY != realY) {
            var pct = (nowTime - animeYTime) / animeTime.toDouble()
            if (pct > 1) {
                nowY = realY
                pct = 1.0
            } else {
                pct = EaseUtils.easeOutExpo(pct)
            }
            GL11.glTranslated(0.0, (realY - nowY) * pct, 0.0)
        } else {
            animeYTime = nowTime
        }
        GL11.glTranslated(0.0, nowY.toDouble(), 0.0)

        //X-Axis Animation
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
        GL11.glTranslated(width - (width * pct), 0.0, 0.0)
        GL11.glTranslatef(-width.toFloat(), 0F, 0F)
        RenderUtils.customRounded(0F, 10F, 130F, 30F, 4.5F, 4.5F, 0F, 0F, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(0F, 28.5f, max(width - width * ((nowTime - displayTime) / (animeTime * 2F + time)), 0F), 30F, Color(255, 255, 255))
        Fonts.SFApple35.drawString(content, 15F, 17F, Color(255, 255, 255).rgb, true)
        when (type) {
            NotifyType.ERROR -> {
                RenderUtils.drawImage(
                        ResourceLocation("crosssine/ui/notifications/icons/crosssine/cross.png"),
                        1,
                        (12.3).toInt(),
                        12,
                        12
                )
            }

            NotifyType.SUCCESS -> {
                RenderUtils.drawImage(
                        ResourceLocation("crosssine/ui/notifications/icons/crosssine/tick.png"),
                        1,
                        (12.3).toInt(),
                        12,
                        12
                )
            }

            NotifyType.WARNING -> {
                RenderUtils.drawImage(
                        ResourceLocation("crosssine/ui/notifications/icons/crosssine/warning.png"),
                        1,
                        (12.3).toInt(),
                        12,
                        12
                )
            }

            NotifyType.INFO -> {
                RenderUtils.drawImage(
                        ResourceLocation("crosssine/ui/notifications/icons/crosssine/info.png"),
                        1,
                        (12.3).toInt(),
                        12,
                        12
                )
            }
        }
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