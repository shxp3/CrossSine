package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.KeyEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Listens for Forge mouse button events and forwards them into CrossSine's
 * event system as [KeyEvent]s using the mouse-button encoding from [KeybindHelper].
 *
 * Left click (button 0) is excluded per user requirement.
 * Events are only forwarded when no GUI screen is open (same rule as keyboard binds).
 */
object MouseKeybindHandler {

    @SubscribeEvent
    fun onMouseEvent(event: MouseEvent) {
        if (!event.buttonstate) return                               // only on press
        if (event.button <= 0) return                               // skip left click
        if (Minecraft.getMinecraft().currentScreen != null) return  // skip in GUIs

        CrossSine.eventManager.callEvent(KeyEvent(KeybindHelper.codeFromMouseButton(event.button)))
    }
}
