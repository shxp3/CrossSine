
package net.ccbluex.liquidbounce.utils;

import kotlin.Triple;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.AnimationUtil;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class RotationUtils extends MinecraftInstance implements Listenable {

    private static Random random = new Random();

    private static int keepLength;
    private static int revTick;
    public static float prevHeadPitch;
    public static float headPitch;
    public static Float playerYaw;
    public static double smoothYaw;
    public static double smoothPitch;

    public static Rotation targetRotation;
    public static Rotation freeLookRotation;
    public static Rotation serverRotation = new Rotation(0.0F, 0.0F);
    public static boolean keepCurrentRotation = false;

    private static double x = random.nextDouble();
    private static double y = random.nextDouble();
    private static double z = random.nextDouble();
    public static VecRotation faceBlock(final BlockPos blockPos) {
        if (blockPos == null)
            return null;

        VecRotation vecRotation = null;

        for(double xSearch = 0.1D; xSearch < 0.9D; xSearch += 0.1D) {
            for(double ySearch = 0.1D; ySearch < 0.9D; ySearch += 0.1D) {
                for (double zSearch = 0.1D; zSearch < 0.9D; zSearch += 0.1D) {
                    final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                    final Vec3 posVec = new Vec3(blockPos).addVector(xSearch, ySearch, zSearch);
                    final double dist = eyesPos.distanceTo(posVec);

                    final double diffX = posVec.xCoord - eyesPos.xCoord;
                    final double diffY = posVec.yCoord - eyesPos.yCoord;
                    final double diffZ = posVec.zCoord - eyesPos.zCoord;

                    final double diffXZ = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

                    final Rotation rotation = new Rotation(
                            MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F),
                            MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)))
                    );

                    final Vec3 rotationVector = getVectorForRotation(rotation);
                    final Vec3 vector = eyesPos.addVector(rotationVector.xCoord * dist, rotationVector.yCoord * dist,
                            rotationVector.zCoord * dist);
                    final MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false,
                            false, true);

                    if (obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        final VecRotation currentVec = new VecRotation(posVec, rotation);

                        if (vecRotation == null || getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))
                            vecRotation = currentVec;
                    }
                }
            }
        }

        return vecRotation;
    }
    public static Rotation getRotationsEntity(EntityLivingBase entity) {
        return RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight() - 0.4, entity.posZ);
    }
    public static Rotation getRotationsNonLivingEntity(Entity entity) {
        return RotationUtils.getRotations(entity.posX, entity.posY + (entity.getEntityBoundingBox().maxY-entity.getEntityBoundingBox().minY)*0.5, entity.posZ);
    }
    public static void faceBow(final Entity target, final boolean silent, final boolean predict, final float predictSize) {
        final EntityPlayerSP player = mc.thePlayer;

        final double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0) - (player.posX + (predict ? (player.posX - player.prevPosX) : 0));
        final double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? (player.posY - player.prevPosY) : 0)) - player.getEyeHeight();
        final double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0) - (player.posZ + (predict ? (player.posZ - player.prevPosZ) : 0));
        final double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        float velocity = player.getItemInUseDuration() / 20F;
        velocity = (velocity * velocity + velocity * 2) / 3;

        if(velocity > 1) velocity = 1;

        final Rotation rotation = new Rotation(
                (float) (Math.atan2(posZ, posX) * 180 / Math.PI) - 90,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(velocity * velocity * velocity * velocity - 0.006F * (0.006F * (posSqrt * posSqrt) + 2 * posY * (velocity * velocity)))) / (0.006F * posSqrt)))
        );

        if(silent)
            setTargetRotation(rotation, 0);
        else
            limitAngleChange(new Rotation(player.rotationYaw, player.rotationPitch), rotation,(float) (10 +
                    new Random().nextInt(6))).toPlayer(mc.thePlayer);
    }

    public static Rotation toRotation(final Vec3 vec, final boolean predict) {
        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY +
                mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        if(predict) {
            if(mc.thePlayer.onGround) {
                eyesPos.addVector(mc.thePlayer.motionX, 0.0, mc.thePlayer.motionZ);
            }else eyesPos.addVector(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
        }

        final double diffX = vec.xCoord - eyesPos.xCoord;
        final double diffY = vec.yCoord - eyesPos.yCoord;
        final double diffZ = vec.zCoord - eyesPos.zCoord;

        return new Rotation(MathHelper.wrapAngleTo180_float(
                (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F
        ), MathHelper.wrapAngleTo180_float(
                (float) (-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))
        ));
    }

    public static Vec3 getCenter(final AxisAlignedBB bb) {
        return new Vec3(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
    }

    public static VecRotation calculateCenter(final String calMode, final Boolean randMode, final double randomRange, final boolean legitRandom, final AxisAlignedBB bb, final boolean predict, final boolean throughWalls) {

        VecRotation vecRotation = null;

        double xMin;
        double yMin;
        double zMin;
        double xMax;
        double yMax;
        double zMax;
        double xDist;
        double yDist;
        double zDist;

        xMin = 0.15D; xMax = 0.85D; xDist = 0.1D;
        yMin = 0.15D; yMax = 1.00D; yDist = 0.1D;
        zMin = 0.15D; zMax = 0.85D; zDist = 0.1D;

        Vec3 curVec3 = null;

        switch(calMode) {
            case "HalfUp":
                xMin = 0.40D; xMax = 0.90D; xDist = 0.1D;
                yMin = 0.30D; yMax = 0.90D; yDist = 0.1D;
                zMin = 0.40D; zMax = 0.90D; zDist = 0.1D;
                break;
            case "CenterSimple":
                xMin = 0.45D; xMax = 0.55D; xDist = 0.0125D;
                yMin = 0.65D; yMax = 0.75D; yDist = 0.0125D;
                zMin = 0.45D; zMax = 0.55D; zDist = 0.0125D;
                break;
            case "CenterLine":
                xMin = 0.45D; xMax = 0.451D; xDist = 0.0125D;
                yMin = 0.50D; yMax = 0.90D; yDist = 0.1D;
                zMin = 0.45D; zMax = 0.451D; zDist = 0.0125D;
                break;
            case "CenterHead":
                xMin = 0.45D; xMax = 0.55D; xDist = 0.0125D;
                yMin = 0.85D; yMax = 0.95D; yDist = 0.1D;
                zMin = 0.45D; zMax = 0.55D; zDist = 0.0125D;
                break;
            case "CenterBody":
                xMin = 0.45D; xMax = 0.55D; xDist = 0.0125D;
                yMin = 0.70D; yMax = 0.95D; yDist = 0.1D;
                zMin = 0.45D; zMax = 0.55D; zDist = 0.0125D;
                break;
            case "LockHead":
                xMin = 0.549D; xMax = 0.55D; xDist = 0.0125D;
                yMin = 0.949D; yMax = 0.95D; yDist = 0.1D;
                zMin = 0.549D; zMax = 0.55D; zDist = 0.0125D;
                break;
        }

        for(double xSearch = xMin; xSearch < xMax; xSearch += xDist) {
            for (double ySearch = yMin; ySearch < yMax; ySearch += yDist) {
                for (double zSearch = zMin; zSearch < zMax; zSearch += zDist) {
                    final Vec3 vec3 = new Vec3(bb.minX + (bb.maxX - bb.minX) * xSearch, bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch);
                    final Rotation rotation = toRotation(vec3, predict);

                    if(throughWalls || isVisible(vec3)) {
                        final VecRotation currentVec = new VecRotation(vec3, rotation);

                        if (vecRotation == null || (getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))) {
                            vecRotation = currentVec;
                            curVec3 = vec3;
                        }
                    }
                }
            }
        }

        if(vecRotation == null || !randMode)
            return vecRotation;

        double rand1 = random.nextDouble();
        double rand2 = random.nextDouble();
        double rand3 = random.nextDouble();

        final double xRange = bb.maxX - bb.minX;
        final double yRange = bb.maxY - bb.minY;
        final double zRange = bb.maxZ - bb.minZ;
        double minRange = 999999.0D;

        if(xRange<=minRange) minRange = xRange;
        if(yRange<=minRange) minRange = yRange;
        if(zRange<=minRange) minRange = zRange;

        rand1 = rand1 * minRange * randomRange;
        rand2 = rand2 * minRange * randomRange;
        rand3 = rand3 * minRange * randomRange;

        final double xPrecent = minRange * randomRange / xRange;
        final double yPrecent = minRange * randomRange / yRange;
        final double zPrecent = minRange * randomRange / zRange;

        Vec3 randomVec3 = legitRandom ? new Vec3(curVec3.xCoord, curVec3.yCoord - yPrecent * (curVec3.yCoord - bb.minY) + rand2, curVec3.zCoord) : new  Vec3(curVec3.xCoord - xPrecent * (curVec3.xCoord - bb.minX) + rand1, curVec3.yCoord - yPrecent * (curVec3.yCoord - bb.minY) + rand2, curVec3.zCoord - zPrecent * (curVec3.zCoord - bb.minZ) + rand3);

        final Rotation randomRotation = toRotation(randomVec3, predict);
        vecRotation =  new VecRotation(randomVec3, randomRotation);

        return vecRotation;
    }
    public static double getRotationDifference(final Entity entity) {
        final Rotation rotation = toRotation(getCenter(entity.getEntityBoundingBox()), true);

        return getRotationDifference(rotation, new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
    }

    public static double getRotationDifference(final Rotation rotation) {
        return serverRotation == null ? 0D : getRotationDifference(rotation, serverRotation);
    }
    public static double getRotationDifference(final Rotation a, final Rotation b) {
        return Math.hypot(getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }
    @NotNull
    public static Rotation limitAngleChange(final Rotation currentRotation, final Rotation targetRotation, float turnSpeed) {
        final float yawDifference = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        final float pitchDifference = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());
        return new Rotation(
                currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)),
                currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)
                ));
    }
    public static float getAngleDifference(final float a, final float b) {
        return ((((a - b) % 360F) + 540F) % 360F) - 180F;
    }

    public static Vec3 getVectorForRotation(final Rotation rotation) {
        float yawCos = MathHelper.cos(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-rotation.getYaw() * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-rotation.getPitch() * 0.017453292F);
        float pitchSin = MathHelper.sin(-rotation.getPitch() * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public static boolean isFaced(final Entity targetEntity, double blockReachDistance) {
        return RaycastUtils.raycastEntity(blockReachDistance, entity -> entity == targetEntity) != null;
    }

    public static boolean isVisible(final Vec3 vec3) {
        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        return mc.theWorld.rayTraceBlocks(eyesPos, vec3) == null;
    }
    public static Rotation getDirectionToBlock(double x, double y, double z, EnumFacing enumFacing) {
        final EntityEgg var4 = new EntityEgg(mc.theWorld);
        var4.posX = x + 0.5;
        var4.posY = y + 0.5;
        var4.posZ = z + 0.5;
        var4.posX += enumFacing.getDirectionVec().getX() * 0.5;
        var4.posY += enumFacing.getDirectionVec().getY() * 0.5;
        var4.posZ += enumFacing.getDirectionVec().getZ() * 0.5;
        return getRotations(var4.posX, var4.posY, var4.posZ);
    }
    public static Rotation getFaceRotation(EnumFacing face, BlockPos blockPos) {
        Vec3i faceVec = face.getDirectionVec();
        Vec3 blockFaceVec = new Vec3(faceVec.getX() * 0.5, faceVec.getY() * 0.5, faceVec.getZ() * 0.5);
        blockFaceVec = blockFaceVec.add(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        blockFaceVec = blockFaceVec.addVector(0.5, 0.5, 0.5);
        return getRotations(blockFaceVec);
    }

    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        if (serverRotation != null) {
            smoothYaw = (smoothYaw * (1 - ClientTheme.INSTANCE.getSmoothFactor().get()) + serverRotation.getYaw() * ClientTheme.INSTANCE.getSmoothFactor().get());
            smoothPitch = (smoothPitch * (1 - ClientTheme.INSTANCE.getSmoothFactor().get()) + headPitch * ClientTheme.INSTANCE.getSmoothFactor().get());
        }
    }

    @EventTarget
    public void onTick(final TickEvent event) {
        if(rotating()) {
            keepLength--;

            if (keepLength <= 0) {
                if(revTick>0) {
                    revTick--;
                    reset();
                }else reset();
            }
        }

        if(random.nextGaussian() > 0.8D) x = Math.random();
        if(random.nextGaussian() > 0.8D) y = Math.random();
        if(random.nextGaussian() > 0.8D) z = Math.random();
    }
    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if(packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            if(targetRotation != null && !keepCurrentRotation && (targetRotation.getYaw() != serverRotation.getYaw() || targetRotation.getPitch() != serverRotation.getPitch())) {
                packetPlayer.yaw = targetRotation.getYaw();
                packetPlayer.pitch = targetRotation.getPitch();
                packetPlayer.rotating = true;
            }

            if(packetPlayer.rotating) serverRotation = new Rotation(packetPlayer.yaw, packetPlayer.pitch);
        }
        if (packet instanceof C03PacketPlayer.C06PacketPlayerPosLook || packet instanceof C03PacketPlayer.C05PacketPlayerLook) {
            playerYaw = ((C03PacketPlayer) packet).yaw;
        }
    }
    @EventTarget
    public void onMotion(MotionEvent event) {
        prevHeadPitch = headPitch;
        // When only freeLookRotation is active (no targetRotation), the head
        // should stay at the player's real rotation — freeLookRotation is
        // silent (packet-only), so the visible model must not turn.
        if (freeLookRotation != null && targetRotation == null) {
            headPitch = mc.thePlayer.rotationPitch;
            mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw;
        } else {
            headPitch = serverRotation.getPitch();
            mc.thePlayer.rotationYawHead = targetRotation != null ? (float) smoothYaw : serverRotation.getYaw();
        }
    }
    public static void setTargetRotation(final Rotation rotation, final int keepLength) {
        if(Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;
        if (ClientTheme.INSTANCE.getSmoothRotationSS().get()) {
            targetRotation = new Rotation((float) AnimationUtil.animate(serverRotation.getYaw(), rotation.getYaw(), ClientTheme.INSTANCE.getSmoothFactorSS().get()), (float) AnimationUtil.animate(serverRotation.getPitch(), rotation.getPitch(), ClientTheme.INSTANCE.getSmoothFactorSS().get())).fixedSensitivity(mc.gameSettings.mouseSensitivity);
        } else {
            targetRotation = rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity);
        }
        RotationUtils.keepLength = keepLength;
        RotationUtils.revTick = 0;
    }
    public static void setFreeLookRotationReverse(final Rotation rotation, final int keepLength, final int revTick) {
        if(Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;
        freeLookRotation = rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity);
        RotationUtils.keepLength = keepLength;
        RotationUtils.revTick = revTick+1;
    }
    public static void setFreeLookRotation(final Rotation rotation, final int keepLength) {
        if(Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;
        if (ClientTheme.INSTANCE.getSmoothRotationSS().get()) {
            freeLookRotation = new Rotation((float) AnimationUtil.animate(serverRotation.getYaw(), rotation.getYaw(), ClientTheme.INSTANCE.getSmoothFactorSS().get()), (float) AnimationUtil.animate(serverRotation.getPitch(), rotation.getPitch(), ClientTheme.INSTANCE.getSmoothFactorSS().get())).fixedSensitivity(mc.gameSettings.mouseSensitivity);
        } else {
            freeLookRotation = rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity);
        }
        RotationUtils.keepLength = keepLength;
    }

    public static void setTargetRotationReverse(final Rotation rotation, final int keepLength, final int revTick) {
        if(Double.isNaN(rotation.getYaw()) || Double.isNaN(rotation.getPitch())
                || rotation.getPitch() > 90 || rotation.getPitch() < -90)
            return;

        targetRotation = rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity);
        RotationUtils.keepLength = keepLength;
        RotationUtils.revTick = revTick+1;
    }

    public static void reset() {
        keepLength = 0;
        if(revTick>0) {
            targetRotation = new Rotation(targetRotation.getYaw()-getAngleDifference(targetRotation.getYaw(), mc.thePlayer.rotationYaw)/revTick
                    , targetRotation.getPitch()-getAngleDifference(targetRotation.getPitch(), mc.thePlayer.rotationPitch)/revTick);
        }else targetRotation = null;
        if (freeLookRotation != null) {
            if(revTick>0) {
                // Smoothly transition server rotation back toward the player's real look direction
                freeLookRotation = new Rotation(freeLookRotation.getYaw() - getAngleDifference(freeLookRotation.getYaw(), mc.thePlayer.rotationYaw) / revTick
                        , freeLookRotation.getPitch() - getAngleDifference(freeLookRotation.getPitch(), mc.thePlayer.rotationPitch) / revTick);
            } else {
                freeLookRotation = null;
            }
        }
    }

    public static Rotation getRotations(double posX, double posY, double posZ) {
        EntityPlayerSP player = RotationUtils.mc.thePlayer;
        double x = posX - player.posX;
        double y = posY - (player.posY + (double)player.getEyeHeight());
        double z = posZ - player.posZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / 3.141592653589793) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / 3.141592653589793));
        return new Rotation(yaw,pitch);
    }
    public static Rotation getRotations(Vec3 pos) {
        EntityPlayerSP player = RotationUtils.mc.thePlayer;
        double x = pos.xCoord - player.posX;
        double y = pos.yCoord - (player.posY + (double)player.getEyeHeight());
        double z = pos.zCoord - player.posZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float)(Math.atan2(z, x) * 180.0 / 3.141592653589793) - 90.0f;
        float pitch = (float)(-(Math.atan2(y, dist) * 180.0 / 3.141592653589793));
        return new Rotation(yaw,pitch);
    }
    public static Rotation rotationSmooth(Rotation currentRotation, Rotation targetRotation, float smooth) {
        return new Rotation(currentRotation.getYaw()+((targetRotation.getYaw()-currentRotation.getYaw())/smooth),
                currentRotation.getPitch()+((targetRotation.getPitch()-currentRotation.getPitch())/smooth));
    }
    @Override
    public boolean handleEvents() {
        return true;
    }

    public static boolean rotating() {
        return targetRotation != null || freeLookRotation != null;
    }

    public static void fixMovement(final StrafeEvent event, final float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();

        final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(mc.thePlayer.rotationYaw, forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                final double difference = wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }
    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }
    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

}
