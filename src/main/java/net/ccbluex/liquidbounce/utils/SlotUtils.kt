package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.awt.Color

object SlotUtils : Listenable {
    private var prevSlot = -1
    private var spoofSlot = 0
    var spoofing = false
    var changed = false
    var module = ""
    private var animProgress = 0F
    fun setSlot(slot: Int, spoof: Boolean, module: String) {
        if (!changed) {
            prevSlot = mc.thePlayer.inventory.currentItem
            changed = true
        }
        spoofSlot = slot
        mc.thePlayer.inventory.currentItem = slot
        spoofing = spoof
        this.module = module
    }

    fun stopSet() {
        if (changed) {
            if (prevSlot != -1) {
                mc.thePlayer.inventory.currentItem = prevSlot
                prevSlot = -1
            }
            spoofing = false
            changed = false
        }
        module = ""
    }

    fun getSlot(): Int {
        return if (spoofing) prevSlot else mc.thePlayer.inventory.currentItem
    }

    fun getStack(): ItemStack? {
        return if (spoofing) mc.thePlayer.inventory.getStackInSlot(prevSlot) else mc.thePlayer.inventory.getCurrentItem()
    }

    fun setPrevSlot(slot: Int) {
        prevSlot = slot
    }

    override fun handleEvents(): Boolean {
        return true
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        animProgress += (0.0075F * 0.25F * deltaTime * if (changed && prevSlot != mc.thePlayer.inventory.currentItem) 1F else -1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        if (animProgress <= 0F) return
        val alpha = EaseUtils.easeInOutCirc(animProgress.toDouble()).toFloat().coerceIn(0F, 1F)
        val pop = EaseUtils.easeInOutCirc(animProgress.toDouble()).toFloat()
        val scale = (1.2F - pop * 0.20F).coerceIn(0.5F, 1.15F)

        val itemStack = if (spoofSlot in 0..9) mc.thePlayer.inventory.getStackInSlot(spoofSlot) else null

        val useFbo = OpenGlHelper.isFramebufferEnabled()

        if (itemStack != null && alpha > 0.01F && useFbo) renderItemToFbo(itemStack)

        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        val cx = width / 2.0
        val cy = (height - 72F).toDouble()

        GlStateManager.pushMatrix()
        GlStateManager.translate(cx, cy, 0.0)
        GlStateManager.scale(scale.toDouble(), scale.toDouble(), 1.0)
        GlStateManager.translate(-cx, -cy, 0.0)

        renderRect(alpha)
        GlStateManager.resetColor()
        if (itemStack != null && alpha > 0.01F) {
            if (useFbo) {
                compositeItemFbo(alpha)
            } else {
                // Fast Render fallback — direct vanilla draw, no alpha fade
                // on the item icon itself.
                val itemY = (height - 80F).toInt()
                val itemX = if (itemStack.item is ItemBlock) width / 2 - 30 else width / 2 - 9
                RenderUtils.renderItemIcon(itemX, itemY, itemStack)
            }
            GlStateManager.resetColor()
        }

        GlStateManager.popMatrix()
        GlStateManager.resetColor()
    }


    private fun renderRect(alpha: Float) {
        val itemStack = if (spoofSlot in 0..9) mc.thePlayer.inventory.getStackInSlot(spoofSlot) else null
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        val rectTop = height - 82F
        val rectBottom = rectTop + 20F
        if (animProgress > 0F) {
            if (itemStack != null && itemStack.item is ItemBlock) {
                val string: String =
                    "Amount: " + if (Scaffold.state) Scaffold.blockAmount - Scaffold.placeTick else itemStack.stackSize
                val stringWidth = Fonts.SFBold35.getStringWidth(string) + if (itemStack.stackSize < 10) 3F else 0F
                if (Interface.isBlurActive()) {
                    BlurUtils.blurAreaRounded(
                        width / 2F + -35F,
                        rectTop,
                        width / 2F + -35F + 32F + stringWidth,
                        rectBottom, 5F, 10F * alpha
                    )
                }
                RenderUtils.drawBloomRoundedRect(
                    width / 2F + -35F,
                    rectTop,
                    width / 2F + -35F + 32F + stringWidth,
                    rectBottom,
                    3F,
                    1F,
                    Color(0, 0, 0, (120 * alpha).toInt()),
                    RenderUtils.ShaderBloom.BLOOMONLY
                )
                if (alpha >= 0.1F) {
                    Fonts.SFBold35.drawCenteredString(
                        string, width / 2 + 15F, rectTop + 7F,
                        Color(255, 255, 255, (255 * alpha).toInt()).rgb, true
                    )
                }
            } else {
                if (Interface.isBlurActive()) {
                    BlurUtils.blurAreaRounded(
                        width / 2F + -11F,
                        rectTop,
                        width / 2F + -11F + 20F,
                        rectBottom,
                        5F,
                        10F * alpha
                    )
                }
                RenderUtils.drawBloomRoundedRect(
                    width / 2F + -11F,
                    rectTop,
                    width / 2F + -11F + 20F,
                    rectBottom,
                    3F,
                    1F,
                    Color(0, 0, 0, (120 * alpha).toInt()),
                    RenderUtils.ShaderBloom.BLOOMONLY
                )
            }
        }
    }

    private fun renderItemToFbo(itemStack: ItemStack) {
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        val itemY = (height - 80F).toInt()
        val itemX = if (itemStack.item is ItemBlock) width / 2 - 30 else width / 2 - 9

        val fbo = ensureFbo() ?: run {
            RenderUtils.renderItemIcon(itemX, itemY, itemStack)
            return
        }
        fbo.framebufferClear()
        fbo.bindFramebuffer(false)
        RenderUtils.renderItemIcon(itemX, itemY, itemStack)
        mc.framebuffer.bindFramebuffer(false)
    }

    private fun compositeItemFbo(alpha: Float) {
        val fbo = itemFbo ?: return
        val a = (255F * alpha).toInt().coerceIn(0, 255)
        val f2 = fbo.framebufferWidth.toDouble() / fbo.framebufferTextureWidth.toDouble()
        val f3 = fbo.framebufferHeight.toDouble() / fbo.framebufferTextureHeight.toDouble()

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)
        GlStateManager.enableTexture2D()
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
        fbo.bindFramebufferTexture()
        GlStateManager.color(1F, 1F, 1F, alpha)

        val tess = Tessellator.getInstance()
        val wr = tess.worldRenderer
        val scaled = ScaledResolution(mc)
        val sw = scaled.scaledWidth.toDouble()
        val sh = scaled.scaledHeight.toDouble()
        // UVs flipped vertically (FBO texture is bottom-up vs. GUI ortho),
        // matching BlurUtils' composite convention.
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        wr.pos(0.0, sh, 0.0).tex(0.0, 0.0).color(255, 255, 255, a).endVertex()
        wr.pos(sw, sh, 0.0).tex(f2, 0.0).color(255, 255, 255, a).endVertex()
        wr.pos(sw, 0.0, 0.0).tex(f2, f3).color(255, 255, 255, a).endVertex()
        wr.pos(0.0, 0.0, 0.0).tex(0.0, f3).color(255, 255, 255, a).endVertex()
        tess.draw()

        fbo.unbindFramebufferTexture()
        GlStateManager.enableAlpha()
        GlStateManager.enableDepth()
        GlStateManager.depthMask(true)
        GlStateManager.disableBlend()
        GlStateManager.color(1F, 1F, 1F, 1F)
    }

    private var itemFbo: Framebuffer? = null
    private var fboWidth = 0
    private var fboHeight = 0

    private fun ensureFbo(): Framebuffer? {
        if (!OpenGlHelper.isFramebufferEnabled()) return null
        val dw = mc.displayWidth
        val dh = mc.displayHeight
        if (dw <= 0 || dh <= 0) return null
        val existing = itemFbo
        if (existing == null || fboWidth != dw || fboHeight != dh) {
            existing?.deleteFramebuffer()
            val fbo = Framebuffer(dw, dh, true)
            fbo.setFramebufferColor(0F, 0F, 0F, 0F)
            itemFbo = fbo
            fboWidth = dw
            fboHeight = dh
            return fbo
        }
        return existing
    }
}