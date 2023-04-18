package net.ccbluex.liquidbounce.utils.AutoArmor;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ArmorPiecee {

    private final ItemStack itemStack;
    private final int slot;

    public ArmorPiecee(ItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    public int getArmorType() {
        return ((ItemArmor) itemStack.getItem()).armorType;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
