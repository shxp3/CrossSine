package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.awt.Color
import kotlin.jvm.internal.Intrinsics

abstract class Value<T>(val name: String, var value: T) {
    val default = value
    var textHovered: Boolean = false

    private var displayableFunc: () -> Boolean = { true }

    fun displayable(func: () -> Boolean): Value<T> {
        displayableFunc = func
        return this
    }

    val displayable: Boolean
        get() = displayableFunc()

    fun set(newValue: T) {
        if (newValue == value) return

        val oldValue = get()

        try {
            onChange(oldValue, newValue)
            changeValue(newValue)
            onChanged(oldValue, newValue)
            CrossSine.configManager.smartSave()
        } catch (e: Exception) {
            ClientUtils.logError("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
        }
    }


    open fun get() = value

    open fun setDefault() {
        value = default
    }

    open fun changeValue(value: T) {
        this.value = value
    }

    abstract fun toJson(): JsonElement?
    abstract fun fromJson(element: JsonElement)

    protected open fun onChange(oldValue: T, newValue: T) {}
    protected open fun onChanged(oldValue: T, newValue: T) {}


    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (value is String && other is String) {
            return (value as String).equals(other, true)
        }
        return value?.equals(other) ?: false
    }

    fun contains(text: String/*, ignoreCase: Boolean*/): Boolean {
        return if (value is String) {
            (value as String).contains(text, true)
        } else {
            false
        }
    }

    open fun ColorValue(name: String, value: Int) {
        Intrinsics.checkParameterIsNotNull(name, "name")
        ColorValue(name, value)
    }
}
