package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredStringFade
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.text.JTextComponent.KeyBinding

@ModuleInfo(name = "HUD", "HUD", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object HUD : Module() {
    val test1 = FloatValue("Test-X", 0F, -10F, 10F)
    val test2 = FloatValue("Test-Y", 0F, -10F, 10F)
    val test3 = FloatValue("Test-X1", 0F, -10F, 10F)
    val hudtext = BoolValue("HUDText", false)
    val title = TitleValue(".clientusername (name)")
    val buttonValue = BoolValue("ContainerButton", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    val inventoryAnimation = BoolValue("InventoryAnimation", false)
    val noF5 = BoolValue("NoF5-Crosshair", false)
    val ColorGuiInGameValue = IntegerValue("ColorGuiInGame", 0, 0, 9)
    var scafState = false
    var fadeProgress = 0F

    @EventTarget
    fun onTick(event: TickEvent) {
        mc.guiAchievement.clearAchievements()
        if (Keyboard.isKeyDown(Keyboard.KEY_PERIOD) && mc.currentScreen == null) {
            mc.displayGuiScreen(GuiChat("."))
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner) return
        CrossSine.hud.render(false, event.partialTicks)
        if (hudtext.get()) {
            var width = ""
            val name = "CrossSine"
            val other = " | ${CrossSine.USER_NAME} | ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))}"
            val leagth = Fonts.fontTenacityBold40.getStringWidth(name) + Fonts.fontTenacityBold35.getStringWidth(other)
            RenderUtils.customRounded(2F, 3.5F, leagth + 6F, Fonts.fontTenacityBold40.FONT_HEIGHT + 5F, 0F, 0F, 5F, 5F, Color(0,0,0,180).rgb)
            RenderUtils.drawAnimatedGradient(2.0, 3.0, leagth + 6.0, 4.0, ClientTheme.getColor(0).rgb, ClientTheme.getColor(90).rgb)
            GlowUtils.drawGlow(3.79F, 6.07F, 3.83F + Fonts.fontTenacityBold40.getStringWidth(name).toFloat(), 7.21F, 9, ClientTheme.getColor(1))
            for (l in name.indices) {
                Fonts.fontTenacityBold40.drawString(name[l].toString(), 5F + Fonts.fontTenacityBold40.getStringWidth(width).toFloat(), 5.5F, ClientTheme.getColor(l * -135).rgb, true)
                width += name[l].toString()
            }
            Fonts.fontTenacityBold35.drawString(other, Fonts.fontTenacityBold40.getStringWidth("CrossSine").toFloat() + 5F, 6.5F, Color(255,255,255).rgb)
            GlStateManager.resetColor()
        }
        scafCounter(event)
    }


    fun scafCounter(event : Render2DEvent) {
        val scaleW = ScaledResolution(mc).scaledWidth
        val scaleH = ScaledResolution(mc).scaledHeight
        val font = Fonts.SFApple35
        val text = "$blocksAmount Blocks"
        val centerLe = font.getStringWidth(text) / 2
        if (Scaffold.counterMode.equals("OFF")) return
        if (Scaffold.state) scafState = true
        fadeProgress += (0.0075F * 0.85F * RenderUtils.deltaTime * if (Scaffold.state) -1F else 1F)
        fadeProgress = fadeProgress.coerceIn(0F, 1F)
        if (fadeProgress >= 1F) scafState = false
        if (scafState) {
            when (Scaffold.counterMode.get().lowercase()) {
                "simple" -> {
                    GlStateManager.enableBlend()
                    mc.fontRendererObj.drawCenteredStringFade(
                        "Blocks : $blocksAmount",
                        scaleW / 2F,
                        scaleH / 2 + 20F,
                        Color(255, 255, 255, fadeAlpha(255))
                    )
                    GlStateManager.disableAlpha()
                    GlStateManager.disableBlend()
                    GlStateManager.resetColor()
                }

                "normal" -> {
                    RenderUtils.drawRoundedRect(
                        scaleW / 2 - 3F - centerLe,
                        scaleH - 80F,
                        scaleW / 2 + 3F + centerLe,
                        scaleH - 93.05F,
                        2F,
                        Color(0, 0, 0, fadeAlpha(100)).rgb
                    )
                    GlowUtils.drawGlow(
                        scaleW / 2 - 3F - centerLe + 0.5F,
                        scaleH - 80F - 13.5F,
                        3F + centerLe + 23F,
                        13F,
                        8,
                        Color(0, 0, 0, fadeAlpha(200))
                    )
                    GlStateManager.enableBlend()
                    font.drawCenteredString(
                        text,
                        scaleW / 2F,
                        scaleH - 90F,
                        Color(255, 255, 255, fadeAlpha(255)).rgb
                    )
                    GlStateManager.disableAlpha()
                    GlStateManager.disableBlend()
                    GlStateManager.resetColor()
                }
                "rise" -> {
                    GlStateManager.pushMatrix()
                    val info = blocksAmount.toString()
                    val slot = InventoryUtils.findAutoBlockBlock(Scaffold.highBlock.get())
                    val height = event.scaledResolution.scaledHeight
                    val width = event.scaledResolution.scaledWidth
                    val w2=(mc.fontRendererObj.getStringWidth(info))
                    RenderUtils.drawRoundedCornerRect(
                        (width - w2 - 20) / 2f,
                        height * 0.8f - 24f,
                        (width + w2 + 18) / 2f,
                        height * 0.8f + 12f,
                        5f,
                        Color(20, 20, 20, fadeAlpha(100)).rgb
                    )
                    var stack: ItemStack? = null
                    stack = if (slot != -1) {
                        mc.thePlayer.inventory.getStackInSlot(InventoryUtils.findAutoBlockBlock(Scaffold.highBlock.get()) - 36)
                    } else {
                        ItemStack(Item.getItemById(166), 0, 0)
                    }

                    RenderHelper.enableGUIStandardItemLighting()
                    GlStateManager.enableBlend()
                    mc.renderItem.renderItemIntoGUI(stack, width / 2 - 9, (height * 0.8 - 20).toInt())
                    ColorUtils.setColour(Color(255,255,255, fadeAlpha(255)).rgb)
                    RenderHelper.disableStandardItemLighting()
                    mc.fontRendererObj.drawCenteredString(info, width / 2f, height * 0.8f, Color(255,255,255, fadeAlpha(255)).rgb, false)
                    GlStateManager.disableAlpha()
                    GlStateManager.disableBlend()
                    GlStateManager.popMatrix()
                }
            }
        }
    }
    fun fadeAlpha(alpha: Int): Int {
        return alpha - (fadeProgress * alpha).toInt()
    }

    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack
                if (itemStack != null && itemStack.item is ItemBlock && InventoryUtils.canPlaceBlock((itemStack.item as ItemBlock).block)) {
                    amount += itemStack.stackSize
                }
            }
            return amount
        }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        CrossSine.hud.update()
    }

    @EventTarget
    fun onScreen(event: ScreenEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return
        }

    }

    @EventTarget
    fun onKey(event: KeyEvent) {
        CrossSine.hud.handleKey('a', event.key)
    }

}