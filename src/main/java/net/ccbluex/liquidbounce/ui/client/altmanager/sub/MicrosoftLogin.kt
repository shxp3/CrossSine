package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.compat.OAuthServer
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground
import net.ccbluex.liquidbounce.ui.client.gui.ThemedUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class MicrosoftLogin(private val prevGui: GuiScreen) : GuiScreen() {
    private var stage = "Initializing..."
    private lateinit var server: OAuthServer
    private var cancelHover = 0f
    private var enterAnim   = 0f
    private var exitAnim    = 0f
    private var exitAction: (() -> Unit)? = null

    override fun initGui() {
        server = MicrosoftAccount.Companion.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {
            override fun openUrl(url: String) {
                stage = "Check your browser to continue..."
                ClientUtils.logInfo("Opening URL: $url")
                MiscUtils.showURL(url)
            }
            override fun authError(error: String) { stage = "§cError: $error" }
            override fun authResult(account: MicrosoftAccount) {
                if (CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.any { it.name == account.name }) {
                    stage = "§cAlready added"; return
                }
                CrossSine.fileManager.accountsConfig.altManagerMinecraftAccounts.add(account)
                CrossSine.fileManager.saveConfig(CrossSine.fileManager.accountsConfig)
                mc.displayGuiScreen(prevGui)
            }
        })
        enterAnim = 0f
        exitAnim  = 0f
        exitAction = null
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        enterAnim += (1f - enterAnim) * 0.1f

        // ── Exit animation ─────────────────────────────────────────────────
        if (exitAction != null) {
            exitAnim += (1f - exitAnim) * 0.13f
            if (exitAnim > 0.97f) {
                val action = exitAction; exitAction = null; action?.invoke(); return
            }
        }

        val scale = ThemedUI.lerp(0.85f, 1f, enterAnim) * ThemedUI.lerp(1f, 0.85f, exitAnim)
        val alpha = (enterAnim * (1f - exitAnim)).coerceIn(0f, 1f)

        val accent = ThemedUI.accent()
        ThemedBackground.draw(width, height)

        val cx = width / 2f; val cy = height / 2f
        GL11.glPushMatrix()
        GL11.glTranslatef(cx, cy, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-cx, -cy, 0f)
        GL11.glColor4f(1f, 1f, 1f, alpha)

        val sw = width.toFloat(); val sh = height.toFloat()
        val panW = 340f; val panH = 170f
        val panX = sw / 2f - panW / 2f
        val panY = sh / 2f - panH / 2f
        val innerX = panX + 18f
        val innerW = panW - 36f

        ThemedUI.drawPanel(panX, panY, panW, panH, accent)
        val divY = ThemedUI.drawSplitTitle(innerX, panY + 16f, "Microsoft", " Login", accent, innerW)

        // Stage text centered
        val f = Fonts.SFBold40
        val tw = f.getStringWidth(stage)
        f.drawStringWithShadow(stage, panX + panW / 2f - tw / 2f, divY + 24f,
            Color(225, 228, 240, 230).rgb)

        // Pulsing dots
        val t = (System.currentTimeMillis() % 1500L) / 1500f
        for (i in 0..2) {
            val phase = (t - i * 0.18f + 1f) % 1f
            val a = (Math.sin(phase * Math.PI).toFloat()).coerceAtLeast(0f)
            val cx = panX + panW / 2f - 14f + i * 14f
            val cy = divY + 24f + f.FONT_HEIGHT + 16f
            RoundedUtil.drawRound(cx - 3f, cy - 3f, 6f, 6f, 3f,
                Color(accent.red, accent.green, accent.blue, (60 + 180 * a).toInt()))
        }

        // Cancel button
        val bW = 120f; val bH = 22f
        val bX = panX + panW / 2f - bW / 2f
        val bY = panY + panH - bH - 16f
        cancelHover = ThemedUI.drawButton(bX, bY, bW, bH, "Cancel",
            mouseX, mouseY, cancelHover, accent, danger = true)

        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        val sw = width.toFloat(); val sh = height.toFloat()
        val panW = 340f; val panH = 170f
        val panX = sw / 2f - panW / 2f; val panY = sh / 2f - panH / 2f
        val bW = 120f; val bH = 22f
        val bX = panX + panW / 2f - bW / 2f
        val bY = panY + panH - bH - 16f
        if (mouseWithinBounds(mouseX, mouseY, bX, bY, bX + bW, bY + bH)) cancel()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) cancel()
    }

    private fun cancel() {
        try { server.stop(true) } catch (_: Throwable) {}
        exitAction = { mc.displayGuiScreen(prevGui) }; exitAnim = 0f
    }

    override fun doesGuiPauseGame() = false
}