
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.visual.Crosshair;
import net.ccbluex.liquidbounce.features.module.modules.client.Interface;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.ccbluex.liquidbounce.features.module.modules.client.Scoreboard;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.FontUtils;
import net.ccbluex.liquidbounce.utils.SlotUtils;
import net.ccbluex.liquidbounce.utils.render.BlurUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.render.ShaderUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.FoodStats;
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

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@Mixin(GuiIngame.class)
public abstract class MixinGuiInGame extends MixinGui {
    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

    @Shadow
    protected int recordPlayingUpFor;

    // ── Hotbar item-animation state (see renderTooltip) ──────────────────
    // Static so it survives across the multiple mixin-instance lifetimes
    // a single MC session might create (none in practice, but the static
    // also documents that this is global UI state, not per-instance).
    private static int  hotbarLastSelectedSlot = -1;
    private static long hotbarSwitchTimeMs    = 0L;
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
    private static final String[] allowedDomains = {
            ".ac", ".academy", ".accountant", ".accountants", ".actor", ".adult", ".ag", ".agency", ".ai", ".airforce",
            ".am", ".amsterdam", ".apartments", ".app", ".archi", ".army", ".art", ".asia", ".associates", ".at",
            ".attorney", ".au", ".auction", ".auto", ".autos", ".baby", ".band", ".bar", ".barcelona", ".bargains",
            ".bayern", ".be", ".beauty", ".beer", ".berlin", ".best", ".bet", ".bid", ".bike", ".bingo", ".bio", ".biz",
            ".biz.pl", ".black", ".blog", ".blue", ".boats", ".boston", ".boutique", ".build", ".builders", ".business",
            ".buzz", ".bz", ".ca", ".cab", ".cafe", ".camera", ".camp", ".capital", ".car", ".cards", ".care", ".careers",
            ".cars", ".casa", ".cash", ".casino", ".catering", ".cc", ".center", ".ceo", ".ch", ".charity", ".chat",
            ".cheap", ".church", ".city", ".cl", ".claims", ".cleaning", ".clinic", ".clothing", ".cloud", ".club", ".logo",
            ".co", ".co.in", ".co.jp", ".co.kr", ".co.nz", ".co.uk", ".co.za", ".coach", ".codes", ".coffee", ".college",
            ".com", ".com.ag", ".com.au", ".com.br", ".com.bz", ".com.logo", ".com.co", ".com.es", ".com.mx", ".com.pe",
            ".com.ph", ".com.pl", ".com.ru", ".com.tw", ".community", ".company", ".computer", ".condos", ".construction",
            ".consulting", ".contact", ".contractors", ".cooking", ".cool", ".country", ".coupons", ".courses", ".credit",
            ".creditcard", ".cricket", ".cruises", ".cymru", ".cz", ".dance", ".date", ".dating", ".de", ".deals", ".degree",
            ".delivery", ".democrat", ".dental", ".dentist", ".design", ".dev", ".diamonds", ".digital", ".direct",
            ".directory", ".discount", ".dk", ".doctor", ".dog", ".domains", ".download", ".earth", ".education", ".email",
            ".energy", ".engineer", ".engineering", ".enterprises", ".equipment", ".es", ".estate", ".eu", ".events",
            ".exchange", ".expert", ".exposed", ".express", ".fail", ".faith", ".family", ".fan", ".fans", ".farm",
            ".fashion", ".film", ".finance", ".financial", ".firm.in", ".fish", ".fishing", ".fit", ".fitness", ".flights",
            ".florist", ".fm", ".football", ".forsale", ".foundation", ".fr", ".fun", ".fund", ".furniture", ".futbol",
            ".fyi", ".gallery", ".games", ".garden", ".gay", ".gen.in", ".gg", ".gifts", ".gives", ".glass", ".global",
            ".gmbh", ".gold", ".golf", ".graphics", ".gratis", ".green", ".gripe", ".group", ".gs", ".guide", ".guru",
            ".hair", ".haus", ".health", ".healthcare", ".hockey", ".holdings", ".holiday", ".homes", ".horse", ".hospital",
            ".host", ".house", ".idv.tw", ".immo", ".immobilien", ".in", ".inc", ".ind.in", ".industries", ".info",
            ".info.pl", ".ink", ".institute", ".insure", ".international", ".investments", ".io", ".irish", ".ist",
            ".istanbul", ".it", ".jetzt", ".jewelry", ".jobs", ".jp", ".kaufen", ".kim", ".kitchen", ".kiwi", ".kr", ".la",
            ".land", ".law", ".lawyer", ".lease", ".legal", ".lgbt", ".life", ".lighting", ".limited", ".limo", ".live",
            ".llc", ".loan", ".loans", ".london", ".love", ".ltd", ".ltda", ".luxury", ".maison", ".makeup", ".management",
            ".market", ".marketing", ".mba", ".me", ".me.uk", ".media", ".melbourne", ".memorial", ".men", ".menu", ".miami",
            ".mobi", ".moda", ".moe", ".money", ".monster", ".mortgage", ".motorcycles", ".movie", ".ms", ".mx", ".nagoya",
            ".name", ".navy", ".ne.kr", ".net", ".net.ag", ".net.au", ".net.br", ".net.bz", ".net.logo", ".net.co", ".net.in",
            ".net.nz", ".net.pe", ".net.ph", ".net.pl", ".net.ru", ".network", ".news", ".ninja", ".nl", ".no", ".nom.co",
            ".nom.es", ".nom.pe", ".nrw", ".nyc", ".okinawa", ".one", ".onl", ".online", ".org", ".org.ag", ".org.au",
            ".org.logo", ".org.es", ".org.in", ".org.nz", ".org.pe", ".org.ph", ".org.pl", ".org.ru", ".org.uk", ".page",
            ".paris", ".partners", ".parts", ".party", ".pe", ".pet", ".ph", ".photography", ".photos", ".pictures", ".pink",
            ".pizza", ".pl", ".place", ".plumbing", ".plus", ".poker", ".porn", ".press", ".pro", ".productions", ".promo",
            ".properties", ".protection", ".pub", ".pw", ".quebec", ".quest", ".racing", ".re.kr", ".realestate", ".recipes",
            ".red", ".rehab", ".reise", ".reisen", ".rent", ".rentals", ".repair", ".report", ".republican", ".rest",
            ".restaurant", ".review", ".reviews", ".rich", ".rip", ".rocks", ".rodeo", ".ru", ".run", ".ryukyu", ".sale",
            ".salon", ".sarl", ".school", ".schule", ".science", ".se", ".security", ".services", ".sex", ".sg", ".sh",
            ".shiksha", ".shoes", ".shop", ".shopping", ".show", ".singles", ".site", ".ski", ".skin", ".soccer", ".social",
            ".software", ".solar", ".solutions", ".space", ".storage", ".store", ".stream", ".studio", ".study", ".style",
            ".supplies", ".supply", ".support", ".surf", ".surgery", ".sydney", ".systems", ".tax", ".taxi", ".team", ".tech",
            ".technology", ".tel", ".tennis", ".theater", ".theatre", ".tienda", ".tips", ".tires", ".today", ".tokyo",
            ".tools", ".tours", ".town", ".toys", ".top", ".trade", ".training", ".travel", ".tube", ".tv", ".tw", ".uk",
            ".university", ".uno", ".us", ".vacations", ".vegas", ".ventures", ".vet", ".viajes", ".video", ".villas", ".vin",
            ".vip", ".vision", ".vodka", ".vote", ".voto", ".voyage", ".wales", ".watch", ".webcam", ".website", ".wedding",
            ".wiki", ".win", ".wine", ".work", ".works", ".world", ".ws", ".wtf", ".xxx", ".xyz", ".yachts", ".yoga",
            ".yokohama", ".zone"
    };

    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    private void injectCrosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Interface.INSTANCE.getState()) {
            if (mc.gameSettings.thirdPersonView != 0)
                cir.setReturnValue(false);
        }
        if (Crosshair.INSTANCE.getState()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void injectScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo callbackInfo) {
        if (Scoreboard.INSTANCE.getState() && Interface.INSTANCE.getState()) {
            net.minecraft.scoreboard.Scoreboard scoreboard = scoreObjective.getScoreboard();
            float posY = Scoreboard.INSTANCE.getPosY();
            float posX = Scoreboard.INSTANCE.getPosX();
            Color color = new Color(0, 0, 0, Scoreboard.INSTANCE.getAlpha());
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

            int i = Scoreboard.INSTANCE.getFontValue().get().getStringWidth(scoreObjective.getDisplayName());

            for (Score score : collection) {
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + (Scoreboard.INSTANCE.getShowNumber().get() ? (EnumChatFormatting.RED + "" + score.getScorePoints()) : "");
                i = Math.max(i, Scoreboard.INSTANCE.getFontValue().get().getStringWidth(s));
            }

            int i1 = collection.size() * Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT;
            float j1 = 2 + i1 + Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT;
            int k1 = 3;
            float screenWidth = scaledResolution.getScaledWidth();
            float screenHeight = scaledResolution.getScaledHeight();
            boolean rightSide = posX > screenWidth / 2F;

            int width = i + k1 + 4;

            // Clamp posY ไม่ให้ scoreboard หลุดออกนอกจอแนวตั้ง
            posY = Math.max(Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT + 4, posY);
            posY = Math.min(screenHeight - j1 - 4, posY);

            int l1;
            if (rightSide) {
                l1 = (int) (posX - width);
                l1 = Math.max(2, l1); // ไม่ให้ติดขอบซ้าย
            } else {
                l1 = (int) posX;
                l1 = Math.min((int)(screenWidth - width - 2), l1); // ไม่ให้เกินขอบขวา
            }

            int j = 0;
            if (Interface.isBlurActive()) {
                BlurUtils.blurAreaRounded(l1 - 4 - 2,
                        (int) (j1 - i1 + posY - Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT) - 2 - 2,
                        l1 - 2 + width + 4 + 2,
                        (int) (j1 + posY) + 4 + 2,
                        7F,
                        10F);
            }
            RenderUtils.drawBloomRoundedRect(
                    l1 - 4 - 5,
                    (int) (j1 - i1 + posY - Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT) - 2 - 5,
                    l1 - 2 + width + 4 + 5,
                    (int) (j1 + posY) + 4 + 5,
                    7F,
                    2.5F,
                    color,
                    RenderUtils.ShaderBloom.BLOOMONLY
            );

            float boxLeft   = l1 - 4 - 2;
            float boxRight  = l1 - 2 + width + 4 + 2;
            float boxTop    = (j1 - i1 + posY - Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT) - 4;
            float boxBottom = (j1 + posY) + 6;
            // updateBounds expects: x1=boxLeft, y1=boxTop, then RELATIVE offset from posX/posY
            // because HUDModule stores x2 = posX + width2, y2 = posY + height2
            Scoreboard.INSTANCE.updateBounds(boxLeft, boxTop, boxRight - posX, boxBottom - posY);

            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                String s2 = EnumChatFormatting.RED + "" + score1.getScorePoints();
                int k = (int) j1 - j * Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT;
                int l = (int) (scaledResolution.getScaledWidth() - k1 + 2 - 4 + posX);

                // Clamp scoreX ไม่ให้หลุดขอบ
                int scoreTextW = Scoreboard.INSTANCE.getFontValue().get().getStringWidth(s2);
                float scoreX = rightSide
                        ? posX - scoreTextW - 6
                        : l1 + width - scoreTextW - 2;
                scoreX = Math.max(l1, Math.min(scoreX, screenWidth - scoreTextW - 2));

                for (String domain : allowedDomains) {
                    if (s1.contains(domain)) {
                        s1 = "crosssine.github.io";
                        break;
                    }
                }
                if (s1.equals("crosssine.github.io")) {
                    FontUtils.INSTANCE.drawGradientString(Scoreboard.INSTANCE.getFontValue().get(), s1, l1, (int) (k + posY), ClientTheme.INSTANCE.getColor(0, false).getRGB(), ClientTheme.INSTANCE.getColor(180, false).getRGB(), 1F, Scoreboard.INSTANCE.getTextShadow().get());
                } else {
                    float textX = rightSide ? (posX - width) : l1;
                    Scoreboard.INSTANCE.getFontValue().get().drawString(s1, textX, k + posY, 553648127, Scoreboard.INSTANCE.getTextShadow().get());
                }
                if (Scoreboard.INSTANCE.getShowNumber().get()) {
                    Scoreboard.INSTANCE.getFontValue().get().drawString(s2, scoreX, k + posY, 553648127, Scoreboard.INSTANCE.getTextShadow().get());
                }

                if (j == collection.size()) {
                    String s3 = scoreObjective.getDisplayName();
                    Scoreboard.INSTANCE.getFontValue().get().drawString(s3, l1 + i / 2F - Scoreboard.INSTANCE.getFontValue().get().getStringWidth(s3) / 2F, k - Scoreboard.INSTANCE.getFontValue().get().FONT_HEIGHT + posY, 553648127, Scoreboard.INSTANCE.getTextShadow().get());
                }
            }
        }
        if (!CrossSine.INSTANCE.getDestruced()) {
            callbackInfo.cancel();
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    protected void renderTooltip(ScaledResolution sr, float partialTicks) {
        if (this.mc.getRenderViewEntity() instanceof EntityPlayer) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            EntityPlayer entityPlayer = (EntityPlayer) this.mc.getRenderViewEntity();
            int i = sr.getScaledWidth() / 2;
            // ── Hotbar background ────────────────────────────────────
            // 3-way switch: Vanilla widgets / single rounded chip / Modern
            // separated cards. animationSlot.value is in pixel-space (slot
            // index * 20, smoothed) and is shared across all three modes
            // so swapping styles never re-snaps the highlight position.
            //
            // HOTBAR_Y_LIFT lifts the whole bar (background + items) up
            // by N px from vanilla y. Applied uniformly so item icons stay
            // centred inside their slot card. Vanilla item y is
            // `sh - 16 - 3 = sh - 19`; with lift=3 it becomes `sh - 22`.
            String style = Interface.INSTANCE.getHotbarStyle().get();
            final int HOTBAR_Y_LIFT = 3;
            int barLeft   = i - 91;
            int barTop    = sr.getScaledHeight() - 22 - HOTBAR_Y_LIFT;
            int barBottom = sr.getScaledHeight() - 2  - HOTBAR_Y_LIFT;
            float animX = (float) Interface.INSTANCE.getAnimationSlot().value;

            if (style.equals("Modern")) {
                GlStateManager.pushMatrix();
                // Optional blur behind the whole bar — no per-card blur to
                // keep the cost flat and avoid seam artefacts between
                // adjacent cards.
                if (Interface.isBlurActive()) {
                    BlurUtils.blurAreaRounded(barLeft - 2F, barTop - 2F,
                            barLeft + 184F, barBottom + 2F, 4.5F, 10F);
                }
                // 9 rounded cards. Each card is 18 wide, 2 px gap between,
                // matches vanilla's 20-px slot pitch so renderHotbarItem
                // positions below stay byte-identical.
                Color cardBg = new Color(15, 15, 18, 180);
                for (int j = 0; j < 9; j++) {
                    float cx = barLeft + j * 20 + 1;
                    RenderUtils.drawRoundedRect(cx, barTop, cx + 18F, barBottom, 3F, cardBg.getRGB());
                }
                // Animated selection highlight — uses ClientTheme accent.
                Color accent = ClientTheme.INSTANCE.getColor(0, true);
                Color accentSoft = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70);
                float selX = barLeft + animX + 1F;
                // Soft fill behind the selected slot
                RenderUtils.drawRoundedRect(selX, barTop, selX + 18F, barBottom, 3F, accentSoft.getRGB());
                // Outer bloom glow
                RenderUtils.drawBloomRoundedRect(selX, barTop, selX + 18F, barBottom,
                        3F, 1.4F, accent, RenderUtils.ShaderBloom.BLOOMONLY);
                // Bottom indicator strip — small accent bar peeking out of
                // the bottom of the card
                RenderUtils.drawRoundedRect(selX + 4F, barBottom - 0.5F,
                        selX + 14F, barBottom + 1.2F, 0.5F, accent.getRGB());
                GlStateManager.popMatrix();
            } else if (style.equals("Rounded")) {
                GlStateManager.pushMatrix();
                float rndTop = sr.getScaledHeight() - 22 - HOTBAR_Y_LIFT;
                float rndBot = sr.getScaledHeight()       - HOTBAR_Y_LIFT;
                float highlightX = barLeft + (float) Interface.INSTANCE.getAnimationSlot().value;
                if (Interface.isBlurActive()) {
                    BlurUtils.blurAreaRounded((i - 91) - 2.5F, rndTop - 2.5F, (i + 91) + 2.5F, rndBot + 2.5F, 4.5F, 10F);
                }
                RenderUtils.drawBloomRoundedRect((i - 91) - 2.5F, rndTop - 2.5F, (i + 91) + 2.5F, rndBot + 2.5F, 4.5f, 4.5F, new Color(0, 0, 0, 80), RenderUtils.ShaderBloom.BOTH);
                RenderUtils.drawRoundedRect(highlightX + 2F, rndBot - 3F, highlightX + 22F - 2F, rndTop + 2F, 3f, new Color(0, 0, 0, 100).getRGB());
                GlStateManager.popMatrix();
            } else {
                this.mc.getTextureManager().bindTexture(widgetsTexPath);
                float f = this.zLevel;
                this.zLevel = -90.0F;
                this.drawTexturedModalRect(i - 91, sr.getScaledHeight() - 22 - HOTBAR_Y_LIFT, 0, 0, 182, 22);
                this.drawTexturedModalRect((int) (i - 91 - 1 + Interface.INSTANCE.getAnimationSlot().value), sr.getScaledHeight() - 22 - 1 - HOTBAR_Y_LIFT, 0, 22, 24, 22);
                this.zLevel = f;
            }
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();
            // ── Item icons + per-slot animation ────────────────────────
            // Two animation layers stacked on the selected slot:
            //  1) Pop-on-switch: when the held slot changes, the new slot
            //     scales from 0.75× → 1× over 220 ms with easeOutBack
            //     overshoot. Sells the "I just grabbed this item" feel.
            //  2) Idle bob: while held, the icon bobs ±0.6 px vertically
            //     and pulses its scale by ±2 % on a 1.6 s sin cycle. Very
            //     subtle — meant to be felt, not noticed.
            //
            // The two factors multiply, so during a switch the bob is
            // dampened to 0 and ramps in as the pop-in completes.
            //
            // State (last-seen slot + switch time) is tracked in static
            // fields below; mixin instances aren't long-lived but the
            // class is, which is exactly the lifetime we want.
            int selectedSlot = entityPlayer.inventory.currentItem;
            boolean modern   = style.equals("Modern");
            long nowMs       = System.currentTimeMillis();
            if (selectedSlot != hotbarLastSelectedSlot) {
                hotbarLastSelectedSlot = selectedSlot;
                hotbarSwitchTimeMs    = nowMs;
            }
            float popT = Math.min(1F, (nowMs - hotbarSwitchTimeMs) / 220F);
            // easeOutBack: overshoots ~1.10× and settles back to 1.
            float c1 = 1.70158F;
            float c3 = c1 + 1F;
            float popEase = 1F + c3 * (float) Math.pow(popT - 1F, 3F) + c1 * (float) Math.pow(popT - 1F, 2F);
            // Idle bob, attenuated by popT so it only kicks in after the
            // switch-in animation is done settling.
            float phase = ((nowMs % 1600L) / 1600F) * (float) (Math.PI * 2.0);
            float bobY   = (float) Math.sin(phase) * 0.6F  * popT;
            float pulse  = 1F + (float) Math.sin(phase) * 0.02F * popT;
            float baseScale = modern ? 1.12F : 1.0F;
            float finalScale = baseScale * (0.75F + 0.25F * popEase) * pulse;
            for (int j = 0; j < 9; ++j) {
                int k = sr.getScaledWidth() / 2 - 90 + j * 20 + 2;
                int l = sr.getScaledHeight() - 16 - 3 - HOTBAR_Y_LIFT;
                boolean isSel = j == selectedSlot;
                boolean transform = isSel && (modern || Math.abs(finalScale - 1F) > 0.001F || Math.abs(bobY) > 0.001F);
                if (transform) {
                    GlStateManager.pushMatrix();
                    float cxItem = k + 8F;
                    float cyItem = l + 8F;
                    GlStateManager.translate(cxItem, cyItem + bobY, 0F);
                    GlStateManager.scale(finalScale, finalScale, 1F);
                    GlStateManager.translate(-cxItem, -cyItem, 0F);
                }
                this.renderHotbarItem(j, k, l, partialTicks, entityPlayer);
                if (transform) GlStateManager.popMatrix();
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

    /**
     * Hide vanilla hearts / armor / food when [Interface.statBars] is on.
     *
     * We tried `ci.cancel()` on the @Inject head, but in this environment
     * Mixin's cancel path doesn't reliably short-circuit the call (likely
     * a Forge event wrapper running before our handler). Instead we use an
     * off-screen `translate` trick — the same idea as rendering vanilla to
     * a throwaway FBO, but without paying for an actual framebuffer:
     *
     *   HEAD   → pushMatrix + translate(0, +99999, 0)   // vanilla draws far below the screen
     *   RETURN → popMatrix                              // restore state for everything after
     *
     * The vanilla draw still executes (so any side effects like
     * `playerHealth` field updates inside the method still happen), it
     * just lands at y ≈ +99999 where nobody can see it. Our replacement
     * bars are then rendered through `Interface.onRender2D → HotbarStatBars`
     * as the single source of truth for visible HUD.
     *
     * If the player is riding a living entity vanilla calls
     * `renderHealthMount` instead of this method, so the mount HP bar still
     * appears as expected (we don't override that path).
     */
    @Inject(method = "renderPlayerStats", at = @At("HEAD"))
    private void injectStatBarsHead(ScaledResolution sr, CallbackInfo ci) {
        if (Interface.INSTANCE.getState() && Interface.INSTANCE.getStatBars().get()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, 99999F, 0F);
        }
    }

    @Inject(method = "renderPlayerStats", at = @At("RETURN"))
    private void injectStatBarsReturn(ScaledResolution sr, CallbackInfo ci) {
        if (Interface.INSTANCE.getState() && Interface.INSTANCE.getStatBars().get()) {
            GlStateManager.popMatrix();
        }
    }

    @Overwrite
    public FontRenderer getFontRenderer() {
        return CrossSine.INSTANCE.getDestruced() || !Interface.INSTANCE.getState() ? mc.fontRendererObj : Fonts.SFBold40;
    }

    /**
     * @author
     * @reason
     */
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
            ItemStack lvt_1_1_ = SlotUtils.INSTANCE.getStack();
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

