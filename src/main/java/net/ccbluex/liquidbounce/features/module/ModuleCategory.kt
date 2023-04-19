/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module

import lombok.Getter
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.normal.Main
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.objects.Drag
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.utils.render.Scroll

enum class ModuleCategory(val displayName: String, val configName: String, val htmlIcon: String) {
    COMBAT("Combat", "Combat", "&#xe000;"),
    PLAYER("Player", "Player", "&#xe7fd;"),
    MOVEMENT("Movement", "Movement", "&#xe566;"),
    VISUAL("Visual", "Visual", "&#xe417;"),
    WORLD("World", "World", "&#xe55b;"),
    GHOST("Ghost", "Ghost", "%#xe594"),
    OTHER("Other", "Other", "&#xe868;"),
    CLIENT("Client", "Client", "&#xe869;"),;

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
