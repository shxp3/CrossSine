package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.modules.client.hud.HUDModule
import net.ccbluex.liquidbounce.features.value.*
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import java.io.File

class ClientHUDConfig(file: File) : FileConfig(file) {

    override fun loadConfig(config: String) {
        val json = JsonParser().parse(config).asJsonObject
        for (module in CrossSine.moduleManager.modules) {
            // Only load modules that opted out of the normal config
            if (module.loadConfig) continue
            if (!json.has(module.name)) continue

            val moduleObject = json.getAsJsonObject(module.name)

            if (moduleObject.has("state"))
                module.state = moduleObject.get("state").asBoolean

            for (value in module.values) {
                if (!moduleObject.has(value.name)) continue
                try {
                    value.fromJson(moduleObject.get(value.name))
                } catch (_: Exception) {}
            }

            if (module is HUDModule) {
                if (moduleObject.has("posX"))
                    module.posX = moduleObject.get("posX").asFloat
                if (moduleObject.has("posY"))
                    module.posY = moduleObject.get("posY").asFloat
            }
        }
    }

    override fun saveConfig(): String {
        val json = JsonObject()
        for (module in CrossSine.moduleManager.modules) {
            // Only save modules that opted out of the normal config
            if (module.loadConfig) continue

            val moduleObject = JsonObject()
            moduleObject.addProperty("state", module.state)

            for (value in module.values) {
                val element = value.toJson() ?: continue
                moduleObject.add(value.name, element)
            }

            if (module is HUDModule) {
                moduleObject.addProperty("posX", module.posX)
                moduleObject.addProperty("posY", module.posY)
            }

            json.add(module.name, moduleObject)
        }
        return FileManager.PRETTY_GSON.toJson(json)
    }
}