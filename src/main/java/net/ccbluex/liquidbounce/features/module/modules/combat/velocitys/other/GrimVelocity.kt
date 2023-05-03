package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S32PacketConfirmTransaction

class GrimVelocity : VelocityMode("Grim") {
    private val cancelPacket = IntegerValue("S32", 4, 2, 6).displayable {velocity.modeValue.equals("Grim")}
    private val resetPersec = IntegerValue("Time", 6, 4, 10).displayable {velocity.modeValue.equals("Grim")}
    private val noscaffold = BoolValue("NoScaffold", false).displayable {velocity.modeValue.equals("Grim")}
    private val scaffold = CrossSine.moduleManager[Scaffold::class.java]!!
    var grimTCancel = 0
    var updates = 0

    override fun onEnable() {
        grimTCancel = 0
    }

    override fun onPacket(event: PacketEvent) {
        var packet = event.packet
        if (!scaffold.state || !noscaffold.get()) {
            if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
                event.cancelEvent()
                grimTCancel = cancelPacket.get()
            }
            if (packet is S32PacketConfirmTransaction && grimTCancel > 0) {
                event.cancelEvent()
                grimTCancel--
            }
        }
        else {
            grimTCancel = 0
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        updates++

        if (resetPersec.get() > 0) {
            if (updates >= 0 || updates >= resetPersec.get()) {
                updates = 0
                if (grimTCancel > 0){
                    grimTCancel--
                }
            }
        }
    }
}