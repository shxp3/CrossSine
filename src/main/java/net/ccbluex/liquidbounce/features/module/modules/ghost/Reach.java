package net.ccbluex.liquidbounce.features.module.modules.ghost;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.FloatValue;
import org.jetbrains.annotations.Nullable;

@ModuleInfo(name = "Reach", spacedName = "Reach", category = ModuleCategory.GHOST)
public class Reach extends Module {
    public static FloatValue ReachMax = new FloatValue("Max", 3.5f, 3f, 7f);
    public static FloatValue ReachMin = new FloatValue("Min", 3.5f, 3f, 7f);


    public static double getReach() {
        double min = Math.min(ReachMin.getValue() + 0.55, ReachMax.getValue() + 0.55);
        double max = Math.max(ReachMin.getValue() + 0.55, ReachMax.getValue() + 0.55);
        return Math.random() * (max - min) + min;
    }
    @Nullable
    @Override
    public String getTag() {
        return ReachMin.get() + "-" + ReachMax.get();
    }
}