package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClientSettings
import java.io.File

/**
 * Persists [ClientSettings] (ClickGUI category visibility, generic-panel toggles,
 * etc.) to a standalone `clientsettings.json` file — independent from
 * `themeColor.json` so theme exports / imports don't drag GUI layout state with
 * them.
 */
class ClientSettingsConfig(file: File) : FileConfig(file) {

    override fun loadConfig(config: String) {
        val json = JsonParser().parse(config).asJsonObject

        ClientSettings.hiddenCategories.clear()
        if (json.has("HiddenCategories")) {
            json.getAsJsonArray("HiddenCategories").forEach {
                ClientSettings.hiddenCategories.add(it.asString)
            }
        }

        ClientSettings.hiddenPanels.clear()
        if (json.has("HiddenPanels")) {
            json.getAsJsonArray("HiddenPanels").forEach {
                ClientSettings.hiddenPanels.add(it.asString)
            }
        }
    }

    override fun saveConfig(): String {
        val json = JsonObject()
        val cats = JsonArray()
        ClientSettings.hiddenCategories.sorted().forEach { cats.add(com.google.gson.JsonPrimitive(it)) }
        json.add("HiddenCategories", cats)

        val panels = JsonArray()
        ClientSettings.hiddenPanels.sorted().forEach { panels.add(com.google.gson.JsonPrimitive(it)) }
        json.add("HiddenPanels", panels)

        return FileManager.PRETTY_GSON.toJson(json)
    }
}
