package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.font.CFontRenderer
import net.ccbluex.liquidbounce.font.FontLoaders
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max


/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", blur = true)
class Notifications(x: Double = 0.0, y: Double = 0.0, scale: Float = 1F,side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    /**
     * Example notification for CustomHUD designer
     */
    private val exampleNotification = Notification("Notification", "This is an example notification.", NotifyType.INFO)

    /**
     * Draw element
     */
    override fun drawElement(partialTicks: Float): Border? {
        // bypass java.util.ConcurrentModificationException
        CrossSine.hud.notifications.map { it }.forEachIndexed { index, notify ->
            GL11.glPushMatrix()

            if (notify.drawNotification(index, FontLoaders.C16)) {
                CrossSine.hud.notifications.remove(notify)
            }
            GlStateManager.resetColor()
            GL11.glPopMatrix()
        }

        if (mc.currentScreen is GuiHudDesigner) {
            if (!CrossSine.hud.notifications.contains(exampleNotification)) {
                CrossSine.hud.addNotification(exampleNotification)
            }

            exampleNotification.fadeState = FadeState.STAY
            exampleNotification.displayTime = System.currentTimeMillis()

            return Border(-exampleNotification.width.toFloat(), -exampleNotification.height.toFloat(), 0F, 0F)
        }

        return null
    }

    override fun drawBoarderBlur(blurRadius: Float) {}
}


class Notification(
    val title: String,
    val content: String,
    val type: NotifyType,
    val time: Int = 1500,
    private val animeTime: Int = 500
) {
    var width = 100
    var height = 30

    private val classicHeight = 30
    var x = 0F
    var textLengthtitle = 0
    var textLengthcontent = 0
    var textLength = 0f
    init {
        textLengthtitle = Fonts.font35.getStringWidth(title)
        textLengthcontent = Fonts.font35.getStringWidth(content)
        textLength = textLengthcontent.toFloat() + textLengthtitle.toFloat()
    }

    var fadeState = FadeState.IN
    private var nowY = -height
    var displayTime = System.currentTimeMillis()
    private var animeXTime = System.currentTimeMillis()
    private var animeYTime = System.currentTimeMillis()

    /**
     * Draw notification
     */
    fun drawNotification(
        index: Int, font: CFontRenderer
    ): Boolean {
        this.width = 100.coerceAtLeast(
            font.getStringWidth(content)
                .coerceAtLeast(font.getStringWidth(title)) + 15
        )
        val realY = -(index + 1) * height
        val nowTime = System.currentTimeMillis()
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
            RenderUtils.drawRoundedRect(0F, 10F, 130F, 30F, 2F, Color(0, 0, 0, 200).rgb)
            RenderUtils.drawRect(0F, classicHeight - 1.5F, max(width - width * ((nowTime - displayTime) / (animeTime * 2F + time)), 0F), classicHeight.toFloat(), Color(255,255,255))
            font.drawString(content, 15F, 17F, Color(255, 255, 255).rgb, false)
            if(type.renderColor == Color(0xFF2F2F)){
                RenderUtils.drawImage(ResourceLocation("crosssine/ui/notifications/icons/crosssine/cross.png"),1,(12.3).toInt(),12,12)
            } else if(type.renderColor == Color(0x60E092)){
                RenderUtils.drawImage(ResourceLocation("crosssine/ui/notifications/icons/crosssine/tick.png"),1,(12.3).toInt(),12,12)
            } else if(type.renderColor == Color(0xF5FD00)){
                RenderUtils.drawImage(ResourceLocation("crosssine/ui/notifications/icons/crosssine/warning.png"),1,(12.3).toInt(),12,12)
            } else if(type.renderColor == Color(0x6490A7)){
                RenderUtils.drawImage(ResourceLocation("crosssine/ui/notifications/icons/crosssine/info.png"),1,(12.3).toInt(),12,12)
            }
            return false
    }

}

//NotifyType Color
enum class NotifyType(var renderColor: Color) {
    SUCCESS(Color(0x60E092)),
    ERROR(Color(0xFF2F2F)),
    WARNING(Color(0xF5FD00)),
    INFO(Color(0x6490A7));
}
enum class FadeState { IN, STAY, OUT, END }


