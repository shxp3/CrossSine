package net.ccbluex.liquidbounce.features.module.modules.client;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;

@ModuleInfo(name = "MemoryFix",  category = ModuleCategory.CLIENT)
public class MemoryFix extends Module {
    @Override
    public void onEnable() {
        Runtime.getRuntime().gc();
    }
}
