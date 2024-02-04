package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.setMotion
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion


class BlocksMC : SpeedMode("BlocksMC") {

    private val DamageBoostValue = BoolValue("DamageBoost", true)
    private val GroundBoost = BoolValue("GroundBoost", true)
    private val lowHop = BoolValue("LowHop", false)
    private var offGroundTicks = 0
    private var onGroundTicks = 0
    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            onGroundTicks++
            offGroundTicks = 0
        } else {
            offGroundTicks++
            onGroundTicks = 0
        }
        if (lowHop.get()) {
            if (isMoving()) {
                if (mc.thePlayer.onGround) {
                    strafe(0.356767f)
                    setMotion(0.410)
                    mc.thePlayer.jump()
                } else mc.thePlayer.posY -= 0.1
                // MovementUtils.strafe(0.376767);
            }
            setMotion(0.350)
        } else {
            if (isMoving()) {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    if (GroundBoost.get()) {
                        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                            strafe(0.5F)
                        } else {
                            strafe(0.425F)
                        }
                    }
                }
                if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                   strafe(MovementUtils.getSpeed())
                } else strafe(0.375)
                if (DamageBoostValue.get() && mc.thePlayer.hurtTime > 6 && CrossSine.combatManager.inCombat) {
                    strafe(0.45995554f)
                }
            } else {
                MovementUtils.resetMotion(false)
            }
        }
    }
}