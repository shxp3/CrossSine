package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.verus

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer

class DamageLongJump: LongJumpMode("DamageVerus") {
    private val verusSpeed: FloatValue = object : FloatValue("${valuePrefix}Speed", 6F, 1F, 10F) {
        override fun onChange(oldValue: Float, newValue: Float) {
            if (verusBoostSpeed.get() > newValue) set(verusBoostSpeed.get())
        }
    }
    private val verusBoostSpeed: FloatValue = object : FloatValue("${valuePrefix}Boost", 4.25f, 1f, 10f) {
        override fun onChange(oldValue: Float, newValue: Float) {
            if (verusSpeed.get() < newValue) set(verusSpeed.get())
        }
    }
    private val verusY = FloatValue("${valuePrefix}MotionY", 0.42F, 0.30F, 2F)
    private var verjump = 0
    private var damaged = false
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer) {
            if (verjump < 3) {
                packet.onGround = false
            }
        }
    }

    override fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround && verjump < 4) {
            MovementUtils.jump(true)
            verjump += 1
        }
        if (mc.thePlayer.hurtTime == 9) {
            damaged = true
            mc.thePlayer.motionY = verusY.get().toDouble()
        }
        MovementUtils.strafe(if (mc.thePlayer.hurtTime > 7) verusBoostSpeed.get() else verusSpeed.get())
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