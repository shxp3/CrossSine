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

    val horizontalValue = FloatValue("Horizontal", 0F, 0F, 1F)
    val verticalValue = FloatValue("Vertical", 0F, 0F, 1F)

    fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            val horizontal = horizontalValue.get()
            val vertical = verticalValue.get()

            if (horizontal.toInt() == 0 && vertical.toInt() == 0) {
                event.cancelEvent()
            }

            packet.motionX = (packet.getMotionX() * horizontal).toInt()
            packet.motionY = (packet.getMotionY() * vertical).toInt()
            packet.motionZ = (packet.getMotionZ() * horizontal).toInt()
        }
    }
}
