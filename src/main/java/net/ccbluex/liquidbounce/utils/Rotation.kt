package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) {

    /**
     * Set rotations to [player]
     */
        fun toPlayer(player: EntityPlayer) {
        if ((yaw.isNaN() || pitch.isNaN()))
            return
        var yaw = 0F
        var pitch = 0F
        if (ClientTheme.smoothRotationSS.get()) {
            yaw = AnimationUtils.animate(this.yaw, RotationUtils.serverRotation.yaw, ClientTheme.smoothFactorSS.get())
            pitch = AnimationUtils.animate(this.pitch, RotationUtils.serverRotation.pitch, ClientTheme.smoothFactorSS.get())
        } else {
            yaw = this.yaw
            pitch = this.pitch
        }
        player.rotationYaw = yaw
        player.rotationPitch = pitch
    }

    /**
     * Patch gcd exploit in aim
     *
     * @see net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender
     */
    fun fixedSensitivity(sensitivity: Float = MinecraftInstance.mc.gameSettings.mouseSensitivity): Rotation {
        val gcd = getFixedAngleDelta(sensitivity)
        if (ClientTheme.gcdFix.get()) {
            yaw = getFixedSensitivityAngle(yaw, serverRotation.yaw, gcd)
            pitch = getFixedSensitivityAngle(pitch, serverRotation.pitch, gcd)
        }

        return this.withLimitedPitch()
    }
    private fun withLimitedPitch(value: Float = 90f): Rotation {
        pitch = pitch.coerceIn(-value, value)
        return this
    }
    /**
     * Apply strafe to player
     *
     * @author bestnub
     */
    fun toDirection(): Vec3 {
        val f: Float = MathHelper.cos(-yaw * 0.017453292f - Math.PI.toFloat())
        val f1: Float = MathHelper.sin(-yaw * 0.017453292f - Math.PI.toFloat())
        val f2: Float = -MathHelper.cos(-pitch * 0.017453292f)
        val f3: Float = MathHelper.sin(-pitch * 0.017453292f)
        return Vec3((f1 * f2).toDouble(), f3.toDouble(), (f * f2).toDouble())
    }
    override fun toString(): String {
        return "Rotation(yaw=$yaw, pitch=$pitch)"
    }
    companion object {
        private fun getFixedAngleDelta(sensitivity: Float = MinecraftInstance.mc.gameSettings.mouseSensitivity) = (sensitivity * 0.6f + 0.2f).pow(3) * 1.2f
        fun getFixedSensitivityAngle(targetAngle: Float, startAngle: Float = 0f, gcd: Float = getFixedAngleDelta()) =
            startAngle + ((targetAngle - startAngle) / gcd).roundToInt() * gcd
    }
}

/**
 * Rotation with vector
 */
data class VecRotation(val vec: Vec3, val rotation: Rotation)

/**
 * Rotation with place info
 */
data class PlaceRotation(val placeInfo: PlaceInfo, val rotation: Rotation)
