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
import net.ccbluex.liquidbounce.utils.extensions.drawCenteredString
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import org.lwjgl.input.Mouse
import java.awt.Color

class BlinkNofall : NoFallMode("Blink") {
    private val fallDistValue = IntegerValue("${valuePrefix}FallDistance", 9, 3, 15)
    private val textValue = BoolValue("${valuePrefix}ShowText", false)
    private val colorTheme = BoolValue("${valuePrefix}ColorTheme", false).displayable { textValue.get() }
    private val limitValue = BoolValue("${valuePrefix}limitUse", true)
    private val noMouse = BoolValue("${valuePrefix}NoMouse", false)
    private var start = false
    private var disable = false

    override fun onPacket(event: PacketEvent) {
            val state = (KillAura.state || Scaffold.state || SafeWalk.state || Speed.state || (noMouse.get() && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))))
            if (limitValue.get() && state) {
                start = false
                disable = false
                return
            }
            if (PlayerUtils.isOnEdge() && mc.thePlayer.fallDistance < fallDistValue.get()) {
                start = true
            }
            if (start) {
                Blink.array = false
                Blink.state = true
                if (event.packet is C03PacketPlayer) {
                    event.packet.onGround = true
                }
            } else {
                Blink.state = false
                Blink.array = true
            }
            if (mc.thePlayer.fallDistance > 0.5) {
                disable = true
            }
            if (mc.thePlayer.onGround && disable) {
                disable = false
                start = false
            }
    }

    override fun onRender2D(event: Render2DEvent) {
        if (start) {
            mc.fontRendererObj.drawCenteredString(
                "Blinking : ${BlinkUtils.bufferSize()}",
                ScaledResolution(mc).scaledWidth / 2F,
                ScaledResolution(mc).scaledHeight / 2F + 10F,
                if (colorTheme.get()) ClientTheme.getColor(1).rgb else Color.WHITE.rgb,
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
}