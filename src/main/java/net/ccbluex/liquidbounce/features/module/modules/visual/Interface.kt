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
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredStringFade
import net.ccbluex.liquidbounce.utils.render.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

@ModuleInfo(name = "Interface", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object Interface : Module() {
    private val hudText = BoolValue("HUD-Text", true)
    private val smoothFont = BoolValue("Smooth", false)
    private val roblox = BoolValue("RobloxHUD", false)
    val title = TitleValue(".clientusername (name)")
    val buttonValue = BoolValue("ContainerButton", false)
    val inventoryParticle = BoolValue("InventoryParticle", false)
    val inventoryAnimation = BoolValue("InventoryAnimation", false)
    val noF5 = BoolValue("NoF5-Crosshair", false)
    val shaders = BoolValue("Shader", true)
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
        if (hudText.get()) {
            var width = ""
            val name = "CrossSine"
            for (l in name.indices) {
                getFont(smoothFont.get()).drawString(name[l].toString(), 5F +  getFont(smoothFont.get()).getStringWidth(width).toFloat(), 5.5F, ClientTheme.getColor(l * -135).rgb, true)
                width += name[l].toString()
            }
        }
        if (roblox.get()) {
            RoundedUtil.drawRound(4.5F,4.5F, 15F, 15F , 4F, Color(0,0,0,150))
            RoundedUtil.drawRound(24.5F,4.5F, 15F, 15F , 4F, Color(0,0,0,150))
            RoundedUtil.drawRound(ScaledResolution(mc).scaledWidth - 23F,4F, 15F, 15F , 4F, Color(0,0,0,150))
            RenderUtils.drawImage(ResourceLocation("crosssine/ui/roblox/Ro.png"), 5, 5, 14, 14)
            RenderUtils.drawImage(ResourceLocation("crosssine/ui/roblox/docRo.png"), 25, 5, 14, 14)
            RenderUtils.drawImage(ResourceLocation("crosssine/ui/roblox/Ro2.png"), ScaledResolution(mc).scaledWidth - 22, 5, 13, 13)
        }
    }
    private fun getFont(smooth: Boolean) : FontRenderer {
        return if (smooth) {
            Fonts.fontComfortaa40
        } else {
            mc.fontRendererObj
        }
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