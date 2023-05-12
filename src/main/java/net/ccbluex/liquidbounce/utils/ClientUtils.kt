package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.modules.player.KillSay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent
import org.apache.logging.log4j.LogManager

object
ClientUtils : MinecraftInstance() {
    @JvmStatic
    val logger = LogManager.getLogger("CrossSine")


    fun logInfo(msg: String) {
        logger.info(msg)
    }

    fun logWarn(msg: String) {
        logger.warn(msg)
    }

    fun logError(msg: String) {
        logger.error(msg)
    }

    fun logError(msg: String, t: Throwable) {
        logger.error(msg, t)
    }

    fun logDebug(msg: String) {
        logger.debug(msg)
    }

    fun displayAlert(message: String) {
        displayChatMessage("[" + CrossSine.COLORED_NAME + "] " + message)
    }

    fun displayChatMessage(message: String) {
        if (mc.thePlayer == null) {
            logger.info("(MCChat) $message")
            return
        }
        val jsonObject = JsonObject()
        jsonObject.addProperty("text", message)
        mc.thePlayer.addChatMessage(IChatComponent.Serializer.jsonToComponent(jsonObject.toString()))
    }

    fun reloadClient() {
        CrossSine.commandManager = CommandManager()
        CrossSine.commandManager.registerCommands()
        CrossSine.isStarting = true
        CrossSine.isLoadingConfig = true
        CrossSine.scriptManager.disableScripts()
        CrossSine.scriptManager.unloadScripts()
        for (module in CrossSine.moduleManager.modules)
            CrossSine.moduleManager.generateCommand(module)
        CrossSine.scriptManager.loadScripts()
        CrossSine.scriptManager.enableScripts()
        Fonts.loadFonts()
        CrossSine.configManager.load(CrossSine.configManager.nowConfig, false)
        KillSay.loadFile()
        CrossSine.fileManager.loadConfig(CrossSine.fileManager.accountsConfig)
        CrossSine.fileManager.loadConfig(CrossSine.fileManager.friendsConfig)
        CrossSine.fileManager.loadConfig(CrossSine.fileManager.xrayConfig)
        CrossSine.fileManager.loadConfig(CrossSine.fileManager.hudConfig)
        CrossSine.isStarting = false
        CrossSine.isLoadingConfig = false
        System.gc()
    }

    /**
     * Minecraft instance
     */
    val mc = Minecraft.getMinecraft()!!

    enum class EnumOSType(val friendlyName: String) {
        WINDOWS("win"), LINUX("linux"), MACOS("mac"), UNKNOWN("unk");
    }
}
