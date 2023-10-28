package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class BlocksMCLowHop : SpeedMode("BlocksMCLowHop") {

    override fun onUpdate() {
        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
                MovementUtils.strafe(0.45995554f)
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    mc.thePlayer.motionY = 0.22
                } else if (mc.thePlayer.fallDistance < -0.20) {
                    mc.thePlayer.motionY = -0.10
                }
            } else {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    mc.thePlayer.motionY = 0.42
                }
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe(0.38)
                } else {
                    MovementUtils.strafe(MovementUtils.getSpeed())
                }
            }
        } else {
             MovementUtils.resetMotion(false)
        }
    }
}