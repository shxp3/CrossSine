package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.value.Value
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.AnimationHelper
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.render.Translate
import org.lwjgl.input.Keyboard

open class Module : MinecraftInstance(), Listenable {
    // Module information
    val translate = Translate(0F,0F)
    val animation: AnimationHelper
    var name: String
    var update: Boolean = false
    private var suffix: String? = null
    private val properties: List<Value<*>> = ArrayList()
    private var toggled = false
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


    init {
        name = moduleInfo.name
        animation = AnimationHelper(this)
        category = moduleInfo.category
        keyBind = moduleInfo.keyBind
        array = moduleInfo.array
        canEnable = moduleInfo.canEnable
        autoDisable = moduleInfo.autoDisable
        moduleCommand = moduleInfo.moduleCommand
        triggerType = moduleInfo.triggerType
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

            // Play sound and add notification
            if (!CrossSine.isStarting && moduleInfo.array) {
                if (value) {
                    CrossSine.hud.addNotification(Notification("Module", "Enabled $localizedName", NotifyType.SUCCESS))
                } else {
                    CrossSine.hud.addNotification(Notification("Module", "Disable $localizedName", NotifyType.ERROR))
                }
            }

            // Call on enabled or disabled
            try {
                field = canEnable && value
                if (value) {
                    onEnable()
                } else {
                    onDisable()
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

    // Tag
    open val tag: String?
        get() = null

    /**
     * Toggle module
     */
    fun toggle() {
        state = !state
    }
    open fun getSuffix(): String? {
        return suffix
    }

    open fun setSuffix(suffix: String?) {
        this.suffix = suffix
    }

    open fun getProperties(): List<Value<*>?>? {
        return properties
    }

    open fun hasMode(): Boolean {
        return suffix != null
    }
    open fun isToggled(): Boolean {
        return toggled
    }
    open fun toggleSilent() {
        this.toggled = !this.toggled
        if (this.toggled) {
            onEnable()
        } else {
            onDisable()
        }
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
     * Get all values of module
     */
    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass, this)

    /**
     * Get module by [valueName]
     */
    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state
}
