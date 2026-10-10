
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import com.google.common.base.Predicates;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.SilentAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.Reach;
import net.ccbluex.liquidbounce.features.module.modules.visual.*;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.animation.Animation;
import net.ccbluex.liquidbounce.utils.animation.Easing;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.GETFIELD;


@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    protected MixinEntityRenderer(int[] lightmapColors, DynamicTexture lightmapTexture, float torchFlickerX, float bossColorModifier, float bossColorModifierPrev, Minecraft mc, float thirdPersonDistanceTemp, float thirdPersonDistance) {
        this.lightmapColors = lightmapColors;
        this.lightmapTexture = lightmapTexture;
        this.torchFlickerX = torchFlickerX;
        this.bossColorModifier = bossColorModifier;
        this.bossColorModifierPrev = bossColorModifierPrev;
        this.mc = mc;
        this.thirdPersonDistanceTemp = thirdPersonDistanceTemp;
        this.thirdPersonDistance = thirdPersonDistance;
    }
    @Shadow
    public abstract void loadShader(ResourceLocation resourceLocationIn);
    @Shadow
    private final int[] lightmapColors;
    @Shadow
    private final DynamicTexture lightmapTexture;
    @Shadow
    public abstract void setupCameraTransform(float partialTicks, int pass);
    @Shadow
    private float torchFlickerX;

    @Shadow
    private float bossColorModifier;

    @Shadow
    private float bossColorModifierPrev;
    @Shadow
    private Entity pointedEntity;

    @Shadow
    private boolean lightmapUpdateNeeded;

    @Shadow
    private Minecraft mc;

    @Shadow
    private float thirdPersonDistanceTemp;

    @Shadow
    private float thirdPersonDistance;

    @Shadow
    private boolean cloudFog;
    public double prevRenderX = 0d;
    public double prevRenderY = 0d;
    public double prevRenderZ = 0d;
    private static final double SMOOTHING_FACTOR = 0.05;
    @Inject(method = "renderWorldPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z", shift = At.Shift.BEFORE))
    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo callbackInfo) {
        CrossSine.eventManager.callEvent(new Render3DEvent(partialTicks));
    }
    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void injectHurtCameraEffect(CallbackInfo callbackInfo) {
        if (!CrossSine.moduleManager.getModule(HurtCam.class).getModeValue().get().equalsIgnoreCase("Vanilla")) {
            callbackInfo.cancel();
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void orientCamera(float p_orientCamera_1_) {
        Entity entity = this.mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)p_orientCamera_1_;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)p_orientCamera_1_ + (double)f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)p_orientCamera_1_;
        prevRenderX = prevRenderX * (1 - SMOOTHING_FACTOR) + d0 * SMOOTHING_FACTOR;
        prevRenderY = prevRenderY * (1 - SMOOTHING_FACTOR) + d1 * SMOOTHING_FACTOR;
        prevRenderZ = prevRenderZ * (1 - SMOOTHING_FACTOR) + d2 * SMOOTHING_FACTOR;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPlayerSleeping()) {
            f = (float)((double)f + (double)1.0F);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);
            if (!this.mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
                ForgeHooksClient.orientBedCamera(this.mc.theWorld, blockpos, iblockstate, entity);
                GlStateManager.rotate((FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.prevRotationYaw) + ((FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw) - (FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.prevRotationYaw)) * p_orientCamera_1_ + 180.0F, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate((FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.prevRotationPitch) + ((FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.rotationPitch) - (FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.prevRotationPitch)) * p_orientCamera_1_, -1.0F, 0.0F, 0.0F);
            }
        } else if (this.mc.gameSettings.thirdPersonView > 0) {
            double d3 = (this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * p_orientCamera_1_);
            if (this.mc.gameSettings.debugCamEnable) {
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
            } else {
                float f1 = (FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw);
                float f2 = (FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.rotationPitch);
                if (this.mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = (double)(-MathHelper.sin(f1 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d5 = (double)(MathHelper.cos(f1 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d6 = (double)(-MathHelper.sin(f2 / 180.0F * (float)Math.PI)) * d3;

                for(int i = 0; i < 8; ++i) {
                    float f3 = (float)((i & 1) * 2 - 1);
                    float f4 = (float)((i >> 1 & 1) * 2 - 1);
                    float f5 = (float)((i >> 2 & 1) * 2 - 1);
                    f3 *= 0.1F;
                    f4 *= 0.1F;
                    f5 *= 0.1F;
                    MovingObjectPosition movingobjectposition = this.mc.theWorld.rayTraceBlocks(new Vec3(d0 + (double)f3, d1 + (double)f4, d2 + (double)f5), new Vec3(d0 - d4 + (double)f3 + (double)f5, d1 - d6 + (double)f4, d2 - d5 + (double)f5));
                    if (movingobjectposition != null) {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));
                        if (!CameraClip.INSTANCE.getState()) {
                            if (d7 < d3) {
                                d3 = d7;
                            }
                        }
                    }
                }

                if (this.mc.gameSettings.thirdPersonView == 2) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate((FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.rotationPitch) - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate((FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw) - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
                GlStateManager.rotate(f1 - (FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw), 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - (FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.rotationPitch), 1.0F, 0.0F, 0.0F);

                GlStateManager.rotate((FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw), 0.0F, 1.0F, 0.0F);

                if (SmoothCamera.INSTANCE.getState()) {
                    GlStateManager.translate(prevRenderX - d0, d1 - prevRenderY, prevRenderZ - d2);
                }

                GlStateManager.rotate(-(FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw), 0.0F, 1.0F, 0.0F);
            }
        } else {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }

        if (!this.mc.gameSettings.debugCamEnable) {
            float yaw = (FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.prevRotationYaw) + ((FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.rotationYaw) - (FreeLook.perspectiveToggled ? FreeLook.cameraYaw : entity.prevRotationYaw)) * p_orientCamera_1_ + 180.0F;
            float pitch = (FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.prevRotationPitch) + ((FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.rotationPitch) - (FreeLook.perspectiveToggled ? FreeLook.cameraPitch : entity.prevRotationPitch)) * p_orientCamera_1_;
            float roll = 0.0F;
            if (entity instanceof EntityAnimal) {
                EntityAnimal entityanimal = (EntityAnimal)entity;
                yaw = entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * p_orientCamera_1_ + 180.0F;
            }

            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, p_orientCamera_1_);
            EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup((EntityRenderer) (Object) this, entity, block, p_orientCamera_1_, yaw, pitch, roll);
            MinecraftForge.EVENT_BUS.post(event);
            GlStateManager.rotate(event.roll, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(event.pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(event.yaw, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)p_orientCamera_1_;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)p_orientCamera_1_ + (double)f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)p_orientCamera_1_;
        this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, p_orientCamera_1_);
    }
    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.BEFORE))
    private void setupCameraViewBobbingBefore(final CallbackInfo callbackInfo) {
        if (CrossSine.moduleManager.getModule(Tracers.class).getState()) GL11.glPushMatrix();
    }

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V", shift = At.Shift.AFTER))
    private void setupCameraViewBobbingAfter(final CallbackInfo callbackInfo) {
        if (CrossSine.moduleManager.getModule(Tracers.class).getState()) GL11.glPopMatrix();
    }

    /**
     * @author Liuli
     */


    @Redirect(method = "updateCameraAndRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHasFocus:Z", opcode = GETFIELD))
    public boolean updateCameraAndRender(Minecraft minecraft) {
        if (FreeLook.isEnabled) {
            if (!CrossSine.moduleManager.getModule(FreeLook.class).getReverse().get())
                return FreeLook.overrideMouse();
            else return true;
        } else return mc.inGameHasFocus && Display.isActive();
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;viewBobbing:Z", ordinal = 0))
    public boolean setupCameraTransform(GameSettings instance) {
        return (CrossSine.moduleManager.getModule(ViewBobing.class).getMiniViewBobing().get() || !CrossSine.moduleManager.getModule(ViewBobing.class).getState()) && mc.gameSettings.viewBobbing;
    }
    @Inject(method = "getMouseOver", at = @At("HEAD"), cancellable = true)
    private void getMouseOver(float p_getMouseOver_1_, CallbackInfo ci) {
        Entity entity = mc.getRenderViewEntity();
        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;

            double d0 = mc.playerController.getBlockReachDistance();
            Vec3 vec3 = entity.getPositionEyes(p_getMouseOver_1_);
            Rotation rotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            Vec3 vec31 = RotationUtils.getVectorForRotation(RotationUtils.targetRotation != null ? RotationUtils.targetRotation : RotationUtils.freeLookRotation != null ? RotationUtils.freeLookRotation : rotation);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            mc.objectMouseOver = entity.worldObj.rayTraceBlocks(vec3, vec32, false, false, true);
            double d1 = d0;
            boolean flag = false;
            if (mc.playerController.extendedReach()) {
                d1 = 6;
            } else if (d0 > 3) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            pointedEntity = null;
            Vec3 vec33 = null;
            List<Entity> list = mc.theWorld.getEntities(Entity.class, Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith() && p_apply_1_ != entity));
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.getCollisionBorderSize();

                final ArrayList<AxisAlignedBB> boxes = new ArrayList<>();
                boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));

                for (final AxisAlignedBB axisalignedbb : boxes) {
                    MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
                    if (axisalignedbb.isVecInside(vec3)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                            d2 = 0;
                        }
                    } else if (movingobjectposition != null) {
                        double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                        if (d3 < d2 || d2 == 0) {
                            if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0) {
                                    pointedEntity = entity1;
                                    vec33 = movingobjectposition.hitVec;
                                }
                            } else {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > (SilentAura.INSTANCE.getState() ? SilentAura.INSTANCE.getReach() : Reach.INSTANCE.getState() ? Reach.INSTANCE.getReach() : 3)) {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, Objects.requireNonNull(vec33), null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }

            mc.mcProfiler.endSection();
        }

        ci.cancel();
    }
    private float getNightVisionBrightness(EntityLivingBase p_getNightVisionBrightness_1_, float p_getNightVisionBrightness_2_) {
        int i = p_getNightVisionBrightness_1_.getActivePotionEffect(Potion.nightVision).getDuration();
        return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float) i - p_getNightVisionBrightness_2_) * 3.1415927F * 0.2F) * 0.3F;
    }
}