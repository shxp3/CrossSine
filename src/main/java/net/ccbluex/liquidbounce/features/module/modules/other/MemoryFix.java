package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;

@ModuleInfo(name = "MemoryFix",  spacedName = "Memory Fix",category = ModuleCategory.OTHER)
public class MemoryFix extends Module {
    @Override
    public void onEnable() {
        Runtime.getRuntime().gc();
    }
}
