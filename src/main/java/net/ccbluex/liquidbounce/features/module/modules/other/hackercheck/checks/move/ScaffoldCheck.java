package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.checks.move;

import net.ccbluex.liquidbounce.features.module.modules.other.HackerDetector;
import net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.Check;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;

import java.util.Iterator;
import java.util.LinkedList;

public class ScaffoldCheck extends Check {

    private static class BlockRecord {
        final BlockPos pos;
        final boolean wasAir;
        final int tick;
        BlockRecord(BlockPos pos, boolean wasAir, int tick) {
            this.pos = pos;
            this.wasAir = wasAir;
            this.tick = tick;
        }
    }

    private final LinkedList<BlockRecord> records = new LinkedList<>();
    private BlockPos lastPos = null;
    private int scaffoldBuffer = 0;

    public ScaffoldCheck(EntityOtherPlayerMP playerMP) {
        super(playerMP);
        name = "Scaffold";
        checkViolationLevel = 8;
    }

    @Override
    public void onLivingUpdate() {
        if (!HackerDetector.INSTANCE.scaffoldValue.get()) return;

        if (handlePlayer.getHeldItem() == null || !(handlePlayer.getHeldItem().getItem() instanceof ItemBlock) || !handlePlayer.onGround) {
            if (scaffoldBuffer > 0) scaffoldBuffer--;
            return;
        }

        BlockPos currentPos = handlePlayer.getPosition();

        // Record block state below feet when player moves to a new block position
        if (lastPos == null || !currentPos.equals(lastPos)) {
            BlockPos belowPos = currentPos.down(1);
            boolean isAir = BlockUtils.getBlock(belowPos) instanceof BlockAir;
            records.add(new BlockRecord(belowPos, isAir, handlePlayer.ticksExisted));
        }
        lastPos = currentPos;

        // Safety cap
        if (records.size() > 60) records.clear();

        // Check records older than 10 ticks: was air then, solid now = block was placed
        int airToSolid = 0;
        Iterator<BlockRecord> it = records.iterator();
        while (it.hasNext()) {
            BlockRecord rec = it.next();
            if (handlePlayer.ticksExisted - rec.tick >= 10) {
                boolean nowSolid = !(BlockUtils.getBlock(rec.pos) instanceof BlockAir);
                if (rec.wasAir && nowSolid) {
                    airToSolid++;
                }
                it.remove();
            }
        }

        if (airToSolid > 0) {
            scaffoldBuffer += airToSolid * 2;
            if (scaffoldBuffer > 12) {
                flag("Scaffold (air→solid trail x" + airToSolid + ")", 3);
                scaffoldBuffer = 6;
            }
        } else if (scaffoldBuffer > 0) {
            scaffoldBuffer--;
        }
    }

    @Override
    public void reset() {
        super.reset();
        scaffoldBuffer = 0;
        records.clear();
        lastPos = null;
    }
}
