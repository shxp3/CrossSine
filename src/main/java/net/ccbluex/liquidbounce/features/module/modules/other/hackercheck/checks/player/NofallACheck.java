package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.player;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class NofallACheck extends Check {
    boolean fall;
    public NofallACheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "NofallA";
        checkViolationLevel = 20;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.fallDistance > 3) {
            fall = true;
        }
        if (fall && handlePlayer.fallDistance == 0 && handlePlayer.hurtTime == 0 && !handlePlayer.isInWater()) {
            flag("Not take any damage", 2);
            fall = false;
        }
    }
}
