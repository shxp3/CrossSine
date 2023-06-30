/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.Command

class FakeNameCommand : Command("SetFakeName", emptyArray()){
    override fun execute(args: Array<String>) {
        if(args.size > 2) {
            val module = CrossSine.moduleManager.getModule(args[1]) ?: return
            module.name = args[2]
            module.spacedName = args[3]
        } else
            chatSyntax("SetFakeName <Module> <Name> <SpecName>")
    }
    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val moduleName = args[0]

        return when (args.size) {
            1 -> CrossSine.moduleManager.modules
                    .map { it.name }
                    .filter { it.startsWith(moduleName, true) }
                    .toList()
            else -> emptyList()
        }
    }
}