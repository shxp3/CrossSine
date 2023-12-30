
package net.ccbluex.liquidbounce.event;

@FunctionalInterface
public interface Listener<Event> {
    void call(Event event);
}