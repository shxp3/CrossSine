package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class BlocksMCBoost : SpeedMode("BlocksMCBoost") {

    private val ResetOnDisableValue = BoolValue("MotionStop", false)
    override fun onUpdate() {

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(0.3455555555F)
            } else MovementUtils.strafe(0.27F)
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
        }

        if (mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
            MovementUtils.strafe(0.3899555555555555f)
        }

    }

    override fun onDisable() {
        if (ResetOnDisableValue.get()){
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }
}