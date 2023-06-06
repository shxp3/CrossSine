package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation

@ModuleInfo(name = "AntiFireBall", "Anti FireBall", category = ModuleCategory.COMBAT)
class   AntiFireBall : Module() {
    private val timer = MSTimer()

    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val rotationValue = BoolValue("Rotation",true)

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityFireball && mc.thePlayer.getDistanceToEntity(entity) < 5.5 && timer.hasTimePassed(300)) {
                if(rotationValue.get()) {
                    RotationUtils.setTargetRotation(RotationUtils.getRotationsNonLivingEntity(entity))
                }

                mc.thePlayer.sendQueue.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                if (swingValue.equals("Normal")) {
                    mc.thePlayer.swingItem()
                } else if (swingValue.equals("Packet")) {
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }

                timer.reset()
                break
            }
        }
    }
}