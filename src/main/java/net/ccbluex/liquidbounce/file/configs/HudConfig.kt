package net.ccbluex.liquidbounce.file.configs

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.ui.client.hud.Config
import java.io.File

class HudConfig(file: File) : FileConfig(file) {
    override fun loadConfig(config: String) {
        CrossSine.hud.clearElements()
        CrossSine.hud = Config(config).toHUD()
    }

    override fun saveConfig(): String {
        return Config(CrossSine.hud).toJson()
    }
}