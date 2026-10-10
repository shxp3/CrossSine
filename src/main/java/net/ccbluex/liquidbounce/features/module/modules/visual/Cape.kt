package net.ccbluex.liquidbounce.features.module.modules.visual



import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.util.ResourceLocation
import java.util.*

@ModuleInfo(name = "Cape", category = ModuleCategory.VISUAL)
class Cape : Module() {

    val styleValue = ListValue(
        "Style",
        arrayOf(
            "CrossSine",
            "Astolfo",
            "Black",
            "Rise",
            "Novoline",
            "Styles",
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
                capeCache[value.uppercase(Locale.getDefault())] = CapeStyle.CROSSSINE
            }
        }
        return capeCache[value.uppercase(Locale.getDefault())]!!.location
    }

    enum class CapeStyle(val location: ResourceLocation) {
        CROSSSINE(ResourceLocation("crosssine/cape/crosssine.png")),
        ASTOLFO(ResourceLocation("crosssine/cape/astolfo.png")),
        BLACK(ResourceLocation("crosssine/cape/black.png")),
        RISE(ResourceLocation("crosssine/cape/risecape.png")),
        NOVOLINE(ResourceLocation("crosssine/cape/novoline.png")),
        STYLES(ResourceLocation("crosssine/cape/styles.png")),
    }

    override val tag: String
        get() = styleValue.get()

    init {
        state = true
    }
}