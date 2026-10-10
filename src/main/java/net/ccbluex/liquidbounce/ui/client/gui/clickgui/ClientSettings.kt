package net.ccbluex.liquidbounce.ui.client.gui.clickgui

import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.features.module.ModuleCategory

/**
 * Persistent ClickGUI / client-side preferences that don't belong to a config
 * profile (so they survive config switches) and that we don't want to mix into
 * `ClientTheme`.
 *
 * Currently stores:
 *  - Which [ModuleCategory] panels the user has hidden in the DropdownGui.
 *  - Whether the Config / Settings utility panels are hidden.
 *
 * Persistence is handled by [net.ccbluex.liquidbounce.file.configs.ClientSettingsConfig]
 * (a separate `clientsettings.json` file).
 */
object ClientSettings {

    /** Hidden module-category names (enum name keys). */
    val hiddenCategories: MutableSet<String> = mutableSetOf()

    /** Hidden generic-panel ids (e.g. "Config", "Settings"). */
    val hiddenPanels: MutableSet<String> = mutableSetOf()

    fun isCategoryVisible(c: ModuleCategory): Boolean = c.name !in hiddenCategories
    fun setCategoryVisible(c: ModuleCategory, visible: Boolean) {
        if (visible) hiddenCategories.remove(c.name) else hiddenCategories.add(c.name)
    }
    fun toggleCategory(c: ModuleCategory) {
        if (c.name in hiddenCategories) hiddenCategories.remove(c.name) else hiddenCategories.add(c.name)
    }

    fun isPanelVisible(id: String): Boolean = id !in hiddenPanels
    fun setPanelVisible(id: String, visible: Boolean) {
        if (visible) hiddenPanels.remove(id) else hiddenPanels.add(id)
    }
    fun togglePanel(id: String) {
        if (id in hiddenPanels) hiddenPanels.remove(id) else hiddenPanels.add(id)
    }

    /** Persist to disk via FileManager. Safe to call frequently; FileManager skips if not ready. */
    fun save() {
        try {
            CrossSine.fileManager.saveConfigs(CrossSine.fileManager.clientSettingsConfig)
        } catch (_: Throwable) {
            // FileManager not ready yet (e.g. during early init) - ignore.
        }
    }
}
