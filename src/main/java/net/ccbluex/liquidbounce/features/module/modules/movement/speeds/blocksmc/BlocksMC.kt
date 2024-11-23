package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.blocksmc

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.potion.Potion


class BlocksMC : SpeedMode("BlocksMC") {

    private val damageBoostValue = BoolValue("Damage-Boost", true)
    private val damageCustomValue = BoolValue("Custom-Boost", false)
    private val damageValue =  FloatValue("Boost-Value", 0.65F, 0.1F, 2F)
    private val groundBoost = BoolValue("GroundBoost", true)
    private val lowHopValue = BoolValue("LowHop", false)
    override fun onUpdate() {
            if (isMoving()) {
                if (lowHopValue.get() && mc.thePlayer.hurtTime > 0 && CrossSine.combatManager.inCombat) {
                    strafe(0.45995554f)
                    if (mc.thePlayer.onGround) {
                        MovementUtils.jump(false)
                        mc.thePlayer.motionY = 0.22
                    } else if (mc.thePlayer.fallDistance < -0.20) {
                        mc.thePlayer.motionY = -0.10
                    }
                } else if (mc.thePlayer.onGround) {
                    MovementUtils.jump(false)
                    if (groundBoost.get()) {
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
                if (damageBoostValue.get() && mc.thePlayer.hurtTime > 6 && ((KillAura.state && KillAura.currentTarget != null) || (KillAura2.state && KillAura2.target != null))) {
                    strafe(if (damageCustomValue.get()) damageValue.get() else 0.455F)
                }
            } else {
                MovementUtils.resetMotion(false)
            }
    }
}