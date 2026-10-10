package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlin.random.Random

class FloatRangeValue(
    name: String,
    minValue: Float,
    maxValue: Float,
    val minimum: Float = 0F,
    val maximum: Float = Float.MAX_VALUE,
    val suffix: String = "",
    displayable: () -> Boolean = { true }
) : Value<ClosedFloatingPointRange<Float>>(name, minValue.coerceIn(minimum, maximum)..maxValue.coerceIn(minimum, maximum)) {

    private var minValue: Float = minValue.coerceIn(minimum, maximum)
    private var maxValue: Float = maxValue.coerceIn(minimum, maximum)

    private val defaultMin: Float = this.minValue
    private val defaultMax: Float = this.maxValue

    /** Cached range — `min..max` allocates a new object every get() otherwise. */
    private var cachedRange: ClosedFloatingPointRange<Float> = this.minValue..this.maxValue

    init {
        if (this.minValue > this.maxValue) {
            this.maxValue = this.minValue
            cachedRange = this.minValue..this.maxValue
        }
    }

    override fun get(): ClosedFloatingPointRange<Float> = cachedRange

    fun getMin(): Float = minValue
    fun getMax(): Float = maxValue
    fun contains(value: Float): Boolean = value >= minValue && value <= maxValue

    fun getRandom(): Float = Random.nextFloat() * (maxValue - minValue) + minValue

    fun setMin(value: Float) {
        minValue = value.coerceIn(minimum, maximum)
        if (minValue > maxValue) maxValue = minValue
        cachedRange = minValue..maxValue
    }

    fun setMax(value: Float) {
        maxValue = value.coerceIn(minimum, maximum)
        if (maxValue < minValue) minValue = maxValue
        cachedRange = minValue..maxValue
    }
    override fun setDefault() {
        minValue = defaultMin
        maxValue = defaultMax
        cachedRange = minValue..maxValue
    }

    override fun toJson(): JsonElement {
        val obj = JsonObject()
        obj.addProperty("min", minValue)
        obj.addProperty("max", maxValue)
        return obj
    }

    override fun fromJson(element: JsonElement) {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            setMin(obj["min"]?.asFloat ?: minValue)
            setMax(obj["max"]?.asFloat ?: maxValue)
        }
    }
}