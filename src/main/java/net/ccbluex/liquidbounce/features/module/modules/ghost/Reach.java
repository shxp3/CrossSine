package net.ccbluex.liquidbounce.features.module.modules.ghost;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.FloatValue;
import org.jetbrains.annotations.Nullable;

@ModuleInfo(name = "Reach", category = ModuleCategory.GHOST)
public class Reach extends Module {
    public static FloatValue ReachMax = new FloatValue("Max", 3.5f, 0f, 7f);
    public static FloatValue ReachMin = new FloatValue("Min", 3.5f, 0f, 7f);
    public static BoolValue ThroughWall = new BoolValue("Wall", false);


    public static double getReach() {
        double min = Math.min(ReachMin.getValue(), ReachMax.getValue());
        double max = Math.max(ReachMin.getValue(), ReachMax.getValue());
        return Math.random() * (max - min) + min;
    }

    @Nullable
    @Override
    public String getTag() {
        return ReachMax.get() + "-" + ReachMin.get();
    }
}