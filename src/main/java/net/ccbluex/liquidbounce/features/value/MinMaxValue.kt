//package net.ccbluex.liquidbounce.features.value
//
//import com.google.gson.JsonElement
//import com.google.gson.JsonPrimitive
//
///**
// * Integer value represents a value with a integer
// */
////open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE) : Value<Int>(name, value) {
//
//open class MinMaxValue(name: String, value: Int, val min: Int = 0, val max: Int = 0, maximum: Int = Integer.MAX_VALUE, val suffix: String, displayable: () -> Boolean)
//    : Value<Int>(name, value) {
//
//    constructor(name: String, value: Int, min: Int, max: Int, maximum: Int, displayable: () -> Boolean): this(name, value, min, max, maximum,"", displayable)
//    constructor(name: String, value: Int, min: Int, max: Int, maximum: Int, suffix: String): this(name, value, min, max, maximum, suffix, { true } )
//    constructor(name: String, value: Int, min: Int, max: Int, maximum: Int): this(name, value, min, max, maximum, { true } )
//
//
//
//    fun set(newValue: Number) {
//        set(newValue.toInt())
//    }
//
//    override fun toJson() = JsonPrimitive(value)
//
//    override fun fromJson(element: JsonElement) {
//        if (element.isJsonPrimitive) {
//            value = element.asInt
//        }
//    }
//}