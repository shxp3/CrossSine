
package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.PathUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.tickTimer;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "Teleport",category = ModuleCategory.OTHER)
public class Teleport extends Module {

    private final BoolValue ignoreNoCollision = new BoolValue("IgnoreNoCollision", true);
    private final ListValue modeValue = new ListValue("Mode", new String[] {"Tp", "Blink", "Flag", "Rewinside", "OldRewinside", "Spoof", "Minesucht", "AAC3.5.0","BWRel","Karhu"}, "Tp");
    private final ListValue buttonValue = new ListValue("Button", new String[] {"Left", "Right", "Middle"}, "Middle");
    private final BoolValue needSneak = new BoolValue("NeedSneak", true);
    private final tickTimer flyTimer = new tickTimer();
    private boolean hadGround;
    private double fixedY;
    private final List<Packet<?>> packets = new ArrayList<>();
    private boolean disableLogger = false;
    private boolean zitter = false;
    private boolean doTeleport = false;
    private boolean freeze = false;
    private final tickTimer freezeTimer = new tickTimer();
    private final tickTimer respawnTimer = new tickTimer();

    private int delay;
    private BlockPos endPos;
    private MovingObjectPosition objectPosition;

    private double endX = 0.0D;
    private double endY = 0.0D;
    private double endZ = 0.0D;

    @Override
    public void onEnable() {
        int matrixStage = -1;

        if(modeValue.equals("AAC3.5.0")) {
            alert("§c>>> §a§lTeleport §fAAC 3.5.0 §c<<<");
            alert("§cHow to teleport: §aPress " + buttonValue.get() + " mouse button.");
            alert("§cHow to cancel teleport: §aDisable teleport module.");
        }
    }

    @Override
    public void onDisable() {
        fixedY = 0D;
        delay = 0;
        mc.timer.timerSpeed = 1F;
        endPos = null;
        hadGround = false;
        freeze = false;
        disableLogger = false;
        flyTimer.reset();

        packets.clear();
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        final int buttonIndex = Arrays.asList(buttonValue.getValues()).indexOf(buttonValue.get());

        if(modeValue.equals("AAC3.5.0")) {
            freezeTimer.update();

            if(freeze && freezeTimer.hasTimePassed(40)) {
                freezeTimer.reset();
                freeze = false;
                setState(false);
            }

            if(!flyTimer.hasTimePassed(60)) {
                flyTimer.update();

                if(mc.thePlayer.onGround) {
                   MovementUtils.INSTANCE.jump(true, false, 0.42);
                }else{
                    MovementUtils.INSTANCE.forward(zitter ? -0.21D : 0.21D);
                    zitter = !zitter;
                }

                hadGround = false;
                return;
            }

            if(mc.thePlayer.onGround)
                hadGround = true;

            if(!hadGround)
                return;

            if(mc.thePlayer.onGround)
                mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + 0.2D, mc.thePlayer.posZ);

            final float vanillaSpeed = 2F;
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            if(mc.gameSettings.keyBindJump.isKeyDown())
                mc.thePlayer.motionY += vanillaSpeed;
            if(mc.gameSettings.keyBindSneak.isKeyDown())
                mc.thePlayer.motionY -= vanillaSpeed;
            MovementUtils.INSTANCE.strafe(vanillaSpeed);

            if(Mouse.isButtonDown(buttonIndex) && !doTeleport) {
                mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY - 11, mc.thePlayer.posZ);

                disableLogger = true;
                packets.forEach(packet -> mc.getNetHandler().addToSendQueue(packet));

                freezeTimer.reset();
                freeze = true;
            }

            doTeleport = Mouse.isButtonDown(buttonIndex);
            return;
        }


        if(mc.currentScreen == null && Mouse.isButtonDown(buttonIndex) && delay <= 0) {
            endPos = objectPosition.getBlockPos();


            if(Objects.requireNonNull(BlockUtils.getBlock(endPos)).getMaterial() == Material.air) {
                endPos = null;
                return;
            }

            alert("§7[§8§lTeleport§7] §3Position was set to §8" + endPos.getX() + "§3, §8" + ((Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getCollisionBoundingBox(mc.theWorld, objectPosition.getBlockPos(), Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getDefaultState()) == null ? endPos.getY() + Objects.requireNonNull(BlockUtils.getBlock(endPos)).getBlockBoundsMaxY() : Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getCollisionBoundingBox(mc.theWorld, objectPosition.getBlockPos(), Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getDefaultState()).maxY) + fixedY) + "§3, §8" + endPos.getZ());
            delay = 6;
            endX = (double) endPos.getX() + 0.5D;
            endY = (double) endPos.getY() + 1.0D;
            endZ = (double) endPos.getZ() + 0.5D;
        }

        if(delay > 0)
            --delay;

        if(endPos != null) {
            switch(modeValue.get().toLowerCase()) {
                case "blink":
                    if(mc.thePlayer.isSneaking() || !needSneak.get()) {
                        // Sneak
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, Action.STOP_SNEAKING));

                        // Teleport
                        PathUtils.findBlinkPath(endX, endY, endZ).forEach(vector3d -> {
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vector3d.xCoord, vector3d.yCoord, vector3d.zCoord, true));
                            mc.thePlayer.setPosition(endX, endY, endZ);
                        });

                        // Sneak
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, Action.START_SNEAKING));

                        // Notify
                        alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                        endPos=null;
                    }
                    break;
                case "flag":
                    if(mc.thePlayer.isSneaking() || !needSneak.get()) {
                        // Sneak
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, Action.STOP_SNEAKING));

                        // Teleport
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 5D, mc.thePlayer.posZ, true));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX + 0.5D, mc.thePlayer.posY, mc.thePlayer.posZ + 0.5D, true));

                        MovementUtils.INSTANCE.forward(0.04D);

                        // Sneak
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, Action.START_SNEAKING));
                        // Notify
                        alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                        endPos = null;
                    }
                    break;
                case "bwrel":
                    if(mc.thePlayer.isSneaking() || !needSneak.get()) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 9.2507838107252498276, mc.thePlayer.posZ);
                        mc.thePlayer.motionY = 1.042026214225532854;
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    }
                    break;
                case "rewinside":
                    mc.thePlayer.motionY = 0.1;
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.6D, mc.thePlayer.posZ, true));

                    if((int) mc.thePlayer.posX == endX && (int) mc.thePlayer.posY == endY && (int) mc.thePlayer.posZ == endZ) {
                        alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                        endPos = null;
                    }else
                        alert("§7[§8§lTeleport§7] §3Teleport try...");
                    break;
                case "oldrewinside":
                    mc.thePlayer.motionY = 0.1;

                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));

                    if((int) mc.thePlayer.posX == endX && (int) mc.thePlayer.posY == endY && (int) mc.thePlayer.posZ == endZ) {
                        alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                        endPos = null;
                    }else
                        alert("§7[§8§lTeleport§7] §3Teleport try...");

                    MovementUtils.INSTANCE.forward(0.04D);
                    break;
                case "minesucht":
                    if(!mc.thePlayer.isSneaking() && needSneak.get())
                        break;

                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                    endPos=null;
                    break;

                case "tp":
                    if(!mc.thePlayer.isSneaking() && needSneak.get())
                        break;

                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, true));
                    mc.thePlayer.setPosition(endX, endY, endZ);
                    alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                    endPos=null;
                    break;

                case "karhu":
                    if(!mc.thePlayer.isSneaking() && needSneak.get())
                        break;

                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(endX, endY, endZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
                    mc.thePlayer.setPosition(endX, endY, endZ);

                    alert("§7[§8§lTeleport§7] §3You were teleported to §8" + endX + "§3, §8" + endY + "§3, §8" + endZ);
                    endPos=null;
                    break;
            }
        }
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if(modeValue.equals("AAC3.5.0"))
            return;

        final Vec3 lookVec = new Vec3(mc.thePlayer.getLookVec().xCoord * 300, mc.thePlayer.getLookVec().yCoord * 300, mc.thePlayer.getLookVec().zCoord * 300);
        final Vec3 posVec = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + 1.62, mc.thePlayer.posZ);

        objectPosition = mc.thePlayer.worldObj.rayTraceBlocks(posVec, posVec.add(lookVec), false, ignoreNoCollision.get(), false);

        if (objectPosition == null || objectPosition.getBlockPos() == null)
            return;

        final BlockPos belowBlockPos = new BlockPos(objectPosition.getBlockPos().getX(), objectPosition.getBlockPos().getY() - 1, objectPosition.getBlockPos().getZ());

        fixedY = BlockUtils.getBlock(objectPosition.getBlockPos()) instanceof BlockFence ? (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(objectPosition.getBlockPos().getX() + 0.5D - mc.thePlayer.posX, objectPosition.getBlockPos().getY() + 1.5D - mc.thePlayer.posY, objectPosition.getBlockPos().getZ() + 0.5D - mc.thePlayer.posZ)).isEmpty() ? 0.5D : 0D) : BlockUtils.getBlock(belowBlockPos) instanceof BlockFence ? (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(objectPosition.getBlockPos().getX() + 0.5D - mc.thePlayer.posX, objectPosition.getBlockPos().getY() + 0.5D - mc.thePlayer.posY, objectPosition.getBlockPos().getZ() + 0.5D - mc.thePlayer.posZ)).isEmpty() || Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getCollisionBoundingBox(mc.theWorld, objectPosition.getBlockPos(), Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getDefaultState()) == null ? 0D : 0.5D - Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getBlockBoundsMaxY()) : BlockUtils.getBlock(objectPosition.getBlockPos()) instanceof BlockSnow ? Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getBlockBoundsMaxY() - 0.125D : 0D;

        final int x = objectPosition.getBlockPos().getX();
        final double y = (Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getCollisionBoundingBox(mc.theWorld, objectPosition.getBlockPos(), Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getDefaultState()) == null ? objectPosition.getBlockPos().getY() + Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getBlockBoundsMaxY() : Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getCollisionBoundingBox(mc.theWorld, objectPosition.getBlockPos(), Objects.requireNonNull(BlockUtils.getBlock(objectPosition.getBlockPos())).getDefaultState()).maxY) - 1D + fixedY;
        final int z = objectPosition.getBlockPos().getZ();

        if(!(BlockUtils.getBlock(objectPosition.getBlockPos()) instanceof BlockAir)) {
            final RenderManager renderManager = mc.getRenderManager();

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_BLEND);
            glLineWidth(2F);
            glDisable(GL_TEXTURE_2D);
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            RenderUtils.glColor(modeValue.equals("minesucht") && mc.thePlayer.getPosition().getY() != y + 1 ? new Color(255, 0, 0, 90) : !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(x + 0.5D - mc.thePlayer.posX, y + 1D - mc.thePlayer.posY, z + 0.5D - mc.thePlayer.posZ)).isEmpty() ? new Color(255, 0, 0, 90) : new Color(0, 255, 0, 90));
            RenderUtils.drawFilledBox(new AxisAlignedBB(x - renderManager.renderPosX, (y + 1) - renderManager.renderPosY, z - renderManager.renderPosZ, x - renderManager.renderPosX + 1.0D, y + 1.2D - renderManager.renderPosY, z - renderManager.renderPosZ + 1.0D));
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_DEPTH_TEST);
            glDepthMask(true);
            glDisable(GL_BLEND);

            RenderUtils.renderNameTag(Math.round(mc.thePlayer.getDistance(x + 0.5D, y + 1D, z + 0.5D)) + "m", x + 0.5, y + 1.7, z + 0.5);
            GlStateManager.resetColor();
        }
    }

    @EventTarget
    public void onMove(final MoveEvent event) {
        if (modeValue.equals("aac3.5.0") && freeze) {
            event.zeroXZ();
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if(disableLogger)
            return;

        if(packet instanceof C03PacketPlayer) {
            final C03PacketPlayer packetPlayer = (C03PacketPlayer) packet;

            switch(modeValue.get().toLowerCase()) {
                case "spoof":
                    if(endPos == null)
                        break;

                    packetPlayer.x = endPos.getX() + 0.5D;
                    packetPlayer.y = endPos.getY() + 1;
                    packetPlayer.z = endPos.getZ() + 0.5D;
                    mc.thePlayer.setPosition(endPos.getX() + 0.5D, endPos.getY() + 1, endPos.getZ() + 0.5D);
                    break;
                case "aac3.5.0":
                    if(!flyTimer.hasTimePassed(60))
                        return;

                    event.cancelEvent();

                    if(!(packet instanceof C03PacketPlayer.C04PacketPlayerPosition) && !(packet instanceof C03PacketPlayer.C06PacketPlayerPosLook))
                        return;

                    packets.add(packet);
                    break;
            }
        }
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}
