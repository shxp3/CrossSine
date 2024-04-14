package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold

@ModuleInfo(name = "TowerHypixel", category = ModuleCategory.OTHER)
object TowerHypixel : Module() {
    override fun onEnable() {
        if (Scaffold.state && !Scaffold.towerModeValue.equals("Hypixel")) {
            state = false
        }
    }
}