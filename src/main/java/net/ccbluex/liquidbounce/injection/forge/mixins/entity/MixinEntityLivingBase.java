/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.features.module.modules.client.ClientRender;
import net.ccbluex.liquidbounce.features.module.modules.movement.*;
import net.ccbluex.liquidbounce.features.module.modules.other.ViaVersionFix;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {

    @Shadow
    protected boolean isJumping;
    @Shadow
    private int jumpTicks;

    @Shadow
    protected abstract float getJumpUpwardsMotion();

    @Shadow
    public abstract PotionEffect getActivePotionEffect(Potion potionIn);

    @Shadow
    public abstract boolean isPotionActive(Potion potionIn);

    @Shadow
    public void onLivingUpdate() {
    }

    @Shadow
    public float swingProgress;
    @Shadow
    public float renderYawOffset;

    @Shadow
    protected abstract void updateFallState(double y, boolean onGroundIn, Block blockIn, BlockPos pos);

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    protected abstract void updateAITick();


    @Overwrite
    protected float updateDistance(float p_1101461, float p_1101462) {
        float rotationYaw = this.rotationYaw;
        ClientRender clientRender = CrossSine.moduleManager.getModule(ClientRender.class);
        if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP) {
            if (clientRender.getState()) {
                if (clientRender.getPlayerYaw() != null) {
                    rotationYaw = clientRender.getPlayerYaw();
                }
            }
        }
        float f = MathHelper.wrapAngleTo180_float(p_1101461 - this.renderYawOffset);
        this.renderYawOffset += f * 0.3F;
        float f1 = MathHelper.wrapAngleTo180_float(rotationYaw - this.renderYawOffset);
        boolean flag = f1 < 75.0F || f1 >= 75.0F;

        if (f1 < -75.0F) {
            f1 = -75.0F;
        }

        if (f1 >= 75.0F) {
            f1 = 75.0F;
        }

        this.renderYawOffset = rotationYaw - f1;
        if (f1 * f1 > 2500.0F) {
            this.renderYawOffset += f1 * 0.2F;
        }

        if (flag) {
            p_1101462 *= -1.0F;
        }

        return p_1101462;
    }
    /**
     * @author CCBlueX
     * @author CoDynamic
     * Modified by Co Dynamic
     * Date: 2023/02/15
     */
    @Overwrite
    protected void jump() {
        if (!this.equals(Minecraft.getMinecraft().thePlayer)) {
            return;
        }

        /**
         * Jump Process Fix
         * use updateFixState to reset Jump Fix state
         * @param fixedYaw  The yaw player should have (NOT RotationYaw)
         * @param strafeFix StrafeFix Module
         */

        final JumpEvent jumpEvent = new JumpEvent(MovementUtils.INSTANCE.getJumpMotion());
        CrossSine.eventManager.callEvent(jumpEvent);
        if (jumpEvent.isCancelled())
            return;

        this.motionY = jumpEvent.getMotion();
        final Sprint sprint = CrossSine.moduleManager.getModule(Sprint.class);
        final StrafeFix strafeFix = CrossSine.moduleManager.getModule(StrafeFix.class);

        if (this.isSprinting()) {
            float fixedYaw = this.rotationYaw;
            if(RotationUtils.targetRotation != null && strafeFix.getDoFix()) {
                fixedYaw = RotationUtils.targetRotation.getYaw();
            }
            if(sprint.getState() && sprint.getJumpDirectionsValue().get()) {
                fixedYaw += MovementUtils.INSTANCE.getMovingYaw() - this.rotationYaw;
            }
            this.motionX -= MathHelper.sin(fixedYaw / 180F * 3.1415927F) * 0.2F;
            this.motionZ += MathHelper.cos(fixedYaw / 180F * 3.1415927F) * 0.2F;
        }

        this.isAirBorne = true;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;isJumping:Z", ordinal = 1))
    private void onJumpSection(CallbackInfo callbackInfo) {
        final Jesus jesus = CrossSine.moduleManager.getModule(Jesus.class);

        if (jesus.getState() && !isJumping && !isSneaking() && isInWater() &&
                jesus.getModeValue().equals("Legit")) {
            this.updateAITick();
        }
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 0.005D))
    private double ViaVersion_MovementThreshold(double constant) {
        if (Objects.requireNonNull(CrossSine.moduleManager.getModule(ViaVersionFix.class)).getState())
            return 0.003D;
        return 0.005D;
    }

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void headLiving(CallbackInfo callbackInfo) {
        final JumpDelayChanger jumpDelayChanger = CrossSine.moduleManager.getModule(JumpDelayChanger.class);
        if (jumpDelayChanger.getState())
        jumpTicks = jumpDelayChanger.getJumpDelayValue().get();
    }

    @Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
    private void getLook(CallbackInfoReturnable<Vec3> callbackInfoReturnable) {
        if (((EntityLivingBase) (Object) this) instanceof EntityPlayerSP)
            callbackInfoReturnable.setReturnValue(getVectorForRotation(this.rotationPitch, this.rotationYaw));
    }

    @Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
    private void isPotionActive(Potion p_isPotionActive_1_, final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        final NoRender NoRender = CrossSine.moduleManager.getModule(NoRender.class);

        if ((p_isPotionActive_1_ == Potion.confusion || p_isPotionActive_1_ == Potion.blindness) && NoRender.getState() && NoRender.getConfusionEffect().get())
            callbackInfoReturnable.setReturnValue(false);
    }

    /**
     * @author Liuli
     */
    @Overwrite
    private int getArmSwingAnimationEnd() {
        int speed = this.isPotionActive(Potion.digSpeed) ? 6 - (1 + this.getActivePotionEffect(Potion.digSpeed).getAmplifier()) : (this.isPotionActive(Potion.digSlowdown) ? 6 + (1 + this.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);

        if (Animations.INSTANCE.getState()) {
            if (this.equals(Minecraft.getMinecraft().thePlayer)) {
                speed = (int) (speed * Animations.INSTANCE.getSwingSpeedValue().get());
            }
        } else {
            if (this.equals(Minecraft.getMinecraft().thePlayer)) {
                speed = (int) (speed);
            }
        }

        return speed;
    }
}