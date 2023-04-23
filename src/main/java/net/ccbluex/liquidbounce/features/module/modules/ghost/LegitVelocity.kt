package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo(name = "LegitVelocity", category = ModuleCategory.GHOST)
class LegitVelocity : Module() {

    val horizontalValue = IntegerValue("Horizontal", 0, 0, 100)
    val verticalValue = IntegerValue("Vertical", 0, 0, 100)

    fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            val horizontal = horizontalValue.get()
            val vertical = verticalValue.get()

            if (horizontal.toFloat() == 0F && vertical.toFloat() == 0F) {
                event.cancelEvent()
            }

            packet.motionX = (packet.getMotionX() * horizontal)
            packet.motionY = (packet.getMotionY() * vertical)
            packet.motionZ = (packet.getMotionZ() * horizontal)
        }
    }
}
