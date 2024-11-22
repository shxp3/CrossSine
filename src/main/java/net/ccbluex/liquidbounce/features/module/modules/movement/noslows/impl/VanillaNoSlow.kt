package net.ccbluex.liquidbounce.features.module.modules.movement.noslows.impl

import net.ccbluex.liquidbounce.features.module.modules.movement.noslows.NoSlowMode

class VanillaNoSlow : NoSlowMode("Vanilla") {
    override fun slow(): Float {
        return 1F
    }
}