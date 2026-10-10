package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.special.AutoReconnect
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import java.io.File

class SpecialConfig(file: File) : FileConfig(file) {

    override fun loadConfig(config: String) {
        val json = JsonParser().parse(config).asJsonObject
        AutoReconnect.delay = 5000

        if (json.has("prefix")) {
            CrossSine.commandManager.prefix = json.get("prefix").asCharacter
        }
        if (json.has("auto-reconnect")) {
            AutoReconnect.delay = json.get("auto-reconnect").asInt
        }
        if (json.has("stylised")) {
            GuiAltManager.stylisedAlts = json.get("stylised").asBoolean
        }
        if (json.has("unformattedAlts")) {
            GuiAltManager.unformattedAlts = json.get("unformattedAlts").asBoolean
        }
        if (json.has("unformattedAlts")) {
            GuiAltManager.altsLength = json.get("altsLength").asInt
        }
    }

    override fun saveConfig(): String {
        val json = JsonObject()
        json.addProperty("auto-reconnect", AutoReconnect.delay)
        json.addProperty("prefix", CrossSine.commandManager.prefix)
        json.addProperty("stylised", GuiAltManager.stylisedAlts)
        json.addProperty("unformattedAlts", GuiAltManager.unformattedAlts)
        json.addProperty("altsLength", GuiAltManager.altsLength)

        return FileManager.PRETTY_GSON.toJson(json)
    }
}