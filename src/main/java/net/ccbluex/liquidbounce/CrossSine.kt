package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.discordrpc.CrossSineRPC
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.macro.MacroManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.visual.Interface
import net.ccbluex.liquidbounce.features.special.*
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.config.ConfigManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.ui.client.gui.EnumLaunchFilter
import net.ccbluex.liquidbounce.ui.client.gui.GuiLaunchOptionSelectMenu
import net.ccbluex.liquidbounce.ui.client.gui.LaunchFilterInfo
import net.ccbluex.liquidbounce.ui.client.gui.LaunchOption
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.Display
import kotlin.concurrent.thread

object CrossSine {
    // Client information
    const val CLIENT_NAME = "CrossSine"
    const val CLIENT_CLOUD = "https://crosssine.github.io/cloud"
    const val CLIENT_WEBSITE = "crosssine.github.io"
    var USER_NAME = ""
    var CUSTOM_DOMAIN = ".customdomain [domain]"
    val CLIENT_STATUS = ListValue("ClientVersion", arrayOf("Release", "Beta"), "Beta")
    const val COLORED_NAME = "§CC§FrossSine"
    const val CLIENT_CREATOR = "Shape"
    val CLIENT_VERSION = "39"
    var destruced = false
    var needConfig = false

    const val CLIENT_LOADING = "Installing CrossSine"

    @JvmField
    val CLIENT_TITLE = "$CLIENT_NAME B$CLIENT_VERSION" + if (CLIENT_STATUS.equals("Beta")) " (Beta)" else ""


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


    // Some UI things
    lateinit var hud: HUD
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager
    lateinit var ClientRPC: CrossSineRPC


    // Menu Background
    var background: ResourceLocation? = null

    val launchFilters = mutableListOf<EnumLaunchFilter>()
    val dynamicLaunchOptions: Array<LaunchOption>
        get() = ClassUtils.resolvePackage(
            "${LaunchOption::class.java.`package`.name}.options",
            LaunchOption::class.java
        )
            .filter {
                val annotation = it.getDeclaredAnnotation(LaunchFilterInfo::class.java)
                if (annotation != null) {
                    return@filter annotation.filters.toMutableList() == launchFilters
                }
                false
            }
            .map {
                try {
                    it.newInstance()
                } catch (e: IllegalAccessException) {
                    ClassUtils.getObjectInstance(it) as LaunchOption
                }
            }.toTypedArray()

    /**
     * Execute if client will be started
     */
    fun initClient() {
        ClientUtils.logInfo("Loading $CLIENT_NAME $CLIENT_VERSION, by $CLIENT_CREATOR")
        ClientUtils.logInfo("Initialzing...")
        Display.setTitle("Initialzing...")
        val startTime = System.currentTimeMillis()
        // Create file manager
        fileManager = FileManager()
        configManager = ConfigManager()

        // Create event manager
        eventManager = EventManager()

        // Register listeners
        Display.setTitle("Loading event")
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(ClientFixes)
        eventManager.registerListener(ClientSpoof())
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(BungeeCordSpoof())
        eventManager.registerListener(ServerSpoof)
        eventManager.registerListener(SessionUtils())
        eventManager.registerListener(StatisticsUtils())
        eventManager.registerListener(LocationCache())
        // Create command manager
        commandManager = CommandManager()

        ClientRPC = CrossSineRPC
        Display.setTitle("Load config")
        fileManager.loadConfigs(
            fileManager.accountsConfig,
            fileManager.friendsConfig,
            fileManager.specialConfig,
            fileManager.themeConfig,
            fileManager.subscriptsConfig
        )
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

        // Register commands
        commandManager.registerCommands()

        // KeyBindManager
        keyBindManager = KeyBindManager()

        combatManager = CombatManager()
        eventManager.registerListener(combatManager)

        mainMenu = GuiLaunchOptionSelectMenu()


        // Set HUD
        hud = HUD.createDefault()

        fileManager.loadConfigs(fileManager.hudConfig, fileManager.xrayConfig)


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
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION loaded in ${(System.currentTimeMillis() - startTime)}ms!")
    }


    /**
     * Execute if client ui type is selected
     */
    fun startClient() {
        needConfig = true
        dynamicLaunchOptions.forEach {
            it.start()
        }

        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()
        // Set is starting status
        isStarting = false
        isLoadingConfig = false
        thread {
            try {
                ClientRPC.run()
            } catch (throwable: Throwable) {
                ClientUtils.logError("", throwable)
            }
        }
        Display.setTitle(CLIENT_TITLE)
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started!")
        needConfig = false
        Interface.state = true
    }


    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        needConfig = true
        if (!isStarting && !isLoadingConfig) {
            ClientUtils.logInfo("Shutting down $CLIENT_NAME $CLIENT_VERSION!")

            // Call client shutdown
            eventManager.callEvent(ClientShutdownEvent())

            // Save all available configs
            configManager.save(true, true)
            fileManager.saveAllConfigs()
            dynamicLaunchOptions.forEach {
                it.stop()
            }
        }
        ClientRPC.stop()
    }
}
