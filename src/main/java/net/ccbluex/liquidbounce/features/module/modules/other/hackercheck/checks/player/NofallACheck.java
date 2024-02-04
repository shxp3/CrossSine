package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.player;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class NofallACheck extends Check {
    public NofallACheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "NofallA";
        checkViolationLevel = 20;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.fallDistance > 3 && handlePlayer.onGround && !handlePlayer.isSpectator()) {
            flag("InAir but packet Ground", 5);
        }
    }
}
