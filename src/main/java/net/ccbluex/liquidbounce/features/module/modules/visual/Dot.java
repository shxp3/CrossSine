package net.ccbluex.liquidbounce.features.module.modules.visual;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;


@ModuleInfo(name = "Dot", category = ModuleCategory.VISUAL)
public class Dot extends Module {
    private final static BoolValue onlyTarget = new BoolValue("Only-Rotation", false);
    private int spin;

    @Override
    public void onDisable() {
        spin = 0;
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        double maxDistance = 3.0;
        spin += 2;
        // คำนวณตำแหน่งเริ่มต้นของ ray trace
        Vec3 startVec = mc.thePlayer.getPositionEyes(event.getPartialTicks());
        Vec3 lookVec = mc.thePlayer.getVectorForRotation(RotationUtils.rotating() ? (float) RotationUtils.smoothPitch : mc.thePlayer.rotationPitch, RotationUtils.rotating() ? (float) RotationUtils.smoothYaw : mc.thePlayer.rotationYaw);
        Vec3 endVec = startVec.addVector(lookVec.xCoord * maxDistance, lookVec.yCoord * maxDistance, lookVec.zCoord * maxDistance);

        // ทำ ray trace เพื่อหาจุดตัดกับสิ่งกีดขวาง
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(startVec, endVec, false, true, false);

        // ปรับระยะของจุดตามสิ่งกีดขวาง
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            maxDistance = mop.hitVec.distanceTo(startVec);
        } else {
            // ตรวจสอบ entity ที่อยู่ใกล้
            Entity pointedEntity = getPointedEntity(mc.thePlayer, maxDistance, event.getPartialTicks());
            if (pointedEntity != null) {
                maxDistance = mc.thePlayer.getDistanceToEntity(pointedEntity);
            }
        }

        // คำนวณตำแหน่งของจุด
        Vec3 dotVec = startVec.addVector(lookVec.xCoord * maxDistance, lookVec.yCoord * maxDistance, lookVec.zCoord * maxDistance);

        // render จุด
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        if (!onlyTarget.get() || RotationUtils.rotating()) {
            renderDot(dotVec.xCoord - mc.getRenderManager().viewerPosX,
                    dotVec.yCoord - mc.getRenderManager().viewerPosY,
                    dotVec.zCoord - mc.getRenderManager().viewerPosZ);
        }
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    private Entity getPointedEntity(Entity entity, double range, float partialTicks) {
        Entity pointedEntity = null;
        Vec3 startVec = entity.getPositionEyes(partialTicks);
        Vec3 lookVec = entity.getLook(partialTicks);
        Vec3 endVec = startVec.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
        float f1 = 1.0F;
        java.util.List<Entity> list = entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, entity.getEntityBoundingBox().addCoord(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range).expand(f1, f1, f1));
        double d2 = 0.0D;

        for (int i = 0; i < list.size(); ++i) {
            Entity entity1 = list.get(i);

            if (entity1.canBeCollidedWith()) {
                float f2 = entity1.getCollisionBorderSize();
                net.minecraft.util.AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f2, f2, f2);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(startVec, endVec);

                if (axisalignedbb.isVecInside(startVec)) {
                    if (0.0D < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = startVec.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity1;
                            }
                        } else {
                            pointedEntity = entity1;
                            d2 = d3;
                        }
                    }
                }
            }
        }

        return pointedEntity;
    }

    private void renderDot(double x, double y, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(spin - mc.thePlayer.rotationYaw, 1.0F, 1.0F, 1.0F);
        // render กล่อง
        float size = 0.08F; // ขนาดของกล่อง
        AxisAlignedBB box = new AxisAlignedBB(-size, -size, -size, size, size, size);
        RenderUtils.drawAxisAlignedBB(box, ClientTheme.INSTANCE.getColorWithAlpha(0, 120,true), true, true, 2F);

        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }
}
