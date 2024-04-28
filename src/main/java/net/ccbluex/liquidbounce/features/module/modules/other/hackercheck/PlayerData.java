package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;

public class PlayerData {
    public int b;
    public int c;
    public int d;
    public int e;
    public int f;
    public boolean k;
    public int h;
    public int i;
    public double posY;
    public double posZ;
    public double posX;
    public double deltaXZ;

    public void update(EntityPlayer entityPlayer) {
        final int ticksExisted = entityPlayer.ticksExisted;
        this.posX = entityPlayer.posX - entityPlayer.lastTickPosX;
        this.posY = entityPlayer.posY - entityPlayer.lastTickPosY;
        this.posZ = entityPlayer.posZ - entityPlayer.lastTickPosZ;
        this.deltaXZ = Math.max(Math.abs(this.posX), Math.abs(this.posZ));
        if (this.deltaXZ >= 0.07) {
            ++this.c;
            this.e = ticksExisted;
        }
        else {
            this.c = 0;
        }
        if (Math.abs(this.posY) >= 0.1) {
            this.b = ticksExisted;
        }
        if (entityPlayer.isSneaking()) {
            this.f = ticksExisted;
        }
        if (entityPlayer.isSwingInProgress && entityPlayer.isBlocking()) {
            ++this.d;
        }
        else {
            this.d = 0;
        }
        if (entityPlayer.isSprinting() && entityPlayer.isUsingItem()) {
            ++this.i;
        }
        else {
            this.i = 0;
        }
        if (entityPlayer.rotationPitch >= 70.0f && entityPlayer.getHeldItem() != null && entityPlayer.getHeldItem().getItem() instanceof ItemBlock) {
            if (entityPlayer.swingProgressInt == 1) {
                if (!this.k && entityPlayer.isSneaking()) {
                    ++this.h;
                }
                else {
                    this.h = 0;
                }
            }
        }
        else {
            this.h = 0;
        }
    }

    public void updateSneak(final EntityPlayer entityPlayer) {
        this.k = entityPlayer.isSneaking();
    }
}