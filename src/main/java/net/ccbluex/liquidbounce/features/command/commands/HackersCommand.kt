package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.other.FakeHacker
import java.awt.Desktop
import java.io.File
import java.nio.file.Files

class HackersCommand : Command("Hackers", emptyArray()) {
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when (args[1].lowercase()) {
                "list" -> {
                    for (name in FakeHacker.nameList) {
                        alert("> $name")
                    }
                }
                "add" -> {
                    FakeHacker.nameList.add(args[2])
                }
                "remove" -> {
                    if (FakeHacker.nameList.contains(args[2])) {
                        FakeHacker.nameList.remove(args[2])
                    } else {
                        alert("§CNot Found")
                    }
                }
            }
        } else {
            chatSyntax(arrayOf("list",
                "add",
                "remove"
            ))
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("list", "add", "remove").filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
