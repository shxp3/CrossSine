package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.discordrpc.CrossSineRPC
import net.ccbluex.liquidbounce.utils.MouseKeybindHandler
import net.ccbluex.liquidbounce.web.LocalWebServer
import net.minecraftforge.common.MinecraftForge
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.macro.MacroManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.ccbluex.liquidbounce.features.module.modules.other.SelfDestructCommand
import net.ccbluex.liquidbounce.features.special.ClientSpoof
import net.ccbluex.liquidbounce.features.special.CombatManager
import net.ccbluex.liquidbounce.features.special.NotificationManager
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.config.ConfigManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.ui.client.gui.ClickGUIModule
import net.ccbluex.liquidbounce.ui.client.gui.GuiWelcome
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.ClientCommandHandler
import org.lwjgl.opengl.Display
import kotlin.concurrent.thread

object CrossSine {
    // Client information
    const val CLIENT_NAME = "CrossSine"
    const val CLIENT_CLOUD = "https://crosssine.github.io/cloud"
    private var CLIENT_STATUS = false
    const val COLORED_NAME = "§CC§FrossSine"
    const val CLIENT_CREATOR = "Shape"
    /** Display + update-check version — keep in sync with crosssine.github.io/version.json */
    const val CLIENT_VERSION = "26.0.2"
    var destruced = false

    const val CLIENT_LOADING = "Initialzing Minecraft"

    @JvmField
    val CLIENT_TITLE = "$CLIENT_NAME $CLIENT_VERSION" + if (CLIENT_STATUS) " (Beta)" else ""

    var isStarting = true
    var isLoadingConfig = true


    // Managers
    lateinit var moduleManager: ModuleManager

    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager
    lateinit var combatManager: CombatManager
    lateinit var macroManager: MacroManager
    lateinit var configManager: ConfigManager
    lateinit var notification: NotificationManager

    // Some UI things
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager
    lateinit var clientRPC: CrossSineRPC

    /**
     * Execute if client will be started
     */
    fun initClient() {
        ClientUtils.logInfo("Loading $CLIENT_NAME $CLIENT_VERSION, by $CLIENT_CREATOR")
        ClientUtils.logInfo("Initialzing...")
        Display.setTitle("Initialzing CrossSine...")
        val startTime = System.currentTimeMillis()
        // Create file manager
        fileManager = FileManager()
        configManager = ConfigManager()
        ClientCommandHandler.instance.registerCommand(SelfDestructCommand())
        // Create event manager
        eventManager = EventManager()
        // Create event manager
        Display.setTitle("Loading event")
        eventManager = EventManager()
        MinecraftForge.EVENT_BUS.register(MouseKeybindHandler)
        // Forge-side handler that hides vanilla hearts/armor/food when
        // Interface.Stat-Bars is on (see HotbarStatBarsHandler kdoc — Mixin
        // inject didn't reliably suppress them in this build).
        MinecraftForge.EVENT_BUS.register(net.ccbluex.liquidbounce.features.module.modules.client.impl.HotbarStatBarsHandler)
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(ClientSpoof())
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(SessionUtils())
        eventManager.registerListener(StatisticsUtils())
        eventManager.registerListener(SlotUtils)
        eventManager.registerListener(PacketUtils)
        // Create command manager
        commandManager = CommandManager()
        // Create Notification Manager
        notification = NotificationManager()
        clientRPC = CrossSineRPC
        // Load client fonts
        Display.setTitle("Load Fonts")
        Fonts.loadFonts()
        macroManager = MacroManager()
        eventManager.registerListener(macroManager)
        // Setup module manager and register modules
        Display.setTitle("Load Module")
        moduleManager = ModuleManager()
        moduleManager.registerModules()

        try {
            // ScriptManager, Remapper will be lazy loaded when scripts are enabled
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }

        Display.setTitle("Load config")
        fileManager.loadConfigs(
            fileManager.accountsConfig,
            fileManager.friendsConfig,
            fileManager.specialConfig,
            fileManager.themeConfig,
            fileManager.clientSettingsConfig,
            fileManager.clienthudConfig,
            fileManager.subscriptsConfig
        )
        // Register commands
        commandManager.registerCommands()

        // KeyBindManager
        keyBindManager = KeyBindManager()

        combatManager = CombatManager()
        eventManager.registerListener(combatManager)

        mainMenu = GuiWelcome()
        moduleManager.registerModule(ClickGUIModule())
        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()
        // Set is starting status
        isStarting = false
        isLoadingConfig = false
        Interface.state = true


        fileManager.loadConfigs(fileManager.xrayConfig)


        ClientUtils.logInfo("Loading Script Subscripts...")
        Display.setTitle("Loading Script")
        try {

            // ScriptManager
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }
        Display.setTitle(CLIENT_TITLE)
        LocalWebServer.start()
        ClientUpdater.checkAsync()
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started!")
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION loaded in ${(System.currentTimeMillis() - startTime)}ms!")
    }


    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        if (!isStarting && !isLoadingConfig) {
            ClientUtils.logInfo("Shutting down $CLIENT_NAME $CLIENT_VERSION!")

            // Call client shutdown
            eventManager.callEvent(ClientShutdownEvent())

            // Save all available configs
            LocalWebServer.stop()
            configManager.save(true, true)
            fileManager.saveAllConfigs()
        }
    }
}
