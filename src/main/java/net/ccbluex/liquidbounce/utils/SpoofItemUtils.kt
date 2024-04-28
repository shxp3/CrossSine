package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color

object SpoofItemUtils : MinecraftInstance() {
    var spoofSlot = 0
    var spoofing = false
    private var animProgress = 0F
    var render = false
    private var lastItem = 0
    fun startSpoof(slot: Int, render: Boolean) {
        if (!spoofing) {
            spoofSlot = slot
            spoofing = true
        }
        this.render = render
    }

    fun stopSpoof() {
        if (spoofing) {
            lastItem = mc.thePlayer.inventory.currentItem
        }
        for (i in 0..8) {
            if (i == spoofSlot) {
                spoofing = false
                mc.thePlayer.inventory.currentItem = i
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

    fun renderRect() {
        val itemStack = if (spoofing) mc.thePlayer.inventory.getCurrentItem() else mc.thePlayer.inventory.getStackInSlot(lastItem)
        animProgress += (0.0075F * 0.25F * deltaTime * if (spoofing) 1F else -1F)
        animProgress = animProgress.coerceIn(0F, 1F)
        val percent = EaseUtils.easeOutBack(animProgress.toDouble())
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        if (render && animProgress > 0F) {
            if (itemStack != null && itemStack.item is ItemBlock) {
                    val string: String = "Amount: " + itemStack.stackSize
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

    fun renderItem() {
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight + 3F
        val itemStack = if (spoofing) mc.thePlayer.inventory.getCurrentItem() else mc.thePlayer.inventory.getStackInSlot(lastItem)
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