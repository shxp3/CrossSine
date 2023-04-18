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
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S2BPacketChangeGameState

@ModuleInfo(name = "WorldTime", category = ModuleCategory.VISUAL)
class WorldTime : Module() {
   private val customWorldTimeValue = IntegerValue("CustomTime", 1000, 0, 24000)


    var i = 0L

    override fun onDisable() {
        i = 0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.theWorld.worldTime = customWorldTimeValue.get().toLong()
    }
}
