package net.ccbluex.liquidbounce.event

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method

interface Listenable {
    fun handleEvents(): Boolean
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventTarget(val ignoreCondition: Boolean = false)

internal class EventHook(val eventClass: Listenable, method: Method, eventTarget: EventTarget) {
    val isIgnoreCondition = eventTarget.ignoreCondition

    /** Bound MethodHandle — avoids reflective Method.invoke on every dispatch. */
    private val handle: MethodHandle = MethodHandles.lookup().unreflect(method).bindTo(eventClass)

    fun call(event: Event) {
        handle.invoke(event)
    }
}
