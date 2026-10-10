package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.awt.Color

open class ColorValue(name: String, value: Color, private val alpha: Boolean = false): Value<Color>(name, value) {

    fun getAlpha(): Boolean = alpha

    override fun toJson(): JsonElement {
        val color = value
        return JsonObject().apply {
            addProperty("r", color.red)
            addProperty("g", color.green)
            addProperty("b", color.blue)
            addProperty("a", color.alpha)
        }
    }

    override fun fromJson(element: JsonElement) {
        if (!element.isJsonObject) return

        val obj = element.asJsonObject

        val r = obj.get("r")?.asInt ?: 255
        val g = obj.get("g")?.asInt ?: 255
        val b = obj.get("b")?.asInt ?: 255
        val a = obj.get("a")?.asInt ?: 255

        value = Color(r, g, b, a)
    }
}