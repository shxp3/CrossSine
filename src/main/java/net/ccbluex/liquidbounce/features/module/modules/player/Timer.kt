/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.EnumAutoDisableType
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

@ModuleInfo(name = "Timer", spacedName = "Timer", category = ModuleCategory.PLAYER, autoDisable = EnumAutoDisableType.RESPAWN)
class Timer : Module() {

    private val speedValue = FloatValue("Speed", 2F, 0.1F, 10F)
    private val onMoveValue = BoolValue("OnlyMove", true)
    private val onStrafeValue = BoolValue("OnlyStrafe", false)

    override fun onDisable() {
        if (mc.thePlayer == null) {
            return
        }

        mc.timer.timerSpeed = 1F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (onMoveValue.get() && MovementUtils.isMoving() || onStrafeValue.get() && MovementUtils.isStrafing()) {
            mc.timer.timerSpeed = speedValue.get()
        } else {
            mc.timer.timerSpeed = speedValue.get()
        }
    }

    override val tag: String?
        get() = "${speedValue.get()}"
}
