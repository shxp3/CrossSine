package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.util.MathHelper
import kotlin.math.abs

@ModuleInfo(name = "MovementFix", category = ModuleCategory.MOVEMENT)
object MovementFix : Module() {

    private val silentFixValue = BoolValue("Silent", true)

    /**
     * Strafe Fix
     * Code by Co Dynamic
     * Date: 2023/02/15
     */

    var silentFix = false
    var doFix = false
    private var isOverwrite = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!isOverwrite) {
            silentFix = silentFixValue.get()
            doFix = true
        }
    }

    override fun onDisable() {
        doFix = false
    }

    fun applyForceStrafe(isSilent: Boolean, runStrafeFix: Boolean) {
        silentFix = isSilent
        doFix = runStrafeFix
        isOverwrite = true
    }

    fun runStrafeFixLoop(isSilent: Boolean, event: StrafeEvent) {
        if (event.isCancelled) {
            return
        }
        val (yaw) = RotationUtils.targetRotation ?: return
        var strafe = event.strafe
        var forward = event.forward
        var friction = event.friction
        var factor = strafe * strafe + forward * forward

        val angleDiff = ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw - 22.5f - 135.0f) + 180.0) / (45.0).toDouble()).toInt()
        val calcYaw = if(isSilent) { yaw + 45.0f * angleDiff.toFloat() } else yaw

        var calcMoveDir = abs(strafe).coerceAtLeast(abs(forward))
        calcMoveDir *= calcMoveDir
        val calcMultiplier = MathHelper.sqrt_float(calcMoveDir / 1.0f.coerceAtMost(calcMoveDir * 2.0f))

        if (isSilent) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    if ((abs(forward) > 0.005 || abs(strafe) > 0.005) && !(abs(forward) > 0.005 && abs(strafe) > 0.005)) {
                        friction /= calcMultiplier
                    } else if (abs(forward) > 0.005 && abs(strafe) > 0.005) {
                        friction *= calcMultiplier
                    }
                }
            }
        }
        if (factor >= 1.0E-4F) {
            factor = MathHelper.sqrt_float(factor)

            if (factor < 1.0F) {
                factor = 1.0F
            }

            factor = friction / factor
            strafe *= factor
            forward *= factor

            val yawSin = MathHelper.sin((calcYaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((calcYaw * Math.PI / 180F).toFloat())

            mc.thePlayer.motionX += strafe * yawCos - forward * yawSin
            mc.thePlayer.motionZ += forward * yawCos + strafe * yawSin
        }
        event.cancelEvent()
    }
}
