
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.item.*
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing


@ModuleInfo(name = "NoSlow", category = ModuleCategory.MOVEMENT)
class NoSlow : Module() {
    //Basic settings
    private val modeValue = ListValue("PacketMode", arrayOf("Vanilla","SwitchItem", "OldIntave", "Interact"), "Vanilla")
    private val onlyGround = BoolValue("OnlyGround", false)
    private val onlyMove = BoolValue("OnlyMove", false)
    private val onlyKillAura = BoolValue("OnlyAura", false)
    private val blockModifyValue = BoolValue("Blocking", true)
    private val blockMultiplier = FloatValue("BlockMultiplier", 1.0F, 0.2F, 1.0F).displayable { blockModifyValue.get() }
    private val consumeModifyValue = BoolValue("Consume", true)
    private val consumeMultiplier = FloatValue("ConsumeMultiplier", 1.0F, 0.2F, 1.0F).displayable { consumeModifyValue.get() }
    private val bowModifyValue = BoolValue("Bow", true)
    private val bowMultiplier = FloatValue("BowMultiplier", 1.0F, 0.2F, 1.0F).displayable { bowModifyValue.get() }
    val soulSandValue = BoolValue("SoulSand", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if(mc.thePlayer == null || mc.theWorld == null)
            return
        if (shouldNoSlow) {
            when (modeValue.get().lowercase()) {
                "switchitem" -> {
                    PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem  % 8 + 1))
                    PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }
                "oldintave" -> {
                    if (mc.thePlayer.heldItem.item !is ItemSword && event.eventState == EventState.PRE) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    }
                }
            }
        }
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if(mc.thePlayer == null || mc.theWorld == null)
            return
        if (shouldNoSlow) {
            if (modeValue.equals("Interact")) {
                if (((mc.thePlayer.isUsingItem && !mc.thePlayer.isBlocking) && mc.thePlayer.ticksExisted % 3 == 0)) {
                    PacketUtils.sendPacketNoEvent(
                        C08PacketPlayerBlockPlacement(
                            BlockPos(-1, -1, -1),
                            EnumFacing.UP.index,
                            null,
                            0.0f,
                            0.0f,
                            0.0f
                        )
                    )
                }
            }
        }
    }
    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        if(mc.thePlayer == null || mc.theWorld == null || !shouldNoSlow)
            return

        event.forward = getMultiplier(mc.thePlayer.heldItem?.item, true)
        event.strafe = getMultiplier(mc.thePlayer.heldItem?.item, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk -> {
            if (consumeModifyValue.get())
                if (isForward) this.consumeMultiplier.get() else this.consumeMultiplier.get() else 0.2F
        }
        is ItemSword -> {
            if (blockModifyValue.get())
                if (isForward) this.blockMultiplier.get() else this.blockMultiplier.get() else 0.2F
        }
        is ItemBow -> {
            if (bowModifyValue.get())
                if (isForward) this.bowMultiplier.get() else this.bowMultiplier.get() else 0.2F
        }
        else -> 0.2F
    }
    val shouldNoSlow : Boolean
        get() = (!onlyMove.get() || MovementUtils.isMoving()) && (!onlyGround.get() || mc.thePlayer.onGround) && (mc.thePlayer.heldItem.item !is ItemSword || !onlyKillAura.get() || KillAura.state && KillAura.currentTarget != null || KillAura2.state && KillAura2.target != null ||  KillAura2.state && KillAura2.target != null) && (blockModifyValue.get() && (mc.thePlayer.isBlocking || KillAura.blockingStatus) && mc.thePlayer.heldItem?.item is ItemSword)
                || (bowModifyValue.get() && mc.thePlayer.isUsingItem && mc.thePlayer.heldItem?.item is ItemBow)
                || (consumeModifyValue.get() && mc.thePlayer.isUsingItem && (mc.thePlayer.heldItem?.item is ItemFood || mc.thePlayer.heldItem?.item is ItemPotion || mc.thePlayer.heldItem?.item is ItemBucketMilk))
    override val tag: String
        get() = modeValue.get()
}