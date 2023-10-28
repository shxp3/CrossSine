package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion

class BlocksMC : SpeedMode("BlocksMC") {

    private val DamageBoostValue = BoolValue("DamageBoost", false)

    override fun onUpdate() {

        if (MovementUtils.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
            if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)){ MovementUtils.strafe(MovementUtils.getSpeed()) } else MovementUtils.strafe(0.38)
            if (DamageBoostValue.get() && mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
                MovementUtils.strafe(0.45995554f)
            }
        } else {
            MovementUtils.resetMotion(false)
        }


    }

}