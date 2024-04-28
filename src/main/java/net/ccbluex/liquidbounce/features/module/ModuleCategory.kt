package net.ccbluex.liquidbounce.features.module

import lombok.Getter
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.objects.Drag
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.render.Scroll

enum class ModuleCategory(val displayName: String, val configName: String) {
    COMBAT("Combat", "Combat"),
    PLAYER("Player", "Player"),
    MOVEMENT("Movement", "Movement"),
    VISUAL("Visual", "Visual"),
    WORLD("World", "World"),
    GHOST("Ghost", "Ghost"),
    OTHER("Other", "Other"),
    SCRIPT("Script", "Script");

    var namee: String? = null
    var posX = 0
    var expanded = false

    @Getter
    val scroll: Scroll = Scroll()

    @Getter
    var drag: Drag? = null
    var posY = 20

    open fun ModuleCategory(name: String?) {
        namee = name
        posX = 40 + Main.categoryCount * 120
        drag = Drag(posX.toFloat(), posY.toFloat())
        expanded = true
        Main.categoryCount++
    }

}
