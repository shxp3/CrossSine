package net.ccbluex.liquidbounce.features.module.modules.movement.flights.default

import net.ccbluex.liquidbounce.features.module.modules.movement.flights.FlightMode

class CreativeFlight : FlightMode("Creative") {
    override fun onEnable() {
        mc.thePlayer.capabilities.isFlying = true
    }

    override fun onDisable() {
        mc.thePlayer.capabilities.isFlying = false
    }
}