package net.ccbluex.liquidbounce.features.module.modules.client.impl

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.features.module.modules.client.TargetHUD
import net.ccbluex.liquidbounce.features.module.modules.combat.InfiniteAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.SilentAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Flight
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.other.Disabler
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts.*
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.animation.Animation
import net.ccbluex.liquidbounce.utils.animation.Easing
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.skin
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ShaderUtil
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object DynamicIsland : MinecraftInstance() {

    // ── Public state read by other modules ─────────────────────────────────
    var disabler: Float? = null
    var mainTarget: EntityLivingBase? = null

    // ── Cached render-side constants ───────────────────────────────────────
    /** Color(255,255,255,255).rgb — was allocated dozens of times per frame. */
    private const val WHITE = -1
    private val BAR_BG     = Color(50, 50, 50, 255).rgb
    private val TH_BAR_BG  = Color(0, 0, 0, 200).rgb
    private val TH_BORDER  = Color.WHITE.rgb

    /** DecimalFormat is not thread-safe, but the HUD draw path is single-
     *  thread (render thread only) so hoisting these saves ~3 allocations
     *  every frame in the BedAura / Flight / Scaffold / Speed branches. */
    private val FMT_BPS   = DecimalFormat("#.##")
    private val FMT_INT0  = DecimalFormat("0",   DecimalFormatSymbols(Locale.ENGLISH))
    private val FMT_SPEED = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH))

    // ── Target layout + animation drivers ──────────────────────────────────
    private var posX  = 0F
    private var posY  = 0F
    private var posX2 = 0F
    private var posY2 = 0F
    private var alpha = 0

    private val animPosX  = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosY  = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosX2 = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animPosY2 = Animation(Easing.EASE_IN_OUT_BACK, 500)
    private val animAlpha = Animation(Easing.EASE_IN_OUT_BACK, 500)

    private var prevBlock = 0F

    fun draw(event: Render2DEvent) {
        // Step animations once.
        animPosX.run(posX.toDouble())
        animPosY.run(posY.toDouble())
        animPosX2.run(posX2.toDouble())
        animPosY2.run(posY2.toDouble())
        animAlpha.run(alpha.toDouble())

        // Snapshot interpolated values so we don't hit the Animation getter
        // (which does easing math) 20+ times per frame.
        val x1 = animPosX.value.toFloat()
        val y1 = animPosY.value.toFloat()
        val x2 = animPosX2.value.toFloat()
        val y2 = animPosY2.value.toFloat()
        val aInt = animAlpha.value.toInt()
        GL11.glPushMatrix()
        if (Interface.isBlurActive()) {
            BlurUtils.blurAreaRounded(x1 + 1F, y1 + 1F, x2 - 1F, y2 - 1F, 8F, 10F)
        }
        RenderUtils.drawBloomRoundedRect(
            x1, y1, x2, y2, 6F, 1F,
            Color(10, 10, 10, aInt),
            RenderUtils.ShaderBloom.BLOOMONLY
        )

        GlStateManager.resetColor()
        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x1, y1, x2, y2, 0F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)

        val player    = mc.thePlayer
        val sr        = event.scaledResolution
        val halfWidth = sr.scaledWidth / 2F

        val heldStack    = player.heldItem
        val holdingBlock = heldStack != null && heldStack.item is ItemBlock
        val blockPad     = if (holdingBlock) 15F else 13F

        // Resolve who the island should "talk about" this frame.
        mainTarget = when {
            mc.currentScreen is GuiChat                              -> player
            InfiniteAura.state && InfiniteAura.lastTarget != null    -> InfiniteAura.lastTarget
            Interface.attackTarget != null                           -> Interface.attackTarget
            else                                                     -> null
        }
        val target = mainTarget

        when {
            !TargetHUD.state && target != null && Interface.targetHudDN.get()-> {
                if ((KillAura.state || SilentAura.state) && mc.currentScreen !is GuiChat) {
                    var rowY = 0F
                    var widestName = 0F
                    for (entity in mc.theWorld.loadedEntityList) {
                        if (!EntityUtils.isSelected(entity, true)) continue
                        val ent = entity as? EntityLivingBase ?: continue
                        val dist = player.getDistanceToEntityBox(ent)
                        val inKa = KillAura.state   && dist <= KillAura.discoverRangeValue.get()
                        val inSa = SilentAura.state && dist <= SilentAura.discoverValue.get()
                        if (!inKa && !inSa) continue

                        val nameW = SFBold35.getStringWidth(ent.name).toFloat()
                        if (nameW > widestName) widestName = nameW

                        if (inKa) { drawTH(rowY, widestName, ent, ent.name, sr); rowY += 30F }
                        if (inSa) { drawTH(rowY, widestName, ent, ent.name, sr); rowY += 30F }
                    }
                } else {
                    val name = target.name
                    drawTH(0F, SFBold35.getStringWidth(name).toFloat(), target, name, sr)
                }
            }

            // ── Flight: title + BPS subtitle ───────────────────────────────
            Flight.state -> {
                val title = "Flight"
                val sub   = "Speed : ${FMT_BPS.format(MovementUtils.bps)} BPS"
                drawTwoLineCentered(title, sub, halfWidth, blockPad, x1, y1, includeTail = false)
            }

            // ── Speed countdown ────────────────────────────────────────────
            Speed.state && Speed.flagged && Speed.flagCheck.get() -> {
                val remainSec = (Speed.timerMS.time + Speed.flagMS.get() - System.currentTimeMillis()) / 1000.0
                val text  = "Disable Speed : ${FMT_SPEED.format(remainSec)}"
                val textW = SFBold35.getStringWidth(text)
                set(halfWidth - textW / 2F - 9F, 11F,
                    halfWidth + textW / 2F + 9F,
                    14F + SFBold35.height + 12F)
                SFBold35.drawStringWithShadow(text, x1 + 10F, y1 + 9F, WHITE)
            }

            // ── BedAura: title + progress bar ──────────────────────────────
            BedAura.state && !BedAura.delay.hasTimePassed(1000) -> {
                val title    = "BedAura"
                val blockTxt = if (BedAura.pos == null) "None" else BlockUtils.getBlock(BedAura.pos)!!.localizedName
                val sub      = "Blocks : $blockTxt | Progress : ${FMT_INT0.format(BedAura.animation.value * 100)}%"
                val titleW   = SFBold35.getStringWidth(title)
                set(halfWidth - titleW / 2F - 60F, 11F,
                    halfWidth + titleW / 2F + 60F,
                    14F + SFBold35.height + SFBold30.height + 2F + 12F + 7F)
                SFBold35.drawCenteredString(title, halfWidth, y1 + 9F, WHITE)
                SFBold30.drawCenteredString(sub, halfWidth, y1 + 9F + SFBold35.height + 2F, WHITE)
                drawProgressBar(x1, x2, y2, BedAura.animation.value.toFloat())
            }

            // ── Scaffold: title + BPS subtitle + block icon + progress ─────
            Scaffold.state && player.inventory.getCurrentItem() != null && holdingBlock -> {
                val title = "Scaffold"
                val sub   = "${Scaffold.blockAmount - Scaffold.placeTick} Blocks left - ${FMT_BPS.format(MovementUtils.bps)} BPS"
                val titleW   = SFBold35.getStringWidth(title)
                set(halfWidth - titleW / 2F - 60F, 11F,
                    halfWidth + titleW / 2F + 60F,
                    14F + SFBold35.height + SFBold30.height + 2F + 12F + 7F)
                SFBold35.drawCenteredString(title, halfWidth, y1 + 9F, WHITE)
                SFBold30.drawCenteredString(sub, halfWidth, y1 + 9F + SFBold35.height + 2F, WHITE)
                RenderUtils.renderItemIcon(halfWidth.toInt() - 37, 15, heldStack)
                drawProgressBar(x1, x2, y2, prevBlock)
                prevBlock = prevBlock.animSmooth(
                    (Scaffold.blockAmount.toFloat() - Scaffold.placeTick.toFloat()) / 64F,
                    0.001F
                )
            }

            // ── Idle: client / player / server info ────────────────────────
            else -> {
                val text  = "CrossSine §f| ${player.name} | ${ServerUtils.getRemoteIp()} | ${Minecraft.getDebugFPS()}fps"
                val textW = SFBold35.getStringWidth(text)
                set(halfWidth - textW / 2F - 9F, 11F,
                    halfWidth + textW / 2F + 9F,
                    14F + SFBold35.height + 12F)
                SFBold35.drawStringWithShadow(text, x1 + 10F, y1 + 9F, ClientTheme.getColorWithAlpha(0, 255).rgb)
            }
        }

        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()
        GL11.glPopMatrix()
    }

    /**
     * Common layout used by Flight / Scaffold / SlotUtils branches —
     * a centred title with a subtitle line beneath it, sized to whichever
     * line is wider. `includeTail` adds an extra 7px of vertical space at
     * the bottom (Scaffold's progress bar needs room).
     */
    private fun drawTwoLineCentered(
        title: String, sub: String,
        halfWidth: Float, blockPad: Float,
        x1: Float, y1: Float,
        includeTail: Boolean,
        titleOffsetX: Float = 0F
    ) {
        val titleW = SFBold35.getStringWidth(title)
        val subW   = SFBold30.getStringWidth(sub)
        val subWider = titleW < subW
        val halfTextW = (if (subWider) subW else titleW) / 2F
        val adjust = if (subWider) 2F else 0F
        val tail = if (includeTail) 7F else 0F
        set(halfWidth - halfTextW - blockPad + adjust, 11F,
            halfWidth + halfTextW + blockPad - adjust,
            14F + SFBold35.height + 12F + 4F + SFBold30.height + tail)
        SFBold35.drawCenteredString(title, halfWidth + titleOffsetX, y1 + 9F, WHITE)
        SFBold30.drawStringWithShadow(sub, x1 + 10F, y1 + 9F + SFBold30.height + 7F, WHITE)
    }

    /** Bottom progress bar shared by BedAura + Scaffold. */
    private fun drawProgressBar(x1: Float, x2: Float, y2: Float, frac: Float) {
        val left  = x1 + 8F
        val right = x2 - 8F
        val top   = y2 - 12F
        val bot   = y2 - 5F
        RenderUtils.drawRoundedRect(left, top, right, bot, 3.5F, BAR_BG)
        RenderUtils.drawGradientRoundedRect(
            left, top,
            x1 + (x2 - x1 - 8F) * frac, bot,
            3, ClientTheme.getColor(0).rgb, ClientTheme.getColor(90).rgb
        )
    }

    private fun set(startX: Float, startY: Float, endX: Float, endY: Float, alpha: Int = 180) {
        posX  = startX
        posY  = startY
        posX2 = endX
        posY2 = endY
        this.alpha = alpha
    }

    private fun drawTH(posY: Float, stringWidth: Float, target: EntityLivingBase, string: String, sr: ScaledResolution) {
        val halfWidth  = sr.scaledWidth  / 2F
        val halfHeight = sr.scaledHeight / 2F
        set(halfWidth + 60F,
            halfHeight - 18F - (posY / 2),
            halfWidth + 60F + 60F + stringWidth + 5F,
            halfHeight + 18F + (posY / 2),
            100)

        // Snapshot animated values once for this draw.
        val x1 = animPosX.value.toFloat()
        val y1 = animPosY.value.toFloat()
        val x2 = animPosX2.value.toFloat()

        SFBold35.drawCenteredString(
            string,
            x1 + ((x2 - x1) / 2F) + 15F,
            y1 + 7F + posY,
            WHITE
        )

        GL11.glPushMatrix()
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(x1 + 6F, y1 + 5F + posY, x1 + 32F, y1 + 31F + posY, 6F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        RenderUtils.drawHead(target.skin, x1.toInt() + 6, y1.toInt() + 5 + posY.toInt(), 26, 26, Color.WHITE.rgb)
        GlStateManager.resetColor()
        Stencil.dispose()
        GL11.glPopMatrix()

        // NOTE: signature kept identical to original — the 3rd/4th args of
        // this drawRoundedRect overload are (width, height) for the original
        // call site, not (x2, y2). Preserving as-is to avoid visual drift.
        RenderUtils.drawRoundedRect(
            x1 + 40F, y1 + 24.5F + posY,
            ((x2 - 5F) - (x1 + 40F)) - 4F, 4.5F,
            2F, TH_BAR_BG, 1F, TH_BORDER
        )
        RenderUtils.drawGradientRoundedRect(
            x1 + 40F, y1 + 24.5F + posY,
            x1 + 30F + ((x2 - 5F) - (x1 + 30F)) * (target.health / target.maxHealth) - 4F,
            y1 + 29F + posY,
            2, ClientTheme.getColor().rgb, ClientTheme.getColor(90).rgb
        )
    }
}