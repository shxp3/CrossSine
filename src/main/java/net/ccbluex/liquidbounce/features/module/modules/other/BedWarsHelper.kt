package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PreUpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.special.NotificationUtil
import net.ccbluex.liquidbounce.features.special.TYPE
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.util.ResourceLocation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@ModuleInfo("BedWarsHelper", ModuleCategory.OTHER)
class BedWarsHelper : Module() {
    private val autoChest = BoolValue("Auto-Chest", false)
    private val autoClose = BoolValue("Auto-Close", true).displayable { autoChest.get() }
    private val delayValue = IntegerValue("Delay", 150, 0, 500).displayable { autoChest.get() }

    private enum class ChestMode { NONE, DEPOSIT, WITHDRAW }
    private var chestMode = ChestMode.NONE
    private var lastContainer: Any? = null
    private val items: Set<Item> = HashSet(Arrays.asList(Items.iron_ingot, Items.gold_ingot, Items.diamond, Items.emerald))
    private val stoneSwordList = ArrayList<EntityPlayer>()
    private val ironSwordList = ArrayList<EntityPlayer>()
    private val diamondSwordList = ArrayList<EntityPlayer>()
    private val fireBallList = ArrayList<EntityPlayer>()
    private val enderpearlList = ArrayList<EntityPlayer>()
    private val tntList = ArrayList<EntityPlayer>()
    private val obsidianList = ArrayList<EntityPlayer>()
    private val diamondArmorList = ArrayList<EntityPlayer>()
    private val invisibilityPotionList = ArrayList<EntityPlayer>()
    private val delayMS = TimerMS()

    @EventTarget
    fun onPreUpdate(event: PreUpdateEvent) {
        for (entity in mc.theWorld.playerEntities) {
            if (entity is EntityPlayer) {
                if (entity == mc.thePlayer) continue
                if (entity.heldItem.item == Items.stone_sword && !stoneSwordList.contains(entity)) {
                    alert("§C${entity.name}§F has Stone Sword", entity, stoneSwordList)
                }
                if (entity.heldItem.item == Items.iron_sword && !ironSwordList.contains(entity)) {
                    alert("§C${entity.name}§F has Iron Sword", entity, ironSwordList)
                }
                if (entity.heldItem.item == Items.diamond_sword && !diamondSwordList.contains(entity)) {
                    alert("§C${entity.name}§F has Diamond Sword", entity, diamondSwordList)
                }
                if (entity.heldItem.item == Items.fire_charge && !fireBallList.contains(entity)) {
                    alert("§C${entity.name}§F has Fire Ball", entity, fireBallList)
                }
                if (entity.heldItem.item == Items.ender_pearl && !enderpearlList.contains(entity)) {
                    alert("§C${entity.name}§F has Ender Pearl", entity, enderpearlList)
                }
                if (entity.heldItem.item == ItemBlock.getItemById(46) && !tntList.contains(entity)) {
                    alert("§C${entity.name}§F has TNT", entity, tntList)
                }
                if (entity.heldItem.item == ItemBlock.getItemById(49) && !obsidianList.contains(entity)) {
                    alert("§C${entity.name}§F has Obsidian", entity, obsidianList)
                }
                if (isWearingDiamondArmor(entity) && !diamondArmorList.contains(entity)) {
                    alert("§C${entity.name}§F has Diamond Armor", entity, diamondArmorList)
                }
                if (entity.heldItem.item == Potion.invisibility && !invisibilityPotionList.contains(entity)) {
                    alert("§C${entity.name}§F has Invisibility Potion", entity, invisibilityPotionList)
                }
                if (entity.heldItem.item == Items.wooden_sword && (stoneSwordList.contains(entity) || ironSwordList.contains(entity) || diamondSwordList.contains(entity))) {
                    stoneSwordList.remove(entity)
                    ironSwordList.remove(entity)
                    diamondSwordList.remove(entity)
                }
            }
        }
        if (autoChest.get()) {
            val screen = mc.currentScreen
            if (screen is GuiChest) {
            if ((screen.lowerChestInventory == null || !screen.lowerChestInventory.name.contains(
                    ItemStack(Item.itemRegistry.getObject(ResourceLocation("minecraft:chest"))).displayName
                ) || !screen.lowerChestInventory.name.contains(
                    ItemStack(Item.itemRegistry.getObject(ResourceLocation("minecraft:ender_chest"))).displayName
                ))
            ) return
                val container = mc.thePlayer.openContainer
                if (container !== lastContainer) {
                    lastContainer = container
                    val playerHasCurrency = mc.thePlayer.inventory.mainInventory.any {
                        it != null && items.contains(it.item)
                    }
                    chestMode = if (playerHasCurrency) ChestMode.DEPOSIT else ChestMode.WITHDRAW
                    delayMS.reset()
                }
            } else {
                lastContainer = null
                chestMode = ChestMode.NONE
            }

            if (screen is GuiChest && chestMode != ChestMode.NONE) {
                if (!delayMS.hasTimePassed(delayValue.get().toLong())) return

                val container = mc.thePlayer.openContainer
                val windowId = container.windowId
                val slots = container.inventorySlots
                if (slots.size < 36) return
                val chestSize = slots.size - 36

                var found = false
                when (chestMode) {
                    ChestMode.DEPOSIT -> {
                        for (i in chestSize until slots.size) {
                            val stack = slots[i].stack
                            if (stack != null && items.contains(stack.item)) {
                                mc.playerController.windowClick(windowId, i, 0, 1, mc.thePlayer)
                                delayMS.reset()
                                found = true
                                break
                            }
                        }
                    }
                    ChestMode.WITHDRAW -> {
                        for (i in 0 until chestSize) {
                            val stack = slots[i].stack
                            if (stack != null && items.contains(stack.item)) {
                                mc.playerController.windowClick(windowId, i, 0, 1, mc.thePlayer)
                                delayMS.reset()
                                found = true
                                break
                            }
                        }
                    }
                    ChestMode.NONE -> {}
                }
                if (!found) {
                    chestMode = ChestMode.NONE
                    if (autoClose.get()) mc.thePlayer.closeScreen()
                }
            }
        }
    }

    override fun onDisable() {
        chestMode = ChestMode.NONE
        lastContainer = null
    }

    private fun isWearingDiamondArmor(player: EntityPlayer): Boolean {
        val armorInventory = player.inventory.armorInventory

        for (itemStack in armorInventory) {
            if (itemStack.item == Items.diamond_leggings || itemStack.item == Items.diamond_chestplate) {
                return true
            }
        }

        return false
    }
    private fun alert(string: String, entity: EntityPlayer, list: ArrayList<EntityPlayer>) {
        CrossSine.notification.list.add(NotificationUtil("BWH", "$string, Distance: ${DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH)).format(mc.thePlayer.getDistanceToEntity(entity))}", TYPE.WARNING, System.currentTimeMillis(), 3000))
        ClientUtils.displayChatMessage("§F[§CBWH§F] $string, Distance: ${DecimalFormat("0.#", DecimalFormatSymbols(Locale.ENGLISH)).format(mc.thePlayer.getDistanceToEntity(entity))}")
        list.add(entity)
    }
}