package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move;

import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;

public class ScaffoldCheck extends Check {

    public ScaffoldCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Scaffold";
        checkViolationLevel = 3;
    }

    @Override
    public void onLivingUpdate() {
        data.update(handlePlayer);
        boolean b = true;
        if (handlePlayer.isSwingInProgress && handlePlayer.rotationPitch >= 70.0f && handlePlayer.getHeldItem() != null && handlePlayer.getHeldItem().getItem() instanceof ItemBlock && data.c >= 20 && handlePlayer.ticksExisted - data.f >= 30 && handlePlayer.ticksExisted - data.b >= 20) {
            BlockPos blockPos = handlePlayer.getPosition().down(2);
            for (int i = 0; i < 4; ++i) {
                if (!(BlockUtils.getBlock(blockPos) instanceof BlockAir)) {
                    b = false;
                    break;
                }
                blockPos = blockPos.down();
            }
            if (b) {
                flag("Scaffold flag", 3);
            }
        }
    }
}
