package net.ccbluex.liquidbounce.features.module.modules.visual



import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ResourceLocation
import java.util.*

@ModuleInfo(name = "Cape", category = ModuleCategory.VISUAL)
class Cape : Module() {

    val styleValue = ListValue(
        "Style",
        arrayOf(
            "CrossSine",
            "CrossSine2",
            "Astolfo",
            "Crave",
            "MiaSakurajima",
            "Black",
            "White",
            "Moonzy",
            "Rise",
            "Novoline",
            "NoneClient",
            "SexyBanana",
            "NightX",
            "None"
        ),
        "None"
    )

    private val capeCache = hashMapOf<String, CapeStyle>()
    fun getCapeLocation(value: String): ResourceLocation {
        if (capeCache[value.uppercase(Locale.getDefault())] == null) {
            try {
                capeCache[value.uppercase(Locale.getDefault())] =
                    CapeStyle.valueOf(value.uppercase(Locale.getDefault()))
            } catch (e: Exception) {
                capeCache[value.uppercase(Locale.getDefault())] = CapeStyle.NONE
            }
        }
        return capeCache[value.uppercase(Locale.getDefault())]!!.location
    }

    enum class CapeStyle(val location: ResourceLocation) {
        CROSSSINE(ResourceLocation("crosssine/cape/crosssine.png")),
        CROSSSINE2(ResourceLocation("crosssine/cape/crosssine2.png")),
        CRAVE(ResourceLocation("crosssine/cape/crave.png")),
        MIASAKURAJIMA(ResourceLocation("crosssine/cape/miasakurajima.png")),
        ASTOLFO(ResourceLocation("crosssine/cape/astolfo.png")),
        BLACK(ResourceLocation("crosssine/cape/black.png")),
        WHITE(ResourceLocation("crosssine/cape/white.png")),
        MOONZY(ResourceLocation("crosssine/cape/moonzy.png")),
        RISE(ResourceLocation("crosssine/cape/risecape.png")),
        NOVOLINE(ResourceLocation("crosssine/cape/novoline.png")),
        NONECLIENT(ResourceLocation("crosssine/cape/noneclient.png")),
        SEXYBANANA(ResourceLocation("crosssine/cape/sexybanana.png")),
        NIGHTX(ResourceLocation("crosssine/cape/nightx.png")),
        NONE(ResourceLocation(""))
    }

    override val tag: String
        get() = styleValue.get()

    init {
        state = true
    }
}