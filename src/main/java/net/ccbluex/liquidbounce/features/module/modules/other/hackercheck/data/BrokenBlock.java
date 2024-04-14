package net.ccbluex.liquidbounce.features.module.modules.other.hackercheck.data;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BrokenBlock {

    public final Block block;
    public final BlockPos blockPos;
    public final long breakTime;
    public final String tool;
    public List<EntityPlayer> playerList = null;

    public BrokenBlock(Block block, BlockPos blockPos, String tool) {
        this.block = block;
        this.blockPos = blockPos;
        this.breakTime = System.currentTimeMillis();
        this.tool = tool;
    }

    public void addPlayer(EntityPlayer player) {
        if (this.playerList == null) {
            this.playerList = new ArrayList<>();
        }
        this.playerList.add(player);
    }

}
