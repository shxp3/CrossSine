package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemBlock;

public class LegitScaffoldCheck extends Check {
   int sneakTime;
   short sneakFlag;
   int timePassed;
    public LegitScaffoldCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "LegitScaffold";
        checkViolationLevel = 10;
    }

    @Override
    public void onLivingUpdate() {
        if (handlePlayer.isSneaking()) {
            timePassed = 0;
            sneakTime++;
        } else {
            if (sneakTime < 4) {
                sneakFlag += 1;
            }
            sneakTime = 0;
            timePassed++;
        }
        if (timePassed >= 300) {
            sneakFlag = 0;
        }
        if (handlePlayer.rotationPitch > 75 && handlePlayer.rotationPitch < 90 && handlePlayer.isSwingInProgress) {
            if (handlePlayer.getHeldItem().getItem() instanceof ItemBlock) {
                if (MovementUtils.INSTANCE.getSpeed(handlePlayer) >= 0.10 && handlePlayer.onGround && sneakFlag > 5) {
                    flag("LegitScaffold fast sneak", 1);
                }
                if (MovementUtils.INSTANCE.getSpeed(handlePlayer) >= 0.21 && !handlePlayer.onGround && sneakFlag > 5) {
                    flag("LegitScaffold fast sneak", 1);
                }
            }
        }
    }

    @Override
    public void reset() {
        sneakFlag = 0;
        sneakTime = 0;
        timePassed = 0;
    }
}
