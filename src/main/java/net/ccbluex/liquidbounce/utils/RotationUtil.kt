package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.Listenable
import java.util.*
import kotlin.math.*

object RotationUtil : MinecraftInstance(), Listenable {
    override fun handleEvents() = true

    private var keepLength = 0
    @JvmField
    var targetRotation: Rotation? = null
    @JvmField
    var serverRotation: Rotation = Rotation(0f, 0f)

    private val random = Random()
    fun reset() {
        keepLength = 0
        targetRotation = null
    }
    fun getFixedAngleDelta(sensitivity: Float = mc.gameSettings.mouseSensitivity): Float {
        val f = sensitivity * 0.6f + 0.2f
        return f * f * f * 1.2f
    }
    @JvmStatic
    fun getFixedSensitivityAngle(targetAngle: Float, startAngle: Float = 0f): Float {
        val gcd = getFixedAngleDelta()
        val angleDelta = targetAngle - startAngle
        return startAngle + (angleDelta / gcd).roundToInt() * gcd
    }
}