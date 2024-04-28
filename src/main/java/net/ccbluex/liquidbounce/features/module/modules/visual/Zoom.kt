package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "Zoom", category = ModuleCategory.VISUAL)
class Zoom : Module() {
    private val slowSen = BoolValue("Slow Sensitivity", false)
    private val smoothSpeed = FloatValue("Smooth Speed", 0.1F, 0.1F, 5F)
    private val keyValue = KeyValue("KeyBind : ", Keyboard.KEY_C)
    private var oldFov = 0F
    private var smooth = 0F
    override fun onEnable() {
        oldFov = mc.gameSettings.fovSetting
    }

    override fun onDisable() {
        mc.gameSettings.fovSetting = oldFov
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val down: Boolean = Keyboard.isKeyDown(keyValue.get())
        if (mc.currentScreen != null) {
            oldFov = mc.gameSettings.fovSetting
        }
        smooth += (0.0075F * smoothSpeed.get() * RenderUtils.deltaTime * if (down) -1F else 1F)
        smooth = smooth.coerceIn(0F, 1F)
        val percent = EaseUtils.easeInCirc(smooth.toDouble()).toFloat()
        mc.gameSettings.fovSetting *= percent
        if (mc.gameSettings.fovSetting <= 25F) {
            mc.gameSettings.fovSetting = 25F
        }
    }
}