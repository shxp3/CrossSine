package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.data;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.utils.Vector2D;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class PlayerDataSamples {

    /** Used to ensure we chech each player only once per tick, since the World#playerEntities list might contain duplicates */
    public boolean checkedThisTick;
    /** Amount of ticks the player has spent on ground */
    public int onGroundTime;
    /** Amount of ticks the player has spent in air */
    public int airTime;
    /** Amount of ticks since the player started sprinting */
    public int sprintTime = 0;
    /** Amount of ticks since the player has been using an item */
    public int useItemTime = 0;
    /** Amount of ticks since the player started eating */
    public int timeEating = 0;
    /** Amount of ticks since the player finished eating something */
    public int lastEatTime = 50;
    /** True if the item in use is food or potion or milk bucket */
    public boolean usedItemIsConsumable = false;
    /** True when we receive a swing packet from this entity during the last tick */
    public boolean hasSwung = false;
    public final SampleListZ swingList = new SampleListZ(20);
    /** Info about attack that happend this tick if any */
    public final SampleListZ attackList = new SampleListZ(20);

    /* ----- Samples of rotations/positions interpolated by the client ----- */
    public final SampleListD posXList = new SampleListD(10);
    public final SampleListD posYList = new SampleListD(10);
    public final SampleListD posZList = new SampleListD(10);
    public final SampleListD speedXList = new SampleListD(5);
    public final SampleListD speedYList = new SampleListD(5);
    public final SampleListD speedZList = new SampleListD(5);
    /* ----- Client samples end ----- */

    /* ----- Samples of rotations/positions received from the server ----- */
    private int serverUpdates;
    public final SampleListI serverUpdatesList = new SampleListI(5);
    public final SampleListD serverPosXList = new SampleListD(5);
    public final SampleListD serverPosYList = new SampleListD(5);
    public final SampleListD serverPosZList = new SampleListD(5);
    /** Yaw of the player's body [-180, 180] */
    public final SampleListF serverYawList = new SampleListF(5);
    /** Pitch of the player's head [-90, 90] */
    public final SampleListF serverPitchList = new SampleListF(5);
    /** Yaw of the player's head [-180, 180], directly equals to player.rotationYawHead */
    public final SampleListF serverYawHeadList = new SampleListF(5);
    /* ----- Server samples end ----- */

    /** Last time the player broke a block */
    public long lastBreakBlockTime = System.currentTimeMillis();

    public void onTickStart() {
        this.checkedThisTick = false;
        this.hasSwung = false;
        this.serverUpdates = 0;
    }

    public void onTick(EntityPlayer player) {
        this.checkedThisTick = true;
        this.onGroundTime = player.onGround ? this.onGroundTime + 1 : 0;
        this.airTime = player.onGround ? 0 : this.airTime + 1;
        this.sprintTime = player.isSprinting() ? this.sprintTime + 1 : 0;
        final boolean isUsingItem = player.isEating() && player.getHeldItem() != null;
        if (!isUsingItem && this.usedItemIsConsumable && this.useItemTime > 25) {
            this.lastEatTime = 0;
        }
        this.lastEatTime++;
        if (isUsingItem) {
            this.usedItemIsConsumable = player.getHeldItem().getMaxItemUseDuration() == 32;
            this.useItemTime = this.useItemTime + 1;
            this.timeEating = this.usedItemIsConsumable ? this.timeEating + 1 : 0;
        } else {
            this.useItemTime = 0;
            this.timeEating = 0;
        }
        this.swingList.add(this.hasSwung);
        this.posXList.add(player.posX);
        this.posYList.add(player.posY);
        this.posZList.add(player.posZ);
        this.speedXList.add((player.posX - player.lastTickPosX) * 20D);
        this.speedYList.add((player.posY - player.lastTickPosY) * 20D);
        this.speedZList.add((player.posZ - player.lastTickPosZ) * 20D);
        this.serverUpdatesList.add(this.serverUpdates);
    }
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        this.serverUpdates++;
        this.serverPosXList.add(x);
        this.serverPosYList.add(y);
        this.serverPosZList.add(z);
        this.serverYawList.add(yaw);
        this.serverPitchList.add(pitch);
    }

    public void setRotationYawHead(float yawHead) {
        this.serverYawHeadList.add(yawHead);
    }


    /** True if the player's position in the XZ plane is identical to the last tick */
    public boolean isNotMovingXZ() {
        return this.speedXList.get(0) == 0D && this.speedZList.get(0) == 0D;
    }

    public double getSpeedXZ() {
        final double vx = this.speedXList.get(0);
        final double vz = this.speedZList.get(0);
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double getSpeedXZSq() {
        final double vx = this.speedXList.get(0);
        final double vz = this.speedZList.get(0);
        return vx * vx + vz * vz;
    }

    public Vec3 getPositionEyesServer(EntityPlayer player) {
        return new Vec3(this.serverPosXList.get(0), this.serverPosYList.get(0) + (double) player.getEyeHeight(), this.serverPosZList.get(0));
    }

    public Vec3 getLookServer() {
        return getVectorForRotation(this.serverPitchList.get(0), this.serverYawHeadList.get(0));
    }

    public double getMoveLookAngleDiff() {
        return MathHelper.wrapAngleTo180_double(new Vector2D(this.speedZList.get(0), -this.speedXList.get(0)).getOrientedAngle() - this.serverYawHeadList.get(0));
    }

    /**
     * Creates a Vec3 using the pitch and yaw of the entities' rotation.
     */
    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        final float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        final float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        final float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        final float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

}
