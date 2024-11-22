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
    var useGlyphFontRenderer = true

    override fun loadConfig(config: String) {
        val json = JsonParser().parse(config).asJsonObject
        AutoReconnect.delay = 5000
        useGlyphFontRenderer = true

        if (json.has("prefix")) {
            CrossSine.commandManager.prefix = json.get("prefix").asCharacter
        }
        if (json.has("auto-reconnect")) {
            AutoReconnect.delay = json.get("auto-reconnect").asInt
        }
        if (json.has("domain")) {
            CrossSine.CUSTOM_DOMAIN = json.get("domain").asString
        }
        if (json.has("client-user")) {
            CrossSine.USER_NAME = json.get("client-user").asString
        }
        if (json.has("use-glyph-fontrenderer")) {
            useGlyphFontRenderer = json.get("use-glyph-fontrenderer").asBoolean
        }
    }

    override fun saveConfig(): String {
        val json = JsonObject()

        json.addProperty("auto-reconnect", AutoReconnect.delay)
        json.addProperty("client-user", CrossSine.USER_NAME)
        json.addProperty("use-glyph-fontrenderer", useGlyphFontRenderer)
        json.addProperty("domain", CrossSine.CUSTOM_DOMAIN)

        return FileManager.PRETTY_GSON.toJson(json)
    }
}