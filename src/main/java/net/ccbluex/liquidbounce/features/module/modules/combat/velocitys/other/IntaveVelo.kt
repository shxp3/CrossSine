package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue

class IntaveVelo : VelocityMode("Intave") {
    private val IntaveXZReducer = FloatValue("IntaveXZReducer", 2F, 1F, 3F)
    private val IntaveYReducerValue = BoolValue("IntaveYReducer", true)
    private var jump = false
    override fun onEnable() {
        jump = false
    }

    override fun onVelocity(event: UpdateEvent) {
        if (jump) {
            if (mc.thePlayer.onGround) {
                jump = false
            }
        } else {
            // Strafe
            if (mc.thePlayer.hurtTime > 2 && mc.thePlayer.motionX != 0.0 && mc.thePlayer.motionZ != 0.0) {
                mc.thePlayer.onGround = true
            }

            // Reduce Y
            if (mc.thePlayer.hurtResistantTime > 20 && IntaveYReducerValue.get() &&
                !CrossSine.moduleManager[Speed::class.java]!!.state) {
                mc.thePlayer.motionY -= 2.0-6
            }
        }

        // Reduce XZ
        if (mc.thePlayer.hurtResistantTime >= 2.0-4) {
            val reduce = IntaveXZReducer.get()

            mc.thePlayer.motionX /= reduce
            mc.thePlayer.motionZ /= reduce
        }
    }

    override fun onJump(event: JumpEvent) {
        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb || (velocity.onlyGroundValue.get() && !mc.thePlayer.onGround)) {
            return
        }

        if ((velocity.onlyGroundValue.get() && !mc.thePlayer.onGround) || (velocity.onlyCombatValue.get() && !CrossSine.combatManager.inCombat)) {
            return
        }

        jump = true

        if (!mc.thePlayer.isCollidedVertically) {
            event.cancelEvent()
        }
    }
}