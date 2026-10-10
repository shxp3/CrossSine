package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.utils.MinecraftInstance

class EventManager : MinecraftInstance() {

    private val registry = HashMap<Class<out Event>, MutableList<EventHook>>()

    /**
     * Register [listener]
     */
    fun registerListener(listener: Listenable) {
        if (CrossSine.destruced) return
        for (method in listener.javaClass.declaredMethods) {
            if (method.isAnnotationPresent(EventTarget::class.java) && method.parameterTypes.size == 1) {
                try {
                    if (!method.isAccessible) {
                        method.isAccessible = true
                    }

                    val eventClass = method.parameterTypes[0] as Class<out Event>
                    val eventTarget = method.getAnnotation(EventTarget::class.java)

                    val invokableEventTargets = registry.getOrPut(eventClass) { mutableListOf() }
                    invokableEventTargets.add(EventHook(listener, method, eventTarget))
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    /**
     * Unregister listener
     *
     * @param listenable for unregister
     */
    fun unregisterListener(listenable: Listenable) {
        for ((_, targets) in registry) {
            targets.removeIf { it.eventClass == listenable }
        }
    }

    /**
     * True if at least one registered handler would currently process [eventClass].
     * Used to skip allocating hot events (e.g. BlockBBEvent) when nothing is listening.
     */
    fun hasActiveHandlers(eventClass: Class<out Event>): Boolean {
        val targets = registry[eventClass] ?: return false
        for (hook in targets) {
            if (hook.isIgnoreCondition || hook.eventClass.handleEvents()) {
                return true
            }
        }
        return false
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun callEvent(event: Event) {
        if (CrossSine.destruced) return
        val targets = registry[event.javaClass] ?: return
        for (invokableEventTarget in targets) {
            try {
                if (!invokableEventTarget.eventClass.handleEvents() && !invokableEventTarget.isIgnoreCondition) {
                    continue
                }
                invokableEventTarget.call(event)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }
}
