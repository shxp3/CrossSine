
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.modules.visual.Interface;
import net.ccbluex.liquidbounce.features.module.modules.visual.NoRender;
import net.ccbluex.liquidbounce.features.module.modules.visual.Crosshair;
import net.ccbluex.liquidbounce.features.module.modules.visual.ScoreboardModule;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.SpoofItemUtils;
import net.ccbluex.liquidbounce.utils.render.RoundedUtil;
import net.ccbluex.liquidbounce.utils.render.animations.ContinualAnimation;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
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
import java.util.*;
import java.util.List;

import static com.google.gson.internal.$Gson$Types.arrayOf;
import static net.minecraft.client.gui.Gui.drawRect;

@Mixin(GuiIngame.class)
public abstract class MixinGuiInGame extends MixinGui {
    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

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
        if (ScoreboardModule.INSTANCE.getState() && Interface.INSTANCE.getState()) {
            FontRenderer font = ScoreboardModule.INSTANCE.getFont();
            boolean shadow = ScoreboardModule.INSTANCE.getShadow();
            boolean show = ScoreboardModule.INSTANCE.getShowNumber();
            float posY = ScoreboardModule.INSTANCE.getPosY();
            String[] domainList = {".ac",".academy",".accountant",".accountants",".actor",".adult",".ag",".agency",".ai",".airforce",".am",".amsterdam",".apartments",".app",".archi",".army",".art",".asia",".associates",".at",".attorney",".au",".auction",".auto",".autos",".baby",".band",".bar",".barcelona",".bargains",".bayern",".be",".beauty",".beer",".berlin",".best",".bet",".bid",".bike",".bingo",".bio",".biz",".biz.pl",".black",".blog",".blue",".boats",".boston",".boutique",".build",".builders",".business",".buzz",".bz",".ca",".cab",".cafe",".camera",".camp",".capital",".car",".cards",".care",".careers",".cars",".casa",".cash",".casino",".catering",".cc",".center",".ceo",".ch",".charity",".chat",".cheap",".church",".city",".cl",".claims",".cleaning",".clinic",".clothing",".cloud",".club",".cn",".co",".co.in",".co.jp",".co.kr",".co.nz",".co.uk",".co.za",".coach",".codes",".coffee",".college",".com",".com.ag",".com.au",".com.br",".com.bz",".com.cn",".com.co",".com.es",".com.mx",".com.pe",".com.ph",".com.pl",".com.ru",".com.tw",".community",".company",".computer",".condos",".construction",".consulting",".contact",".contractors",".cooking",".cool",".country",".coupons",".courses",".credit",".creditcard",".cricket",".cruises",".cymru",".cz",".dance",".date",".dating",".de",".deals",".degree",".delivery",".democrat",".dental",".dentist",".design",".dev",".diamonds",".digital",".direct",".directory",".discount",".dk",".doctor",".dog",".domains",".download",".earth",".education",".email",".energy",".engineer",".engineering",".enterprises",".equipment",".es",".estate",".eu",".events",".exchange",".expert",".exposed",".express",".fail",".faith",".family",".fan",".fans",".farm",".fashion",".film",".finance",".financial",".firm.in",".fish",".fishing",".fit",".fitness",".flights",".florist",".fm",".football",".forsale",".foundation",".fr",".fun",".fund",".furniture",".futbol",".fyi",".gallery",".games",".garden",".gay",".gen.in",".gg",".gifts",".gives",".glass",".global",".gmbh",".gold",".golf",".graphics",".gratis",".green",".gripe",".group",".gs",".guide",".guru",".hair",".haus",".health",".healthcare",".hockey",".holdings",".holiday",".homes",".horse",".hospital",".host",".house",".idv.tw",".immo",".immobilien",".in",".inc",".ind.in",".industries",".info",".info.pl",".ink",".institute",".insure",".international",".investments",".io",".irish",".ist",".istanbul",".it",".jetzt",".jewelry",".jobs",".jp",".kaufen",".kim",".kitchen",".kiwi",".kr",".la",".land",".law",".lawyer",".lease",".legal",".lgbt",".life",".lighting",".limited",".limo",".live",".llc",".loan",".loans",".london",".love",".ltd",".ltda",".luxury",".maison",".makeup",".management",".market",".marketing",".mba",".me",".me.uk",".media",".melbourne",".memorial",".men",".menu",".miami",".mobi",".moda",".moe",".money",".monster",".mortgage",".motorcycles",".movie",".ms",".mx",".nagoya",".name",".navy",".ne",".ne.kr",".net",".net.ag",".net.au",".net.br",".net.bz",".net.cn",".net.co",".net.in",".net.nz",".net.pe",".net.ph",".net.pl",".net.ru",".network",".news",".ninja",".nl",".no",".nom.co",".nom.es",".nom.pe",".nrw",".nyc",".okinawa",".one",".onl",".online",".org",".org.ag",".org.au",".org.cn",".org.es",".org.in",".org.nz",".org.pe",".org.ph",".org.pl",".org.ru",".org.uk",".page",".paris",".partners",".parts",".party",".pe",".pet",".ph",".photography",".photos",".pictures",".pink",".pizza",".pl",".place",".plumbing",".plus",".poker",".porn",".press",".pro",".productions",".promo",".properties",".protection",".pub",".pw",".quebec",".quest",".racing",".re.kr",".realestate",".recipes",".red",".rehab",".reise",".reisen",".rent",".rentals",".repair",".report",".republican",".rest",".restaurant",".review",".reviews",".rich",".rip",".rocks",".rodeo",".ru",".run",".ryukyu",".sale",".salon",".sarl",".school",".schule",".science",".se",".security",".services",".sex",".sg",".sh",".shiksha",".shoes",".shop",".shopping",".show",".singles",".site",".ski",".skin",".soccer",".social",".software",".solar",".solutions",".space",".storage",".store",".stream",".studio",".study",".style",".supplies",".supply",".support",".surf",".surgery",".sydney",".systems",".tax",".taxi",".team",".tech",".technology",".tel",".tennis",".theater",".theatre",".tienda",".tips",".tires",".today",".tokyo",".tools",".tours",".town",".toys",".top",".trade",".training",".travel",".tube",".tv",".tw",".uk",".university",".uno",".us",".vacations",".vegas",".ventures",".vet",".viajes",".video",".villas",".vin",".vip",".vision",".vodka",".vote",".voto",".voyage",".wales",".watch",".webcam",".website",".wedding",".wiki",".win",".wine",".work",".works",".world",".ws",".wtf",".xxx",".xyz",".yachts",".yoga",".yokohama",".zone", ".vn"};

            Scoreboard scoreboard = scoreObjective.getScoreboard();
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

            int i = font.getStringWidth(scoreObjective.getDisplayName());

            for (Score score : collection) {
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + ChatFormatting.RED + score.getScorePoints();
                i = Math.max(i, font.getStringWidth(s));
            }

            int i1 = collection.size() * font.FONT_HEIGHT;
            int j1 = scaledResolution.getScaledHeight() / 2 + i1 / 3;
            int k1 = 3;
            int l1 = scaledResolution.getScaledWidth() - i - k1;
            int j = 0;
            RoundedUtil.drawRound(l1 - 2, posY + j1 - collection.size() * font.FONT_HEIGHT - font.FONT_HEIGHT, scaledResolution.getScaledWidth() - k1 + 2, (i1 + font.FONT_HEIGHT), 5F, new Color(0, 0, 0, ScoreboardModule.INSTANCE.getAlpha()));
            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                String s2 = ChatFormatting.RED + "" + score1.getScorePoints();
                int k = j1 - j * font.FONT_HEIGHT;
                int l = scaledResolution.getScaledWidth() - k1 + 2;
                for (String domain : domainList) {
                    if (s1.toLowerCase().contains(domain)) {
                        s1 = CrossSine.CLIENT_WEBSITE;
                        break;
                    }
                }
                font.drawString(s1, l1, posY + k, 553648127, shadow);
                if (show) {
                    font.drawString(s2, l - font.getStringWidth(s2),posY + k, 553648127, shadow);
                }
                if (j == collection.size()) {
                    String s3 = scoreObjective.getDisplayName();
                    for (String domain : domainList) {
                        if (s3.toLowerCase().contains(domain)) {
                            s3 = CrossSine.CLIENT_WEBSITE;
                            break;
                        }
                    }
                    font.drawString(s3, l1 + i / 2 - font.getStringWidth(s3) / 2, posY + k - font.FONT_HEIGHT, 553648127, shadow);
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
            this.drawTexturedModalRect(i - 91 - 1 + (Interface.INSTANCE.getSpoofItemSlot().get() ? mc.thePlayer.inventory.currentItem : SpoofItemUtils.INSTANCE.getSlot()) * 20, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
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
        SpoofItemUtils.INSTANCE.renderRect();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        SpoofItemUtils.INSTANCE.renderItem();
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
}
