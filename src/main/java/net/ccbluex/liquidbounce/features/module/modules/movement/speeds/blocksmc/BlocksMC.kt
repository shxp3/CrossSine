package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class BlocksMC : SpeedMode("BlocksMC") {

    private val ResetOnDisableValue = BoolValue("MotionStop", false)
    private val DamageBoostValue = BoolValue("DamageBoost", false)
    private val TimerValue = BoolValue("Timer?", false).displayable { DamageBoostValue.get() }

    override fun onUpdate() {

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(0.3455555555F)
            } else MovementUtils.strafe(0.27F)
            if(mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
        }

        if (DamageBoostValue.get() && mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
            MovementUtils.strafe(0.4599555555555555f)
        }


        if (DamageBoostValue.get() && TimerValue.get() && mc.thePlayer.hurtTime > 0) {
            mc.timer.timerSpeed = 1.05f
        }
        else
            mc.timer.timerSpeed = 1.0f

    }

    override fun onDisable() {
        if (ResetOnDisableValue.get()){
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }
}