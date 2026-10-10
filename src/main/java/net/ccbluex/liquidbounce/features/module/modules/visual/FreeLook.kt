package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.features.module.EnumTriggerType
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.Display

@ModuleInfo(name = "FreeLook", category = ModuleCategory.VISUAL, triggerType = EnumTriggerType.PRESS)
class FreeLook : Module() {
    private val thirdPerson = BoolValue("Third-Person", true)
    val reverse = BoolValue("Reverse", false)
    override fun onEnable() {
        if (!isEnabled) {
            setRotations()
            isEnabled = true
        }
        isReverse = reverse.get()
        perspectiveToggled = true
        previousPerspective = mc.gameSettings.thirdPersonView
        if (thirdPerson.get())
            mc.gameSettings.thirdPersonView = 1
    }

    override fun onDisable() {
        isEnabled = false
        resetPerspective()
    }

    fun setRotations() {
        cameraYaw = mc.thePlayer.rotationYaw
        cameraPitch = mc.thePlayer.rotationPitch
    }

    fun enable() {
        isEnabled = true
        isReverse = false
        perspectiveToggled = true
        setRotations()
        previousPerspective = mc.gameSettings.thirdPersonView
        if (thirdPerson.get())
            mc.gameSettings.thirdPersonView = 1
    }

    fun disable() {
        if (isEnabled) {
            isEnabled = false
            perspectiveToggled = false
            mc.thePlayer.rotationYaw = cameraYaw
            mc.thePlayer.rotationPitch = cameraPitch
        }
    }

    companion object {
        private val mc = MinecraftInstance.mc

        @JvmField
        var isReverse = false

        @JvmField
        var isEnabled = false

        @JvmField
        var perspectiveToggled = false

        @JvmField
        var cameraYaw = 0f

        @JvmField
        var cameraPitch = 0f

        @JvmField
        var prevCameraYaw = 0f

        @JvmField
        var prevCameraPitch = 0f
        private var previousPerspective = 0

        @JvmStatic
        fun overrideMouse(): Boolean {
            if (mc.inGameHasFocus && Display.isActive()) {
                if (!perspectiveToggled) {
                    return true
                }
                mc.mouseHelper.mouseXYChange()
                val f1 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f
                val f2 = f1 * f1 * f1 * 8.0f
                val f3 = mc.mouseHelper.deltaX.toFloat() * f2
                val f4 = mc.mouseHelper.deltaY.toFloat() * f2
                prevCameraYaw = cameraYaw
                cameraYaw += f3 * 0.15f
                prevCameraPitch = cameraPitch
                cameraPitch -= f4 * 0.15f
                if (cameraPitch > 90) cameraPitch = 90f
                if (cameraPitch < -90) cameraPitch = -90f
            }
            return false
        }

        @JvmStatic
        fun resetPerspective() {
            perspectiveToggled = false
            mc.gameSettings.thirdPersonView = previousPerspective
        }
    }
}