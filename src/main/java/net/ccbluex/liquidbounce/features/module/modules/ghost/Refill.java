package net.ccbluex.liquidbounce.features.module.modules.ghost;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;


@ModuleInfo(name = "Refill", category = ModuleCategory.GHOST)
public class Refill extends Module {

    public IntegerValue delay = new IntegerValue("Delay", 500, 0, 2000);
    public BoolValue Soup = new BoolValue("Soup", false);
    public BoolValue Pot = new BoolValue( "Pot", false);
    public BoolValue onInv = new BoolValue( "onInv", false);
    MSTimer time = new MSTimer();
    Item value;

    public long lastMs;
    public static boolean isHotbarFull() {
        for (int i = 0; i <= 36; ++i) {
            ItemStack itemstack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemstack == null) {
                return false;
            }
        }

        return true;
    }

    public static void refill(Item value) {
        for (int i = 9; i < 37; ++i) {
            ItemStack itemstack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (itemstack != null && itemstack.getItem() == value) {
                mc.playerController.windowClick(0, i, 0, 1, mc.thePlayer);
                break;
            }
        }
    }

    @EventTarget
    public void onUpdate(MotionEvent event) {
        if (Soup.getValue()) {
            this.value = Items.mushroom_stew;
        } else if (Pot.getValue()) {
            ItemPotion itempotion = Items.potionitem;
            this.value = ItemPotion.getItemById(373);
        }

        this.refill();
    }

    private void refill() {
        if (!onInv.getValue() || mc.currentScreen instanceof GuiInventory) {
            if (!isHotbarFull() && this.isDelayComplete(delay.getValue())) {
                refill(this.value);
                this.time.reset();
            }
        }
    }

    public boolean isDelayComplete(final long delay) {
        return System.currentTimeMillis() - this.lastMs >= delay;
    }

}
