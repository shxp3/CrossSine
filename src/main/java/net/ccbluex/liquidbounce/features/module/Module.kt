package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.special.NotificationUtil
import net.ccbluex.liquidbounce.features.special.TYPE
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import org.lwjgl.input.Keyboard

open class Module : MinecraftInstance(), Listenable {
    // Module information
    var name: String
    var update: Boolean = false
    var localizedName = ""
        get() = field.ifEmpty { name }
    var category: ModuleCategory
    var keyBind = Keyboard.CHAR_NONE
        set(keyBind) {
            field = keyBind

            if (!CrossSine.isStarting) {
                CrossSine.configManager.smartSave()
            }
        }
    var array = true
        set(array) {
            field = array

            if (!CrossSine.isStarting) {
                CrossSine.configManager.smartSave()
            }
        }
    val canEnable: Boolean
    var autoDisable: EnumAutoDisableType
    var triggerType: EnumTriggerType
    val moduleCommand: Boolean
    val moduleInfo = javaClass.getAnnotation(ModuleInfo::class.java)!!
    var slideStep = 0F
    var module: String? = null
    var loadConfig = true


    init {
        name = moduleInfo.name
        category = moduleInfo.category
        keyBind = moduleInfo.keyBind
        array = moduleInfo.array
        canEnable = moduleInfo.canEnable
        autoDisable = moduleInfo.autoDisable
        moduleCommand = moduleInfo.moduleCommand
        triggerType = moduleInfo.triggerType
        module = moduleInfo.module
        loadConfig = moduleInfo.loadConfig
    }

    open fun onLoad() {
        localizedName = name
    }

    // Current state of module
    var state = false
        set(value) {
            if (field == value) return

            // Call toggle
            onToggle(value)
            // Call on enabled or disabled
            try {
                field = canEnable && value
                if (value) {
                    onEnable()
                    if (array && !CrossSine.isLoadingConfig) {
                        CrossSine.notification.list.add(
                            NotificationUtil(
                                "Module",
                                "Enable ${name}",
                                TYPE.SUCCESS,
                                System.currentTimeMillis(),
                                1000
                            )
                        )
                    }
                } else {
                    onDisable()
                    if (array && !CrossSine.isLoadingConfig) {
                        CrossSine.notification.list.add(
                            NotificationUtil(
                                "Module",
                                "Disable ${name}",
                                TYPE.ERROR,
                                System.currentTimeMillis(),
                                1000
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            // Save module state
            CrossSine.configManager.smartSave()
        }

    // HUD
    val hue = Math.random().toFloat()
    var slide = 0f
    var arrayY = 0F
    var zoom = 0F

    // Tag
    open val tag: String?
        get() = null

    /**
     * Toggle module
     */
    fun toggle() {
        state = !state
    }

    /**
     * Print [msg] to chat as alert
     */
    protected fun alert(msg: String) = ClientUtils.displayAlert(msg)

    /**
     * Print [msg] to chat as plain text
     */
    protected fun chat(msg: String) = ClientUtils.displayChatMessage(msg)

    /**
     * Called when module toggled
     */
    open fun onToggle(state: Boolean) {}

    /**
     * Called when module enabled
     */
    open fun onEnable() {}

    /**
     * Called when module disabled
     */
    open fun onDisable() {}

    /**
     * Called when module initialized
     */
    open fun onInitialize() {}

    /**
     * Cached once — ClassUtils.getValues uses reflection over declaredFields.
     */
    @Transient
    private var cachedValues: List<Value<*>>? = null

    /**
     * Get all values of module
     */
    open val values: List<Value<*>>
        get() {
            cachedValues?.let { return it }
            return ClassUtils.getValues(this.javaClass, this).also { cachedValues = it }
        }

    /**
     * Get module by [valueName]
     */
    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state
}
