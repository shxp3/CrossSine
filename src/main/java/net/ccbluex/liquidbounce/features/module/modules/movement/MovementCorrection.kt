package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.SprintEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.Displace
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.player.BlockIn
import net.ccbluex.liquidbounce.features.module.modules.visual.FreeLook
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.util.MathHelper
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

@ModuleInfo(name = "MovementCorrection", category = ModuleCategory.MOVEMENT)
object MovementCorrection : Module() {

    val modeValue = ListValue("Mode", arrayOf("Silent", "Strict"), "Silent")
    private val debug = BoolValue("Debug", false)

    private const val SPRINT_FORWARD_MIN = 0.784f

    @JvmStatic
    var silentFix = false

    @JvmStatic
    var doFix = false

    override fun onEnable() {
        refreshFlags()
    }

    override fun onDisable() {
        doFix = false
        silentFix = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        refreshFlags()
    }

    private fun refreshFlags() {
        silentFix = modeValue.equals("Silent")
        doFix = true
    }

    private fun activeAimYaw(): Float? {
        val target = RotationUtils.targetRotation
        val freeLook = RotationUtils.freeLookRotation
        if (target == null && freeLook == null
            && (!KillAura2.state || KillAura2.currentTarget == null)
            && !Displace.isDisplacing
            && (!BlockIn.state || !BlockIn.active)
        ) {
            return null
        }
        return target?.yaw ?: freeLook?.yaw ?: RotationUtils.serverRotation.yaw
    }

    private fun snapToKeys(relForward: Float, relStrafe: Float): Pair<Float, Float> {
        val phi = Math.toDegrees(atan2(relStrafe.toDouble(), relForward.toDouble()))
        val snapped = Math.toRadians((phi / 45.0).roundToInt() * 45.0)
        return cos(snapped).roundToInt().toFloat() to sin(snapped).roundToInt().toFloat()
    }

    private fun canSprintNow(): Boolean {
        val player = mc.thePlayer ?: return true
        val aimYaw = activeAimYaw() ?: return true
        val input = player.movementInput ?: return true

        if (abs(input.moveForward) < 0.05f && abs(input.moveStrafe) < 0.05f) return true

        return if (!silentFix) {
            input.moveForward >= 0.8f
        } else {
            val rel = MathHelper.wrapAngleTo180_float(MovementUtils.movingYaw - aimYaw)
            val snapped = (rel / 45f).roundToInt() * 45
            if (debug.get()) ClientUtils.displayAlert("SprintCheck Δ=${"%.1f".format(rel)} snapped=$snapped")
            abs(snapped) <= 45
        }
    }

    @EventTarget
    fun onSprint(event: SprintEvent) {
        event.sprint = event.sprint && canSprintNow()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val aimYaw = RotationUtils.targetRotation?.yaw
            ?: RotationUtils.freeLookRotation?.yaw
            ?: return
        event.yaw = aimYaw
    }

    fun runStrafeFixLoop(isSilent: Boolean, event: StrafeEvent) {
        if (event.isCancelled) return
        val player = mc.thePlayer ?: return

        val target = RotationUtils.targetRotation
        val freeLook = RotationUtils.freeLookRotation
        val aimYaw = target?.yaw ?: freeLook?.yaw ?: return

        val realYaw = if (FreeLook.isEnabled) FreeLook.cameraYaw else player.rotationYaw

        if (MathHelper.abs(MathHelper.wrapAngleTo180_float(realYaw - aimYaw)) < 0.001f) return

        if (event.forward == 0f && event.strafe == 0f) {
            event.cancelEvent()
            return
        }

        val friction = event.friction
        var calcForward: Float
        var calcStrafe: Float

        if (!isSilent) {
            calcForward = event.forward
            calcStrafe = event.strafe
        } else {
            val rawStrafe = event.strafe / 0.98f
            val rawForward = event.forward / 0.98f
            val keyForward = ceil(abs(rawForward)) * rawForward.sign
            val keyStrafe = ceil(abs(rawStrafe)) * rawStrafe.sign

            val diff = (realYaw - aimYaw).toRadians()
            val c = MathHelper.cos(diff)
            val s = MathHelper.sin(diff)
            val (snapF, snapS) = snapToKeys(
                keyForward * c + keyStrafe * s,
                keyStrafe * c - keyForward * s,
            )
            calcForward = snapF
            calcStrafe = snapS

            val modifier = maxOf(abs(event.forward), abs(event.strafe))
            calcForward *= modifier
            calcStrafe *= modifier

            if (debug.get()) {
                ClientUtils.displayAlert(
                    "SilentSnap f=$calcForward s=$calcStrafe " +
                    "Δyaw=${"%.1f".format(MathHelper.wrapAngleTo180_float(realYaw - aimYaw))} " +
                    "movingYaw=${"%.1f".format(MovementUtils.movingYaw)}"
                )
            }
        }

        if (player.isSprinting && calcForward < SPRINT_FORWARD_MIN) {
            player.setSprinting(false)
        }

        var d = calcStrafe * calcStrafe + calcForward * calcForward
        if (d >= 1.0E-4f) {
            d = friction / sqrt(d).coerceAtLeast(1f)
            calcStrafe *= d
            calcForward *= d
            val yawRad = aimYaw.toRadians()
            val yawSin = MathHelper.sin(yawRad)
            val yawCos = MathHelper.cos(yawRad)
            player.motionX += calcStrafe * yawCos - calcForward * yawSin
            player.motionZ += calcForward * yawCos + calcStrafe * yawSin
        }
        event.cancelEvent()
    }
}
