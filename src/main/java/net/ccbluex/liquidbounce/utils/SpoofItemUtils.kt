package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.awt.Color

object SpoofItemUtils : Listenable{
    private var spoofSlot = 0
    var spoofing = false
    private var animProgress = 0F
    var render = false
    fun startSpoof(slot: Int, render: Boolean) {
        if (!spoofing) {
            spoofSlot = slot
            spoofing = true
        }
        this.render = render
    }

    fun stopSpoof() {
        if (spoofing) {
            spoofing = false
            for (i in 0..8) {
                if (i == spoofSlot) {
                    mc.thePlayer.inventory.currentItem = i
                }
            }
        }
    }

    fun getSlot(): Int {
        return if (spoofing) spoofSlot else mc.thePlayer.inventory.currentItem
    }

    fun getStack(): ItemStack? {
        return if (spoofing) mc.thePlayer.inventory.getStackInSlot(spoofSlot) else mc.thePlayer.inventory.getCurrentItem()
    }

    fun setSlot(slot: Int) {
        spoofSlot = slot
    }

    override fun handleEvents(): Boolean {
        return true
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        renderRect()
        GlStateManager.resetColor()
        if (mc.thePlayer.inventory.getCurrentItem() != null) {
            renderItem()
            GlStateManager.resetColor()
        }
    }
    private fun renderRect() {
        val itemStack = mc.thePlayer.inventory.getCurrentItem()
        animProgress += (0.0075F * 0.50F * deltaTime * if (spoofing && spoofSlot != mc.thePlayer.inventory.currentItem) 1F else -1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        val percent = EaseUtils.easeOutBack(animProgress.toDouble())
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        if (render && animProgress > 0F) {
            if (itemStack != null && itemStack.item is ItemBlock) {
                val string: String = "Amount: " + if (Scaffold.state) Scaffold.blockAmount - Scaffold.placeTick else itemStack.stackSize
                val stringWidth = Fonts.font35.getStringWidth(string) + if (itemStack.stackSize < 10) 3F else 0F
                RenderUtils.drawRoundedRect(
                    width / 2F + -35F,
                    height - 2F - (80F * percent.toFloat()),
                    32F + stringWidth,
                    20F,
                    5F,
                    Color(0, 0, 0, (80 * percent).toInt()).rgb,
                    2F,
                    ClientTheme.getColorWithAlpha(0, (255 * percent).toInt()).rgb
                )
                Fonts.font35.drawCenteredString(
                    string, width / 2 + 15F, height + 5F - (80F * percent).toFloat(), Color.WHITE.rgb, true
                )
            } else {
                RenderUtils.drawRoundedRect(
                    width / 2F + -11F,
                    height - 2F - (80F * percent.toFloat()),
                    20F,
                    20F,
                    5F,
                    Color(0, 0, 0, (80 * percent).toInt()).rgb,
                    2F,
                    ClientTheme.getColorWithAlpha(0, (255 * percent).toInt()).rgb
                )
            }
        }
    }

    private fun renderItem() {
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        val itemStack = mc.thePlayer.inventory.getCurrentItem()
        val percent = EaseUtils.easeOutBack(animProgress.toDouble())
        if (itemStack != null && render && animProgress >= 0F) {
            if (itemStack.item is ItemBlock) {
                RenderUtils.renderItemIcon(
                    width / 2 - 30, (height - (80F * percent)).toInt(), itemStack
                )
            } else {
                RenderUtils.renderItemIcon(
                    width / 2 - 9, (height - (80F * percent)).toInt(), itemStack
                )
            }
        }
    }
}