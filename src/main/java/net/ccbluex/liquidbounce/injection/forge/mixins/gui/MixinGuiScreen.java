package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow
    public Minecraft mc;

    @Shadow
    protected List<GuiButton> buttonList;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected FontRenderer fontRendererObj;

    @Shadow
    public abstract void updateScreen();

    @Shadow
    public abstract void handleComponentHover(IChatComponent component, int x, int y);

    @Shadow
    protected abstract void drawHoveringText(List<String> textLines, int x, int y);
    @Shadow
    protected abstract void sendChatMessage(String p_sendChatMessage_1_);

    @Shadow
    protected abstract void actionPerformed(GuiButton p_actionPerformed_1_);
    @ModifyVariable(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"))
    private String mosendChatMessage(String p_sendChatMessage_1_){
        if(p_sendChatMessage_1_.length()>100){
            return p_sendChatMessage_1_.substring(0,100);
        }
        return p_sendChatMessage_1_;
    }
    @Inject(method = "drawDefaultBackground", at = @At("HEAD"), cancellable = true)
    private void drawDefaultBackground(final CallbackInfo callbackInfo){
        if(mc.currentScreen instanceof GuiContainer){
            callbackInfo.cancel();
        }
    }
    /**
     * @author CCBlueX
     */
    @Overwrite
    public void drawBackground(int p_drawBackground_1_) {
        ThemedBackground.draw(width, height);
    }

    @Inject(method = "handleComponentHover", at = @At("HEAD"))
    private void handleHoverOverComponent(IChatComponent component, int x, int y, final CallbackInfo callbackInfo) {
        if (component == null || component.getChatStyle().getChatClickEvent() == null)
            return;

        final ChatStyle chatStyle = component.getChatStyle();

        final ClickEvent clickEvent = chatStyle.getChatClickEvent();
        final HoverEvent hoverEvent = chatStyle.getChatHoverEvent();

        drawHoveringText(Collections.singletonList("§c§l" + clickEvent.getAction().getCanonicalName().toUpperCase() + ": §a" + clickEvent.getValue()), x, y - (hoverEvent != null ? 17 : 0));
    }

    @Inject(method = "drawHoveringText*", at = @At("HEAD"))
    private void drawHoveringText(CallbackInfo ci) {
        // idk why this make font renderer works
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }

    protected abstract void injectedActionPerformed(GuiButton button);
}