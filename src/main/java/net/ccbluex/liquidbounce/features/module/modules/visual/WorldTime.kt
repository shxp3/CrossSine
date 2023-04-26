/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.minecraft.network.play.server.S03PacketTimeUpdate

@ModuleInfo(name = "WorldTime", category = ModuleCategory.WORLD)
class WorldTime : Module() {
    private val customWorldTimeValue = IntegerValue("CustomTime", 1000, 0, 24000)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.theWorld.worldTime = customWorldTimeValue.get().toLong()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S03PacketTimeUpdate) {
            event.cancelEvent()
        }
    }
}