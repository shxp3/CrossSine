package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import java.io.File

class ThemeConfig(file: File) : FileConfig(file) {

    override fun loadConfig(config: String) {
        val json = JsonParser().parse(config).asJsonObject
        if (json.has("Theme")) {
            ClientTheme.ClientColorMode.set(json.get("Theme").asString)
        }
        if (json.has("Fade-Speed")) {
            ClientTheme.fadespeed.set(json.get("Fade-Speed").asInt)
        }
        if (json.has("Index")) {
            ClientTheme.index.set(json.get("Index").asInt)
        }
        if (json.has("GCD")) {
            ClientTheme.gcdFix.set(json.get("GCD").asBoolean)
        }
        if (json.has("SmoothRotation")) {
            ClientTheme.smoothRotation.set(json.get("SmoothRotation").asBoolean)
        }
        if (json.has("SmoothRotationFactor")) {
            ClientTheme.smoothFactor.set(json.get("SmoothRotationFactor").asFloat)
        }
        if (json.has("SmoothRotationSS")) {
            ClientTheme.smoothRotationSS.set(json.get("SmoothRotationSS").asBoolean)
        }
        if (json.has("SmoothRotationFactor")) {
            ClientTheme.smoothFactorSS.set(json.get("SmoothRotationFactorSS").asFloat)
        }
        if (json.has("FullBodyRotation")) {
            ClientTheme.fullBody.set(json.get("FullBodyRotation").asBoolean)
        }
        if (json.has("DropDown-Theme")) {
            ClientTheme.dropdownTheme.set(json.get("DropDown-Theme").asString)
        }
    }

    override fun saveConfig(): String {
        val json = JsonObject()
        json.addProperty("Theme", ClientTheme.ClientColorMode.get())
        json.addProperty("Fade-Speed", ClientTheme.fadespeed.get())
        json.addProperty("Index", ClientTheme.index.get())
        json.addProperty("GCD", ClientTheme.gcdFix.get())
        json.addProperty("SmoothRotation", ClientTheme.smoothRotation.get())
        json.addProperty("SmoothRotationFactor", ClientTheme.smoothFactor.get())
        json.addProperty("SmoothRotationSS", ClientTheme.smoothRotationSS.get())
        json.addProperty("SmoothRotationFactorSS", ClientTheme.smoothFactorSS.get())
        json.addProperty("FullBodyRotation", ClientTheme.fullBody.get())
        json.addProperty("DropDown-Theme", ClientTheme.dropdownTheme.get())
        return FileManager.PRETTY_GSON.toJson(json)
    }
}