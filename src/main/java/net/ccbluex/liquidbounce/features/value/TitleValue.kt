package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * Text value represents a value with a string
 */
open class TitleValue(name: String) : Value<String>(name, "") {
    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {}
}