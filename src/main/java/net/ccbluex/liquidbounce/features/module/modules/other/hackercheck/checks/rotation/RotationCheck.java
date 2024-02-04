package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.rotation;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class RotationCheck extends Check {
    public RotationCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "RotationInvalid";
        checkViolationLevel = 20;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.rotationPitch > 90 || handlePlayer.rotationPitch < -90) {
            flag("Invalid Rotation pitch" ,5);
        }
    }
}
