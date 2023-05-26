package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.visual.Animations;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
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
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBlockTransformations();

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    private Animations animations;

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

    /**
     * @author Liuli
     */
    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        if(animations==null){
            animations = CrossSine.moduleManager.getModule(Animations.class);
        }
        if (mc.thePlayer.getHeldItem() != null && animations.getBlockAnimation().get()) {
            if (this.mc.thePlayer.getItemInUseCount() > 0) {
                final boolean mouseDown = this.mc.gameSettings.keyBindAttack.isKeyDown() &&
                        this.mc.gameSettings.keyBindUseItem.isKeyDown();
                if (mouseDown && this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    swingItem(this.mc.thePlayer);
                }
            }
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
                switch ((killAura.getDisplayBlocking() || animations.getAnythingBlockValue()) ? EnumAction.BLOCK : this.itemToRender.getItemUseAction()) {
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
                        if(animations.getState() && animations.getAnimationMode().equals("Full") || animations.getState() && animations.getAnimationMode().equals("Normal")){
                            GL11.glTranslated(0.0, 0.0, 0.0);
                            switch (animations.getBlockingModeValue().get()) {
                                case "1.7": {
                                    transformFirstPersonItem(f, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Akrien": {
                                    transformFirstPersonItem(f1, 0.0F);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Tap" : {
                                    this.tap(f, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Tap2" : {
                                    this.tap2(f, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Chill" : {
                                    this.transformFirstPersonItem(f / 2.0f - 0.18f, 0.0f);
                                    GL11.glRotatef(f1 * 60.0f / 2.0f, -f1 / 2.0f, -0.0f, -16.0f);
                                    GL11.glRotatef(-f1 * 30.0f, 1.0f, f1 / 2.0f, -1.0f);
                                    this.doBlockTransformations();
                                    break;
                                }
                                case "Leaked": {
                                    this.transformFirstPersonItem(f, 0);
                                    this.doBlockTransformations();
                                    GlStateManager.rotate(-MathHelper.sin((float) (MathHelper.sqrt_float(equippedProgress) * Math.PI)) * 30.0F, 0.5F, 0.5F, 0);
                                    break;
                                }
                                case "Leaked2": {
                                    this.transformFirstPersonItem(f, 0);
                                    GlStateManager.translate(0.0F, 0.1F, 0.0F);
                                    this.doBlockTransformations();
                                    GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F) * 35.0F / 2.0F, 0.0F, 1.0F, 1.5F);
                                    GlStateManager.rotate(-MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F) * 135.0F / 4.0F, 1.0F, 1.0F, 0.0F);
                                    break;
                                }
                                case "Remix": {
                                    float f4 = MathHelper.sin((float) (MathHelper.sqrt_float(f1) * 3.1));
                                    GL11.glTranslated(-0.1D, 0.00D, 0.0D);
                                    this.transformFirstPersonItem(-0.45F, 0F);
                                    GlStateManager.rotate(-f4 * 60.0F / 2.0F, f4 / 2.0F, -0.0F, 4.0F);
                                    GlStateManager.rotate(-f4 * 40.0F, 1.0F, f4 / 2.0F, -0.0F);
                                    this.doBlockTransformations();
                                    break;
                                }
                                case "Avatar": {
                                    avatar(f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "ETB": {
                                    etb(f, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Exhibition": {
                                    transformFirstPersonItem(f, 0.83F);
                                    float f4 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.83F);
                                    GlStateManager.translate(-0.5F, 0.2F, 0.2F);
                                    GlStateManager.rotate(-f4 * 0.0F, 0.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(-f4 * 43.0F, 58.0F, 23.0F, 45.0F);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Push": {
                                    push(f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Reverse": {
                                    transformFirstPersonItem(f1, f1);
                                    doBlockTransformations();
                                    GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
                                    break;
                                }
                                case "Shield": {
                                    jello(f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "SigmaNew": {
                                    doItemRenderGLTranslate();
                                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                                    float var11 = MathHelper.sin(f1 * f1 * 3.1415927F);
                                    float var12 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GlStateManager.rotate(var12 * -5.0F, 1.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(var12 * 0.0F, 0.0F, 0.0F, 1.0F);
                                    GlStateManager.rotate(var12 * 25.0F, 0.0F, 1.0F, 0.0F);
                                    doItemRenderGLScale();
                                    doBlockTransformations();
                                    break;
                                }
                                case "SigmaOld": {
                                    sigmaOld(f);
                                    float var15 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GlStateManager.rotate(-var15 * 55.0F / 2.0F, -8.0F, -0.0F, 9.0F);
                                    GlStateManager.rotate(-var15 * 45.0F, 1.0F, var15 / 2.0F, -0.0F);
                                    doBlockTransformations();
                                    GL11.glTranslated(1.2D, 0.3D, 0.5D);
                                    GL11.glTranslatef(-1.0F, mc.thePlayer.isSneaking() ? -0.1F : -0.2F, 0.2F);
                                    GlStateManager.scale(1.2F, 1.2F, 1.2F);
                                    break;
                                }
                                case "Slide": {
                                    slide(f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "SlideDown": {
                                    transformFirstPersonItem(0.2F, f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Swong": {
                                    transformFirstPersonItem(f / 2.0F, 0.0F);
                                    GlStateManager.rotate(-MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F) * 40.0F / 2.0F, MathHelper.sqrt_float(f1) / 2.0F, -0.0F, 9.0F);
                                    GlStateManager.rotate(-MathHelper.sqrt_float(f1) * 30.0F, 1.0F, MathHelper.sqrt_float(f1) / 2.0F, -0.0F);
                                    doBlockTransformations();
                                    break;
                                }
                                case "VisionFX": {
                                    continuity(f1);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Swank": {
                                    GL11.glTranslated(-0.1, 0.15, 0.0);
                                    this.transformFirstPersonItem(f / 0.15f, f1);
                                    final float rot = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927f);
                                    GlStateManager.rotate(rot * 30.0f, 2.0f, -rot, 9.0f);
                                    GlStateManager.rotate(rot * 35.0f, 1.0f, -rot, -0.0f);
                                    this.doBlockTransformations();
                                    break;
                                }
                                case "Jello": {
                                    this.transformFirstPersonItem(0.0f, 0.0f);
                                    this.doBlockTransformations();
                                    final int alpha = (int) Math.min(255L, ((System.currentTimeMillis() % 255L > 127L) ? Math.abs(Math.abs(System.currentTimeMillis()) % 255L - 255L) : (System.currentTimeMillis() % 255L)) * 2L);
                                    GlStateManager.translate(0.3f, -0.0f, 0.4f);
                                    GlStateManager.rotate(0.0f, 0.0f, 0.0f, 1.0f);
                                    GlStateManager.translate(0.0f, 0.5f, 0.0f);
                                    GlStateManager.rotate(90.0f, 1.0f, 0.0f, -1.0f);
                                    GlStateManager.translate(0.6f, 0.5f, 0.0f);
                                    GlStateManager.rotate(-90.0f, 1.0f, 0.0f, -1.0f);
                                    GlStateManager.rotate(-10.0f, 1.0f, 0.0f, -1.0f);
                                    GlStateManager.rotate(abstractclientplayer.isSwingInProgress ? (-alpha / 5.0f) : 1.0f, 1.0f, -0.0f, 1.0f);
                                    break;
                                }
                                case "HSlide": {
                                    transformFirstPersonItem(f1 != 0 ? Math.max(1 - (f1 * 2), 0) * 0.7F : 0, 1F);
                                    doBlockTransformations();
                                    break;
                                }
                                case "None": {
                                    transformFirstPersonItem(0F, 0F);
                                    doBlockTransformations();
                                    break;
                                }
                                case "Rotate": {
                                    rotateSword(f1);
                                    break;
                                }
                                case "Liquid": {
                                    this.transformFirstPersonItem(f + 0.1F, f1);
                                    this.doBlockTransformations();
                                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                    break;
                                }
                                case "Fall": {
                                    doItemRenderGLTranslate();
                                    GlStateManager.translate(0.0F, f * -0.6F, 0.0F);
                                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                                    doItemRenderGLScale();
                                    doBlockTransformations();
                                    break;
                                }
                                case "Yeet": {
                                    doItemRenderGLTranslate();
                                    GlStateManager.translate(0.0F, f * -0.6F, 0.0F);
                                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                                    float var11 = MathHelper.sin(f1 * f1 * 3.1415927F);
                                    float var12 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GlStateManager.rotate(var11 * 0.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(var12 * 0.0F, 0.0F, 0.0F, 1.0F);
                                    GlStateManager.rotate(var12 * -40.0F + 10F, 1.0F, 0.0F, 0.0F);
                                    doItemRenderGLScale();
                                    doBlockTransformations();
                                    break;
                                }
                                case "Yeet2": {
                                    doItemRenderGLTranslate();
                                    GlStateManager.translate(0.0F, f * -0.8F, 0.0F);
                                    GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                                    float var11 = MathHelper.sin(f1 * f1 * 3.1415927F);
                                    float var12 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GlStateManager.rotate(var11 * 0.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(var12 * 0.0F, 0.0F, 0.0F, 1.0F);
                                    GlStateManager.rotate(var12 * -20.0F - 9.5F, 1.0F, 0.0F, 0.0F);
                                    doItemRenderGLScale();
                                    doBlockTransformations();
                                    break;
                                }
                                case "Dortware1": {
                                    float var9 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GL11.glTranslated(-0.04D, 0.0D, 0.0D);
                                    this.transformFirstPersonItem(f / 2.5F, 0.0f);
                                    GlStateManager.rotate(-var9 * 0.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
                                    GlStateManager.rotate(-var9 * 120.0F, 1.0F, var9 / 3.0F, -0.0F);
                                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                    break;
                                }
                                case "Dortware2": {
                                    float var9 = MathHelper.sin(MathHelper.sqrt_float(this.mc.thePlayer.getSwingProgress(partialTicks)) * 3.1415927F);
                                    GL11.glTranslated(0.0D, 0.0D, 0.0D);
                                    this.transformFirstPersonItem(f / Animations.Equip, 0.0f);
                                    GlStateManager.rotate(-var9 * 120.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
                                    GlStateManager.rotate(-var9 * 120.0F, 1.0F, var9 / 3.0F, -0.0F);
                                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                    break;
                                }
                                case "Moon": {
                                    float var9 = MathHelper.sin(MathHelper.sqrt_float(this.mc.thePlayer.getSwingProgress(partialTicks)) * 3.1415927F);
                                    GL11.glTranslated(0.0D, 0.0D, 0.0D);
                                    this.transformFirstPersonItem(f / Animations.Equip, 0.0f);
                                    GlStateManager.rotate(-var9 * 65.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
                                    GlStateManager.rotate(-var9 * 60.0F, 1.0F, var9 / 3.0F, -0.0F);
                                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                    break;
                                }
                                case "Stella": {
                                    this.transformFirstPersonItem(f / Animations.Equip, f1);
                                    GlStateManager.translate(-0.5F, 0.3F, -0.2F);
                                    GlStateManager.rotate(32, 0, 1, 0);
                                    GlStateManager.rotate(-70, 1, 0, 0);
                                    GlStateManager.rotate(40, 0, 1, 0);
                                    break;
                                }
                                case "Zoom": {
                                    this.Zoom(f / Animations.Equip, f1);
                                    GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                    GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                    GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                    GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                    break;
                                }
                                case "Astolfo": {
                                    transformFirstPersonItem(f / Animations.Equip, f1);
                                    GL11.glTranslated(0.0D, 0.0D, 0.0D);
                                    float Swang = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
                                    GlStateManager.rotate(Swang * -50.0F / 2.0F, -Swang, -0.0F, 9.0F);
                                    GlStateManager.rotate(Swang * 40.0F, 1.0F, -Swang / 2.0F, -0.0F);
                                    doBlockTransformations();
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
                if (animations.getState() && animations.getAnimationMode().equals("Normal") || !animations.getState() || animations.getState() && !animations.getFluxAnimation().get() && animations.getAnimationMode().equals("Full"))
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
        if(animations.getState() && animations.getAnimationMode().equals("Full")) {
            GlStateManager.translate(animations.getItemPosXValue().get(), animations.getItemPosYValue().get(), animations.getItemPosZValue().get());
        }else{
            GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        }
    }

    private void doItemRenderGLScale(){
        if(animations.getState() && animations.getAnimationMode().equals("Full")) {
            GlStateManager.scale(animations.getItemScaleValue().get(), animations.getItemScaleValue().get(), animations.getItemScaleValue().get());
        }else if (animations.getBlockAnimation().get()) {
                if (!mc.thePlayer.isUsingItem()) {
                    GlStateManager.scale(0.39F, 0.39F, 0.39F);
                } else {
                    GlStateManager.scale(0.38F, 0.38F, 0.38F);
                }
        } else {
            GlStateManager.scale(0.4F, 0.4F, 0.4F);
        }
    }

    private void sigmaOld(float f) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, f * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(0F, 0.0F, 1.0F, 0.2F);
        GlStateManager.rotate(0F, 0.2F, 0.1F, 1.0F);
        GlStateManager.rotate(0F, 1.3F, 0.1F, 0.2F);
        doItemRenderGLScale();
    }

    //methods in LiquidBounce b73 Animation-No-Cross
    private void avatar(float swingProgress) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float f2 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f2 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f2 * -40.0F, 1.0F, 0.0F, 0.0F);
        doItemRenderGLScale();
    }

    private void slide(float var9) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var11 = MathHelper.sin(var9 * var9 * 3.1415927F);
        float var12 = MathHelper.sin(MathHelper.sqrt_float(var9) * 3.1415927F);
        GlStateManager.rotate(var11 * 0.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(var12 * 0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(var12 * -40.0F, 1.0F, 0.0F, 0.0F);
        doItemRenderGLScale();
    }

    private void rotateSword(float f1){
        genCustom();
        doBlockTransformations();
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(MathHelper.sqrt_float(f1) * 10.0F * 40.0F, 1.0F, -0.0F, 2.0F);
    }

    private void genCustom() {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, (float) 0.0 * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin((float) 0.0 * (float) 0.0 * 3.1415927F);
        float var4 = MathHelper.sin(MathHelper.sqrt_float((float) 0.0) * 3.1415927F);
        GlStateManager.rotate(var3 * -34.0F, 0.0F, 1.0F, 0.2F);
        GlStateManager.rotate(var4 * -20.7F, 0.2F, 0.1F, 1.0F);
        GlStateManager.rotate(var4 * -68.6F, 1.3F, 0.1F, 0.2F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }


    private void jello(float var12) {
        doItemRenderGLTranslate();
        GlStateManager.rotate(48.57F, 0.0F, 0.24F, 0.14F);
        float var13 = MathHelper.sin(var12 * var12 * 3.1415927F);
        float var14 = MathHelper.sin(MathHelper.sqrt_float(var12) * 3.1415927F);
        GlStateManager.rotate(var13 * -35.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var14 * 0.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var14 * 20.0F, 1.0F, 1.0F, 1.0F);
        doItemRenderGLScale();
    }

    private void continuity(float var10) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var12 = -MathHelper.sin(var10 * var10 * 3.1415927F);
        float var13 = MathHelper.cos(MathHelper.sqrt_float(var10) * 3.1415927F);
        float var14 = MathHelper.abs(MathHelper.sqrt_float((float) 0.1) * 3.1415927F);
        GlStateManager.rotate(var12 * var14 * 30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(var13 * 0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(var13 * 20.0F, 1.0F, 0.0F, 0.0F);
        doItemRenderGLScale();
    }

    public void sigmaNew(float var22, float var23) {
        doItemRenderGLTranslate();
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var24 = MathHelper.sin(var23 * MathHelper.sqrt_float(var22) * 3.1415927F);
        float var25 = MathHelper.abs(MathHelper.sqrt_double(var22) * 3.1415927F);
        GlStateManager.rotate(var24 * 20.0F * var25, 0.0F, 1.0F, 1.0F);
        doItemRenderGLScale();
    }

    private void etb(float equipProgress, float swingProgress) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float var4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(var3 * -34.0F, 0.0F, 1.0F, 0.2F);
        GlStateManager.rotate(var4 * -20.7F, 0.2F, 0.1F, 1.0F);
        GlStateManager.rotate(var4 * -68.6F, 1.3F, 0.1F, 0.2F);
        doItemRenderGLScale();
    }

    private void push(float idc) {
        doItemRenderGLTranslate();
        GlStateManager.translate(0.0F, (float) 0.1 * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(idc * idc * 3.1415927F);
        float var4 = MathHelper.sin(MathHelper.sqrt_float(idc) * 3.1415927F);
        GlStateManager.rotate(var3 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(var4 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(var4 * -10.0F, 1.0F, 1.0F, 1.0F);
        doItemRenderGLScale();
    }
    private void Zoom(float p_178096_1_, float p_178096_2_) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, p_178096_1_ * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(p_178096_2_ * p_178096_2_ * (float) Math.PI);
        float var4 = MathHelper.sin(MathHelper.sqrt_float(p_178096_2_) * (float) Math.PI);
        GlStateManager.rotate(var3 * -20.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -20.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -20.0F, 0.0F, 0.0F, 0.0F);
        doItemRenderGLScale();
    }
    private void tap(float var2, float swingProgress) {
        float smooth = (swingProgress*0.8f - (swingProgress*swingProgress)*0.8f);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F,  var2 * -0.15F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(smooth * -90.0F, 0.0F, 1.0F, 0.0F);
        doItemRenderGLScale();
    }
    private void tap2(float var2, float swing) {
        float var4 = MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.translate(0.0F,  var2 * -0.15F, 0.0F);
        GlStateManager.rotate(30 , 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(var4 * -30.0F, 0.0F, 1.0F, 0.0F);
        doItemRenderGLScale();
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
    private void swingItem(EntityPlayerSP player) {
        int swingEnd = player.isPotionActive(Potion.digSpeed) ?
                (6 - (1 + player.getActivePotionEffect(Potion.digSpeed).getAmplifier())) : (player.isPotionActive(Potion.digSlowdown) ?
                (6 + (1 + player.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2) : 6);
        if (!player.isSwingInProgress || player.swingProgressInt >= swingEnd / 2 || player.swingProgressInt < 0) {
            player.swingProgressInt = -1;
            player.isSwingInProgress = true;
        }
    }
}