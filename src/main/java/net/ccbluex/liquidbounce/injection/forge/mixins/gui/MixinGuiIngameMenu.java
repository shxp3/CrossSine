package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.login.LoginUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends MixinGuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        if (!CrossSine.INSTANCE.getDestruced()) {
            if (!this.mc.isIntegratedServerRunning()) {
                this.buttonList.add(new GuiButton(1337, this.width / 2 - 100, this.height / 4 + 128, "Reconnect"));
                this.buttonList.add(new GuiButton(1068, this.width / 2 - 100, this.height / 4 + 128 + 24, "Switcher"));
                this.buttonList.add(new GuiButton(1078, this.width / 2 - 100, this.height / 4 + 128 + 140, "Key Bind Manager"));
                this.buttonList.add(new GuiButton(16578, this.width / 2 - 100, this.height / 4 + 128 + 115, "RandomOffline and Reconnect"));
            } else {
                this.buttonList.add(new GuiButton(1068, this.width / 2 - 100, this.height / 4 + 128, "Switcher"));
                this.buttonList.add(new GuiButton(1078, this.width / 2 - 100, this.height / 4 + 128 + 105, "Key Bind Manager"));
                this.buttonList.add(new GuiButton(16578, this.width / 2 - 100, this.height / 4 + 128 + 80, "RandomOffline and Reconnect"));
            }
        }
    }
    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {
        if(button.id == 1337) {
            mc.theWorld.sendQuittingDisconnectingPacket();
            ServerUtils.connectToLastServer();
        }
         if(button.id == 16578) {
            mc.theWorld.sendQuittingDisconnectingPacket();
            ServerUtils.connectToLastServer();
             LoginUtils.INSTANCE.randomCracked();
        }
        if (button.id == 1068) {
            mc.displayGuiScreen(new GuiMultiplayer((GuiScreen) (Object) this));
        }
        if (button.id == 1078) {
            mc.displayGuiScreen(CrossSine.keyBindManager);
        }
    }
}