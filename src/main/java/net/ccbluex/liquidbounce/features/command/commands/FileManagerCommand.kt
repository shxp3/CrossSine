package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils

class FileManagerCommand : Command("FileManager", emptyArray()) {

    override fun execute(args: Array<String>) {
        if (args.isNotEmpty()) {
            when (args[1].lowercase()) {
                "save" -> {
                    CrossSine.fileManager.saveAllConfigs()
                    alert("Successfully saved config")
                }
                "load" -> {
                    CrossSine.fileManager.loadConfig(CrossSine.fileManager.accountsConfig)
                    CrossSine.fileManager.loadConfig(CrossSine.fileManager.friendsConfig)
                    CrossSine.fileManager.loadConfig(CrossSine.fileManager.xrayConfig)
                    CrossSine.fileManager.loadConfig(CrossSine.fileManager.themeConfig)
                    CrossSine.fileManager.loadConfig(CrossSine.fileManager.clienthudConfig)
                    alert("Successfully loaded config")
                }
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        return listOf("save",
            "load").filter { it.startsWith(args[0], true) }
    }
}