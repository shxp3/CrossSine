package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat;

import net.ccbluex.liquidbounce.features.module.modules.other.HackerDetector;
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class AutoBlockCheck extends Check {
    short blockingTime = 0, buffer = 0;
    public AutoBlockCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "AutoBlock";
        checkViolationLevel = 2;
    }

    @Override
    public void onLivingUpdate() {
        if (HackerDetector.INSTANCE.autoBlockValue.get()) {
            if (handlePlayer.isBlocking()) ++blockingTime;
            else blockingTime = 0;
            if (blockingTime > 10 && handlePlayer.isSwingInProgress) {
                if (++buffer > 5) flag("swing when blocking " + blockingTime, 1);
            } else {
                buffer = 0;
            }
        }
    }

    @Override
    public String description() {
        return "Swing when blocking";
    }

    @Override
    public String reportName() {
        return "killAura";
    }
}