package net.ccbluex.liquidbounce.features.module.modules.ghost

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.timer.TimerMS
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemPotion

@ModuleInfo(name = "ReFill", category = ModuleCategory.GHOST)
class Refill : Module() {
    private val delayValue = IntegerValue("Delay", 500,0 , 2000)
    private val soupValue = BoolValue("Soup", false)
    private val potValue = BoolValue("Pot", false)
    private val onInv = BoolValue("On-Inventory", false)
    private val time = TimerMS()
    private fun add(value: Item) {
        for (i in 9 until 37) {
            val itemStack = mc.thePlayer!!.inventoryContainer.getSlot(i).stack
            if (itemStack != null && itemStack.item == value) {
                mc.playerController!!.windowClick(0, i, 0, 1, mc.thePlayer)
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val value = if (soupValue.get()) Items.mushroom_stew else if (potValue.get()) ItemPotion.getItemById(373) else null
        refill(value)
    }

    private fun refill(value: Item?) {
        if (!onInv.get() || mc.currentScreen is GuiInventory) {
                if (InventoryUtils.hasSpaceHotbar() && time.hasTimePassed(delayValue.get().toLong())) {
                value?.let { add(it) }
                time.reset()
            }
        }
    }
}