/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.client.gui.inventory.GuiInventory

@ModuleInfo(name = "NoInvClose", spacedName = "NoInvClose",category = ModuleCategory.OTHER)
class NoInvClose : Module() {
    @EventTarget
    fun onPacket(event: PacketEvent){
        if (mc.theWorld == null || mc.thePlayer == null) return
        
        if (event.packet is S2EPacketCloseWindow && mc.currentScreen is GuiInventory) event.cancelEvent()
    }
}