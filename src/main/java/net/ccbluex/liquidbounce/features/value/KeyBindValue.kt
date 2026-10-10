package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.utils.KeybindHelper
import org.lwjgl.input.Keyboard

/**
 * A keybind value that can be configured per-module (independent of the
 * module's own enable/disable keybind). Stores an LWJGL key code; 0 = unbound.
 *
 * Modules query [isKeyDown] each frame/tick to decide what to do.
 */
open class KeyBindValue(name: String, value: Int = Keyboard.KEY_NONE) : Value<Int>(name, value) {

    /** Display string for the bound key/button, or "None" if unbound. */
    val keyName: String
        get() = if (value == Keyboard.KEY_NONE) "None" else KeybindHelper.getDisplayName(value)

    /** Returns true while the bound key/button is physically held. */
    fun isKeyDown(): Boolean = KeybindHelper.isDown(value)

    override fun toJson(): JsonElement = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (!element.isJsonPrimitive) return
        val p = element.asJsonPrimitive
        value = when {
            p.isNumber -> p.asInt
            p.isString -> {
                val s = p.asString
                if (s.isEmpty() || s.equals("None", ignoreCase = true)) Keyboard.KEY_NONE
                else try { Keyboard.getKeyIndex(s.uppercase()) } catch (_: Throwable) { Keyboard.KEY_NONE }
            }
            else -> Keyboard.KEY_NONE
        }
    }
}
