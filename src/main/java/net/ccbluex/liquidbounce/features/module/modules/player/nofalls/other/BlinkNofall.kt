package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.other

import ibxm.Player
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.ghost.SafeWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FontValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.down
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import org.lwjgl.input.Mouse
import java.awt.Color

class BlinkNofall : NoFallMode("Blink") {
    private val limitValue = BoolValue("${valuePrefix}limitUse", true)
    private var start = false
    private var disable = false

    override fun onPacket(event: PacketEvent) {
        val state = (KillAura.state || Scaffold.state || SafeWalk.state || Speed.state)
        if (limitValue.get() && state) {
            return
        }
        if (PlayerUtils.isOnEdge() && getBP(1F) && getBP(2F) && getBP(3.5F) && !start) {
            start = true
        }
        if (start) {
            Blink.array = false
            Blink.state = true
            if (mc.thePlayer.fallDistance > 1.5) {
                disable = true
                if (event.packet is C03PacketPlayer) {
                    event.packet.onGround = true
                }
            }
        } else {
            Blink.state = false
            Blink.array = true
        }
        if (mc.thePlayer.onGround && disable) {
            start = false
            disable = false
        }
    }

    override fun onRender2D(event: Render2DEvent) {
        if (start) {
            val string = "Blinking : ${BlinkUtils.bufferSize()}"
                mc.fontRendererObj.drawCenteredString(
                    string,
                    ScaledResolution(mc).scaledWidth / 2F,
                    ScaledResolution(mc).scaledHeight / 2F + 10F,
                    ClientTheme.getColor(1).rgb,
                    true
                )
        }
    }

    override fun onDisable() {
        start = false
        disable = false
    }

    override fun onEnable() {
        start = false
        disable = false
    }
    private fun getBP(pos: Float) : Boolean {
        return mc.theWorld?.getBlockState(BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - pos, mc.thePlayer.posZ))!!.block is BlockAir
    }
}