 
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.ccbluex.liquidbounce.features.module.modules.visual.Crosshair;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.utils.ItemSpoofUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.client.gui.Gui.drawRect;
import static net.minecraft.client.gui.Gui.icons;

@Mixin(GuiIngame.class)
public abstract class MixinGuiInGame extends MixinGui {

    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);
    @Shadow
    protected abstract void renderVignette(float p_renderVignette_1_, ScaledResolution p_renderVignette_2_);
    @Shadow
    protected abstract void renderScoreboard(ScoreObjective p_renderScoreboard_1_, ScaledResolution p_renderScoreboard_2_);
    @Shadow
    protected abstract void renderPumpkinOverlay(ScaledResolution p_renderPumpkinOverlay_1_);
    @Shadow
    protected abstract void renderPortal(float p_renderPortal_1_, ScaledResolution p_renderPortal_2_);
    @Shadow
    protected abstract void renderPlayerStats(ScaledResolution p_renderPlayerStats_1_);
    @Shadow
    protected abstract void renderHorseJumpBar(ScaledResolution p_renderHorseJumpBar_1_, int p_renderHorseJumpBar_2_);
    @Shadow
    protected abstract void renderDemo(ScaledResolution p_renderDemo_1_);
    @Shadow
    protected abstract void renderSelectedItem(ScaledResolution p_renderSelectedItem_1_);
    @Shadow
    protected abstract void renderExpBar(ScaledResolution p_renderExpBar_1_, int p_renderExpBar_2_);
    @Shadow
    protected abstract void renderBossHealth();
    @Shadow
    protected abstract boolean showCrosshair();
    @Shadow
    @Final
    protected static ResourceLocation widgetsTexPath;
    @Shadow
    @Final
    protected GuiSpectator spectatorGui;
    @Shadow
    @Final
    protected GuiNewChat persistantChatGUI;
    @Shadow
    @Final
    protected GuiOverlayDebug overlayDebug;;

    @Shadow
    @Final
    protected GuiPlayerTabOverlay overlayPlayerList;

    @Shadow
    @Final
    protected Minecraft mc;
    @Shadow
    protected int recordPlayingUpFor;
    @Shadow
    protected boolean recordIsPlaying;
    @Shadow
    protected int titlesTimer;
    @Shadow
    protected int titleFadeIn;
    @Shadow
    protected int updateCounter;
    @Shadow
    protected int titleDisplayTime;
    @Shadow
    protected int titleFadeOut;
    @Shadow
    protected String recordPlaying = "";
    @Shadow
    protected String displayedTitle = "";
    @Shadow
    protected String displayedSubTitle = "";
    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    private void injectCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Crosshair.INSTANCE.getState() || HUD.INSTANCE.getState()) {
            if (mc.gameSettings.thirdPersonView != 0 && HUD.INSTANCE.getNoF5().get())
                cir.setReturnValue(false);
        }
    }
    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void injectScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo callbackInfo) {
        if (scoreObjective != null) ColorUtils.stripColor(scoreObjective.getDisplayName());

        final NoRender NoRender = CrossSine.moduleManager.getModule(NoRender.class);
        if ((NoRender.getState() && NoRender.getScoreBoard().get()) || CrossSine.moduleManager.getModule(HUD.class).getState())
            callbackInfo.cancel();
    }
    /**
     * @author liulihaocai
     */
    @Overwrite
    protected void renderTooltip(ScaledResolution sr, float partialTicks) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(widgetsTexPath);
            EntityPlayer entityPlayer = (EntityPlayer)this.mc.getRenderViewEntity();
            int i = sr.getScaledWidth() / 2;
            float f = this.zLevel;
            this.zLevel = -90.0F;
            this.drawTexturedModalRect(i - 91, sr.getScaledHeight() - 22, 0, 0, 182, 22);
            this.drawTexturedModalRect(i - 91 - 1 + ItemSpoofUtils.INSTANCE.getSlot() * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
            this.zLevel = f;
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            for(int j = 0; j < 9; ++j) {
                int k = sr.getScaledWidth() / 2 - 90 + j * 20 + 2;
                int l = sr.getScaledHeight() - 16 - 3;
                this.renderHotbarItem(j, k, l, partialTicks, entityPlayer);
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
        CrossSine.eventManager.callEvent(new Render2DEvent(partialTicks, StaticStorage.scaledResolution));
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void renderOverlay(final CallbackInfo callbackInfo) {
        final NoRender NoRender = CrossSine.moduleManager.getModule(NoRender.class);

        if(NoRender.getState() && NoRender.getPumpkinEffect().get())
            callbackInfo.cancel();
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void injectBossHealth(CallbackInfo callbackInfo) {
        final NoRender NoRender = (NoRender) CrossSine.moduleManager.getModule(NoRender.class);
        if (NoRender.getState() && NoRender.getBossHealth().get())
            callbackInfo.cancel();
    }
    @Overwrite
    public void renderGameOverlay(float p_renderGameOverlay_1_) {
        ScaledResolution lvt_2_1_ = new ScaledResolution(this.mc);
        int lvt_3_1_ = lvt_2_1_.getScaledWidth();
        int lvt_4_1_ = lvt_2_1_.getScaledHeight();
        this.mc.entityRenderer.setupOverlayRendering();
        GlStateManager.enableBlend();
        if (Minecraft.isFancyGraphicsEnabled()) {
            this.renderVignette(this.mc.thePlayer.getBrightness(p_renderGameOverlay_1_), lvt_2_1_);
        } else {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }

        ItemStack lvt_5_1_ = this.mc.thePlayer.inventory.armorItemInSlot(3);
        if (this.mc.gameSettings.thirdPersonView == 0 && lvt_5_1_ != null && lvt_5_1_.getItem() == Item.getItemFromBlock(Blocks.pumpkin)) {
            this.renderPumpkinOverlay(lvt_2_1_);
        }

        if (!this.mc.thePlayer.isPotionActive(Potion.confusion)) {
            float lvt_6_1_ = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * p_renderGameOverlay_1_;
            if (lvt_6_1_ > 0.0F) {
                this.renderPortal(lvt_6_1_, lvt_2_1_);
            }
        }

        if (this.mc.playerController.isSpectator()) {
            this.spectatorGui.renderTooltip(lvt_2_1_, p_renderGameOverlay_1_);
        } else {
            this.renderTooltip(lvt_2_1_, p_renderGameOverlay_1_);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(icons);
        GlStateManager.enableBlend();
        if (this.showCrosshair()) {
            GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
            GlStateManager.enableAlpha();
            this.drawTexturedModalRect(lvt_3_1_ / 2 - 7, lvt_4_1_ / 2 - 7, 0, 0, 16, 16);
        }

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        this.mc.mcProfiler.startSection("bossHealth");
        this.renderBossHealth();
        this.mc.mcProfiler.endSection();
        if (this.mc.playerController.shouldDrawHUD()) {
            this.renderPlayerStats(lvt_2_1_);
        }

        GlStateManager.disableBlend();
        float lvt_7_3_;
        int lvt_8_3_;
        int lvt_6_3_;
        if (this.mc.thePlayer.getSleepTimer() > 0) {
            this.mc.mcProfiler.startSection("sleep");
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            lvt_6_3_ = this.mc.thePlayer.getSleepTimer();
            lvt_7_3_ = (float)lvt_6_3_ / 100.0F;
            if (lvt_7_3_ > 1.0F) {
                lvt_7_3_ = 1.0F - (float)(lvt_6_3_ - 100) / 10.0F;
            }

            lvt_8_3_ = (int)(220.0F * lvt_7_3_) << 24 | 1052704;
            drawRect(0, 0, lvt_3_1_, lvt_4_1_, lvt_8_3_);
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            this.mc.mcProfiler.endSection();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        lvt_6_3_ = lvt_3_1_ / 2 - 91;
        if (this.mc.thePlayer.isRidingHorse()) {
            this.renderHorseJumpBar(lvt_2_1_, lvt_6_3_);
        } else if (this.mc.playerController.gameIsSurvivalOrAdventure()) {
            this.renderExpBar(lvt_2_1_, lvt_6_3_);
        }

        if (this.mc.gameSettings.heldItemTooltips && !this.mc.playerController.isSpectator()) {
            this.renderSelectedItem(lvt_2_1_);
        } else if (this.mc.thePlayer.isSpectator()) {
            this.spectatorGui.renderSelectedItem(lvt_2_1_);
        }

        if (this.mc.isDemo()) {
            this.renderDemo(lvt_2_1_);
        }

        if (this.mc.gameSettings.showDebugInfo) {
            this.overlayDebug.renderDebugInfo(lvt_2_1_);
        }

        int lvt_9_4_;
        if (this.recordPlayingUpFor > 0) {
            this.mc.mcProfiler.startSection("overlayMessage");
            lvt_7_3_ = (float)this.recordPlayingUpFor - p_renderGameOverlay_1_;
            lvt_8_3_ = (int)(lvt_7_3_ * 255.0F / 20.0F);
            if (lvt_8_3_ > 255) {
                lvt_8_3_ = 255;
            }

            if (lvt_8_3_ > 8) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(lvt_3_1_ / 2), (float)(lvt_4_1_ - 68), 0.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                lvt_9_4_ = 16777215;
                if (this.recordIsPlaying) {
                    lvt_9_4_ = MathHelper.hsvToRGB(lvt_7_3_ / 50.0F, 0.7F, 0.6F) & 16777215;
                }

                mc.fontRendererObj.drawString(this.recordPlaying, -mc.fontRendererObj.getStringWidth(this.recordPlaying) / 2, -4, lvt_9_4_ + (lvt_8_3_ << 24 & -16777216));
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        if (this.titlesTimer > 0) {
            this.mc.mcProfiler.startSection("titleAndSubtitle");
            lvt_7_3_ = (float)this.titlesTimer - p_renderGameOverlay_1_;
            lvt_8_3_ = 255;
            if (this.titlesTimer > this.titleFadeOut + this.titleDisplayTime) {
                float lvt_9_2_ = (float)(this.titleFadeIn + this.titleDisplayTime + this.titleFadeOut) - lvt_7_3_;
                lvt_8_3_ = (int)(lvt_9_2_ * 255.0F / (float)this.titleFadeIn);
            }

            if (this.titlesTimer <= this.titleFadeOut) {
                lvt_8_3_ = (int)(lvt_7_3_ * 255.0F / (float)this.titleFadeOut);
            }

            lvt_8_3_ = MathHelper.clamp_int(lvt_8_3_, 0, 255);
            if (lvt_8_3_ > 8) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float)(lvt_3_1_ / 2), (float)(lvt_4_1_ / 2), 0.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.pushMatrix();
                GlStateManager.scale(4.0F, 4.0F, 4.0F);
                lvt_9_4_ = lvt_8_3_ << 24 & -16777216;
                mc.fontRendererObj.drawString(this.displayedTitle, (float)(-mc.fontRendererObj.getStringWidth(this.displayedTitle) / 2), -10.0F, 16777215 | lvt_9_4_, true);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
                mc.fontRendererObj.drawString(this.displayedSubTitle, (float)(-mc.fontRendererObj.getStringWidth(this.displayedSubTitle) / 2), 5.0F, 16777215 | lvt_9_4_, true);
                GlStateManager.popMatrix();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }

            this.mc.mcProfiler.endSection();
        }

        Scoreboard lvt_7_4_ = this.mc.theWorld.getScoreboard();
        ScoreObjective lvt_8_4_ = null;
        ScorePlayerTeam lvt_9_5_ = lvt_7_4_.getPlayersTeam(this.mc.thePlayer.getName());
        if (lvt_9_5_ != null) {
            int lvt_10_1_ = lvt_9_5_.getChatFormat().getColorIndex();
            if (lvt_10_1_ >= 0) {
                lvt_8_4_ = lvt_7_4_.getObjectiveInDisplaySlot(3 + lvt_10_1_);
            }
        }

        ScoreObjective lvt_10_2_ = lvt_8_4_ != null ? lvt_8_4_ : lvt_7_4_.getObjectiveInDisplaySlot(1);
        if (lvt_10_2_ != null) {
            this.renderScoreboard(lvt_10_2_, lvt_2_1_);
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, (float)(lvt_4_1_ - 48), 0.0F);
        this.mc.mcProfiler.startSection("chat");
        this.persistantChatGUI.drawChat(this.updateCounter);
        this.mc.mcProfiler.endSection();
        GlStateManager.popMatrix();
        lvt_10_2_ = lvt_7_4_.getObjectiveInDisplaySlot(0);
        if (this.mc.gameSettings.keyBindPlayerList.isKeyDown() && (!this.mc.isIntegratedServerRunning() || this.mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1 || lvt_10_2_ != null)) {
            this.overlayPlayerList.updatePlayerList(true);
            this.overlayPlayerList.renderPlayerlist(lvt_3_1_, lvt_7_4_, lvt_10_2_);
        } else {
            this.overlayPlayerList.updatePlayerList(false);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
    }
}
