package net.ccbluex.liquidbounce.features.module.modules.movement.flights.ncp;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.MoveEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlyMode;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.FloatValue;
import net.ccbluex.liquidbounce.utils.*;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

public class LatestNCP extends FlyMode{
    final BoolValue teleportValue = new BoolValue("Teleport", false);
    final BoolValue timerValue = new BoolValue("Timer", true);
    final FloatValue addValue = new FloatValue("AddSpeed", 0.0F, 0.0F, 1.5F);
    private boolean started, notUnder, clipped;
    private int offGroundTicks;
    public LatestNCP() {
        super("LatestNCP");
    }
    @EventTarget
    @Override
    public void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else offGroundTicks++;

        if (timerValue.get()){
            if (!mc.thePlayer.onGround) {
                mc.timer.timerSpeed = 0.4f;
            } else {
                mc.timer.timerSpeed = 1.0F;
            }
        }
        final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, 1, 0);

        if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty() || started) {
            switch (offGroundTicks) {
                case 0:
                    if (notUnder) {
                        if (clipped) {
                            started = true;
                            MovementUtils.INSTANCE.strafe(9.5 + addValue.get());
                            mc.thePlayer.motionY = 0.42f;
                            notUnder = false;
                        }
                    }
                    break;

                case 1:
                    if (started) MovementUtils.INSTANCE.strafe(8.0 + addValue.get());
                    break;
            }
        } else {
            notUnder = true;

            if (clipped) return;

            clipped = true;

            if (teleportValue.get()){
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
                PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false));
            }
        }

        MovementUtils.INSTANCE.strafe();

    }
    @Override
    public void onEnable() {
        notUnder = false;
        started = false;
        clipped = false;
    }
}
