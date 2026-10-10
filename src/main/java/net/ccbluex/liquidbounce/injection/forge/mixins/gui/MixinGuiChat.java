 
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.modules.client.GuiChatModule;
import net.ccbluex.liquidbounce.features.module.modules.client.*;
import net.ccbluex.liquidbounce.features.module.modules.client.SessionInfo;
import net.ccbluex.liquidbounce.utils.MouseUtils;
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
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends MixinGuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private List<String> foundPlayerNames;

    @Shadow
    private boolean waitingOnAutocomplete;
    @Shadow
    private boolean playerNamesFound;

    @Shadow
    public abstract void onAutocompleteResponse(String[] p_onAutocompleteResponse_1_);
    @Shadow
    public abstract void autocompletePlayerNames();

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
    protected void keyTyped(char p_keyTyped_1_, int p_keyTyped_2_) throws IOException {
        this.waitingOnAutocomplete = false;
        if (p_keyTyped_2_ == 15) {
            this.autocompletePlayerNames();
        } else {
            this.playerNamesFound = false;
        }

        if (p_keyTyped_2_ == 1) {
            mc.displayGuiScreen(null);
        } else if (p_keyTyped_2_ != 28 && p_keyTyped_2_ != 156) {
            if (p_keyTyped_2_ == 200) {
                this.getSentHistory(-1);
            } else if (p_keyTyped_2_ == 208) {
                this.getSentHistory(1);
            } else if (p_keyTyped_2_ == 201) {
                this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
            } else if (p_keyTyped_2_ == 209) {
                this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
            } else {
                this.inputField.textboxKeyTyped(p_keyTyped_1_, p_keyTyped_2_);
            }
        } else {
            String s = this.inputField.getText().trim();
            if (s.length() > 0) {
                this.sendChatMessage(s);
            }

            mc.displayGuiScreen(null);
        }

    }
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
        long time = System.currentTimeMillis();
        RenderUtils.drawBloomRoundedRect(2F, this.height - 14F, this.width - 2F, this.height - 2F, 3F, 3F, new Color(0,0,0,80), RenderUtils.ShaderBloom.BOTH);
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
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    protected void mouseClicked(int p_mouseClicked_1_, int p_mouseClicked_2_, int p_mouseClicked_3_, CallbackInfo callbackInfo) throws IOException {
        final Scoreboard sb = Scoreboard.INSTANCE;
        final TargetHUD th = TargetHUD.INSTANCE;
        final KeyStrokes ks = KeyStrokes.INSTANCE;
        final SessionInfo si = SessionInfo.INSTANCE;
        final ArrayListModule al = ArrayListModule.INSTANCE;
        if (p_mouseClicked_3_ == 0) {
            if (MouseUtils.mouseWithinBounds(p_mouseClicked_1_, p_mouseClicked_2_, sb.getX1(), sb.getY1(), sb.getX2(), sb.getY2())) {
                sb.setDragging(true);
                sb.setDragOffsetX(p_mouseClicked_1_ - sb.getPosX());
                sb.setDragOffsetY(p_mouseClicked_2_ - sb.getPosY());
            }
            if (MouseUtils.mouseWithinBounds(p_mouseClicked_1_, p_mouseClicked_2_, th.getX1(), th.getY1(), th.getX2(), th.getY2())) {
                th.setDragging(true);
                th.setDragOffsetX(p_mouseClicked_1_ - th.getPosX());
                th.setDragOffsetY(p_mouseClicked_2_ - th.getPosY());
            }
            if (MouseUtils.mouseWithinBounds(p_mouseClicked_1_, p_mouseClicked_2_, ks.getX1(), ks.getY1(), ks.getX2(), ks.getY2())) {
                ks.setDragging(true);
                ks.setDragOffsetX(p_mouseClicked_1_ - ks.getPosX());
                ks.setDragOffsetY(p_mouseClicked_2_ - ks.getPosY());
            }
            if (MouseUtils.mouseWithinBounds(p_mouseClicked_1_, p_mouseClicked_2_, si.getX1(), si.getY1(), si.getX2(), si.getY2())) {
                si.setDragging(true);
                si.setDragOffsetX(p_mouseClicked_1_ - si.getPosX());
                si.setDragOffsetY(p_mouseClicked_2_ - si.getPosY());
            }
            if (MouseUtils.mouseWithinBounds(p_mouseClicked_1_, p_mouseClicked_2_, al.getX1(), al.getY1(), al.getX2(), al.getY2())) {
                al.setDragging(true);
                al.setDragOffsetX(p_mouseClicked_1_ - al.getPosX());
                al.setDragOffsetY(p_mouseClicked_2_ - al.getPosY());
            }
        }
    }
    @Inject(method = "onGuiClosed", at = @At("HEAD"), cancellable = true)
    public void onGuiClosed(CallbackInfo ci) {
        final Scoreboard sb = Scoreboard.INSTANCE;
        final TargetHUD th = TargetHUD.INSTANCE;
        final KeyStrokes ks = KeyStrokes.INSTANCE;
        final SessionInfo si = SessionInfo.INSTANCE;
        final ArrayListModule al = ArrayListModule.INSTANCE;
        sb.setDragging(false);
        th.setDragging(false);
        ks.setDragging(false);
        si.setDragging(false);
        al.setDragging(false);
    }
}