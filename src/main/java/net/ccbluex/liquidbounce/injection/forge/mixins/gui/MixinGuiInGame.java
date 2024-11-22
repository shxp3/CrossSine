
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.visual.Crosshair;
import net.ccbluex.liquidbounce.features.module.modules.visual.Interface;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.ccbluex.liquidbounce.features.module.modules.visual.ScoreboardModule;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.SpoofItemUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static net.minecraft.client.gui.Gui.drawRect;

@Mixin(GuiIngame.class)
public abstract class MixinGuiInGame extends MixinGui {
    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);
    @Shadow
    protected int recordPlayingUpFor;
    @Shadow
    protected int remainingHighlightTicks;
    @Shadow
    protected ItemStack highlightingItemStack;
    @Shadow
    protected int titlesTimer;
    @Shadow
    protected String displayedTitle = "";
    @Shadow
    protected String displayedSubTitle = "";
    @Shadow
    protected GuiStreamIndicator streamIndicator;

    @Shadow
    @Final
    protected static ResourceLocation widgetsTexPath;
    @Shadow
    @Final
    protected Minecraft mc;
    @Shadow
    protected final Random rand = new Random();
    @Shadow
    protected int updateCounter;
    @Shadow
    protected int playerHealth = 0;
    @Shadow
    protected int lastPlayerHealth = 0;
    @Shadow
    protected long lastSystemTime = 0L;
    @Shadow
    protected long healthUpdateCounter = 0L;
    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    private void injectCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Interface.INSTANCE.getState()) {
            if (mc.gameSettings.thirdPersonView != 0 && Interface.INSTANCE.getNoF5().get())
                cir.setReturnValue(false);
        }
        if (Crosshair.INSTANCE.getState()) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void injectScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo callbackInfo) {
        if (ScoreboardModule.INSTANCE.getState() && Interface.INSTANCE.getState()|| !Interface.INSTANCE.getState()) {
            Scoreboard scoreboard = scoreObjective.getScoreboard();
            float posY = ScoreboardModule.INSTANCE.getPosY();
            float round = ScoreboardModule.INSTANCE.getRounded();
            Color color = new Color(0,0,0, ScoreboardModule.INSTANCE.getAlpha());
            Collection<Score> collection = scoreboard.getSortedScores(scoreObjective);
            List<Score> list = Lists.newArrayList(Iterables.filter(collection, new Predicate<Score>() {
                public boolean apply(Score p_apply_1_) {
                    return p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#");
                }
            }));

            if (list.size() > 15) {
                collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
            } else {
                collection = list;
            }

            int i = mc.fontRendererObj.getStringWidth(scoreObjective.getDisplayName());

            for (Score score : collection) {
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + (ScoreboardModule.INSTANCE.getShowNumber() ? (EnumChatFormatting.RED + "" + score.getScorePoints()) : "");
                i = Math.max(i, mc.fontRendererObj.getStringWidth(s));
            }

            int i1 = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
            int j1 = scaledResolution.getScaledHeight() / 2 + i1 / 3;
            int k1 = 3;
            int l1 = scaledResolution.getScaledWidth() - i - k1;
            int j = 0;
            int width = i + k1 + 4;
            RenderUtils.drawRoundedRect(
                    l1 - 4,
                    (int) (j1 - i1 + posY - mc.fontRendererObj.FONT_HEIGHT) - 2,
                    l1 - 2 + width + 4,
                    (int) (j1 + posY) + 4,
                    round,
                    color.getRGB()
            );

            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                String s2 = EnumChatFormatting.RED + "" + score1.getScorePoints();
                int k = j1 - j * mc.fontRendererObj.FONT_HEIGHT;
                int l = scaledResolution.getScaledWidth() - k1 + 2;

                mc.fontRendererObj.drawString(s1, l1, k + posY, 553648127, ScoreboardModule.INSTANCE.getShadow());

                if (ScoreboardModule.INSTANCE.getShowNumber()) {
                    mc.fontRendererObj.drawString(s2, l - mc.fontRendererObj.getStringWidth(s2), k + posY, 553648127, ScoreboardModule.INSTANCE.getShadow());
                }

                if (j == collection.size()) {
                    String s3 = scoreObjective.getDisplayName();
                    mc.fontRendererObj.drawString(s3, l1 + i / 2F - mc.fontRendererObj.getStringWidth(s3) / 2F, k - mc.fontRendererObj.FONT_HEIGHT + posY, 553648127, ScoreboardModule.INSTANCE.getShadow());
                }
            }
        }
        callbackInfo.cancel();
    }
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
            this.drawTexturedModalRect(i - 91 - 1 + SpoofItemUtils.INSTANCE.getSlot() * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
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
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void injectBossHealth(CallbackInfo callbackInfo) {
        final NoRender NoRender = (NoRender) CrossSine.moduleManager.getModule(NoRender.class);
        if (NoRender.getState() && NoRender.getBossHealth().get())
            callbackInfo.cancel();
    }
    @Overwrite
    public FontRenderer getFontRenderer() {
        return CrossSine.INSTANCE.getDestruced() ? mc.fontRendererObj : Fonts.font40;}
    @Overwrite
    public void updateTick() {
        if (this.recordPlayingUpFor > 0) {
            --this.recordPlayingUpFor;
        }

        if (this.titlesTimer > 0) {
            --this.titlesTimer;
            if (this.titlesTimer <= 0) {
                this.displayedTitle = "";
                this.displayedSubTitle = "";
            }
        }

        ++this.updateCounter;
        this.streamIndicator.updateStreamAlpha();
        if (this.mc.thePlayer != null) {
            ItemStack lvt_1_1_ = SpoofItemUtils.INSTANCE.getStack();
            if (lvt_1_1_ == null) {
                this.remainingHighlightTicks = 0;
            } else if (this.highlightingItemStack == null || lvt_1_1_.getItem() != this.highlightingItemStack.getItem() || !ItemStack.areItemStackTagsEqual(lvt_1_1_, this.highlightingItemStack) || !lvt_1_1_.isItemStackDamageable() && lvt_1_1_.getMetadata() != this.highlightingItemStack.getMetadata()) {
                this.remainingHighlightTicks = 40;
            } else if (this.remainingHighlightTicks > 0) {
                --this.remainingHighlightTicks;
            }

            this.highlightingItemStack = lvt_1_1_;
        }

    }
}
