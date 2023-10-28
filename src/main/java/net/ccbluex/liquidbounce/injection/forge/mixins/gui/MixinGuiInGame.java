/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
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
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiIngame.class)
public abstract class MixinGuiInGame extends MixinGui {

    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

    @Shadow
    @Final
    protected static ResourceLocation widgetsTexPath;

    @Shadow
    @Final
    protected GuiPlayerTabOverlay overlayPlayerList;

    @Shadow
    @Final
    protected Minecraft mc;

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo callbackInfo) {
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
    private void renderPumpkinOverlay(final CallbackInfo callbackInfo) {
        final NoRender NoRender = CrossSine.moduleManager.getModule(NoRender.class);

        if(NoRender.getState() && NoRender.getPumpkinEffect().get())
            callbackInfo.cancel();
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void renderBossHealth(CallbackInfo callbackInfo) {
        final NoRender NoRender = (NoRender) CrossSine.moduleManager.getModule(NoRender.class);
        if (NoRender.getState() && NoRender.getBossHealth().get())
            callbackInfo.cancel();
    }

    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    private void injectCrosshair(CallbackInfoReturnable<Boolean> cir) {
        final Crosshair crossHair = CrossSine.moduleManager.getModule(Crosshair.class);
        if (crossHair.getState())
            cir.setReturnValue(false);
    }
}
