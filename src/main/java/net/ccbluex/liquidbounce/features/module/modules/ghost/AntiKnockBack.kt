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

@ModuleInfo(name = "AntiKnockBack", category = ModuleCategory.GHOST)
class AntiKnockBack : Module() {
    val CancleVelocity = BoolValue("CancleVelocity", false)
    val AntionlyGroundValue = BoolValue("Onlyground", false)
    val XValue = FloatValue("X", 1.0F, 0.0F, 1.0F).displayable { !CancleVelocity.get() }
    val YValue = FloatValue("Y", 1.0F, 0.0F, 1.0F).displayable { !CancleVelocity.get() }
    val chanceValue = IntegerValue("Chance", 100, 0, 100).displayable { !CancleVelocity.get() }

    @EventTarget
    fun onVelocityPacket(event: PacketEvent) {
        if (AntionlyGroundValue.get() && mc.thePlayer.onGround) {
            if (event.packet is S12PacketEntityVelocity && CancleVelocity.get()) {
                event.cancelEvent()
            } else
                if (RandomUtils.nextInt(1, 100) <= chanceValue.get()) {
                    if (event.packet is S12PacketEntityVelocity) {
                        event.packet.motionX = (event.packet.getMotionX() * XValue.get()).toInt()
                        event.packet.motionY = (event.packet.getMotionY() * YValue.get()).toInt()
                        event.packet.motionZ = (event.packet.getMotionZ() * XValue.get()).toInt()
                    }
                    if (XValue.get() == 0.0F && YValue.get() == 0.0F) {
                        event.cancelEvent()
                    }
                }
        }
        else
        {
            if (event.packet is S12PacketEntityVelocity && CancleVelocity.get()) {
                event.cancelEvent()
            } else
                if (RandomUtils.nextInt(1, 100) <= chanceValue.get()) {
                    if (event.packet is S12PacketEntityVelocity) {
                        event.packet.motionX = (event.packet.getMotionX() * XValue.get()).toInt()
                        event.packet.motionY = (event.packet.getMotionY() * YValue.get()).toInt()
                        event.packet.motionZ = (event.packet.getMotionZ() * XValue.get()).toInt()
                    }
                    if (XValue.get() == 0.0F && YValue.get() == 0.0F) {
                        event.cancelEvent()
                    }
                }
        }
    }
}
