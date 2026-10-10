
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.KeyEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.player.InvManager;
import net.ccbluex.liquidbounce.features.module.modules.client.Interface;
import net.ccbluex.liquidbounce.features.module.modules.world.Stealer;
import net.ccbluex.liquidbounce.ui.client.gui.ThemedBackground;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.extensions.RendererExtensionKt;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;


@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends MixinGuiScreen {

    @Shadow
    protected int xSize;
    @Shadow
    protected int ySize;
    @Shadow
    protected int guiLeft;
    @Shadow
    public Container inventorySlots;
    @Shadow
    protected int guiTop;

    private long guiOpenTime = -1;

    private boolean translated = false;

    @Shadow
    protected abstract boolean checkHotbarKeys(int keyCode);

    @Shadow private int dragSplittingButton;
    @Shadow private int dragSplittingRemnant;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGuiReturn(CallbackInfo callbackInfo) {
        guiOpenTime = System.currentTimeMillis();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }
    @Overwrite
    public void onGuiClosed() {
        if (this.mc.thePlayer != null) {
            this.inventorySlots.onContainerClosed(this.mc.thePlayer);
            InvManager.INSTANCE.setCurrentSlot(-1);
            Stealer.INSTANCE.setCurrentSlot(-1);
        }

    }
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void drawScreenHead(CallbackInfo callbackInfo) {
        if (!CrossSine.INSTANCE.getDestruced()) {

            Stealer stealer = CrossSine.moduleManager.getModule(Stealer.class);
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen guiScreen = mc.currentScreen;
            if (stealer.getState() && stealer.getFreelookValue().get() && guiScreen instanceof GuiChest) {
                if (!stealer.getSilentValue().get()) {
                    mc.inGameHasFocus = true;
                    mc.mouseHelper.grabMouseCursor();
                }
            }
            if (stealer.getState() && stealer.getSilentValue().get() && guiScreen instanceof GuiChest) {
                GuiChest chest = (GuiChest) guiScreen;
                if (!(stealer.getChestTitleValue().get() && (chest.lowerChestInventory == null || !chest.lowerChestInventory.getName().contains(new ItemStack(Item.itemRegistry.getObject(new ResourceLocation("minecraft:chest"))).getDisplayName())))) {
                    // mouse focus
                    mc.setIngameFocus();
                    mc.currentScreen = guiScreen;

                    // hide GUI
                    if (stealer.getSilentTitleValue().get() && stealer.getSilentValue().get()) {
                        RendererExtensionKt.drawCenteredString(Fonts.fontSFUI35, "ChestStealer Silent", width / 2, (height / 2) + 30, 0xffffffff, true);
                    }
                    callbackInfo.cancel();
                }
            } else {
                mc.currentScreen.drawWorldBackground(0);
                if (Interface.INSTANCE.getInventoryAnimation().get() && Interface.INSTANCE.getState() && guiScreen instanceof GuiInventory) {
                    double pct = Math.max(300 - (System.currentTimeMillis() - guiOpenTime), 0) / ((double) 300);
                    if (pct != 0) {
                        GL11.glPushMatrix();
                        pct = EaseUtils.INSTANCE.easeInCirc(pct);

                        double scale = 1 - pct;
                        GL11.glScaled(scale, scale, scale);
                        GL11.glTranslated(((guiLeft + (xSize * 0.5 * pct)) / scale) - guiLeft,
                                ((guiTop + (ySize * 0.5d * pct)) / scale) - guiTop,
                                0);
                        translated = true;
                    }
                }
            }
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void checkCloseClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (!CrossSine.INSTANCE.getDestruced()) {
            if (mouseButton - 100 == mc.gameSettings.keyBindInventory.getKeyCode()) {
                mc.thePlayer.closeScreen();
                ci.cancel();
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void checkHotbarClicks(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        checkHotbarKeys(mouseButton - 100);
    }

    @Inject(method = "updateDragSplitting", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void fixRemnants(CallbackInfo ci) {
        if (this.dragSplittingButton == 2) {
            this.dragSplittingRemnant = mc.thePlayer.inventory.getItemStack().getMaxStackSize();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreenReturn(CallbackInfo callbackInfo) {
        if (translated) {
            GL11.glPopMatrix();
            translated = false;
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        Stealer stealer = CrossSine.moduleManager.getModule(Stealer.class);
        try {
            if (stealer.getState() && mc.currentScreen instanceof GuiChest)
                CrossSine.eventManager.callEvent(new KeyEvent(keyCode == 0 ? typedChar + 256 : keyCode));
        }catch (Exception e){

        }
    }
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawSlot(Slot slot, CallbackInfo ci) {
        final InvManager invManager = InvManager.INSTANCE;
        final Stealer stealer = Stealer.INSTANCE;

        int x = slot.xDisplayPosition;
        int y = slot.yDisplayPosition;


        Color color0 = new Color(80, 80, 80, 255);

        int currentSlotChestStealer = stealer.getCurrentSlot();
        int currentSlotInvCleaner = invManager.getCurrentSlot();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (mc.currentScreen instanceof GuiChest) {
            if (stealer.getState() && !stealer.getSilentValue().get()) {
                if (slot.slotNumber == currentSlotChestStealer && currentSlotChestStealer != -1) {
                    RenderUtils.drawBloomRoundedRect(x, y, x + 16, y + 16, 3, 2.5F, color0, RenderUtils.ShaderBloom.BOTH);
                }
            }
        }

        if (mc.currentScreen instanceof GuiInventory) {
                if (invManager.getState()) {
                    if (slot.slotNumber == currentSlotInvCleaner && currentSlotInvCleaner != -1) {
                        RenderUtils.drawBloomRoundedRect(x, y, x + 16, y + 16, 3, 2.5F, color0, RenderUtils.ShaderBloom.BOTH);
                    }
                }
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}