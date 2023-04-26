package net.ccbluex.liquidbounce.features.macro

import net.ccbluex.liquidbounce.CrossSine

class Macro(val key: Int, val command: String) {
    fun exec() {
        CrossSine.commandManager.executeCommands(command)
    }
}