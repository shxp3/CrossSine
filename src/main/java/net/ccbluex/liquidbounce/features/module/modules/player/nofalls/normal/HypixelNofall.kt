package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.normal

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class HypixelNofall : NoFallMode("WatchDog") {
    override fun onPacket(event: PacketEvent) {
        if(event.packet is C03PacketPlayer && mc.thePlayer.fallDistance > 2.5) {
            event.packet.onGround = true
            CrossSine.moduleManager.getModule(Blink::class.java)!!.state = true
        }
    }
    override fun onDisable() {
        CrossSine.moduleManager.getModule(Blink::class.java)!!.state = false
    }
}