package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.features.module.modules.combat.InfiniteAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.SilentAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.render.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils.*
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow

@ModuleInfo("TargetHUD", ModuleCategory.CLIENT, loadConfig = false)
object TargetHUD : HUDModule() {
    val mode = ListValue("Mode", arrayOf("CrossSine", "Modern", "Simple", "RavenB4"), "CrossSine")
    val followTarget = BoolValue("Follow-Target", false)
    private var attackTarget: EntityLivingBase? = null
    private val targetTimer = TimerMS()
    private var mainTarget: EntityLivingBase? = null
    private var animProgress = 0F
    private var easingHealth = 0F
    private val posY2: Float = -40F
    private val posX2: Float = 40F
    private var targetScreenX = 0f
    private var targetScreenY = 0f
    private var visible = false


    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (attackTarget != event.targetEntity) {
            if (EntityUtils.isSelected(event.targetEntity, true)) {
                attackTarget = event.targetEntity as EntityLivingBase
            }
        }
        targetTimer.reset()
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val actualTarget = if (InfiniteAura.lastTarget != null) InfiniteAura.lastTarget
        else if (mc.currentScreen is GuiChat) mc.thePlayer
        else if (KillAura.state && KillAura.currentTarget != null) KillAura.currentTarget
        else if (SilentAura.state && SilentAura.target != null) SilentAura.target
        else attackTarget
        animProgress += (0.0075F * 0.5F * deltaTime * if (actualTarget != null) -1F else 1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        if (actualTarget != null) {
            if (mainTarget != actualTarget) {
                mainTarget = actualTarget
                easingHealth = actualTarget.health
            }
        } else if (animProgress >= 1F)
            mainTarget = null
        if (mainTarget == null) {
            easingHealth = 0F
        }
        if (targetTimer.hasTimePassed(500)) {
            attackTarget = null
        }

        // ── Pop-up animation pipeline ───────────────────────────────────
        // Pivot is computed *directly* from the draw origin + box size for
        // the current mode, NOT from `(x1, x2)` written by updateBounds().
        // Reason: `HUDModule.updateBounds` always sets
        //     x2 = posX + width2,  y2 = posY + height2
        // i.e. it offsets x2/y2 by the dragged hotkey-mode position even
        // when the caller passed an unrelated origin via width1/height
        // (which is exactly what the follow-target draw does — width1 =
        // posX2 = 40, but x2 still reads posX = wherever the user dragged
        // the fixed HUD). That coordinate mix made the pivot in follow
        // mode land far away from the actual HUD box → the popup looked
        // like it was flying off in a random direction every time it
        // appeared. Computing pivot directly here sidesteps that bug
        // entirely without disturbing the bounds logic that drag() and
        // hover-outline rely on.
        val percent = EaseUtils.easeInBack(animProgress.toDouble())
        val scale   = (1.0 - percent).coerceAtLeast(0.0)
        val target  = mainTarget

        if (target != null) {
            easingHealth += ((target.health - easingHealth) / 2.0F.pow(10.0F - 3)) * deltaTime
            easingHealth = easingHealth.coerceIn(0F, 20F)

            // Resolve draw mode for THIS frame. If follow is requested
            // but the target is currently off-screen, suppress the draw
            // entirely instead of teleporting back to the fixed HUD
            // position — that was the "popup teleport" the user reported
            // when a target walked out of view.
            val followMode = followTarget.get() && mc.currentScreen !is GuiChat
            val canFollow  = followMode && visible && target != mc.thePlayer
            val skipDraw   = followMode && !canFollow

            if (!skipDraw) {
                val drawX = if (canFollow) posX2 else posX
                val drawY = if (canFollow) posY2 else posY
                val (boxW, boxH) = boxSize(target)
                val pivotX = drawX + boxW / 2F
                val pivotY = drawY + boxH / 2F

                glPushMatrix()
                if (canFollow) {
                    val sr = ScaledResolution(mc)
                    val sx = targetScreenX / mc.displayWidth  * sr.scaledWidth
                    val sy = targetScreenY / mc.displayHeight * sr.scaledHeight
                    glTranslatef(sx, sy, 0f)
                }
                if (!mode.equals("RavenB4")) {
                    // Scale around the box centre: T(P) · S · T(-P)
                    glTranslatef(pivotX, pivotY, 0F)
                    glScaled(scale, scale, scale)
                    glTranslatef(-pivotX, -pivotY, 0F)
                }
                when (mode.get().lowercase()) {
                    "crosssine" -> drawCrossSine()
                    "ravenb4"   -> drawRavenB4()
                    "modern"    -> drawModern()
                    "simple"    -> drawSimple()
                }
                glPopMatrix()
            }
        }

        draw()
        drag()
    }

    /**
     * Logical (pre-scale) size of the HUD box for [target] under the
     * currently selected drawing mode. Mirrors the constants used inside
     * each `draw*()` function — keep these in sync if you change the
     * draw layout. Used purely as the pivot for the pop-up animation,
     * so a small mismatch is cosmetic, not structural.
     */
    private fun boxSize(target: EntityLivingBase): kotlin.Pair<Float, Float> = when (mode.get().lowercase()) {
        "crosssine" -> {
            val nameW = Fonts.font35.getStringWidth(target.name)
            val w = if (nameW > 50F) nameW + 27F else 100F
            kotlin.Pair(6F + w, 33F)
        }
        "modern", "simple" -> kotlin.Pair(40F + Fonts.minecraftFont.getStringWidth(target.name).toFloat(), 24F)
        "ravenb4" -> {
            val font = Fonts.minecraftFont
            val hpW = font.getStringWidth(DecimalFormat("##0.0", DecimalFormatSymbols(Locale.ENGLISH)).format(target.health))
            val nameW = font.getStringWidth(target.displayName.formattedText)
            kotlin.Pair((nameW + hpW + 23).toFloat(), 35F)
        }
        else -> kotlin.Pair(100F, 30F)
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {

        val target = mainTarget ?: return

        val renderManager = mc.renderManager

        val x = target.lastTickPosX + (target.posX - target.lastTickPosX) * mc.timer.renderPartialTicks - renderManager.renderPosX
        val y = target.lastTickPosY + (target.posY - target.lastTickPosY) * mc.timer.renderPartialTicks - renderManager.renderPosY + target.height
        val z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * mc.timer.renderPartialTicks - renderManager.renderPosZ

        val projected = ProjectionUtil.project2D(x, y, z)

        if (projected != null) {
            targetScreenX = projected[0]
            targetScreenY = projected[1]
            visible = true
        } else {
            visible = false
        }
    }

    private fun drawCrossSine() {
        if (Interface.isBlurActive() && !followTarget.get()) {
            BlurUtils.blurAreaRounded(x1, y1, x2, y2, 2F, 10F)
        }
        val fonts = Fonts.font35
        val fonts2 = Fonts.font24
        val decimalFormat3 = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH))
        val string =
            "${decimalFormat3.format(mc.thePlayer.getDistanceToEntity(mainTarget!!))}m - " + if (mc.thePlayer.health > easingHealth) "+${
                decimalFormat3.format(mc.thePlayer.health - easingHealth)
            }" else "-${decimalFormat3.format(easingHealth - mc.thePlayer.health)}"
        val width: Float =
            if (fonts.getStringWidth(mainTarget!!.name) > 50F) fonts.getStringWidth(mainTarget!!.name).toFloat() + 27F else 100F
        ShaderUtil.drawRoundedRect(
            -2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            -2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            6F + width + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            33F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            2F,
            2F,
            Color(0, 0, 0, fadeAlpha(180))
        )
        GlStateManager.enableBlend()
        drawHead(mainTarget!!.skin, 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX).toInt(), 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY).toInt(), 23, 23, Color(255, 255, 255, fadeAlpha(255)).rgb)
        fonts.drawString(mainTarget!!.name, 28F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), 11F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), Color(150, 150, 150, fadeAlpha(255)).rgb)
        fonts2.drawString(
            string,
            width - fonts2.getStringWidth(string) + 2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            23F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            Color(150, 150, 150, fadeAlpha(255)).rgb
        )
        RoundedUtil.drawRound(2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), 27.5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), width, 2F, 1F, Color(0, 0, 0, fadeAlpha(180)))
        RoundedUtil.drawGradientRound(
            2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            27.5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            (width * easingHealth / mainTarget!!.maxHealth),
            2F,
            1F,
            ClientTheme.getColorWithAlpha(0, fadeAlpha(255)),
            ClientTheme.getColorWithAlpha(360, fadeAlpha(255)),
            ClientTheme.getColorWithAlpha(90, fadeAlpha(255)),
            ClientTheme.getColorWithAlpha(180, fadeAlpha(255))
        )
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        updateBounds(
            -2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            -2F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            6F + width,
            33F
            )
    }

    private fun drawModern() {
        if (Interface.isBlurActive() && !followTarget.get()) {
            BlurUtils.blurAreaRounded(x1, y1, x2, y2, 2F, 10F)
        }
        val target = mainTarget ?: return
        val fonts = Fonts.minecraftFont
        val leagth = fonts.getStringWidth(target.name)
        RoundedUtil.drawRound((if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), 40F + leagth, 24F, 2F, Color(0, 0, 0, fadeAlpha(180)))
        drawRect(
            28F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            20F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            28F + ((leagth + 6F) * (easingHealth / target.maxHealth)) + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            21F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            ColorUtils.reAlpha(
                BlendUtils.getHealthColor(target.health, target.maxHealth), fadeAlpha(255)
            ).rgb
        )
        GlStateManager.enableBlend()
        fonts.drawString(target.name, 31F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), 5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), Color(255, 255, 255, fadeAlpha(255)).rgb, true)
        drawHead(target.skin, 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX).toInt(), 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY).toInt(), 20, 20, Color(255, 255, 255, fadeAlpha(255)).rgb)
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        updateBounds(
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            40F + Fonts.minecraftFont.getStringWidth(target.name),
            24F
        )
    }
    private fun drawSimple() {
        if (Interface.isBlurActive() && !followTarget.get()) {
            BlurUtils.blurAreaRounded(x1, y1, x2, y2, 2F, 10F)
        }
        val target = mainTarget ?: return
        val fonts = Fonts.minecraftFont
        val leagth = fonts.getStringWidth(target.name)
        RoundedUtil.drawRound((if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), 40F + leagth, 24F, 2F, Color(0, 0, 0, fadeAlpha(180)))
        drawRect(
            28F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            20F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            28F + ((leagth + 6F) * (easingHealth / target.maxHealth)) + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            21F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            ColorUtils.reAlpha(
                BlendUtils.getHealthColor(target.health, target.maxHealth), fadeAlpha(255)
            ).rgb
        )
        GlStateManager.enableBlend()
        fonts.drawString(target.name, 31F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), 5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), Color(255, 255, 255, fadeAlpha(255)).rgb, true)
        drawHead(target.skin, 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX).toInt(), 2 + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY).toInt(), 20, 20, Color(255, 255, 255, fadeAlpha(255)).rgb)
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        updateBounds(
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            40F + Fonts.minecraftFont.getStringWidth(target.name),
            24F
        )
    }

    private fun drawRavenB4() {
        val target = mainTarget ?: return
        val decimalFormat2 = DecimalFormat("##0.0", DecimalFormatSymbols(Locale.ENGLISH))
        val font = Fonts.minecraftFont
        val hp = decimalFormat2.format(target.health)
        val hplength = font.getStringWidth(decimalFormat2.format(target.health))
        val length = font.getStringWidth(target.displayName.formattedText)
        if (Interface.isBlurActive() && !followTarget.get()) {
            BlurUtils.blurAreaRounded(x1, y1, x2, y2, 4F, 10F)
        }
        GlStateManager.pushMatrix()
        drawRoundedGradientOutlineCorner(
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            length + hplength + 23F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            35F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            2F, 8F,
            ClientTheme.setColor(true, fadeAlpha(255)).rgb,
            ClientTheme.setColor(false, fadeAlpha(255)).rgb
        )
        drawRoundedRect((if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), length + hplength + 23F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX), 35F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY), 4F, Color(0, 0, 0, fadeAlpha(100)).rgb)
        GlStateManager.enableBlend()
        font.drawStringWithShadow(
            target.displayName.formattedText,
            6F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            8F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            Color(255, 255, 255, fadeAlpha(255)).rgb
        )
        font.drawStringWithShadow(
            if (target.health > mc.thePlayer.health) "L" else "W",
            length + hplength + 11.6F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            8F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            if (target.health > mc.thePlayer.health) Color(255, 0, 0, fadeAlpha(255)).rgb else Color(
                0,
                255,
                0,
                fadeAlpha(255)
            ).rgb
        )
        font.drawStringWithShadow(
            hp,
            length + 8F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            8F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            ColorUtils.reAlpha(BlendUtils.getHealthColor(target.health, target.maxHealth), fadeAlpha(255)).rgb
        )
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        drawRoundedRect(
            5.0F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            29.55F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            length + hplength + 18F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            25F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            2F,
            Color(0, 0, 0, fadeAlpha(110)).rgb,
        )
        drawRoundedGradientRectCorner(
            5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            25F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            8F + (easingHealth / 20) * (length + hplength + 10F) + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            29.5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            4F,
            ClientTheme.setColor(true, fadeAlpha(100)).rgb,
            ClientTheme.setColor(false, fadeAlpha(100)).rgb
        )
        drawRoundedGradientRectCorner(
            5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            25F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            8F + (target.health / 20) * (length + hplength + 10F) + (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            29.5F + (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            4F,
            ClientTheme.setColor(true, fadeAlpha(255)).rgb,
            ClientTheme.setColor(false, fadeAlpha(255)).rgb
        )
        GlStateManager.popMatrix()
        updateBounds(
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posX2 else posX),
            (if (followTarget.get() && mc.currentScreen !is GuiChat) posY2 else posY),
            (40F + mc.fontRendererObj.getStringWidth(target.displayName.formattedText)),
            35F
            )
    }

    private fun fadeAlpha(alpha: Int): Int {
        return alpha - (animProgress * alpha).toInt()
    }

    override val tag: String?
        get() = mode.get()
}