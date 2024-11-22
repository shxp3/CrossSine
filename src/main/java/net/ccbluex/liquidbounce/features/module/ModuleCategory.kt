package net.ccbluex.liquidbounce.features.module

enum class ModuleCategory(val displayName: String, val configName: String) {
    COMBAT("Combat", "Combat"),
    PLAYER("Player", "Player"),
    MOVEMENT("Movement", "Movement"),
    VISUAL("Visual", "Visual"),
    WORLD("World", "World"),
    OTHER("Other", "Other"),
    SCRIPT("Script", "Script");
}
