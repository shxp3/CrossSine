package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo("LegitVelocity", "Legit Velocity",ModuleCategory.GHOST)
class LegitVelocity : Module() {
    private val h = IntegerValue("XZ", 0 ,0, 100)
    private val v = IntegerValue("Y", 0 ,0, 100)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val p = event.packet
        if (p is S12PacketEntityVelocity) {

            if (h.get() == 0 && v.get() == 0) {
                event.cancelEvent()
            }

            p.motionX = (p.getMotionX() * h.get() / 100)
            p.motionY = (p.getMotionY() * v.get() / 100)
            p.motionZ = (p.getMotionZ() * h.get() / 100)
        }
    }
}