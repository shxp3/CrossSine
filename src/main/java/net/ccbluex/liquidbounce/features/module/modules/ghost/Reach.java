package net.ccbluex.liquidbounce.features.module.modules.ghost;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.FloatValue;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

@ModuleInfo(name = "Reach", spacedName = "Reach", category = ModuleCategory.GHOST)
public class Reach extends Module {
    public static FloatValue ReachMax = new FloatValue("Max", 3.5f, 3f, 7f);
    public static FloatValue ReachMin = new FloatValue("Min", 3.5f, 3f, 7f);
    public static BoolValue tw = new BoolValue("ThroughBlocks", false);
    public static double getReach() {
        double min = Math.min(ReachMin.getValue(), ReachMax.getValue());
        double max = Math.max(ReachMin.getValue(), ReachMax.getValue());
        return Math.random() * (max - min) + min;
    }

    @EventTarget
    public boolean onMouse(MouseEvent ev) {
        if (ev.getButton() >= 0) {
            if (!tw.get() && mc.objectMouseOver != null) {
                BlockPos p = mc.objectMouseOver.getBlockPos();
                if (p != null && mc.theWorld.getBlockState(p).getBlock() != Blocks.air) {
                    return false;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getTag() {
        return ReachMin.get() + "-" + ReachMax.get();
    }
}