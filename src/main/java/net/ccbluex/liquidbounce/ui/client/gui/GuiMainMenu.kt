package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.ClientUpdater
import net.ccbluex.liquidbounce.utils.MouseUtils.mouseWithinBounds
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.util.ResourceLocation
import java.util.UUID
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GuiMainMenu : GuiScreen(), GuiYesNoCallback {

    private val hoverAnims = FloatArray(8)
    private val btnEnter = FloatArray(6)
    private var showChangelog = false
    private var changelogAnim = 0f
    private var enterAnim = 0f
    private var exitAnim = 0f
    private var exitTarget: GuiScreen? = null
    private var pulse = 0f
    private var openMs = 0L

    private var panX = 0f
    private var panY = 0f
    private var panW = 200f
    private var panH = 0f
    private var innerX = 0f
    private var innerW = 0f
    private var navBtnY = FloatArray(4)
    private var navBtnH = 32f
    private var botY1 = 0f
    private var botY2 = 0f
    private var botH = 26f
    private var clBtnX = 0f
    private var clBtnY = 0f
    private var clBtnW = 0f
    private var clBtnH = 0f
    private var updBtnX = 0f
    private var updBtnY = 0f
    private var updBtnW = 0f
    private var updBtnH = 0f
    private var updVisible = false

    // Fixed starfield (no random drifting orbs)
    private val stars = Array(48) { i ->
        Star(
            fx = ((i * 73) % 997) / 997f,
            fy = ((i * 41) % 991) / 991f,
            size = 0.8f + (i % 5) * 0.35f,
            phase = i * 0.37f
        )
    }

    override fun initGui() {
        buttonList.clear()
        enterAnim = 0f
        exitAnim = 0f
        exitTarget = null
        changelogAnim = if (showChangelog) 1f else 0f
        openMs = System.currentTimeMillis()
        for (i in btnEnter.indices) btnEnter[i] = 0f
        for (i in hoverAnims.indices) hoverAnims[i] = 0f
        if (ClientUpdater.status == ClientUpdater.Status.IDLE || ClientUpdater.status == ClientUpdater.Status.FAILED) {
            ClientUpdater.checkAsync(force = ClientUpdater.status == ClientUpdater.Status.FAILED)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val accent = ClientTheme.getColor(0)
        val accent2 = ClientTheme.getColor(10)
        val t = (System.currentTimeMillis() - openMs) / 1000f
        pulse = (sin(t * 1.4) * 0.5 + 0.5).toFloat()

        val px = ((mouseX / sw) - 0.5f) * 2f
        val py = ((mouseY / sh) - 0.5f) * 2f

        // Custom background — no ThemedBackground orbs
        drawCoolBackground(sw, sh, accent, accent2, t, px, py)

        enterAnim = lerp(enterAnim, 1f, 0.09f)
        if (exitTarget != null) exitAnim = lerp(exitAnim, 1f, 0.11f)
        val easeIn = easeOutCubic(enterAnim)
        val easeOut = easeInCubic(exitAnim)

        // Cascade: next button starts 0.3s after previous starts (animations overlap)
        val elapsed = (System.currentTimeMillis() - openMs) / 1000f
        for (i in btnEnter.indices) {
            val appear = ThemedUI.stagger(elapsed, i, staggerSec = 0.3f, durationSec = 0.65f)
            btnEnter[i] = appear * (1f - easeOut)
        }

        // ── Side panel ────────────────────────────────────────────────────────
        val pad = 14f
        panW = 200f
        panH = sh - pad * 2
        panX = pad + lerp(-(panW + 30f), 0f, easeIn) - lerp(0f, panW + 30f, easeOut)
        panY = pad
        innerX = panX + 16f
        innerW = panW - 32f

        // Panel: base + round corner gradients
        val panelR = 14f
        RoundedUtil.drawRound(panX, panY, panW, panH, panelR, Color(8, 9, 14, 235))
        RoundedUtil.drawGradientRound(
            panX, panY, panW, panH, panelR,
            withAlpha(accent, 22),
            Color(255, 255, 255, 14),
            withAlpha(accent2, 12),
            withAlpha(accent, 36)
        )
        RoundedUtil.drawGradientCornerLR(
            panX, panY, panW, 70f, panelR,
            withAlpha(accent, 40),
            Color(0, 0, 0, 0)
        )
        RoundedUtil.drawRoundOutline(
            panX, panY, panW, panH, panelR, 1.1f,
            Color(0, 0, 0, 0),
            Color(accent.red, accent.green, accent.blue, 60 + (pulse * 40).toInt())
        )

        // Accent bar
        RoundedUtil.drawGradientVertical(
            panX + 5f, panY + 22f, 3.5f, panH - 44f, 1.7f,
            withAlpha(accent, 230),
            withAlpha(accent2, 70)
        )

        // Brand
        val titleFont = Fonts.Nunito50
        val titleY = panY + 20f
        val tw = titleFont.getStringWidth("Cross").toFloat()
        titleFont.drawStringWithShadow("Cross", innerX, titleY, Color(242, 242, 250).rgb)
        titleFont.drawStringWithShadow("Sine", innerX + tw + 1f, titleY, accent.rgb)
        Fonts.SFBold30.drawString(
            "by ${CrossSine.CLIENT_CREATOR}",
            innerX, titleY + titleFont.FONT_HEIGHT + 1f,
            Color(255, 255, 255, 50).rgb, false
        )

        val divY = titleY + titleFont.FONT_HEIGHT + 14f
        drawLaserLine(innerX, divY, innerW, accent, accent2, t)

        // Nav
        val btnFont = Fonts.SFBold35
        navBtnH = 32f
        val btnGap = 6f
        var btnY = divY + 12f
        val navLabels = arrayOf("Singleplayer", "Multiplayer", "Alt Manager", "Theme Color")
        val navHints = arrayOf("Local worlds", "Join a server", "Accounts", "Accent colors")

        for (i in navLabels.indices) {
            val slide = btnEnter[i]
            val dx = innerX - (1f - slide) * 16f
            navBtnY[i] = btnY

            val over = mouseWithinBounds(mouseX, mouseY, innerX, btnY, innerX + innerW, btnY + navBtnH)
            hoverAnims[i] = lerp(hoverAnims[i], if (over && exitTarget == null) 1f else 0f, 0.2f)
            val h = hoverAnims[i]
            val r = 8f

            // Visible round gradient fill (stronger idle + brighter hover)
            RoundedUtil.drawRound(dx, btnY, innerW, navBtnH, r, Color(12, 13, 20, (220 * slide).toInt()))
            RoundedUtil.drawGradientRound(
                dx, btnY, innerW, navBtnH, r,
                withAlpha(accent, ((55 + h * 90) * slide).toInt()),
                withAlpha(accent, ((90 + h * 110) * slide).toInt()),
                withAlpha(accent2, ((35 + h * 70) * slide).toInt()),
                withAlpha(accent2, ((70 + h * 100) * slide).toInt())
            )
            RoundedUtil.drawRoundOutline(
                dx, btnY, innerW, navBtnH, r, 1f,
                Color(0, 0, 0, 0),
                Color(accent.red, accent.green, accent.blue, ((50 + h * 120) * slide).toInt())
            )
            if (h > 0.05f) {
                RoundedUtil.drawGradientVertical(
                    dx + 4f, btnY + 8f, 2.5f, navBtnH - 16f, 1.2f,
                    withAlpha(accent, (230 * h).toInt()),
                    withAlpha(accent2, (140 * h).toInt())
                )
            }

            // Vertically centered title + hint
            val titleH = btnFont.FONT_HEIGHT.toFloat()
            val hintH = Fonts.SFBold24.FONT_HEIGHT.toFloat()
            val blockH = titleH + 2f + hintH
            val textTop = btnY + (navBtnH - blockH) / 2f
            btnFont.drawStringWithShadow(
                navLabels[i],
                dx + 12f + h * 4f,
                textTop,
                blendColor(Color(175, 178, 192, (230 * slide).toInt()), Color(250, 250, 255, (255 * slide).toInt()), h).rgb
            )
            Fonts.SFBold24.drawString(
                navHints[i],
                dx + 12f + h * 4f,
                textTop + titleH + 2f,
                Color(255, 255, 255, ((50 + h * 70) * slide).toInt()).rgb,
                false
            )
            btnY += navBtnH + btnGap
        }

        // Footer
        botH = 26f
        val botGap = 6f
        botY2 = panY + panH - pad - botH
        botY1 = botY2 - botGap - botH

        val headSize = 20
        val sessionY = botY1 - 14f - headSize
        val sessionSlide = btnEnter[4]
        val sessionX = innerX - (1f - sessionSlide) * 12f

        drawLaserLine(innerX, sessionY - 10f, innerW, accent, accent2, t + 0.8f)

        // Round head (stencil clip — same approach as TargetHUD / DynamicIsland)
        RoundedUtil.drawGradientRound(
            sessionX - 2f, sessionY - 2f, headSize + 4f, headSize + 4f, 7f,
            withAlpha(accent, 90 + (pulse * 40).toInt()),
            withAlpha(accent, 50),
            withAlpha(accent2, 40),
            withAlpha(accent2, 80)
        )
        drawRoundHead(sessionX, sessionY, headSize.toFloat(), 6f)

        Fonts.SFBold30.drawStringWithShadow(
            mc.session.username,
            sessionX + headSize + 8f, sessionY + 2f,
            Color(225, 225, 235, (210 * sessionSlide).toInt()).rgb
        )
        Fonts.SFBold24.drawString(
            "Minecraft 1.8.9",
            sessionX + headSize + 8f,
            sessionY + headSize - Fonts.SFBold24.FONT_HEIGHT,
            Color(255, 255, 255, (48 * sessionSlide).toInt()).rgb,
            false
        )

        hoverAnims[4] = lerp(
            hoverAnims[4],
            if (mouseWithinBounds(mouseX, mouseY, innerX, botY1, innerX + innerW, botY1 + botH) && exitTarget == null) 1f else 0f,
            0.2f
        )
        drawFooterBtn(innerX, botY1, innerW, botH, "Settings", hoverAnims[4], btnEnter[4], accent, false, btnFont)

        hoverAnims[5] = lerp(
            hoverAnims[5],
            if (mouseWithinBounds(mouseX, mouseY, innerX, botY2, innerX + innerW, botY2 + botH) && exitTarget == null) 1f else 0f,
            0.2f
        )
        drawFooterBtn(innerX, botY2, innerW, botH, "Quit", hoverAnims[5], btnEnter[5], accent, true, btnFont)

        // Hero
        drawHero(sw, sh, accent, accent2, easeIn, easeOut, t, px, py)

        // Top-right changelog only (version badge removed)
        val badgeFont = Fonts.SFBold35
        clBtnH = badgeFont.FONT_HEIGHT + 10f
        clBtnW = badgeFont.getStringWidth("Changelog") + 20f
        clBtnX = sw - pad - clBtnW + (1f - easeIn) * 36f
        clBtnY = pad

        hoverAnims[6] = lerp(
            hoverAnims[6],
            if (mouseWithinBounds(mouseX, mouseY, clBtnX, clBtnY, clBtnX + clBtnW, clBtnY + clBtnH) && !showChangelog) 1f else 0f,
            0.2f
        )
        val clH = hoverAnims[6]
        RoundedUtil.drawRound(clBtnX, clBtnY, clBtnW, clBtnH, 7f, Color(10, 11, 16, 210))
        RoundedUtil.drawGradientRound(
            clBtnX, clBtnY, clBtnW, clBtnH, 7f,
            withAlpha(accent, (30 + clH * 90).toInt()),
            withAlpha(accent, (50 + clH * 110).toInt()),
            withAlpha(accent2, (18 + clH * 70).toInt()),
            withAlpha(accent2, (35 + clH * 90).toInt())
        )
        RoundedUtil.drawRoundOutline(
            clBtnX, clBtnY, clBtnW, clBtnH, 7f, 1f, Color(0, 0, 0, 0),
            Color(accent.red, accent.green, accent.blue, (50 + clH * 120).toInt())
        )
        badgeFont.drawStringWithShadow(
            "Changelog", clBtnX + 10f, clBtnY + 5f,
            blendColor(Color(190, 192, 205, 170), Color(250, 250, 255, 240), clH).rgb
        )

        drawUpdateBadge(sw, pad, easeIn, accent, accent2, badgeFont, mouseX, mouseY)

        Fonts.SFBold30.drawString(
            CrossSine.CLIENT_TITLE,
            sw - pad - Fonts.SFBold30.getStringWidth(CrossSine.CLIENT_TITLE),
            sh - pad - Fonts.SFBold30.FONT_HEIGHT,
            Color(255, 255, 255, (40 * easeIn).toInt()).rgb,
            false
        )

        changelogAnim = lerp(changelogAnim, if (showChangelog) 1f else 0f, 0.15f)
        if (changelogAnim > 0.01f) drawChangelog(sw, sh, accent, accent2, changelogAnim)

        if (exitTarget != null && exitAnim > 0.97f) {
            val target = exitTarget
            exitTarget = null
            mc.displayGuiScreen(target)
        }
    }

    // ── Background ────────────────────────────────────────────────────────────

    private fun drawCoolBackground(sw: Float, sh: Float, accent: Color, accent2: Color, t: Float, px: Float, py: Float) {
        // Flat dark base — no floating orbs
        drawRect(0, 0, width, height, Color(5, 6, 10, 255).rgb)

        // Soft radial washes (concentric circles = real round gradient, no bloom shader)
        val gx = sw * 0.62f + px * 14f
        val gy = sh * 0.42f + py * 10f
        drawRadialGlow(gx, gy, min(sw, sh) * 0.55f, accent, 38)
        drawRadialGlow(sw * 0.15f + px * 6f, sh * 0.2f + py * 4f, min(sw, sh) * 0.28f, accent2, 18)
        drawRadialGlow(sw * 0.85f, sh * 0.85f, min(sw, sh) * 0.32f, accent, 14)

        // Subtle grid
        drawGrid(sw, sh, accent, 0.08f)

        // Fixed twinkling stars
        for (s in stars) {
            val twinkle = (sin((t * 2.1 + s.phase).toDouble()) * 0.5 + 0.5).toFloat()
            val a = (18 + twinkle * 55).toInt()
            val x = s.fx * sw
            val y = s.fy * sh
            RenderUtils.drawFilledCircle(x, y, s.size, Color(accent.red, accent.green, accent.blue, a))
        }

        // Vignette
        RenderUtils.drawGradientSidewaysV(0.0, 0.0, sw.toDouble(), 80.0, Color(0, 0, 0, 120).rgb, Color(0, 0, 0, 0).rgb)
        RenderUtils.drawGradientSidewaysV(0.0, (sh - 100).toDouble(), sw.toDouble(), sh.toDouble(), Color(0, 0, 0, 0).rgb, Color(0, 0, 0, 140).rgb)
    }

    private fun drawRadialGlow(cx: Float, cy: Float, radius: Float, color: Color, maxAlpha: Int) {
        // Layered circles → smooth round gradient, avoids bloom GL_INVALID_VALUE
        val layers = 10
        for (i in layers downTo 1) {
            val p = i / layers.toFloat()
            val r = radius * p
            val a = (maxAlpha * (1f - p) * (1f - p)).toInt().coerceIn(0, 255)
            if (a < 2) continue
            RenderUtils.drawFilledCircle(cx, cy, r, Color(color.red, color.green, color.blue, a))
        }
    }

    private fun drawGrid(sw: Float, sh: Float, accent: Color, alphaMul: Float) {
        val step = 28f
        val col = Color(accent.red, accent.green, accent.blue, (22 * alphaMul).toInt().coerceIn(0, 40)).rgb
        var x = 0f
        while (x < sw) {
            RenderUtils.drawRect(x, 0f, x + 0.5f, sh, col)
            x += step
        }
        var y = 0f
        while (y < sh) {
            RenderUtils.drawRect(0f, y, sw, y + 0.5f, col)
            y += step
        }
    }

    private fun drawHero(
        sw: Float, sh: Float, accent: Color, accent2: Color,
        easeIn: Float, easeOut: Float, t: Float, px: Float, py: Float
    ) {
        val stageX = panX + panW + 24f
        val available = sw - stageX - 20f
        if (available < 140f) return

        val cx = stageX + available * 0.52f + px * 8f
        val cy = sh * 0.44f + py * 6f
        val appear = (easeIn - easeOut).coerceIn(0f, 1f)
        if (appear < 0.01f) return

        drawRadialGlow(cx, cy, 120f + pulse * 16f, accent, (36 * appear).toInt())

        // Ultra-smooth elliptical orbits (ribbon + glow passes)
        drawSmoothOrbit(cx, cy, 94f + pulse * 2f, 54f + pulse * 1.5f, t * 32f, 230f, accent, (90 * appear).toInt())
        drawSmoothOrbit(cx, cy, 112f + pulse * 1.5f, 64f, -t * 22f + 55f, 200f, accent2, (60 * appear).toInt())
        drawSmoothOrbit(cx, cy, 76f, 44f, t * 14f + 20f, 180f, accent, (45 * appear).toInt())

        // Orbit satellites (soft glow dots)
        for (i in 0 until 5) {
            val a = t * 0.55f + i * (Math.PI * 2.0 / 5.0)
            val ox = cx + cos(a).toFloat() * 98f
            val oy = cy + sin(a).toFloat() * 56f
            val col = if (i % 2 == 0) accent else accent2
            RenderUtils.drawFilledCircle(ox, oy, 7f, withAlpha(col, (18 * appear).toInt()))
            RenderUtils.drawFilledCircle(ox, oy, 4f, withAlpha(col, (45 * appear).toInt()))
            RenderUtils.drawFilledCircle(ox, oy, 2.2f, withAlpha(col, (140 * appear).toInt()))
        }

        val heroFont = Fonts.Nunito60
        val left = "Cross"
        val right = "Sine"
        val totalW = heroFont.getStringWidth(left) + heroFont.getStringWidth(right) + 2f
        val hx = cx - totalW / 2f
        val hy = cy - heroFont.FONT_HEIGHT / 2f - 8f

        heroFont.drawString(left, hx + 2f, hy + 2f, Color(0, 0, 0, (90 * appear).toInt()).rgb, false)
        heroFont.drawString(right, hx + heroFont.getStringWidth(left) + 2f + 2f, hy + 2f, Color(0, 0, 0, (90 * appear).toInt()).rgb, false)
        heroFont.drawStringWithShadow(left, hx, hy, Color(245, 245, 252, (255 * appear).toInt()).rgb)
        heroFont.drawStringWithShadow(
            right, hx + heroFont.getStringWidth(left) + 2f, hy,
            withAlpha(accent, (255 * appear).toInt()).rgb
        )

        val sub = "precision · control · flow"
        val subW = Fonts.SFBold35.getStringWidth(sub)
        Fonts.SFBold35.drawString(
            sub, cx - subW / 2f, hy + heroFont.FONT_HEIGHT + 8f,
            Color(200, 205, 220, ((75 + pulse * 45) * appear).toInt()).rgb,
            false
        )
    }

    private fun drawChangelog(sw: Float, sh: Float, accent: Color, accent2: Color, anim: Float) {
        val a = easeOutCubic(anim)
        drawRect(0, 0, width, height, Color(0, 0, 0, (175 * a).toInt()).rgb)

        val mW = 330f
        val mH = 290f
        val mX = sw / 2f - mW / 2f
        val mY = sh / 2f - mH / 2f + (1f - a) * 16f

        RoundedUtil.drawRound(mX, mY, mW, mH, 14f, Color(9, 10, 15, (248 * a).toInt().coerceIn(0, 255)))
        RoundedUtil.drawGradientRound(
            mX, mY, mW, mH, 14f,
            withAlpha(accent, (20 * a).toInt()),
            withAlpha(accent, (40 * a).toInt()),
            withAlpha(accent2, (12 * a).toInt()),
            withAlpha(accent2, (28 * a).toInt())
        )
        RoundedUtil.drawRoundOutline(
            mX, mY, mW, mH, 14f, 1.2f, Color(0, 0, 0, 0),
            withAlpha(accent, (90 * a).toInt())
        )
        RoundedUtil.drawGradientVertical(
            mX + 6f, mY + 20f, 3f, mH - 40f, 1.5f,
            withAlpha(accent, (210 * a).toInt()),
            withAlpha(accent2, (70 * a).toInt())
        )

        val hFont = Fonts.SFBold40
        val eFont = Fonts.SFBold30
        hFont.drawStringWithShadow("Changelog", mX + 18f, mY + 16f, withAlpha(accent, (255 * a).toInt()).rgb)
        eFont.drawStringWithShadow(
            "ESC / click to close",
            mX + mW - eFont.getStringWidth("ESC / click to close") - 14f,
            mY + 18f, Color(255, 255, 255, (55 * a).toInt()).rgb
        )
        drawLaserLine(mX + 16f, mY + 16f + hFont.FONT_HEIGHT + 8f, mW - 32f, accent, accent2, System.currentTimeMillis() / 1000f)

        var ey = mY + 16f + hFont.FONT_HEIGHT + 18f
        for ((ver, entries) in CHANGELOG) {
            val pillW = eFont.getStringWidth(ver) + 14f
            RoundedUtil.drawGradientCornerLR(
                mX + 18f, ey - 1f, pillW, eFont.FONT_HEIGHT + 5f, 5f,
                withAlpha(accent, (90 * a).toInt()),
                withAlpha(accent2, (50 * a).toInt())
            )
            eFont.drawStringWithShadow(ver, mX + 25f, ey + 1f, Color(245, 245, 252, (240 * a).toInt()).rgb)
            ey += eFont.FONT_HEIGHT + 12f
            for (entry in entries) {
                RoundedUtil.drawRound(mX + 22f, ey + eFont.FONT_HEIGHT / 2f - 1.5f, 3f, 3f, 1.5f, withAlpha(accent, (180 * a).toInt()))
                eFont.drawStringWithShadow(entry, mX + 32f, ey, Color(205, 208, 220, (210 * a).toInt()).rgb)
                ey += eFont.FONT_HEIGHT + 5f
            }
            ey += 8f
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    override fun mouseClicked(mouseX: Int, mouseY: Int, btn: Int) {
        if (btn != 0 || exitTarget != null) return
        if (showChangelog) { showChangelog = false; return }
        if (updVisible && mouseWithinBounds(mouseX, mouseY, updBtnX, updBtnY, updBtnX + updBtnW, updBtnY + updBtnH)) {
            when (ClientUpdater.status) {
                ClientUpdater.Status.AVAILABLE -> {
                    if (ClientUpdater.downloadUrl.isEmpty()) MiscUtils.showURL(ClientUpdater.htmlUrl)
                    else ClientUpdater.startDownloadAsync()
                }
                ClientUpdater.Status.READY_RESTART -> ClientUpdater.applyAndRestart()
                ClientUpdater.Status.FAILED -> ClientUpdater.checkAsync(force = true)
                ClientUpdater.Status.UP_TO_DATE, ClientUpdater.Status.CHECKING, ClientUpdater.Status.DOWNLOADING ->
                    MiscUtils.showURL(ClientUpdater.htmlUrl)
                else -> ClientUpdater.checkAsync(force = true)
            }
            return
        }
        if (mouseWithinBounds(mouseX, mouseY, clBtnX, clBtnY, clBtnX + clBtnW, clBtnY + clBtnH)) {
            showChangelog = true; return
        }
        val targets = arrayOf<GuiScreen>(
            GuiWorldList(this),
            GuiServerList(this),
            GuiAltManager(this),
            GuiThemeSelect(this)
        )
        for (i in targets.indices) {
            if (mouseWithinBounds(mouseX, mouseY, innerX, navBtnY[i], innerX + innerW, navBtnY[i] + navBtnH)) {
                exitTarget = targets[i]; exitAnim = 0f; return
            }
        }
        if (mouseWithinBounds(mouseX, mouseY, innerX, botY1, innerX + innerW, botY1 + botH)) {
            exitTarget = GuiOptions(this, mc.gameSettings); exitAnim = 0f; return
        }
        if (mouseWithinBounds(mouseX, mouseY, innerX, botY2, innerX + innerW, botY2 + botH)) {
            mc.shutdown()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (showChangelog) showChangelog = false
    }

    override fun doesGuiPauseGame() = false

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun drawFooterBtn(
        x: Float, y: Float, w: Float, h: Float, label: String,
        hov: Float, enter: Float, accent: Color, danger: Boolean, font: GameFontRenderer
    ) {
        val slide = enter
        val dx = x - (1f - slide) * 12f
        val r = 7f
        RoundedUtil.drawRound(dx, y, w, h, r, Color(14, 15, 22, (200 * slide).toInt()))
        if (danger) {
            RoundedUtil.drawGradientRound(
                dx, y, w, h, r,
                Color(160, 40, 50, ((20 + hov * 90) * slide).toInt()),
                Color(210, 60, 70, ((30 + hov * 110) * slide).toInt()),
                Color(120, 30, 40, ((15 + hov * 70) * slide).toInt()),
                Color(190, 50, 60, ((25 + hov * 100) * slide).toInt())
            )
        } else {
            RoundedUtil.drawGradientRound(
                dx, y, w, h, r,
                withAlpha(accent, ((16 + hov * 80) * slide).toInt()),
                withAlpha(accent, ((28 + hov * 110) * slide).toInt()),
                withAlpha(accent, ((12 + hov * 60) * slide).toInt()),
                withAlpha(accent, ((22 + hov * 90) * slide).toInt())
            )
        }
        val outline = if (danger)
            Color(200, 70, 80, ((40 + hov * 120) * slide).toInt())
        else
            Color(accent.red, accent.green, accent.blue, ((35 + hov * 110) * slide).toInt())
        RoundedUtil.drawRoundOutline(dx, y, w, h, r, 1f, Color(0, 0, 0, 0), outline)

        val tCol = if (danger)
            blendColor(Color(160, 145, 150, (220 * slide).toInt()), Color(255, 130, 130, (255 * slide).toInt()), hov)
        else
            blendColor(Color(155, 158, 172, (220 * slide).toInt()), Color(250, 250, 255, (255 * slide).toInt()), hov)
        font.drawStringWithShadow(label, dx + 12f + hov * 3f, y + h / 2f - font.FONT_HEIGHT / 2f, tCol.rgb)
    }

    private fun drawUpdateBadge(
        sw: Float, pad: Float, easeIn: Float, accent: Color, accent2: Color,
        font: GameFontRenderer, mouseX: Int, mouseY: Int
    ) {
        val st = ClientUpdater.status
        updVisible = st != ClientUpdater.Status.IDLE
        if (!updVisible) return

        val label = when (st) {
            ClientUpdater.Status.CHECKING -> "Checking…"
            ClientUpdater.Status.UP_TO_DATE -> "Up to date"
            ClientUpdater.Status.AVAILABLE -> "Update ${ClientUpdater.latestVersion}"
            ClientUpdater.Status.DOWNLOADING -> "Downloading ${(ClientUpdater.progress * 100).toInt()}%"
            ClientUpdater.Status.READY_RESTART -> "Restart to apply"
            ClientUpdater.Status.FAILED -> "Update failed — retry"
            else -> "Check update"
        }

        updBtnH = font.FONT_HEIGHT + 10f
        updBtnW = font.getStringWidth(label) + 20f
        updBtnX = sw - pad - updBtnW + (1f - easeIn) * 36f
        updBtnY = clBtnY + clBtnH + 8f

        hoverAnims[7] = lerp(
            hoverAnims[7],
            if (mouseWithinBounds(mouseX, mouseY, updBtnX, updBtnY, updBtnX + updBtnW, updBtnY + updBtnH) && !showChangelog) 1f else 0f,
            0.2f
        )
        val h = hoverAnims[7]
        val hot = st == ClientUpdater.Status.AVAILABLE || st == ClientUpdater.Status.READY_RESTART
        val baseA = if (hot) 220 else 180

        RoundedUtil.drawRound(updBtnX, updBtnY, updBtnW, updBtnH, 7f, Color(10, 11, 16, baseA))
        RoundedUtil.drawGradientRound(
            updBtnX, updBtnY, updBtnW, updBtnH, 7f,
            withAlpha(if (hot) accent else accent2, (30 + h * 90).toInt()),
            withAlpha(accent, (50 + h * 110).toInt()),
            withAlpha(accent2, (18 + h * 70).toInt()),
            withAlpha(if (hot) accent else accent2, (35 + h * 90).toInt())
        )
        RoundedUtil.drawRoundOutline(
            updBtnX, updBtnY, updBtnW, updBtnH, 7f, 1f, Color(0, 0, 0, 0),
            Color(accent.red, accent.green, accent.blue, (50 + h * 120).toInt())
        )

        if (st == ClientUpdater.Status.DOWNLOADING && ClientUpdater.progress > 0f) {
            val barW = (updBtnW - 8f) * ClientUpdater.progress
            RoundedUtil.drawRound(updBtnX + 4f, updBtnY + updBtnH - 4f, barW, 2f, 1f, withAlpha(accent, 220))
        }

        font.drawStringWithShadow(
            label, updBtnX + 10f, updBtnY + 5f,
            blendColor(
                if (hot) Color(220, 230, 255, 200) else Color(190, 192, 205, 160),
                Color(250, 250, 255, 240),
                h
            ).rgb
        )
    }

    /** Stencil-clipped round head (same pattern as DynamicIsland / TargetHUD heads). */
    private fun drawRoundHead(x: Float, y: Float, size: Float, radius: Float) {
        // Main menu has no thePlayer — resolve skin from session instead.
        val skin = resolveSessionSkin()
        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x, y, x + size, y + size, radius)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        GlStateManager.color(1f, 1f, 1f, 1f)
        RenderUtils.drawHead(skin, x.toInt(), y.toInt(), size.toInt(), size.toInt(), Color.WHITE.rgb)
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
    }

    private fun resolveSessionSkin(): ResourceLocation {
        mc.thePlayer?.skin?.let { return it }
        val profile = mc.session.profile
        val id: UUID? = profile?.id
        if (id != null) {
            mc.netHandler?.getPlayerInfo(id)?.locationSkin?.let { return it }
        }
        val name = mc.session.username
        if (!name.isNullOrEmpty()) {
            return AbstractClientPlayer.getLocationSkin(name)
        }
        return if (id != null) DefaultPlayerSkin.getDefaultSkin(id) else DefaultPlayerSkin.getDefaultSkinLegacy()
    }

    /** Thin ping-pong laser (no white tip). */
    private fun drawLaserLine(x: Float, y: Float, w: Float, accent: Color, accent2: Color, t: Float) {
        val h = 1.5f
        RoundedUtil.drawRound(x, y, w, h, 0.75f, Color(255, 255, 255, 10))
        RoundedUtil.drawGradientHorizontal(
            x, y, w, h, 0.75f,
            withAlpha(accent, 14),
            withAlpha(accent2, 14)
        )

        val laserW = (w * 0.22f).coerceIn(24f, 48f)
        val cycle = (t * 0.85f) % 2f
        val p = if (cycle < 1f) cycle else 2f - cycle
        val lx = x + p * (w - laserW)

        RoundedUtil.drawGradientHorizontal(
            lx, y, laserW, h, 0.75f,
            withAlpha(accent, 0),
            withAlpha(accent, 210)
        )
        RoundedUtil.drawGradientHorizontal(
            lx + laserW * 0.4f, y, laserW * 0.6f, h, 0.75f,
            withAlpha(accent, 230),
            withAlpha(accent2, 0)
        )
    }

    /**
     * Ultra-smooth orbit: filled ribbon (not GL lines) + layered glow.
     * Tip fade uses a smoothstep curve so ends dissolve cleanly.
     */
    private fun drawSmoothOrbit(
        cx: Float, cy: Float, rx: Float, ry: Float,
        startDeg: Float, sweepDeg: Float, color: Color, alpha: Int
    ) {
        if (alpha < 2) return
        val segs = 280
        // Soft outer glow
        drawOrbitRibbon(cx, cy, rx, ry, startDeg, sweepDeg, withAlpha(color, (alpha * 0.18f).toInt()), 5.5f, segs)
        drawOrbitRibbon(cx, cy, rx, ry, startDeg, sweepDeg, withAlpha(color, (alpha * 0.35f).toInt()), 3.2f, segs)
        // Crisp core
        drawOrbitRibbon(cx, cy, rx, ry, startDeg, sweepDeg, withAlpha(color, alpha), 1.35f, segs)
        // Tiny bright core
        drawOrbitRibbon(cx, cy, rx, ry, startDeg, sweepDeg, withAlpha(color, (alpha * 0.55f).toInt()), 0.7f, segs)
    }

    private fun drawOrbitRibbon(
        cx: Float, cy: Float, rx: Float, ry: Float,
        startDeg: Float, sweepDeg: Float, color: Color, thickness: Float, segments: Int
    ) {
        val start = Math.toRadians(startDeg.toDouble())
        val sweep = Math.toRadians(sweepDeg.toDouble())
        val cr = color.red / 255f
        val cg = color.green / 255f
        val cb = color.blue / 255f
        val ca = color.alpha / 255f
        val half = thickness * 0.5f

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            // Smooth tip fade (smoothstep)
            val tipRaw = when {
                t < 0.18f -> t / 0.18f
                t > 0.82f -> (1f - t) / 0.18f
                else -> 1f
            }
            val tip = tipRaw * tipRaw * (3f - 2f * tipRaw)

            val ang = start + sweep * t
            val cosA = cos(ang)
            val sinA = sin(ang)
            val px = cx + cosA * rx
            val py = cy + sinA * ry

            // Ellipse tangent → outward normal (screen-space)
            val tx = (-sinA * rx).toFloat()
            val ty = (cosA * ry).toFloat()
            val len = kotlin.math.sqrt((tx * tx + ty * ty).toDouble()).toFloat().coerceAtLeast(0.0001f)
            val nx = -ty / len
            val ny = tx / len

            val a = ca * tip
            GL11.glColor4f(cr, cg, cb, a)
            GL11.glVertex2d((px + nx * half).toDouble(), (py + ny * half).toDouble())
            GL11.glColor4f(cr, cg, cb, a)
            GL11.glVertex2d((px - nx * half).toDouble(), (py - ny * half).toDouble())
        }
        GL11.glEnd()

        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glPopMatrix()
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOutCubic(x: Float): Float { val t = x.coerceIn(0f, 1f); return 1f - (1f - t) * (1f - t) * (1f - t) }
    private fun easeInCubic(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * t }

    private fun blendColor(a: Color, b: Color, t: Float): Color {
        val i = 1f - t
        return Color(
            (a.red * i + b.red * t).toInt().coerceIn(0, 255),
            (a.green * i + b.green * t).toInt().coerceIn(0, 255),
            (a.blue * i + b.blue * t).toInt().coerceIn(0, 255),
            (a.alpha * i + b.alpha * t).toInt().coerceIn(0, 255),
        )
    }

    private fun withAlpha(c: Color, a: Int) = Color(c.red, c.green, c.blue, a.coerceIn(0, 255))

    private data class Star(val fx: Float, val fy: Float, val size: Float, val phase: Float)

    companion object {
        val CHANGELOG = listOf(
            "26.0.2" to listOf(
                "Make BlockIn faster",
                "Improve Rotation BedDenfender",
                "Fix Scaffold Switch Block flagging"
            ),
        )
    }
}
