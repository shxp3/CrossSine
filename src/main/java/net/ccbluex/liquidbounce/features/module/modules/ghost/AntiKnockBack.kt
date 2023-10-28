package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.network.play.server.S12PacketEntityVelocity

@ModuleInfo(name = "AntiKnockback", spacedName = "AntiKnockback", category = ModuleCategory.GHOST)
class AntiKnockBack : Module() {
    private val xz = IntegerValue("X-Z", 0, 0, 100)
    private val y = IntegerValue("Y", 0, 0, 100)
    private val onlyGround = BoolValue("OnlyGround", false)
    private val onlyAir = BoolValue("OnlyAir", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (onlyGround.get() && !mc.thePlayer.onGround) return
        if (onlyAir.get() && mc.thePlayer.onGround) return
        val e = event.packet
        if (e is S12PacketEntityVelocity) {
            if (mc.thePlayer == null || (mc.theWorld?.getEntityByID(e.entityID) ?: return) != mc.thePlayer) {
                return
            }
            if (xz.get() == 0 && y.get() == 0) {
                event.cancelEvent()
            }
            if (xz.get() == 0 && y.get() == 100) {
                if (CrossSine.combatManager.inCombat) {
                    mc.thePlayer.motionY = e.motionY.toDouble() / 8000.0
                    event.cancelEvent()
                } else {
                    e.motionX = (e.getMotionX() * xz.get() / 100)
                    e.motionY = (e.getMotionY() * y.get() / 100)
                    e.motionZ = (e.getMotionZ() * xz.get() / 100)
                }
            }
                e.motionX = (e.getMotionX() * xz.get() / 100)
                e.motionY = (e.getMotionY() * y.get() / 100)
                e.motionZ = (e.getMotionZ() * xz.get() / 100)
        }
    }
}