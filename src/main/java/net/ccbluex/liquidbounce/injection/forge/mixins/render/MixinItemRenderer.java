package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool;
import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow
    private float prevEquippedProgress;

    @Shadow
    private float equippedProgress;
    @Shadow
    private int equippedItemSlot = -1;
    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityPlayerSP, float partialTicks);

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private  RenderItem itemRenderer;
    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);
    @Shadow
    protected abstract boolean isBlockTranslucent(Block p_isBlockTranslucent_1_);
    @Shadow
    protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBlockTransformations();

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    private Animations animations;

    @Overwrite
    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform) {
        if (heldStack != null) {
            Item item = heldStack.getItem();
            Block block = Block.getBlockFromItem(item);
            GlStateManager.pushMatrix();
            if (this.itemRenderer.shouldRenderItemIn3D(heldStack)) {
                if (animations.getState()) {
                    GlStateManager.scale(((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 2.0, ((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 2.0, ((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 2.0);
                } else {
                    GlStateManager.scale(2.0,2.0,2.0);
                }
                if (this.isBlockTranslucent(block)) {
                    GlStateManager.depthMask(false);
                }
            }
            GlStateManager.scale(((float) Animations.INSTANCE.getItemScaleValue().get() / 100), ((float) Animations.INSTANCE.getItemScaleValue().get() / 100), ((float) Animations.INSTANCE.getItemScaleValue().get() / 100));
            this.itemRenderer.renderItemModelForEntity(heldStack, entityIn, transform);
            if (this.isBlockTranslucent(block)) {
                GlStateManager.depthMask(true);
            }

            GlStateManager.popMatrix();
        }
    }
    /**
     * @author Liuli
     */
    @Overwrite
    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        doItemRenderGLScale();

    }

    @Unique
    private void func_178103_d() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }
    /**
     * @author Liuli
     */
    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        if(animations==null){
            animations = CrossSine.moduleManager.getModule(Animations.class);
        }
        float f = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
        AbstractClientPlayer abstractclientplayer = mc.thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
        float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
        this.rotateArroundXAndY(f2, f3);
        this.setLightMapFromPlayer(abstractclientplayer);
        this.rotateWithPlayerRotations((EntityPlayerSP) abstractclientplayer, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        if (this.itemToRender != null) {
            if (animations.getBlockAnimation().get() && (itemToRender.getItem() instanceof ItemCarrotOnAStick || itemToRender.getItem() instanceof ItemFishingRod)) {
                GlStateManager.translate(0.08F, -0.027F, -0.33F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
            final KillAura killAura = CrossSine.moduleManager.getModule(KillAura.class);


            if (this.itemToRender.getItem() instanceof ItemMap) {
                this.renderItemMap(abstractclientplayer, f2, f, f1);
            } else if ((abstractclientplayer.isUsingItem() || ((itemToRender.getItem() instanceof ItemSword) && killAura.getDisplayBlocking() && killAura.getCurrentTarget() != null))) {
                switch (this.itemToRender.getItemUseAction()) {
                    case NONE:
                        this.transformFirstPersonItem(f, 0.0F);
                        break;
                    case EAT:
                    case DRINK:
                        this.performDrinking(abstractclientplayer, partialTicks);
                        if (animations.getBlockAnimation().get()) {
                            this.transformFirstPersonItem(f, f1);
                        } else {
                            this.transformFirstPersonItem(f, 0.0F);
                        }
                        break;
                    case BLOCK:
                        if(animations.getState()){
                            switch (animations.getBlockingModeValue().get()) {
                                case "1.7": {
                                    transformFirstPersonItem(f, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "1.8": {
                                    this.transformFirstPersonItem(f, f1);
                                    this.doBlockTransformations();
                                    GlStateManager.translate(-0.35F, 0.2F, 0.0F);
                                    break;
                                }
                                case "Spin": {
                                    transformFirstPersonItem(f / 1.4F, 0.0F);
                                    mc.thePlayer.isSwingInProgress = false;
                                    GlStateManager.translate(0, 0.2F, -1);
                                    GlStateManager.rotate(-59, -1, 0, 3);
                                    GlStateManager.rotate(-(System.currentTimeMillis() / 2 % 360), 1, 0, 0.0F);
                                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                }
                                case "Slash": {
                                    final float var = MathHelper.sin((float) (MathHelper.sqrt_float(f1) * Math.PI));
                                    transformFirstPersonItem(f / 1.8f, 0.0f);
                                    this.func_178103_d();
                                    final float var16 = MathHelper.sin((float) (f1 * f1 * Math.PI));
                                    GlStateManager.rotate(-var16 * 0f, 0.0f, 1.0f, 0.0f);
                                    GlStateManager.rotate(-var * 62f, 0.0f, 0.0f, 1.0f);
                                    GlStateManager.rotate(-var * 0f, 1.5f, 0.0f, 0.0f);
                                    break;
                                }
                                case "Sigma4": {
                                    final float var = MathHelper.sin((float) (MathHelper.sqrt_float(f1) * Math.PI));
                                    transformFirstPersonItem(f / 2.0F, 0.0F);
                                    GlStateManager.rotate(-var * 55 / 2.0F, -8.0F, -0.0F, 9.0F);
                                    GlStateManager.rotate(-var * 45, 1.0F, var / 2, 0.0F);
                                    this.func_178103_d();
                                    break;
                                }
                            }
                        }else{
                            this.transformFirstPersonItem(f + 0.1F, f1);
                            this.doBlockTransformations();
                            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                        }
                        break;
                    case BOW:
                        if (animations.getBlockAnimation().get()) {
                            this.transformFirstPersonItem(f, f1);
                        } else {
                            this.transformFirstPersonItem(f, 0.0F);
                        }
                        this.doBowTransformations(partialTicks, abstractclientplayer);
                }
            }else{
                if (!animations.getState() || !animations.getFluxAnimation().get())
                    this.doItemUsedTransformations(f1);
                this.transformFirstPersonItem(f, f1);
            }

            this.renderItem(abstractclientplayer, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        }else if(!abstractclientplayer.isInvisible()) {
            this.renderPlayerArm(abstractclientplayer, f, f1);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void doItemRenderGLTranslate(){
        if(animations.getState()) {
            GlStateManager.translate(0.56F + animations.getItemPosXValue().get(), -0.52F+ animations.getItemPosYValue().get(), -0.71999997F+ animations.getItemPosZValue().get());
        }else{
            GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        }
    }

    private void doItemRenderGLScale(){
        if(animations.getState()) {
            GlStateManager.scale(((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 0.4,((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 0.4,((float) Animations.INSTANCE.getItemScaleValue().get() / 100) * 0.4);
        }else {
            GlStateManager.scale(0.4,0.4,0.4);
        }
    }
    /**
     * @author Liuli
     */
    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void renderFireInFirstPerson(final CallbackInfo callbackInfo) {
        final NoRender NoRender = CrossSine.moduleManager.getModule(NoRender.class);

        if (NoRender.getState() && NoRender.getFireEffect().get()) {
            //vanilla's method
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
            GlStateManager.depthFunc(519);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(515);
            callbackInfo.cancel();
        }
    }
}