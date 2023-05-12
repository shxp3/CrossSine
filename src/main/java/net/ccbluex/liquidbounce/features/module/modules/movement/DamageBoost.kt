package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo(name = "DamageBoost", spacedName = "Damage Boost",category = ModuleCategory.MOVEMENT)
class DamageBoost : Module() {
    private val DamageBoostMode = ListValue("Mode", arrayOf("Minemen", "LegitBoost", "FireBall", "Hypixel", "Custom"), "Minemen")
    private val BoostCustomXZValue = FloatValue("CustomXZBoost", 0F, -10F, 10F).displayable { DamageBoostMode.get().equals("Custom")}
    private val BoostCustomYValue = FloatValue("CustomYBoost", 0F, -10F, 10F).displayable { DamageBoostMode.get().equals("Custom")}

    @EventTarget
    fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        when (DamageBoostMode.get().lowercase()) {
            "custom" -> {
                if (packet is S12PacketEntityVelocity) {
                    val horizontal = BoostCustomXZValue.get()
                    val vertical = BoostCustomYValue.get()

                    packet.motionX = (packet.getMotionX() * horizontal).toInt()
                    packet.motionY = (packet.getMotionY() * vertical).toInt()
                    packet.motionZ = (packet.getMotionZ() * horizontal).toInt()
                }
            }
            "minemen" -> {
                if (packet is S12PacketEntityVelocity) {

                    packet.motionX = (packet.getMotionX() * 6.5F).toInt()
                    packet.motionY = (packet.getMotionY() * 1)
                    packet.motionZ = (packet.getMotionZ() * 6.5F).toInt()
                }
            }
            "legitboost" -> {
                if (packet is S12PacketEntityVelocity) {

                    packet.motionX = (packet.getMotionX() * 1.5F).toInt()
                    packet.motionY = (packet.getMotionY() * 1)
                    packet.motionZ = (packet.getMotionZ() * 1.5F).toInt()
                }
            }
        "fireball" -> {
                if (packet is S12PacketEntityVelocity) {

                    packet.motionX = (packet.getMotionX() * 1F).toInt()
                    packet.motionY = (packet.getMotionY() * 7)
                    packet.motionZ = (packet.getMotionZ() * 1F).toInt()
                }
            }
            "hypixel" -> {
                if (packet is S12PacketEntityVelocity) {

                    packet.motionX = (packet.getMotionX() * 1F).toInt()
                    packet.motionY = (packet.getMotionY() * 2.45F).toInt()
                    packet.motionZ = (packet.getMotionZ() * 1F).toInt()
                }
            }
        }
    }

    override val tag: String?
        get() = DamageBoostMode.get()
}