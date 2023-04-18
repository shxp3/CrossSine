/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.MiscUtils

class Shxp3Command : Command("Shxp3", emptyArray()) {
    override fun execute(args: Array<String>) {
        MiscUtils.showURL("https://www.youtube.com/channel/UC3Pa1-71LcjfT9RHzMGkfng")
        MiscUtils.showURL("https://crosssine.github.io")
        MiscUtils.showURL("https://discord.gg/68qm3qMznG")
        MiscUtils.showURL("https://crosssine.github.io/CrossSine.html")
    }
}