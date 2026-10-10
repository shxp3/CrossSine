package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue

@ModuleInfo(name = "KeepSprint", category = ModuleCategory.COMBAT)
object KeepSprint : Module() {
    private val forwardMotion = FloatValue("Forward", 1F, 0F, 1F)
    private val strafeMotion = FloatValue("Strafe", 1F, 0F, 1F)
    private val bothMotion = FloatValue("BothMotion", 1F, 0F, 1F)
    private val onlyMode = ListValue("Mode", arrayOf("Ground", "Air", "Both"), "Both")
    private val canKeepSprint: Boolean
        get() {
            return when (onlyMode.get().lowercase()) {
                "ground" -> mc.thePlayer.onGround
                "air" -> !mc.thePlayer.onGround
                else -> true
            }
        }

    fun getMotion(): Float? {
        return if (canKeepSprint) if (mc.thePlayer.moveForward != 0F && mc.thePlayer.moveStrafing == 0F) forwardMotion.get() else if (mc.thePlayer.moveStrafing != 0F && mc.thePlayer.moveForward == 0F) strafeMotion.get() else if (mc.thePlayer.moveForward != 0F && mc.thePlayer.moveStrafing != 0F) bothMotion.get() else null else null
    }

    override val tag: String
        get() = if (mc.thePlayer.moveForward != 0F && mc.thePlayer.moveStrafing == 0F) forwardMotion.get()
            .toString() else (if (mc.thePlayer.moveStrafing != 0F && mc.thePlayer.moveForward == 0F) strafeMotion.get()
            .toString() else if (mc.thePlayer.moveForward != 0F && mc.thePlayer.moveStrafing != 0F) bothMotion.get()
            .toString() else forwardMotion.get()).toString()
}
