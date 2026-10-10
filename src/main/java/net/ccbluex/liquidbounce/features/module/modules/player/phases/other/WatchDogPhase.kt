package net.ccbluex.liquidbounce.features.module.modules.player.phases.other

import net.ccbluex.liquidbounce.event.ChatEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.player.phases.PhaseMode
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.BlockPos

class WatchDogPhase : PhaseMode("WatchDog") {
    private var startBlink = false
    private var timerMS = TimerMS()
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S02PacketChat) {
            val chat = packet.chatComponent.unformattedText
            if (chat == "The games start in 3 seconds!") {
                timerMS.reset()
                startBlink = true
                BlinkUtils.setBlinkState(all = true)
                mc.theWorld.setBlockToAir(BlockPos(mc.thePlayer).down())
            }
        }
    }
    override fun onUpdate(event: UpdateEvent) {
        if (timerMS.hasTimePassed(3400)) {
            BlinkUtils.setBlinkState(off = true, release = true)
            startBlink = false
        }
    }
}