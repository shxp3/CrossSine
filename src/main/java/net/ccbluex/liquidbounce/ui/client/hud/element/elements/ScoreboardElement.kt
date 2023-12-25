package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.other.StreamerMode
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard", blur = true)
class ScoreboardElement(
    x: Double = 5.0,
    y: Double = 0.0,
    scale: Float = 1F,
    side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)
) : Element(x, y, scale, side) {


    private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
    private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
    private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
    private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 95, 0, 255)

    private val rectValue = BoolValue("Rect", false)
    private val rectColorModeValue = ListValue("Color", arrayOf("Custom", "ClientTheme"), "Custom").displayable { rectValue.get() }

    private val rectColorRedValue = IntegerValue("Red", 0, 0, 255).displayable { rectColorModeValue.equals("Custom") && rectValue.get() }
    private val rectColorGreenValue = IntegerValue("Green", 111, 0, 255).displayable { rectColorModeValue.equals("Custom") && rectValue.get() }
    private val rectColorBlueValue = IntegerValue("Blue", 255, 0, 255).displayable { rectColorModeValue.equals("Custom") && rectValue.get() }
    private val rectColorBlueAlpha = IntegerValue("Alpha", 255, 0, 255).displayable { rectColorModeValue.equals("Custom") && rectValue.get() }

    private val shadowValue = BoolValue("TextShadow", false)
    private val changeDomain = BoolValue("ChangeDomain", false)
    private val showRedNumbersValue = BoolValue("ShowRedNumbers", false)
    private val fontValue = FontValue("Font", Fonts.minecraftFont)

    private val domainList = arrayOf(".ac",".academy",".accountant",".accountants",".actor",".adult",".ag",".agency",".ai",".airforce",".am",".amsterdam",".apartments",".app",".archi",".army",".art",".asia",".associates",".at",".attorney",".au",".auction",".auto",".autos",".baby",".band",".bar",".barcelona",".bargains",".bayern",".be",".beauty",".beer",".berlin",".best",".bet",".bid",".bike",".bingo",".bio",".biz",".biz.pl",".black",".blog",".blue",".boats",".boston",".boutique",".build",".builders",".business",".buzz",".bz",".ca",".cab",".cafe",".camera",".camp",".capital",".car",".cards",".care",".careers",".cars",".casa",".cash",".casino",".catering",".cc",".center",".ceo",".ch",".charity",".chat",".cheap",".church",".city",".cl",".claims",".cleaning",".clinic",".clothing",".cloud",".club",".cn",".co",".co.in",".co.jp",".co.kr",".co.nz",".co.uk",".co.za",".coach",".codes",".coffee",".college",".com",".com.ag",".com.au",".com.br",".com.bz",".com.cn",".com.co",".com.es",".com.mx",".com.pe",".com.ph",".com.pl",".com.ru",".com.tw",".community",".company",".computer",".condos",".construction",".consulting",".contact",".contractors",".cooking",".cool",".country",".coupons",".courses",".credit",".creditcard",".cricket",".cruises",".cymru",".cz",".dance",".date",".dating",".de",".deals",".degree",".delivery",".democrat",".dental",".dentist",".design",".dev",".diamonds",".digital",".direct",".directory",".discount",".dk",".doctor",".dog",".domains",".download",".earth",".education",".email",".energy",".engineer",".engineering",".enterprises",".equipment",".es",".estate",".eu",".events",".exchange",".expert",".exposed",".express",".fail",".faith",".family",".fan",".fans",".farm",".fashion",".film",".finance",".financial",".firm.in",".fish",".fishing",".fit",".fitness",".flights",".florist",".fm",".football",".forsale",".foundation",".fr",".fun",".fund",".furniture",".futbol",".fyi",".gallery",".games",".garden",".gay",".gen.in",".gg",".gifts",".gives",".glass",".global",".gmbh",".gold",".golf",".graphics",".gratis",".green",".gripe",".group",".gs",".guide",".guru",".hair",".haus",".health",".healthcare",".hockey",".holdings",".holiday",".homes",".horse",".hospital",".host",".house",".idv.tw",".immo",".immobilien",".in",".inc",".ind.in",".industries",".info",".info.pl",".ink",".institute",".insure",".international",".investments",".io",".irish",".ist",".istanbul",".it",".jetzt",".jewelry",".jobs",".jp",".kaufen",".kim",".kitchen",".kiwi",".kr",".la",".land",".law",".lawyer",".lease",".legal",".lgbt",".life",".lighting",".limited",".limo",".live",".llc",".loan",".loans",".london",".love",".ltd",".ltda",".luxury",".maison",".makeup",".management",".market",".marketing",".mba",".me",".me.uk",".media",".melbourne",".memorial",".men",".menu",".miami",".mobi",".moda",".moe",".money",".monster",".mortgage",".motorcycles",".movie",".ms",".mx",".nagoya",".name",".navy",".ne",".ne.kr",".net",".net.ag",".net.au",".net.br",".net.bz",".net.cn",".net.co",".net.in",".net.nz",".net.pe",".net.ph",".net.pl",".net.ru",".network",".news",".ninja",".nl",".no",".nom.co",".nom.es",".nom.pe",".nrw",".nyc",".okinawa",".one",".onl",".online",".org",".org.ag",".org.au",".org.cn",".org.es",".org.in",".org.nz",".org.pe",".org.ph",".org.pl",".org.ru",".org.uk",".page",".paris",".partners",".parts",".party",".pe",".pet",".ph",".photography",".photos",".pictures",".pink",".pizza",".pl",".place",".plumbing",".plus",".poker",".porn",".press",".pro",".productions",".promo",".properties",".protection",".pub",".pw",".quebec",".quest",".racing",".re.kr",".realestate",".recipes",".red",".rehab",".reise",".reisen",".rent",".rentals",".repair",".report",".republican",".rest",".restaurant",".review",".reviews",".rich",".rip",".rocks",".rodeo",".ru",".run",".ryukyu",".sale",".salon",".sarl",".school",".schule",".science",".se",".security",".services",".sex",".sg",".sh",".shiksha",".shoes",".shop",".shopping",".show",".singles",".site",".ski",".skin",".soccer",".social",".software",".solar",".solutions",".space",".storage",".store",".stream",".studio",".study",".style",".supplies",".supply",".support",".surf",".surgery",".sydney",".systems",".tax",".taxi",".team",".tech",".technology",".tel",".tennis",".theater",".theatre",".tienda",".tips",".tires",".today",".tokyo",".tools",".tours",".town",".toys",".top",".trade",".training",".travel",".tube",".tv",".tw",".uk",".university",".uno",".us",".vacations",".vegas",".ventures",".vet",".viajes",".video",".villas",".vin",".vip",".vision",".vodka",".vote",".voto",".voyage",".wales",".watch",".webcam",".website",".wedding",".wiki",".win",".wine",".work",".works",".world",".ws",".wtf",".xxx",".xyz",".yachts",".yoga",".yokohama",".zone", ".vn")

    private val cachedDomains = arrayListOf<String>()

    private val garbageTimer = MSTimer()

    private val hypickleRegex = Regex("[0-9][0-9]/[0-9][0-9]/[0-9][0-9]")
    override fun updateElement() {
        if (garbageTimer.hasTimePassed(30000L)) {
            cachedDomains.clear() // prevent memory leak
            garbageTimer.reset()
        }
    }

    /**
     * Draw element
     */
    override fun drawElement(partialTicks: Float): Border? {

        val fontRenderer = fontValue.get()
        val backColor = backgroundColor().rgb

        val rectColorMode = rectColorModeValue.get()
        val rectCustomColor = Color(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(),
            rectColorBlueAlpha.get()).rgb

        val worldScoreboard: Scoreboard = mc.theWorld.scoreboard
        var currObjective: ScoreObjective? = null
        val playerTeam = worldScoreboard.getPlayersTeam(mc.thePlayer.name)

        if (playerTeam != null) {
            val colorIndex = playerTeam.chatFormat.colorIndex

            if (colorIndex >= 0)
                currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
        }

        val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

        val scoreboard: Scoreboard = objective.scoreboard
        var scoreCollection = scoreboard.getSortedScores(objective)
        val scores = Lists.newArrayList(Iterables.filter(scoreCollection) { input ->
            input?.playerName != null && !input.playerName.startsWith("#")
        })

        scoreCollection = if (scores.size > 15)
            Lists.newArrayList(Iterables.skip(scores, scoreCollection.size - 15))
        else
            scores

        var maxWidth = fontRenderer.getStringWidth(objective.displayName)

        val hud = CrossSine.moduleManager.getModule(HUD::class.java) as HUD

        for (score in scoreCollection) {
            val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
            var name = ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)
            var stripped = StringUtils.fixString(ColorUtils.stripColor(name)!!)
            if(changeDomain.get()){
                if (cachedDomains.contains(stripped)) {
                    name = CrossSine.CUSTOM_DOMAIN
                } else if (ServerUtils.isHypixelDomain(stripped)) {
                    name = CrossSine.CUSTOM_DOMAIN
                    cachedDomains.add(stripped)
                } else
                    for (domain in domainList) {
                        if(stripped.contains(domain,true)){
                            name = CrossSine.CUSTOM_DOMAIN
                            cachedDomains.add(stripped)
                            break;
                        }
                    }
            }
            val width = "$name: ${EnumChatFormatting.RED}${score.scorePoints}"
            maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
        }

        val maxHeight = scoreCollection.size * fontRenderer.FONT_HEIGHT
        val l1 = if (side.horizontal == Side.Horizontal.LEFT) {maxWidth + 3} else {-maxWidth - 3}

        if (side.horizontal == Side.Horizontal.LEFT) {
            Gui.drawRect(l1 + 2, -2, -5, maxHeight + fontRenderer.FONT_HEIGHT, backColor)
            GlStateManager.resetColor()
        }
        else {
            Gui.drawRect(l1 - 2, -2, 5, maxHeight + fontRenderer.FONT_HEIGHT, backColor)
            GlStateManager.resetColor()
        }
        if (rectValue.get() && scoreCollection.size > 0) {


            if (side.horizontal == Side.Horizontal.LEFT)
                RenderUtils.drawGradientRect(l1 + 2, -2, -5, -3, ClientTheme.getColor(0).rgb, ClientTheme.getColor(90).rgb)
            else
                RenderUtils.drawGradientRect(l1 - 2, -2, 5, -3,ClientTheme.getColor(90).rgb, ClientTheme.getColor(0).rgb)
        }

        scoreCollection.forEachIndexed { index, score ->
            val team = scoreboard.getPlayersTeam(score.playerName)

            var name = ScorePlayerTeam.formatPlayerName(team, score.playerName)
            val scorePoints = "${EnumChatFormatting.RED}${score.scorePoints}"

            val width = 5
            val height = maxHeight - index * fontRenderer.FONT_HEIGHT

            var changed = false

            val stripped = StringUtils.fixString(ColorUtils.stripColor(name))

            if (StreamerMode.state && hypickleRegex.containsMatchIn(stripped))
                name = "§7${SimpleDateFormat("MM/dd/yy ").format(System.currentTimeMillis())}" + " §8${RandomUtils.randomString(4)}"

            GlStateManager.resetColor()
            if(changeDomain.get()) {
                if (cachedDomains.contains(stripped)) {
                    name = CrossSine.CUSTOM_DOMAIN
                    changed = true
                } else if (ServerUtils.isHypixelDomain(stripped)) {
                    name = CrossSine.CUSTOM_DOMAIN
                    changed = true
                    cachedDomains.add(stripped)
                } else
                    for (domain in domainList) {
                        if (stripped.contains(domain, true)) {
                            name = CrossSine.CUSTOM_DOMAIN
                            changed = true
                            cachedDomains.add(stripped)
                            break;
                        }
                    }
            }

            if (changed) {
                var stringZ = ""
                for (z in 0..(name.length-1)) {
                    val typeColor = ClientTheme.getColor(z * 135).rgb

                    if (side.horizontal == Side.Horizontal.LEFT)
                        fontRenderer.drawString(name.get(z).toString(), -3F + fontRenderer.getStringWidth(stringZ).toFloat(), height.toFloat(), typeColor, shadowValue.get())
                    else
                        fontRenderer.drawString(name.get(z).toString(), l1.toFloat() + fontRenderer.getStringWidth(stringZ), height.toFloat(), typeColor, shadowValue.get())

                    stringZ += name.get(z).toString()
                }
            } else if (side.horizontal == Side.Horizontal.LEFT)
                fontRenderer.drawString(name, -3F, height.toFloat(), -1, shadowValue.get())
            else
                fontRenderer.drawString(name, l1.toFloat(), height.toFloat(), -1, shadowValue.get())

            if (showRedNumbersValue.get())
                if (side.horizontal == Side.Horizontal.LEFT)
                    fontRenderer.drawString(scorePoints, (l1 + 1 - fontRenderer.getStringWidth(scorePoints)).toFloat(), height.toFloat(), -1, shadowValue.get())
                else
                    fontRenderer.drawString(scorePoints, (width - fontRenderer.getStringWidth(scorePoints)).toFloat(), height.toFloat(), -1, shadowValue.get())

            if (index == scoreCollection.size - 1) {
                val displayName = objective.displayName

                GlStateManager.resetColor()

                fontRenderer.drawString(displayName,
                    if (side.horizontal == Side.Horizontal.LEFT) (maxWidth / 2 - fontRenderer.getStringWidth(displayName) / 2).toFloat() else (l1 + maxWidth / 2 - fontRenderer.getStringWidth(displayName) / 2).toFloat(),
                    (height - fontRenderer.FONT_HEIGHT).toFloat(),
                    -1, shadowValue.get())
            }

        }

        return if (side.horizontal == Side.Horizontal.LEFT) Border(maxWidth.toFloat() + 5, -2F, -5F, maxHeight.toFloat() + fontRenderer.FONT_HEIGHT) else Border(-maxWidth.toFloat() - 5, -2F, 5F, maxHeight.toFloat() + fontRenderer.FONT_HEIGHT)
    }

    private fun backgroundColor() = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(),
        backgroundColorBlueValue.get(), backgroundColorAlphaValue.get())
}