
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.JumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.Jesus;
import net.ccbluex.liquidbounce.features.module.modules.other.ViaVersionFix;
import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
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
import net.minecraftforge.common.ForgeHooks;
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
    public float rotationYawHead;
    @Shadow
    public float prevRotationYawHead;

    @Shadow
    protected abstract float getJumpUpwardsMotion();

    @Shadow
    public void swingItem() {
    }

    @Shadow
    public abstract PotionEffect getActivePotionEffect(Potion potionIn);

    @Shadow
    public abstract boolean isPotionActive(Potion potionIn);

    @Shadow
    public void onLivingUpdate() {
    }

    @Shadow
    public float moveStrafing;
    @Shadow
    public float moveForward;
    @Shadow
    private EntityLivingBase lastAttacker;
    @Shadow
    private int lastAttackerTime;
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


    @Shadow
    public float prevCameraPitch;

    @Overwrite
    protected float updateDistance(float p_110146_1_, float p_110146_2_) {
        float rotationYaw = this.rotationYaw;
        if ((EntityLivingBase) (Object) this instanceof EntityPlayerSP) {
            if (RotationUtils.playerYaw != null) {
                if (this.swingProgress > 0F) {
                    p_110146_1_ = RotationUtils.playerYaw;
                }
                rotationYaw = RotationUtils.playerYaw;
            }
        }
        float f = MathHelper.wrapAngleTo180_float(p_110146_1_ - this.renderYawOffset);
        this.renderYawOffset += f * 0.3F;
        float f1 = MathHelper.wrapAngleTo180_float(rotationYaw - this.renderYawOffset);
        boolean flag = f1 < -90.0F || f1 >= 90.0F;
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
            p_110146_2_ *= -1.0F;
        }

        return p_110146_2_;
    }

    /**
     * @author CCBlueX
     * @author CoDynamic
     * Modified by Co Dynamic
     * Date: 2023/02/15
     */
    @Overwrite
    protected void jump() {
        if (((EntityLivingBase) (Object) this).equals(Minecraft.getMinecraft().thePlayer)) {
            final JumpEvent event = new JumpEvent((float) this.motionY, this.isSprinting());
            CrossSine.eventManager.callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            this.motionY = this.getJumpUpwardsMotion();
            if (this.isPotionActive(Potion.jump)) {
                this.motionY += ((float)(this.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
            }

            if (this.isSprinting() || event.getBoosting()) {
                float f = this.rotationYaw * 0.017453292F;
                this.motionX -= (MathHelper.sin(f) * 0.2F);
                this.motionZ += (MathHelper.cos(f) * 0.2F);
            }

            this.isAirBorne = true;
            ForgeHooks.onLivingJump((EntityLivingBase) (Object) this);
        }
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
        }

        return speed;
    }

    public EntityLivingBase getLastAttacker() {
        return this.lastAttacker;
    }

    public int getLastAttackerTime() {
        return this.lastAttackerTime;
    }
}