package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.module.modules.ghost.AntiKnockBack
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold2
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.features.module.modules.world.BedAura
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.PlayerUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.minecraft.block.BlockAir
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos

class BlinkNofall : NoFallMode("Blink") {
    private var start = false
    private var disable = false
    private var kaState = false
    private var laState = false
    private var bdaState = false
    private var veloState = false
    private var akbState = false
    override fun onPacket(event: PacketEvent) {
            if (PlayerUtils.isOnEdge() && getBP(1) && getBP(2) && getBP(3) && !start && !Scaffold.state && !Scaffold2.state) {
                start = true
                if (KillAura.state) {
                    kaState = true
                    KillAura.state = false
                }
                if (BedAura.state) {
                    bdaState = true
                    BedAura.state = false
                }
                if (KillAura2.state) {
                    laState = true
                    KillAura2.state = false
                }
                if (AntiKnockBack.state) {
                    akbState = true
                    AntiKnockBack.state = false
                }
                if (Velocity.state) {
                    veloState = true
                    Velocity.state = false
                }
            }
        if (start) {
            BlinkUtils.setBlinkState(all = true)
            if (event.packet is C03PacketPlayer) {
                event.packet.onGround = true
            }
            if (mc.thePlayer.fallDistance > 0.2) {
                disable = true
            }
        } else {
            BlinkUtils.setBlinkState(off = true, release = true)
        }
        if (mc.thePlayer.onGround && disable) {
            start = false
            disable = false
            if (kaState) {
                KillAura.state = true
                kaState = false
            }
            if (bdaState) {
                BedAura.state = true
                bdaState = false
            }
            if (laState) {
                KillAura2.state = true
                laState = false
            }
            if (akbState) {
                AntiKnockBack.state = true
                akbState = false
            }
            if (veloState) {
                Velocity.state = true
                veloState = false
            }
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
        if(start) {
            start = false
            if (kaState) {
                KillAura.state = true
                kaState = false
            }
            if (bdaState) {
                BedAura.state = true
                bdaState = false
            }
            if (laState) {
                KillAura2.state = true
                laState = false
            }
            if (akbState) {
                AntiKnockBack.state = true
                akbState = false
            }
            if (veloState) {
                Velocity.state = true
                veloState = false
            }
        }
        disable = false
        BlinkUtils.setBlinkState(off = true, release = true)
    }

    override fun onEnable() {
        start = false
        disable = false
    }

    private fun getBP(pos: Int): Boolean {
        return BlockUtils.getBlock(
            BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - pos,
                mc.thePlayer.posZ
            )
        ) is BlockAir
    }
}