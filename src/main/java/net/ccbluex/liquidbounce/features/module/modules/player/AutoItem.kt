package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.SlotUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemShears
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.MovingObjectPosition


@ModuleInfo(name = "AutoItem", category = ModuleCategory.PLAYER)
object AutoItem : Module() {
    private val autoTool = BoolValue("Auto-Tool", true)
    private val sneakValue = BoolValue("Sneak", false).displayable { autoTool.get() }
    private val onlyTools = BoolValue("Only-Tool", true).displayable { autoTool.get() }
    private val autoWeapon = BoolValue("Auto-Weapon", false)
    private val onlySwordValue = BoolValue("Only-Sword", false).displayable { autoWeapon.get() }
    private val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val spoof = BoolValue("Spoof-Item", true)
    private var prevItem = 0
    var mining = false
    private var bestSlot = 0
    private var attackEnemy = false
    private var prevItemWeapon = 0
    private var spoofTick = 0
    private var delayTick = 0

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (autoTool.get()) {
            if (!mc.gameSettings.keyBindUseItem.isKeyDown && mc.gameSettings.keyBindAttack.isKeyDown && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (!sneakValue.get() || mc.thePlayer.isSneaking)) {
                delayTick++
                var bestSpeed = 0
                if (!mining) {
                    prevItem = mc.thePlayer.inventory.currentItem
                }

                val block = mc.theWorld.getBlockState(mc.objectMouseOver.blockPos).block

                for (i in 0..8) {
                    val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
                    val speed = item.getStrVsBlock(block)

                    if (speed > bestSpeed) {
                        bestSpeed = speed.toInt()
                        bestSlot = i
                    }

                    if (bestSlot != -1) {
                        val item = mc.thePlayer.inventory.getStackInSlot(bestSlot).item
                        if (!onlyTools.get() || (item is ItemShears || item is ItemTool)) {
                            if (delayTick >= delayValue.get()) {
                                SlotUtils.setSlot(bestSlot, spoof.get(), name)
                            }
                        }
                    }
                }
                mining = true
            } else {
                delayTick = 0
                if (mining) {
                    SlotUtils.stopSet()
                    mining = false
                } else {
                    prevItem = mc.thePlayer.inventory.currentItem
                }
            }
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (autoWeapon.get()) {
            if (event.packet is C02PacketUseEntity && event.packet.action == C02PacketUseEntity.Action.ATTACK &&
                attackEnemy
            ) {
                attackEnemy = false

                val (slot, _) = (0..8)
                    .map { Pair(it, mc.thePlayer.inventory.getStackInSlot(it)) }
                    .filter { it.second != null && (it.second.item is ItemSword || (it.second.item is ItemTool && !onlySwordValue.get())) }
                    .maxByOrNull {
                        (it.second.attributeModifiers["generic.attackDamage"].first()?.amount
                            ?: 0.0) + 1.25 * ItemUtils.getEnchantment(it.second, Enchantment.sharpness)
                    } ?: return

                if (slot == mc.thePlayer.inventory.currentItem) { // If in hand no need to swap
                    return
                }

                if (!SlotUtils.changed) {
                    prevItemWeapon = mc.thePlayer.inventory.currentItem
                }
                spoofTick = 15
                SlotUtils.setSlot(slot, spoof.get(), name)
                mc.playerController.updateController()


                // Resend attack packet
                mc.netHandler.addToSendQueue(event.packet)
                event.cancelEvent()
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (autoWeapon.get()) {
            if (spoofTick > 0) {
                if (spoofTick == 1) {
                    SlotUtils.stopSet()
                }
                spoofTick--
            }
        }
    }
}
