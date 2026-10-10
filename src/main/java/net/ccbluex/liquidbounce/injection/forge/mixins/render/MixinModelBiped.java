 
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelBiped.class)
public class MixinModelBiped extends MixinModelBase{
    @Shadow
    public ModelRenderer bipedHead;
    @Shadow
    public ModelRenderer bipedHeadwear;
    @Shadow
    public ModelRenderer bipedBody;
    @Shadow
    public ModelRenderer bipedRightArm;
    @Shadow
    public ModelRenderer bipedLeftArm;
    @Shadow
    public ModelRenderer bipedRightLeg;
    @Shadow
    public ModelRenderer bipedLeftLeg;
    @Shadow
    public int heldItemLeft;
    @Shadow
    public int heldItemRight;
    @Shadow
    public boolean isSneak;
    @Shadow
    public boolean aimedBow;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void setRotationAngles(float p_setRotationAngles_1_, float p_setRotationAngles_2_, float p_setRotationAngles_3_, float p_setRotationAngles_4_, float p_setRotationAngles_5_, float p_setRotationAngles_6_, Entity p_setRotationAngles_7_) {
        this.bipedHead.rotateAngleY = p_setRotationAngles_4_ / 57.295776F;
        this.bipedHead.rotateAngleX = p_setRotationAngles_5_ / 57.295776F;
        if (RotationUtils.serverRotation != null && p_setRotationAngles_7_ instanceof EntityPlayer && p_setRotationAngles_7_.equals(Minecraft.getMinecraft().thePlayer)) {
            this.bipedHead.rotateAngleX = (float) Math.toRadians(RotationUtils.targetRotation != null && ClientTheme.INSTANCE.getSmoothRotation().get() ? RotationUtils.smoothPitch : RenderUtils.interpolate(RotationUtils.headPitch, RotationUtils.prevHeadPitch, Minecraft.getMinecraft().timer.renderPartialTicks));
        }
        this.bipedRightArm.rotateAngleX = MathHelper.cos(p_setRotationAngles_1_ * 0.6662F + 3.1415927F) * 2.0F * p_setRotationAngles_2_ * 0.5F;
        this.bipedLeftArm.rotateAngleX = MathHelper.cos(p_setRotationAngles_1_ * 0.6662F) * 2.0F * p_setRotationAngles_2_ * 0.5F;
        this.bipedRightArm.rotateAngleZ = 0.0F;
        this.bipedLeftArm.rotateAngleZ = 0.0F;
        this.bipedRightLeg.rotateAngleX = MathHelper.cos(p_setRotationAngles_1_ * 0.6662F) * 1.4F * p_setRotationAngles_2_;
        this.bipedLeftLeg.rotateAngleX = MathHelper.cos(p_setRotationAngles_1_ * 0.6662F + 3.1415927F) * 1.4F * p_setRotationAngles_2_;
        this.bipedRightLeg.rotateAngleY = 0.0F;
        this.bipedLeftLeg.rotateAngleY = 0.0F;
        ModelRenderer var10000;
        if (this.isRiding) {
            var10000 = this.bipedRightArm;
            var10000.rotateAngleX += -0.62831855F;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleX += -0.62831855F;
            this.bipedRightLeg.rotateAngleX = -1.2566371F;
            this.bipedLeftLeg.rotateAngleX = -1.2566371F;
            this.bipedRightLeg.rotateAngleY = 0.31415927F;
            this.bipedLeftLeg.rotateAngleY = -0.31415927F;
        }

        if (this.heldItemLeft != 0) {
            this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - 0.31415927F * (float)this.heldItemLeft;
        }

        this.bipedRightArm.rotateAngleY = 0.0F;
        this.bipedRightArm.rotateAngleZ = 0.0F;
        switch (this.heldItemRight) {
            case 0:
            case 2:
            default:
                break;
            case 1:
                this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - 0.31415927F * (float) this.heldItemRight;
                break;
            case 3:
                this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - 0.31415927F * (float) this.heldItemRight;
                if (Animations.INSTANCE.getState() && Animations.INSTANCE.getBlockAnimation().get()) {
                    this.bipedRightArm.rotateAngleY = 0F;
                } else {
                    this.bipedRightArm.rotateAngleY = -0.5235988F;
                }
        }

        this.bipedLeftArm.rotateAngleY = 0.0F;
        float lvt_8_2_;
        float lvt_9_2_;
        if (this.swingProgress > -9990.0F) {
            lvt_8_2_ = this.swingProgress;
            this.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(lvt_8_2_) * 3.1415927F * 2.0F) * 0.2F;
            this.bipedRightArm.rotationPointZ = MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedRightArm.rotationPointX = -MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedLeftArm.rotationPointZ = -MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedLeftArm.rotationPointX = MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleY += this.bipedBody.rotateAngleY;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleY += this.bipedBody.rotateAngleY;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleX += this.bipedBody.rotateAngleY;
            lvt_8_2_ = 1.0F - this.swingProgress;
            lvt_8_2_ *= lvt_8_2_;
            lvt_8_2_ *= lvt_8_2_;
            lvt_8_2_ = 1.0F - lvt_8_2_;
            lvt_9_2_ = MathHelper.sin(lvt_8_2_ * 3.1415927F);
            float lvt_10_1_ = MathHelper.sin(this.swingProgress * 3.1415927F) * -(this.bipedHead.rotateAngleX - 0.7F) * 0.75F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleX = (float)((double)var10000.rotateAngleX - ((double)lvt_9_2_ * 1.2 + (double)lvt_10_1_));
            var10000 = this.bipedRightArm;
            var10000.rotateAngleY += this.bipedBody.rotateAngleY * 2.0F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleZ += MathHelper.sin(this.swingProgress * 3.1415927F) * -0.4F;
        }

        if (this.isSneak) {
            this.bipedBody.rotateAngleX = 0.5F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleX += 0.4F;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleX += 0.4F;
            this.bipedRightLeg.rotationPointZ = 4.0F;
            this.bipedLeftLeg.rotationPointZ = 4.0F;
            this.bipedRightLeg.rotationPointY = 9.0F;
            this.bipedLeftLeg.rotationPointY = 9.0F;
            this.bipedHead.rotationPointY = 1.0F;
        } else {
            this.bipedBody.rotateAngleX = 0.0F;
            this.bipedRightLeg.rotationPointZ = 0.1F;
            this.bipedLeftLeg.rotationPointZ = 0.1F;
            this.bipedRightLeg.rotationPointY = 12.0F;
            this.bipedLeftLeg.rotationPointY = 12.0F;
            this.bipedHead.rotationPointY = 0.0F;
        }

        var10000 = this.bipedRightArm;
        var10000.rotateAngleZ += MathHelper.cos(p_setRotationAngles_3_ * 0.09F) * 0.05F + 0.05F;
        var10000 = this.bipedLeftArm;
        var10000.rotateAngleZ -= MathHelper.cos(p_setRotationAngles_3_ * 0.09F) * 0.05F + 0.05F;
        var10000 = this.bipedRightArm;
        var10000.rotateAngleX += MathHelper.sin(p_setRotationAngles_3_ * 0.067F) * 0.05F;
        var10000 = this.bipedLeftArm;
        var10000.rotateAngleX -= MathHelper.sin(p_setRotationAngles_3_ * 0.067F) * 0.05F;
        if (this.aimedBow) {
            lvt_8_2_ = 0.0F;
            lvt_9_2_ = 0.0F;
            this.bipedRightArm.rotateAngleZ = 0.0F;
            this.bipedLeftArm.rotateAngleZ = 0.0F;
            this.bipedRightArm.rotateAngleY = -(0.1F - lvt_8_2_ * 0.6F) + this.bipedHead.rotateAngleY;
            this.bipedLeftArm.rotateAngleY = 0.1F - lvt_8_2_ * 0.6F + this.bipedHead.rotateAngleY + 0.4F;
            this.bipedRightArm.rotateAngleX = -1.5707964F + this.bipedHead.rotateAngleX;
            this.bipedLeftArm.rotateAngleX = -1.5707964F + this.bipedHead.rotateAngleX;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleX -= lvt_8_2_ * 1.2F - lvt_9_2_ * 0.4F;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleX -= lvt_8_2_ * 1.2F - lvt_9_2_ * 0.4F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleZ += MathHelper.cos(p_setRotationAngles_3_ * 0.09F) * 0.05F + 0.05F;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleZ -= MathHelper.cos(p_setRotationAngles_3_ * 0.09F) * 0.05F + 0.05F;
            var10000 = this.bipedRightArm;
            var10000.rotateAngleX += MathHelper.sin(p_setRotationAngles_3_ * 0.067F) * 0.05F;
            var10000 = this.bipedLeftArm;
            var10000.rotateAngleX -= MathHelper.sin(p_setRotationAngles_3_ * 0.067F) * 0.05F;
        }
        copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }

}