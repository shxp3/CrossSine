/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other.disablers.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.other.disablers.DisablerMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.play.client.C03PacketPlayer

class BoatDisabler : DisablerMode("Boat") {
    private var canModify = false
    private val noGroundValue = BoolValue("${valuePrefix}NoGround", false)
    
    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.ridingEntity != null) {
            mc.thePlayer.rotationPitch = (90.0).toFloat()
            mc.thePlayer.swingItem()
            mc.playerController.attackEntity(mc.thePlayer, mc.thePlayer.ridingEntity)
            mc.thePlayer.swingItem()
            mc.playerController.attackEntity(mc.thePlayer, getNearBoat())
            canModify = true
            disabler.debugMessage("Destroy Boat")
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (mc.thePlayer.ridingEntity != null) {
            canModify = true
        }

        if (canModify && packet is C03PacketPlayer) {
            packet.onGround = !noGroundValue.get()
        }
    }

    override fun onWorld(event: WorldEvent) {
        canModify = false
    }

    override fun onEnable() {
        disabler.debugMessage("Place 2 Boats Next To Each other And Right Click To Use It!")
        canModify = false
    }

    private fun getNearBoat():Entity? {
        val entities = mc.theWorld.loadedEntityList
        for (entity_ in entities) {
            if (entity_ is EntityBoat) {
                if (entity_ != mc.thePlayer.ridingEntity) {
                    return entity_
                }
            }
        }
        return null
    }
}
