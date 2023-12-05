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
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ModuleInfo(name = "HUD", "HUD", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object HUD : Module() {
    val hudtext = BoolValue("HUDText", true)
    val title = TitleValue(".clientusername (name)")
    val buttonValue = BoolValue("ContainerButton", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    val UiShadowValue =
        ListValue("UiEffect", arrayOf("Shadow", "Glow", "None"), "None")
    val ColorGuiInGameValue = IntegerValue("ColorGuiInGame", 0, 0, 9)
    var scafState = false
    var fadeProgress = 0F

    @EventTarget
    fun onTick(event: TickEvent) {
        mc.guiAchievement.clearAchievements()
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (mc.currentScreen is GuiHudDesigner) return
        CrossSine.hud.render(false, event.partialTicks)
        if (hudtext.get()) {
            clientText()
        }
        scafCounter(event)
    }

    fun clientText() {
        val name = "CrossSine"
        val other = " | ${CrossSine.USER_NAME} | ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))}"
        val leagth = Fonts.fontTenacityBold40.getStringWidth(name) + Fonts.fontTenacityBold35.getStringWidth(other)
        RenderUtils.drawRect(2F, 3F, leagth + 6F, Fonts.fontTenacityBold40.FONT_HEIGHT + 5F, Color(0,0,0,180))
        RenderUtils.drawAnimatedGradient(2.0, 3.0, leagth + 6.0, 4.0, ClientTheme.getColor("START"), ClientTheme.getColor("END"))
        for (l in 0..(name.length-1)) {
            var width = ""
            Fonts.fontTenacityBold40.drawString(name.get(l).toString(), 5F + Fonts.fontTenacityBold40.getStringWidth(width), 5.5F, ClientTheme.getColor(l * -135).rgb)
            width += name.get(l).toString()
        }
        Fonts.fontTenacityBold35.drawString(other, Fonts.fontTenacityBold40.getStringWidth("CrossSine").toFloat() + 5F, 6.5F, Color(255,255,255).rgb)
        GlStateManager.resetColor()
    }

    fun scafCounter(event : Render2DEvent) {
        val scaleW = ScaledResolution(mc).scaledWidth
        val scaleH = ScaledResolution(mc).scaledHeight
        val font = Fonts.Nunito40
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
                    var stack = ItemStack(Item.getItemById(166), 0, 0)
                    if (slot != -1) {
                        if (mc.thePlayer.inventory.getCurrentItem() != null) {
                            val handItem = mc.thePlayer.inventory.getCurrentItem().item
                            if (handItem is ItemBlock && InventoryUtils.canPlaceBlock(handItem.block)) {
                                stack = mc.thePlayer.inventory.getCurrentItem()
                            }
                        }
                        if (stack == ItemStack(Item.getItemById(166), 0, 0)) {
                            stack = mc.thePlayer.inventory.getStackInSlot(InventoryUtils.findAutoBlockBlock(Scaffold.highBlock.get()) - 36)
                            if (stack == null) {
                                stack = ItemStack(Item.getItemById(166), 0, 0)
                            }
                        }
                    }

                    RenderHelper.enableGUIStandardItemLighting()
                    GlStateManager.enableBlend()
                    mc.renderItem.renderItemIntoGUI(stack, width / 2 - 9, (height * 0.8 - 20).toInt())
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