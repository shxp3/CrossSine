package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.data.SampleListD;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ScaffoldCheck extends Check {

    public ScaffoldCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Scaffold";
        checkViolationLevel = 5;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.isRiding() || data.serverPosYList.size() < 4) return;
        if (handlePlayer.isSwingInProgress && handlePlayer.hurtTime == 0 && data.serverPitchList.get(0) > 50f && data.getSpeedXZSq() > 9d) {
            final ItemStack itemStack = handlePlayer.getHeldItem();
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock) {
                final double angleDiff = Math.abs(data.getMoveLookAngleDiff());
                final double speedXZSq = data.getSpeedXZSq();
                if (angleDiff > 165d && speedXZSq < 100d) {
                    final double speedY = data.speedYList.get(0);
                    final double avgAccelY = avgAccel(data.serverPosYList);
                    if (isAlmostZero(avgAccelY)) return;
                    if (speedY < 15d && speedY > 4d && avgAccelY > -25d) {
                        flag("Scaffold Flag", 1);
                    } else if (speedY < 4d && speedY > -1d && Math.abs(speedY) > 0.005d && speedXZSq > 25d) {
                        flag("Scaffold Flag", 1);
                    }
                }
            }
        }
    }

    private static double avgAccel(SampleListD list) {
        return 50d * (list.get(3) - list.get(2) - list.get(1) + list.get(0));
    }

    private static boolean isAlmostZero(double d) {
        return Math.abs(d) < 0.001d;
    }
}
