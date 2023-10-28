package net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.fireball

import com.jcraft.jogg.Packet
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumps.LongJumpMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.init.Items
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange

class FireBallLongjump : LongJumpMode("FireBall") {
    private val spoofItem = BoolValue("SpoofItem", false)
    private val modeValue = ListValue("${valuePrefix}Mode", arrayOf("Strafe", "Hypixel"), "Hypixel")
    private val strafeBoost = FloatValue("${valuePrefix}StrafeBoost", 2F, 2F, 9.85F).displayable { modeValue.equals("Strafe") }
    private val strafeY = FloatValue("${valuePrefix}StrafeMotionY", 0.42F, 0.42F, 3F).displayable { modeValue.equals("Strafe") }
    private val hypixelBoost = FloatValue("${valuePrefix}HypixelBoost", 1.83F, 0.5F, 1.91F)
    private var slot = 0
    private var placeTicks = 0
    private var placed = false
    private var previtem = 0
    private var flying = false
    override fun onEnable() {
        longjump.autoDisableValue.set(false)
        longjump.motionResetValue.set(false)
        longjump.autoJumpValue.set(false)
        previtem = mc.thePlayer.inventory.currentItem
        flying = false
    }

    override fun onDisable() {
        if (spoofItem.get()) {
            ItemSpoofUtils.stopSpoof()
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        } else {
            mc.thePlayer.inventory.currentItem = previtem
        }
        flying = false
    }
    override fun onUpdate(event: UpdateEvent) {
        slot = InventoryUtils.findItem(Items.fire_charge)
        if (slot == -1 && !placed) {
            ClientUtils.displayAlert("§CNo FireBall§F")
            longjump.state = false
            return
        }
        if (!placed) {
            placeTicks++
        }
        if (!placed) {
            when (placeTicks) {
                1 -> {
                    if (spoofItem.get()) {
                        ItemSpoofUtils.startSpoof(slot)
                        PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(slot))
                    } else {
                        mc.thePlayer.inventory.currentItem = slot
                    }
                }

                3 -> {
                    PacketUtils.sendPacketNoEvent(C08PacketPlayerBlockPlacement())
                    placed = true
                }
            }
        }
        if (placeTicks <= 4) {
            RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw + 180, 88F))
        }
        if (mc.thePlayer.hurtTime == 0) {
            flying = false
        }
        if (!flying) {
            if (modeValue.equals("Strafe")) {
                if (mc.thePlayer.hurtTime == 9 && placed) {
                    MovementUtils.strafe(strafeBoost.get())
                    mc.thePlayer.motionY = strafeY.get().toDouble()
                    flying = true
                }
            } else {
                if (mc.thePlayer.hurtTime == 9 && placed) {
                    MovementUtils.setSpeed(hypixelBoost.get())
                    flying = true
                }
            }
        }
    }
}