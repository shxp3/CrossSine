package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemBlock;

public class ScaffoldCheck extends Check {
    short flagTime = 0;
    public ScaffoldCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Scaffold";
        checkViolationLevel = 10;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.rotationPitch > 75 && handlePlayer.rotationPitch < 90 && handlePlayer.isSwingInProgress) {
            if (handlePlayer.getHeldItem().getItem() instanceof ItemBlock) {
                if (MovementUtils.INSTANCE.getSpeed(handlePlayer) >= 0.11 && handlePlayer.onGround && !handlePlayer.isSneaking()) {
                    if (++flagTime > 3) {
                        flag("Scaffold Speed limit", 1);
                    }
                }
                if (MovementUtils.INSTANCE.getSpeed(handlePlayer) >= 0.23 && !handlePlayer.onGround && !handlePlayer.isSneaking()) {
                    if (++flagTime > 3) {
                        flag("Scaffold Speed limit", 1);
                    }
                }
            }
        }
    }

    @Override
    public void reset() {
        flagTime = 0;
    }
}
