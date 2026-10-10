package net.ccbluex.liquidbounce.features.module.modules.player.nofalls.normal

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofalls.NoFallMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.timer.tickTimer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemBucket
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import kotlin.math.ceil
import kotlin.math.sqrt

class MLGNofall : NoFallMode("MLG") {
    private val rotation = BoolValue("SilentRotation", false)
    private val turnSpeed = IntegerValue("TurnSpeed", 90, 1, 90)
    private val fallDistance = FloatValue("FallDistance", 3F, 3f, 8F)
    private var placed = false
    private var takeBack = false
    override fun onDisable() {
        placed = false
        takeBack = false
    }
    override fun onUpdate(event: UpdateEvent) {
        if (!takeBack) {
            if (findBuket() != -1) {
                ClientUtils.displayAlert("Failed to find WaterBuket")
                return
            }
            SlotUtils.setSlot(findBuket(), true, nofall.name)
            if (mc.thePlayer.fallDistance >= fallDistance.get()) {
                val limitAngle = RotationUtils.limitAngleChange(RotationUtils.serverRotation, Rotation(MovementUtils.movingYaw, 90F), turnSpeed.get().toFloat())
                if (rotation.get()) {
                    RotationUtils.setTargetRotation(limitAngle, 100)
                } else {
                    limitAngle.toPlayer(mc.thePlayer)
                }
                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    if(!placed) {
                       mc.rightClickMouse()
                       placed = true
                    }
                    if (mc.thePlayer.isInWater) {
                        mc.rightClickMouse()
                        takeBack = true
                        SlotUtils.stopSet()
                    }
                }
            }
        }
    }
    private fun findBuket() : Int {
        var slot = -1
        for (i in 36..44) {
            val itemStack = mc.thePlayer.inventoryContainer.getSlot(i).stack

            if (itemStack != null && (itemStack.item == Items.water_bucket || itemStack.item is ItemBlock && (itemStack.item as ItemBlock).block == Blocks.web)) {
                slot = i - 36
            }
        }
        return slot
    }
}