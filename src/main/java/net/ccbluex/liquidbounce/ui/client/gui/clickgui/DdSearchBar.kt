package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.element.SearchBox
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Centered top search control for [DropdownGui].
 *
 * Collapsed = round icon button · Expanded = elongated field for typing.
 * Filter query is exposed via [query] for category module filtering.
 */
class DdSearchBar {

    companion object {
        private const val BTN = 22f
        private const val MAX_W = 168f
        private const val H = 22f
        private const val TOP = 8f
        private const val ICON = 10

        /** Current filter text (empty = show all). */
        @JvmStatic
        var query: String = ""
            private set

        @JvmStatic
        fun matches(name: String): Boolean {
            val q = query.trim()
            if (q.isEmpty()) return true
            return name.contains(q, ignoreCase = true)
        }

        @JvmStatic
        fun isFiltering(): Boolean = query.trim().isNotEmpty()
    }

    var expanded = false
        private set

    /** Cascade open/close zoom (same idea as category panels). */
    var zoomAnim = 0f

    private var expandAnim = 0f
    private val field = SearchBox(0, 0, 0, 10, 12).also {
        it.maxStringLength = 48
        it.setTextColor(Color(230, 232, 242).rgb)
    }

    fun reset() {
        expanded = false
        expandAnim = 0f
        zoomAnim = 0f
        field.text = ""
        field.isFocused = false
        query = ""
    }

    /** Keep filter query in sync before category panels draw. */
    fun syncQuery() {
        query = field.text
    }

    fun draw(mX: Int, mY: Int, accent: Color, screenW: Int) {
        expandAnim = expandAnim.animSmooth(if (expanded) 1f else 0f, 0.35f)
        val t = EaseUtils.easeOutCubic(expandAnim.toDouble()).toFloat()
        val z = zoomAnim.coerceIn(0f, 1f)
        if (z < 0.01f) return

        val w = BTN + (MAX_W - BTN) * t
        val x = (screenW - w) / 2f
        val y = TOP
        val cx = x + w / 2f
        val cy = y + H / 2f

        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(z, z, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)

        val a2 = Color(
            (accent.red * 0.65f + 80).toInt().coerceAtMost(255),
            (accent.green * 0.65f + 80).toInt().coerceAtMost(255),
            (accent.blue * 0.65f + 90).toInt().coerceAtMost(255)
        )
        val hover = MouseUtils.mouseWithinBounds(mX, mY, x, y, x + w, y + H)
        val radius = H / 2f

        RoundedUtil.drawRound(x, y, w, H, radius, Color(10, 11, 16, 230))
        RoundedUtil.drawGradientRound(
            x, y, w, H, radius,
            Color(accent.red, accent.green, accent.blue, if (hover || expanded) 48 else 28),
            Color(255, 255, 255, if (hover) 18 else 10),
            Color(a2.red, a2.green, a2.blue, if (hover || expanded) 32 else 16),
            Color(accent.red, accent.green, accent.blue, if (hover || expanded) 60 else 36)
        )
        RoundedUtil.drawRoundOutline(
            x, y, w, H, radius, 1f,
            Color(0, 0, 0, 0),
            Color(accent.red, accent.green, accent.blue, if (expanded) 150 else if (hover) 110 else 70)
        )

        val iconX = x + 7f * t + (w - ICON) / 2f * (1f - t)
        val iconY = y + (H - ICON) / 2f
        GlStateManager.disableAlpha()
        GlStateManager.color(1f, 1f, 1f, 0.9f)
        RenderUtils.drawImage2(IconManager.search, iconX, iconY, ICON, ICON)
        GlStateManager.enableAlpha()
        GlStateManager.color(1f, 1f, 1f, 1f)

        if (t > 0.08f) {
            val textX = (x + 22f).toInt()
            val textY = (y + H / 2f - Fonts.Nova40.FONT_HEIGHT / 2f).toInt()
            val textW = (w - 30f - if (field.text.isNotEmpty()) 14f else 0f).toInt().coerceAtLeast(4)
            field.xPosition = textX
            field.yPosition = textY
            field.width = textW
            field.height = Fonts.Nova40.FONT_HEIGHT

            if (field.text.isEmpty()) {
                Fonts.font30SemiBold.drawStringWithShadow(
                    "Search…",
                    x + 22f,
                    y + H / 2f - Fonts.font30SemiBold.FONT_HEIGHT / 2f,
                    Color(140, 145, 160, (t * 210).toInt().coerceIn(0, 210)).rgb
                )
            }
            if (t > 0.35f) field.drawTextBox()

            if (field.text.isNotEmpty() && t > 0.7f) {
                val clearX = x + w - 16f
                Fonts.font30SemiBold.drawStringWithShadow(
                    "×", clearX, y + H / 2f - Fonts.font30SemiBold.FONT_HEIGHT / 2f,
                    Color(180, 184, 198).rgb
                )
            }
        }

        GL11.glPopMatrix()
        query = field.text
    }

    /** @return true if the click was consumed by the search bar */
    fun mouseClicked(mX: Int, mY: Int, btn: Int, screenW: Int): Boolean {
        if (btn != 0 || zoomAnim < 0.4f) return false
        val t = EaseUtils.easeOutCubic(expandAnim.toDouble()).toFloat()
        val w = BTN + (MAX_W - BTN) * t.coerceAtLeast(if (expanded) 0.01f else 0f)
        val hitW = if (expanded || expandAnim > 0.05f) w else BTN
        val x = (screenW - hitW) / 2f
        val y = TOP
        val inside = MouseUtils.mouseWithinBounds(mX, mY, x, y, x + hitW, y + H)

        if (!inside) {
            if (expanded && field.text.isEmpty()) {
                expanded = false
                field.isFocused = false
            } else {
                field.isFocused = false
            }
            return false
        }

        if (!expanded) {
            expanded = true
            field.isFocused = true
            return true
        }

        if (field.text.isNotEmpty() && mX.toFloat() >= x + hitW - 18f) {
            field.text = ""
            query = ""
            field.isFocused = true
            return true
        }

        field.mouseClicked(mX, mY, btn)
        field.isFocused = true
        return true
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!expanded) return false

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (field.text.isNotEmpty()) {
                field.text = ""
                query = ""
                return true
            }
            expanded = false
            field.isFocused = false
            return true
        }

        if (!field.isFocused) return false
        field.textboxKeyTyped(typedChar, keyCode)
        query = field.text
        return true
    }

    fun isFocused(): Boolean = expanded && field.isFocused
}
