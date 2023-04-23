package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.modules.player.Insult
import net.ccbluex.liquidbounce.ui.client.gui.GuiMainMenu
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Text
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.Display

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
        displayChatMessage("[" + LiquidBounce.COLORED_NAME + "] " + message)
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
        LiquidBounce.commandManager = CommandManager()
        LiquidBounce.commandManager.registerCommands()
        LiquidBounce.isStarting = true
        LiquidBounce.isLoadingConfig = true
        LiquidBounce.scriptManager.disableScripts()
        LiquidBounce.scriptManager.unloadScripts()
        for (module in LiquidBounce.moduleManager.modules)
            LiquidBounce.moduleManager.generateCommand(module)
        LiquidBounce.scriptManager.loadScripts()
        LiquidBounce.scriptManager.enableScripts()
        Fonts.loadFonts()
        LiquidBounce.configManager.load(LiquidBounce.configManager.nowConfig, false)
        Insult.loadFile()
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.accountsConfig)
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.friendsConfig)
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.xrayConfig)
        LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
        LiquidBounce.isStarting = false
        LiquidBounce.isLoadingConfig = false
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
