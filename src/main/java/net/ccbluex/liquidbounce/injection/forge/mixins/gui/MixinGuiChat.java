 
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.visual.GuiChatModule;
import net.ccbluex.liquidbounce.features.module.modules.visual.Interface;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.client.gui.Gui.drawRect;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends MixinGuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private List<String> foundPlayerNames;

    @Shadow
    private boolean waitingOnAutocomplete;

    @Shadow
    public abstract void onAutocompleteResponse(String[] p_onAutocompleteResponse_1_);

    @Shadow
    private int sentHistoryCursor;

    @Shadow private String historyBuffer;

    private float yPosOfInputField;
    private float fade = 0;

    final GuiChatModule guiChatModule = CrossSine.moduleManager.getModule(GuiChatModule.class);

    /**
     * @author Liuli
     * 这种客户端验证需要玩家点击一段.开头的100长度字符串，而客户端会自动填充.say来尝试绕过
     * 但是自动填充的.say在需要按上箭头重新发送上一条消息的时候就会因为长度不够导致展示不全
     */
    @Overwrite
    public void getSentHistory(int p_getSentHistory_1_) {
        int i = this.sentHistoryCursor + p_getSentHistory_1_;
        int j = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp_int(i, 0, j);
        if (i != this.sentHistoryCursor) {
            if (i == j) {
                this.sentHistoryCursor = j;
                    setText(this.historyBuffer);
            } else {
                if (this.sentHistoryCursor == j) {
                    this.historyBuffer = this.inputField.getText();
                }

                setText(this.mc.ingameGUI.getChatGUI().getSentMessages().get(i));
                this.sentHistoryCursor = i;
            }
        }
    }

    private void setText(String text){
        if(text.startsWith(String.valueOf(CrossSine.commandManager.getPrefix()))) {
            this.inputField.setMaxStringLength(114514);
        } else {
            if(guiChatModule.getState() && guiChatModule.getChatLimitValue().get()) {
                this.inputField.setMaxStringLength(114514);
            } else {
                this.inputField.setMaxStringLength(100);
            }
        }
        this.inputField.setText(text);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void init(CallbackInfo callbackInfo) {
        inputField.yPosition = height - 5;
        yPosOfInputField = inputField.yPosition;
    }

    /**
     * only trust message in KeyTyped to anti some client click check (like old zqat.top)
     */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void keyTyped(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        String text = inputField.getText();
        if(text.startsWith(String.valueOf(CrossSine.commandManager.getPrefix()))) {
            this.inputField.setMaxStringLength(114514);
            if (keyCode == 28 || keyCode == 156) {
                CrossSine.commandManager.executeCommands(text);
                callbackInfo.cancel();
                mc.ingameGUI.getChatGUI().addToSentMessages(text);
                if(mc.currentScreen instanceof GuiChat)
                    Minecraft.getMinecraft().displayGuiScreen(null);
            }else{
                CrossSine.commandManager.autoComplete(text);
            }
        } else {
            this.inputField.setMaxStringLength(100);
        }
    }

    /**
     * bypass click command auth like kjy.pub
     */
    @Inject(method = "setText", at = @At("HEAD"), cancellable = true)
    private void setText(String newChatText, boolean shouldOverwrite, CallbackInfo callbackInfo) {
        if(shouldOverwrite&&newChatText.startsWith(String.valueOf(CrossSine.commandManager.getPrefix()))){
            setText(CrossSine.commandManager.getPrefix()+"say "+newChatText);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    private void updateScreen(CallbackInfo callbackInfo) {
        final int delta = RenderUtils.deltaTime;

        if (fade < 14) fade += 0.4F * delta;
        if (fade > 14) fade = 14;

        if (yPosOfInputField > height - 12) yPosOfInputField -= 0.4F * delta;
        if (yPosOfInputField < height - 12) yPosOfInputField = height - 12;

        inputField.yPosition = (int) yPosOfInputField - 1;
    }

    @Inject(method = "autocompletePlayerNames", at = @At("HEAD"))
    private void prioritizeClientFriends(final CallbackInfo callbackInfo) {
        foundPlayerNames.sort(
                Comparator.comparing(s -> !CrossSine.fileManager.getFriendsConfig().isFriend(s)));
    }

    /**
     * Adds client command auto completion and cancels sending an auto completion request packet
     * to the server if the message contains a client command.
     *
     * @author NurMarvin
     */
    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void handleClientCommandCompletion(String full, final String ignored, CallbackInfo callbackInfo) {
        if (CrossSine.commandManager.autoComplete(full)) {
            waitingOnAutocomplete = true;

            String[] latestAutoComplete = CrossSine.commandManager.getLatestAutoComplete();

            if (full.toLowerCase().endsWith(latestAutoComplete[latestAutoComplete.length - 1].toLowerCase()))
                return;

            this.onAutocompleteResponse(latestAutoComplete);

            callbackInfo.cancel();
        }
    }

    private void onAutocompleteResponse(String[] autoCompleteResponse, CallbackInfo callbackInfo) {
        if (CrossSine.commandManager.getLatestAutoComplete().length != 0) callbackInfo.cancel();
    }
    public void draw(){
    }
    /**
     * @author CCBlueX
     */
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    public void drawScreen(int mouseX, int mouseY, float partialTicks,CallbackInfo ci) {
        drawRect(2, this.height - 14, this.width - 2, this.height - 2, Integer.MIN_VALUE);
        this.inputField.drawTextBox();

        if (CrossSine.commandManager.getLatestAutoComplete().length > 0 && !inputField.getText().isEmpty() && inputField.getText().startsWith(String.valueOf(CrossSine.commandManager.getPrefix()))) {
            String[] latestAutoComplete = CrossSine.commandManager.getLatestAutoComplete();
            String[] textArray = inputField.getText().split(" ");
            String text = textArray[textArray.length - 1];
            Object[] result = Arrays.stream(latestAutoComplete).filter((str) -> str.toLowerCase().startsWith(text.toLowerCase())).toArray();
            String resultText = "";
            if(result.length>0)
                resultText = ((String)result[0]).substring(Math.min(((String)result[0]).length(),text.length()));

            mc.fontRendererObj.drawStringWithShadow(resultText, 5.5F + inputField.xPosition + mc.fontRendererObj.getStringWidth(inputField.getText()), inputField.yPosition+2f, new Color(165, 165, 165).getRGB());
        }

        IChatComponent ichatcomponent =
                this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

        if (ichatcomponent != null)
            this.handleComponentHover(ichatcomponent, mouseX, mouseY);
        ci.cancel();
    }
}