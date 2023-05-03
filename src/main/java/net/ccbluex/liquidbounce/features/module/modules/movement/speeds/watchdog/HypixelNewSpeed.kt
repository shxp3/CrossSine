package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.watchdog

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class HypixelNewSpeed: SpeedMode("HypixelNew") {

    @EventTarget
    override fun onUpdate() {
            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                mc.thePlayer.jump()
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe(0.48F)
                } else {
                    MovementUtils.strafe()
                }
            }
            if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(0.5F)
            } else {
                if (mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
                    MovementUtils.strafe(0.42F)
                }
            }
    }
}