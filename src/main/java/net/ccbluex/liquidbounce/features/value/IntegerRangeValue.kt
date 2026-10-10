package net.ccbluex.liquidbounce.features.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlin.random.Random

class IntegerRangeValue(
    name: String,
    minValue: Int,
    maxValue: Int,
    val minimum: Int = 0,
    val maximum: Int = Int.MAX_VALUE,
    val suffix: String = "",
    displayable: () -> Boolean = { true }
) : Value<IntRange>(name, minValue.coerceIn(minimum, maximum)..maxValue.coerceIn(minimum, maximum)) {

    private var minValue: Int = minValue.coerceIn(minimum, maximum)
    private var maxValue: Int = maxValue.coerceIn(minimum, maximum)

    private val defaultMin: Int = this.minValue
    private val defaultMax: Int = this.maxValue

    /** Cached range — `min..max` allocates a new IntRange every get() otherwise. */
    private var cachedRange: IntRange = this.minValue..this.maxValue

    init {
        if (this.minValue > this.maxValue) {
            this.maxValue = this.minValue
            cachedRange = this.minValue..this.maxValue
        }
    }

    override fun get(): IntRange = cachedRange

    fun getMin(): Int = minValue
    fun getMax(): Int = maxValue
    fun contains(value: Int): Boolean = value >= minValue && value <= maxValue
    fun contains(value: Float): Boolean = value >= minValue && value <= maxValue
    fun contains(value: Double): Boolean = value >= minValue && value <= maxValue

    fun getRandom(): Int = Random.nextInt(minValue, maxValue + 1)

    fun setMin(value: Int) {
        minValue = value.coerceIn(minimum, maximum)
        if (minValue > maxValue) maxValue = minValue
        cachedRange = minValue..maxValue
    }

    fun setMax(value: Int) {
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
            setMin(obj["min"]?.asInt ?: minValue)
            setMax(obj["max"]?.asInt ?: maxValue)
        }
    }
}