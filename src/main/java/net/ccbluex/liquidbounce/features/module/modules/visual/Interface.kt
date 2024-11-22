package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.font.CFontRenderer
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.FontUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import java.awt.Color

@ModuleInfo(name = "Interface", category = ModuleCategory.VISUAL, array = false, defaultOn = true)
object Interface : Module() {
    private val watermark = BoolValue("Watermark", true)
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
        if (watermark.get()) {
            FontUtils.drawGradientString(
                Fonts.minecraftFont,
                "CrossSine",
                5,
                5,
                ClientTheme.getColor(0, true).rgb,
                ClientTheme.getColor(180, true).rgb,
                2F
            )
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