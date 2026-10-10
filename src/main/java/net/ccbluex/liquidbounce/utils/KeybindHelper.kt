package net.ccbluex.liquidbounce.utils

import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Utilities for keybinds that support both keyboard keys and mouse buttons.
 *
 * Encoding convention (matches Minecraft's own KeyBinding):
 *   keyboard key  → positive LWJGL key code  (e.g. Keyboard.KEY_R = 19)
 *   mouse button  → button - 100             (button 0=left=-100, 1=right=-99, 2=middle=-98 …)
 *
 * Left click (button 0, code -100) is intentionally excluded from bind support.
 */
object KeybindHelper {

    fun isMouseBind(code: Int) = code < 0

    /** Converts an LWJGL mouse button index → stored key code. */
    fun codeFromMouseButton(button: Int): Int = button - 100

    /** Converts a stored key code → LWJGL mouse button index. */
    fun mouseButtonFromCode(code: Int): Int = code + 100

    /**
     * Returns a human-readable name for any key code, including mouse buttons.
     *   0    → "NONE"
     *   -99  → "Mouse2"  (right click)
     *   -98  → "Mouse3"  (middle click)
     *   -97  → "Mouse4"  (extra button)
     *   > 0  → Keyboard.getKeyName(code)
     */
    fun getDisplayName(code: Int): String = when {
        code == 0 -> "NONE"
        code < 0  -> "Mouse${code + 101}"
        else      -> Keyboard.getKeyName(code)
    }

    /**
     * Returns true while the bound key/button is physically held.
     * Safe to call from any thread.
     */
    fun isDown(code: Int): Boolean = when {
        code == 0 -> false
        code < 0  -> {
            val btn = code + 100
            if (btn < 0) false else try { Mouse.isButtonDown(btn) } catch (_: Throwable) { false }
        }
        else -> try { Keyboard.isKeyDown(code) } catch (_: Throwable) { false }
    }
}
