package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.fireball

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.item.ItemFireball
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S12PacketEntityVelocity


class FireBallLongjump : LongJumpMode("FireBall") {
    private val spoofValue = BoolValue("SpoofItem", true)
    private val speedValue = FloatValue("Speed", 1.5F, 0F, 2F)
    private val motionValue = FloatValue("Motion", 0.3F, 0.01F, 0.4F)
    private val strafeValue = BoolValue("Strafe", false)
    private var ticks = -1
    private var setSpeed = false
    private var sentPlace = false
    private var initTicks = 0
    private var velocity = false
    private var thrown = false
    override fun onEnable() {
        if (Velocity.state) {
            Velocity.state = false
            velocity = true
        }
    }
    override fun onDisable() {
        ticks = -1
        setSpeed = false
        sentPlace = false
        initTicks = 0
        if (velocity) {
            Velocity.state = true
            velocity = false
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID != mc.thePlayer.entityId) {
                return
            }
            if (sentPlace) {
                ticks = 0
                setSpeed = true
                thrown = false
            }
        }
    }

    override fun onPreMotion(event: MotionEvent) {
        if (getFBSlot() == -1 && !sentPlace) {
            ClientUtils.displayChatMessage("FireBall not found")
            longjump.state = false
        }
        when (initTicks) {
            1 -> {
                if (getFBSlot() != -1 && getFBSlot() != mc.thePlayer.inventory.currentItem) {
                    SlotUtils.setSlot(getFBSlot(), spoofValue.get(), longjump.name)
                }
                RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw - 180F, 89F), 1)
            }

            2 -> {
                RotationUtils.setTargetRotation(Rotation(MovementUtils.movingYaw - 180F, 89F), 1)
                if (!sentPlace) {
                    mc.rightClickMouse()
                    sentPlace = true
                }
            }

            3 -> SlotUtils.stopSet()
            5 -> if (longjump.autoDisableValue.get()) longjump.state = false
        }

        initTicks++

        if (ticks >= 0) {
            mc.thePlayer.motionY = motionValue.get().toDouble()
            if (strafeValue.get()) MovementUtils.strafe()
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