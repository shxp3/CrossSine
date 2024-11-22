package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.fireball

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.item.ItemFireball
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S12PacketEntityVelocity


class FireBallLongjump : LongJumpMode("FireBall") {
    private val speedValue = FloatValue("Speed", 1.5F, 0F, 2F)
    private val timeValue = IntegerValue("TimeTicks", 30, 10, 60)
    private val motionValue = FloatValue("Motion", 0.3F, 0.01F, 0.4F)
    private val strafeValue = BoolValue("Strafe", false)
    private var prevSlot = -1
    private var ticks = -1
    private var setSpeed = false
    private var sentPlace = false
    private var initTicks = 0
    private var thrown = false
    override fun onDisable() {
        ticks = -1
        prevSlot = -1
        setSpeed = false
        sentPlace = false
        initTicks = 0
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement
            && packet.getStack() != null && packet.getStack().item is ItemFireball
        ) {
            thrown = true
        }
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID != mc.thePlayer.entityId) {
                return
            }
            if (thrown) {
                ticks = 0
                setSpeed = true
                thrown = false
            }
        }
    }

    override fun onPreMotion(event: MotionEvent) {
        when (initTicks) {
            1 -> {
                if (getFBSlot() != -1 && getFBSlot() != mc.thePlayer.inventory.currentItem) {
                    prevSlot = mc.thePlayer.inventory.currentItem
                    SpoofItemUtils.startSpoof(prevSlot, true)
                    mc.thePlayer.inventory.currentItem = getFBSlot()
                }
                RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw - 180F, 89F), 1)
            }

            2 -> {
                RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw - 180F, 89F), 1)
                if (!sentPlace) {
                    PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                    sentPlace = true
                }
            }

            3 -> SpoofItemUtils.stopSpoof()
        }

        initTicks++

        if (ticks > timeValue.get()) {
            if(longjump.autoDisableValue.get()) longjump.state = false
        } else if (ticks >= 0) {
            mc.thePlayer.motionY = motionValue.get().toDouble()
            if (strafeValue.get()) MovementUtils.strafe()
        }

        if (setSpeed) {
            MovementUtils.strafe(speedValue.get())
            ticks++
        }

        if (setSpeed) {
            if (ticks > 1) {
                setSpeed = false
                return
            }
            ticks++
            MovementUtils.strafe(speedValue.get())
        }
    }
    private fun getFBSlot(): Int {
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item is ItemFireball) {
                return i - 36
            }
        }
        return -1
    }
}