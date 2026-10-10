package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.KeyBindValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import kotlin.math.abs

/**
 * Zoom module — while [bind] is held (or toggled, depending on [holdMode]),
 * smoothly animates the camera FOV down to [zoomFov]. Releasing the key
 * animates back to the user's original vanilla FOV.
 *
 * The original FOV is captured live: any time the user changes their FOV in
 * vanilla settings while we are NOT zoomed, that becomes the new baseline.
 */
@ModuleInfo("Zoom", ModuleCategory.VISUAL)
object Zoom : Module() {

    private val bind = KeyBindValue("Bind", Keyboard.KEY_C)
    private val zoomFov = IntegerValue("Zoom-FOV", 30, 5, 110)
    private val speed = FloatValue("Speed", 12F, 1F, 40F)
    private val easingMode = ListValue("Easing", arrayOf("Smooth", "Linear"), "Smooth")
    private val holdMode = BoolValue("Hold-Mode", true)   // false = toggle on press

    private var originalFov: Float = 70F
    private var currentFov: Float = 70F
    private var lastSet: Float = Float.NaN

    private var toggled = false
    private var prevDown = false
    private var initialised = false

    override fun onEnable() {
        captureBaseline()
        currentFov = originalFov
        toggled = false
        prevDown = false
        initialised = true
    }

    override fun onDisable() {
        if (initialised) mc.gameSettings.fovSetting = originalFov
        lastSet = Float.NaN
        toggled = false
        prevDown = false
        initialised = false
    }

    private fun captureBaseline() {
        originalFov = mc.gameSettings.fovSetting
    }

    @EventTarget
    fun onRender(event: Render2DEvent) {
        // If the user changed FOV in vanilla settings while we weren't zoomed,
        // pick that up as the new baseline.
        val cur = mc.gameSettings.fovSetting
        if (lastSet.isNaN() || abs(cur - lastSet) > 0.01F) {
            originalFov = cur
            currentFov = cur
        }

        val held = bind.isKeyDown()
        toggled = if (holdMode.get()) {
            held
        } else {
            if (held && !prevDown) !toggled else toggled
        }
        prevDown = held

        val want = if (toggled && mc.currentScreen == null) zoomFov.get().toFloat() else originalFov

        // Frame-rate independent animation.
        //   speed  ∈ [1, 40]   (user setting)
        //   delta  in milliseconds
        //   "Smooth"  → exponential ease-out  (current += (want - current) * rate)
        //   "Linear"  → constant FOV-units-per-frame
        currentFov = if (easingMode.equals("Linear")) {
            val step = speed.get() * RenderUtils.deltaTime * 0.01F
            when {
                currentFov < want -> (currentFov + step).coerceAtMost(want)
                currentFov > want -> (currentFov - step).coerceAtLeast(want)
                else -> currentFov
            }
        } else {
            val rate = (speed.get() * RenderUtils.deltaTime / 1000F).coerceIn(0F, 1F)
            var next = currentFov + (want - currentFov) * rate
            if (abs(want - next) < 0.05F) next = want
            next
        }

        mc.gameSettings.smoothCamera = held
        mc.gameSettings.fovSetting = currentFov
        lastSet = currentFov
    }

    override val tag: String
        get() = "${zoomFov.get()}"
}
