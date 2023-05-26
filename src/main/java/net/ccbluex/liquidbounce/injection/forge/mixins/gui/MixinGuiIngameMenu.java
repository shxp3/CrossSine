package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;

import net.ccbluex.liquidbounce.features.module.modules.visual.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
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
        if(!this.mc.isIntegratedServerRunning()) {
            this.buttonList.add(new GuiButton(1337, this.width / 2 - 100, this.height / 4 + 128, "Reconnect"));
            this.buttonList.add(new GuiButton(1068,this.width / 2 - 100,this.height / 4 + 128 + 24,"Switcher"));
            this.buttonList.add(new GuiButton(1078,this.width / 2 - 100,this.height / 4 + 128 + 140,"Key Bind Manager"));
            this.buttonList.add(new GuiButton(16578,this.width / 2 - 100,this.height / 4 + 128 + 115,"RandomOffline and Reconnect"));
        } else {
            this.buttonList.add(new GuiButton(1068,this.width / 2 - 100,this.height / 4 + 128,"Switcher"));
            this.buttonList.add(new GuiButton(1078,this.width / 2 - 100,this.height / 4 + 128 + 105,"Key Bind Manager"));
            this.buttonList.add(new GuiButton(16578,this.width / 2 - 100,this.height / 4 + 128 + 80,"RandomOffline and Reconnect"));
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreen(CallbackInfo callbackInfo) {
        final HUD guihudedit = CrossSine.moduleManager.getModule(HUD.class);
        Fonts.minecraftFont.drawStringWithShadow(
                "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Username : §a" + mc.getSession().getUsername(),
                6f,
                6f,
                0xffffff);
        if (!mc.isIntegratedServerRunning()) {
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Server : §a" + mc.getCurrentServerData().serverIP,
                    6f,
                    16f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Brand : §a" + mc.getCurrentServerData().gameVersion,
                    6f,
                    26f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Protocol : §a" + mc.getCurrentServerData().version,
                    6f,
                    36f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Ping : §a" + mc.getCurrentServerData().pingToServer,
                    6f,
                    46f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() +"Players : §a" + mc.getCurrentServerData().populationInfo,
                    6f,
                    56f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Health : §a" + mc.thePlayer.getHealth(),
                    6f,
                    66f,
                    0xffffff);
            Fonts.minecraftFont.drawStringWithShadow(
                    "§" + guihudedit.INSTANCE.getColorGuiInGameValue().getValue() + "Client Version : §a" + CrossSine.CLIENT_VERSION,
                    6f,
                    76f,
                    0xffffff);
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