package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.combat;

import net.ccbluex.liquidbounce.features.module.modules.other.HackerDetector;
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class ReachCheck extends Check {
    int swingBuffer;
    public ReachCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Reach";
        checkViolationLevel = 5;
    }
    public void onLivingUpdate() {
        if (HackerDetector.INSTANCE.reachValue.get()) {

            EntityPlayer target = null;

            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer) {
                    float yaw = getRotationsForEntity(this.handlePlayer, (EntityPlayer) entity)[0];

                    if (this.handlePlayer.rotationYaw >= yaw - 20.0F && this.handlePlayer.rotationYaw <= yaw + 20.0F) {
                        target = (EntityPlayer) entity;
                    }
                }
            }

            if (target != null) {
                if (this.handlePlayer.isSwingInProgress) {
                    this.swingBuffer++;
                }

                if (this.swingBuffer >= 10 && target.hurtResistantTime > 8 && this.handlePlayer.getDistanceSqToEntity(target) >= 3.5D) {
                    flag("Attack other entity for long distance", 1.0D);
                }
            }
        }
    }
    public static float[] getRotationsForEntity(EntityPlayer player, EntityPlayer target) {
        if (player != null && target != null) {
            double diffX = target.posX - player.posX;
            double diffY = target.posY + target.getEyeHeight() * 0.9D - (player.posY + player.getEyeHeight());
            double diffZ = target.posZ - player.posZ;

            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI - 90.0D);
            float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI) + 11.0F;

            return new float[] {
                    player.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw),
                    player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)
            };
        }
        return new float[] { player.rotationYaw, player.rotationPitch };
    }
}
