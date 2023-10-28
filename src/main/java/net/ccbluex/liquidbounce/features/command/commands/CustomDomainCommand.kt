package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class CustomDomainCommand : Command("customdomain", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val str = StringUtils.toCompleteString(args, 1)
            CrossSine.CUSTOM_DOMAIN = str
            return
        }
    }
}
