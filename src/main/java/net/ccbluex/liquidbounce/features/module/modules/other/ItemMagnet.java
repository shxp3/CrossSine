/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import javax.vecmath.Vector3f;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "ItemMagnet", spacedName = "Item Magnet",category = ModuleCategory.OTHER)
public class ItemMagnet extends Module {

    private final ListValue modeValue = new ListValue("Mode", new String[] {"New", "Old"}, "New");
    private final BoolValue resetAfterTpValue = new BoolValue("ResetAfterTP", true);
    private final ListValue buttonValue = new ListValue("Button", new String[] {"Left", "Right", "Middle"}, "Middle");

    private int delay;
    private BlockPos endPos;
    private MovingObjectPosition objectPosition;

    @Override
    public void onDisable() {
        delay = 0;
        endPos = null;
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(mc.currentScreen == null && Mouse.isButtonDown(Arrays.asList(buttonValue.getValues()).indexOf(buttonValue.get())) && delay <= 0) {
            endPos = objectPosition.getBlockPos();

            if(BlockUtils.getBlock(endPos).getMaterial() == Material.air) {
                endPos = null;
                return;
            }

            alert("§7[§b§lItemMagnet§7] §3Position was set to §8" + endPos.getX() + "§3, §8" + endPos.getY() + "§3, §8" + endPos.getZ());
            delay = 6;
        }

        if(delay > 0)
            --delay;

        if(endPos != null && mc.thePlayer.isSneaking()) {
            if(!mc.thePlayer.onGround) {
                final double endX = (double) endPos.getX() + 0.5D;
                final double endY = (double) endPos.getY() + 1D;
                final double endZ = (double) endPos.getZ() + 0.5D;

                switch(modeValue.get().toLowerCase()) {
                    case "old":
                        for(final Vector3f vector3f : vanillaTeleportPositions(endX, endY, endZ, 4D))
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vector3f.getX(), vector3f.getY(), vector3f.getZ(), false));
                        break;
                    case "new":
                        for(final Vector3f vector3f : vanillaTeleportPositions(endX, endY, endZ, 5D)) {
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vector3f.x, vector3f.y, vector3f.z, true));
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 4.0, mc.thePlayer.posZ, true));
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(vector3f.x, vector3f.y, vector3f.z, true));
                            MovementUtils.INSTANCE.forward(0.04);
                        }
                        break;
                }

                if(resetAfterTpValue.get())
                    endPos = null;

                alert("7[§b§lItemMagnet§7 §3Tried to collect items");
            }else
                mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        objectPosition = mc.thePlayer.rayTrace(1000, event.getPartialTicks());

        if(objectPosition.getBlockPos() == null)
            return;

        final int x = objectPosition.getBlockPos().getX();
        final int y = objectPosition.getBlockPos().getY();
        final int z = objectPosition.getBlockPos().getZ();

        if(BlockUtils.getBlock(objectPosition.getBlockPos()).getMaterial() != Material.air) {
            final RenderManager renderManager = mc.getRenderManager();

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_BLEND);
            glLineWidth(2F);
            glDisable(GL_TEXTURE_2D);
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            RenderUtils.glColor(BlockUtils.getBlock(objectPosition.getBlockPos().up()).getMaterial() != Material.air ? new Color(255, 0, 0, 90) : new Color(0, 255, 0, 90));
            RenderUtils.drawFilledBox(new AxisAlignedBB(x - renderManager.renderPosX, (y + 1) - renderManager.renderPosY, z - renderManager.renderPosZ, x - renderManager.renderPosX + 1D, y + 1.2D - renderManager.renderPosY, z - renderManager.renderPosZ + 1D));
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_DEPTH_TEST);
            glDepthMask(true);
            glDisable(GL_BLEND);

            RenderUtils.renderNameTag(Math.round(mc.thePlayer.getDistance(x, y, z)) + "m", x + 0.5, y + 1.7, z + 0.5);
            GlStateManager.resetColor();
        }
    }

    private List<Vector3f> vanillaTeleportPositions(final double tpX, final double tpY, final double tpZ, final double speed) {
        final List<Vector3f> positions = new ArrayList<>();
        double posX = tpX - mc.thePlayer.posX;
        double posZ = tpZ - mc.thePlayer.posZ;
        float yaw = (float) ((Math.atan2(posZ, posX) * 180 / Math.PI) - 90F);
        double tmpX;
        double tmpY = mc.thePlayer.posY;
        double tmpZ;
        double steps = 1;

        for(double d = speed; d < getDistance(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, tpX, tpY, tpZ); d += speed)
            steps++;

        for(double d = speed; d < getDistance(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, tpX, tpY, tpZ); d += speed) {
            tmpX = mc.thePlayer.posX - (Math.sin(Math.toRadians(yaw)) * d);
            tmpZ = mc.thePlayer.posZ + Math.cos(Math.toRadians(yaw)) * d;
            tmpY -= (mc.thePlayer.posY - tpY) / steps;
            positions.add(new Vector3f((float) tmpX, (float) tmpY, (float) tmpZ));
        }

        positions.add(new Vector3f((float) tpX, (float) tpY, (float) tpZ));

        return positions;
    }

    private double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double d0 = x1 - x2;
        double d1 = y1 - y2;
        double d2 = z1 - z2;
        return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
    }

}
