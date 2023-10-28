package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class ClientUsernameCommand : Command("ClientUsername", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            val str = StringUtils.toCompleteString(args, 1)
            CrossSine.USER_NAME = str
            return
        }
    }
}
