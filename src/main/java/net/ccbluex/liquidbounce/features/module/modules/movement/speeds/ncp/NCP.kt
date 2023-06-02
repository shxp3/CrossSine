/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.script.api.global.Chat.alert
import kotlin.math.max

class NCP : SpeedMode("NCP") {
    private val damageboostvalue = BoolValue("${valuePrefix}DamageBoost", false)
    private val timerValue = FloatValue("${valuePrefix}Timer", 1.088f, 1f, 1.1f)

    override fun onEnable() {
        mc.timer.timerSpeed = timerValue.get()
        super.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        mc.thePlayer.jumpMovementFactor = 0.2f
        mc.thePlayer.setVelocity(
            // reduce the motion a bit to avoid flags but don't stop completely
            mc.thePlayer.motionX / 3,
            mc.thePlayer.motionY,
            mc.thePlayer.motionZ / 3
        )
        super.onDisable()
    }

    override fun onUpdate() {
        if (damageboostvalue.get()){
            if (mc.thePlayer.hurtTime > 0) {
                mc.thePlayer.motionX *= 1.018 - Math.random() / 100
                mc.thePlayer.motionZ *= 1.018 - Math.random() / 100
            }
        }
        if (MovementUtils.isMoving()) {
            mc.thePlayer.jumpMovementFactor = 0.02f
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
            }
            // speed adapts based on speed potion
            MovementUtils.strafe(max(MovementUtils.getSpeed(), MovementUtils.getSpeedWithPotionEffects(0.27).toFloat()))
        } else {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }
}