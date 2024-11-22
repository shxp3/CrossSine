package net.ccbluex.liquidbounce.ui.client.other

import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * @author liulihaocai
 * FDPClient
 */
open class PopUI(val title: String) {
    val baseWidth = 150
    val baseHeight = 210
    var animationProgress = 0.0F
    var animatingOut = false

    fun onRender(width: Int, height: Int) {
        GL11.glPushMatrix()
        RenderUtils.drawRect(0F, 0F, width.toFloat(), height.toFloat(), Color(0, 0, 0, 50).rgb)
        val scaleFactor = EaseUtils.easeOutBack(animationProgress.toDouble()).toFloat()

        // คำนวณตำแหน่งกลางหน้าจอ
        val centerX = width / 2F
        val centerY = height / 2F

        // จัดตำแหน่งให้ UI อยู่ตรงกลางเสมอ และซูมจากศูนย์กลาง
        GL11.glTranslatef(centerX, centerY, 0F) // เลื่อนมาที่ศูนย์กลางหน้าจอ
        GL11.glScalef(scaleFactor, scaleFactor, 1F) // ซูมด้วย scaleFactor
        GL11.glTranslatef(-baseWidth / 2F, -baseHeight / 2F, 0F) // เลื่อนกลับไปที่จุดเริ่มต้นของ UI

        RenderUtils.drawRect(0F, 0F, baseWidth.toFloat(), baseHeight.toFloat(), Color.WHITE.rgb)
        Fonts.SFApple40.drawString(title, 8F, 8F, Color.DARK_GRAY.rgb)
        render()

        GL11.glPopMatrix()
        animationProgress += (if (ClickGUIModule.fastRenderValue.get()) 1F else 0.0075F * 0.25F * RenderUtils.deltaTime * if (animatingOut) -1F else 1F)
        animationProgress = animationProgress.coerceIn(0F, 1F)
    }

    fun onClick(width: Int, height: Int, mouseX: Int, mouseY: Int) {
        val scale = (width * 0.2F) / baseWidth
        val scaledMouseX = (mouseX - width * 0.4f) / scale
        val scaledMouseY = (mouseY - height * 0.3f) / scale

        if (scaledMouseX> 0 && scaledMouseY> 0 && scaledMouseX <baseWidth && scaledMouseY <baseHeight) {
            click(scaledMouseX, scaledMouseY)
        } else {
            close()
        }
    }

    fun onStroll(width: Int, height: Int, mouseX: Int, mouseY: Int, wheel: Int) {
        val scale = (width * 0.2F) / baseWidth
        val scaledMouseX = (mouseX - width * 0.4f) / scale
        val scaledMouseY = (mouseY - height * 0.3f) / scale

        if (scaledMouseX> 0 && scaledMouseY> 0 && scaledMouseX <baseWidth && scaledMouseY <baseHeight) {
            scroll(scaledMouseX, scaledMouseY, wheel)
        }
    }

    fun onKey(typedChar: Char, keyCode: Int) {
        key(typedChar, keyCode)
    }

    open fun render() {}

    open fun key(typedChar: Char, keyCode: Int) {}

    open fun close() {}

    open fun click(mouseX: Float, mouseY: Float) {}

    open fun scroll(mouseX: Float, mouseY: Float, wheel: Int) {}
}