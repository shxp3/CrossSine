package net.ccbluex.liquidbounce.features.module.modules.other;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class SelfDestructCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "stopdestruct";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/stopdestruct";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        CrossSine.INSTANCE.setDestruced(false);
        ClientUtils.INSTANCE.reloadClient();
        sender.addChatMessage(new ChatComponentText("SelfDestruct Destruct his self"));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}