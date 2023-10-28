package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.verus

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer

class DamageLongJump: LongJumpMode("DamageVerus") {
    private val verusBoost = FloatValue("${valuePrefix}Boost", 4.25f, 1f, 10f)
    private val verusY = FloatValue("${valuePrefix}MotionY", 0.42F, 0.30F, 2F)
    private var verjump = 0
    private var damaged = false
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer) {
            if (verjump < 4) {
                packet.onGround = false
            }
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && verjump < 4) {
            mc.thePlayer.jump()
            verjump += 1
        }
        if (mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround) {
            damaged = true
            MovementUtils.strafe(verusBoost.get())
            mc.thePlayer.motionY = verusY.get().toDouble()
        }
    }

    override fun onMove(event: MoveEvent) {
        if (!damaged) {
            event.zeroXZ()
        }
    }
    override fun onDisable() {
        verjump = 0
        damaged = false
    }

    override fun onEnable() {
        verjump = 0
        damaged = false
    }
}