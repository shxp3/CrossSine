package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.rotation;

import net.ccbluex.liquidbounce.features.module.modules.other.HackerDetector;
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.MathHelper;

public class RotationCheck extends Check {
    private float lastYaw = Float.MAX_VALUE;
    private float lastPitch = Float.MAX_VALUE;
    private int snapBuffer = 0;
    private int pitchLockBuffer = 0;

    public RotationCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Rotation";
        checkViolationLevel = 15;
    }

    @Override
    public void onLivingUpdate() {
        if (!HackerDetector.INSTANCE.rotationValue.get()) return;

        float yaw = handlePlayer.rotationYaw;
        float pitch = handlePlayer.rotationPitch;

        // 1. Invalid pitch (impossible to send via vanilla client)
        if (pitch > 90 || pitch < -90) {
            flag("Invalid pitch " + pitch, 5);
        }

        if (lastYaw != Float.MAX_VALUE) {
            float yawDelta = Math.abs(MathHelper.wrapAngleTo180_float(yaw - lastYaw));
            float pitchDelta = Math.abs(pitch - lastPitch);

            // 2. Rotation snap: yaw change > 90 deg/tick while attacking is suspicious
            if (yawDelta > 90 && handlePlayer.isSwingInProgress) {
                if (++snapBuffer > 3) {
                    flag(String.format("Snap yaw=%.1f", yawDelta), 2);
                    snapBuffer = 0;
                }
            } else {
                if (snapBuffer > 0) snapBuffer--;
            }

            // 3. Pitch-lock: yaw moving but pitch never changes for 40+ ticks while attacking
            if (yawDelta > 5 && pitchDelta < 0.01f && handlePlayer.isSwingInProgress) {
                if (++pitchLockBuffer > 40) {
                    flag("Pitch lock during attack", 1);
                    pitchLockBuffer = 35;
                }
            } else {
                if (pitchLockBuffer > 0) pitchLockBuffer--;
            }
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }

    @Override
    public void reset() {
        super.reset();
        snapBuffer = 0;
        pitchLockBuffer = 0;
        lastYaw = Float.MAX_VALUE;
        lastPitch = Float.MAX_VALUE;
    }
}
