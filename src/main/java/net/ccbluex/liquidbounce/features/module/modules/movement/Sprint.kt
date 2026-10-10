package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.SprintEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.injection.access.StaticStorage
import java.awt.Color

@ModuleInfo(name = "Sprint", category = ModuleCategory.MOVEMENT, array = false, defaultOn = true)
object Sprint : Module() {

    private val textValue = BoolValue("ShowText", false)
    private val downValue = BoolValue("Down", false).displayable { textValue.get() }

    val allDirectionsValue = BoolValue("AllDirections", false)
    val hungryValue = BoolValue("Hungry", true)
    val sneakValue = BoolValue("Sneak", false)
    val collideValue = BoolValue("Collide", false)

    private val strictValue = BoolValue("Strict", false)

    var shouldSprint = false
        private set

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!textValue.get()) return
        val text = if (mc.thePlayer.isSneaking) "[Sneaking]" else "[Sprinting]"
        val y = if (downValue.get()) StaticStorage.scaledResolution.scaledHeight - 9f else 2f
        mc.fontRendererObj.drawStringWithShadow(text, 2f, y, Color.WHITE.rgb)
    }

    @EventTarget
    fun onSprint(event: SprintEvent) {
        if (strictValue.get()) event.sprint = event.sprint && shouldSprint
    }

    fun check(moveForward: Float) {
        shouldSprint = moveForward >= 0.8f
    }
}