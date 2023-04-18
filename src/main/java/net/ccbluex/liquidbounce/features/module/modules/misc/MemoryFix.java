package net.ccbluex.liquidbounce.features.module.modules.misc;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;

@ModuleInfo(name = "MemoryFix",  category = ModuleCategory.MISC)
public class MemoryFix extends Module {
    @Override
    public void onEnable() {
        Runtime.getRuntime().gc();
    }
}
