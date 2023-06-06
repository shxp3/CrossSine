package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.ghost.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.ghost.HitDelayFix;
import net.ccbluex.liquidbounce.features.module.modules.visual.HUD;
import net.ccbluex.liquidbounce.features.module.modules.other.SoundModule;
import net.ccbluex.liquidbounce.features.module.modules.visual.FreeLook;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.injection.forge.mixins.accessors.MinecraftForgeClientAccessor;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.render.ImageUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Util;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.objectweb.asm.Opcodes.PUTFIELD;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public boolean skipRenderWorld;

    @Shadow
    private Entity renderViewEntity;

    @Shadow
    private int leftClickCounter;

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Shadow
    public WorldClient theWorld;

    @Shadow
    public EntityPlayerSP thePlayer;

    @Shadow
    public EffectRenderer effectRenderer;

    @Shadow
    public PlayerControllerMP playerController;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public int displayWidth;

    @Shadow
    public int displayHeight;
    @Shadow
    private boolean fullscreen;

    @Shadow
    public static int debugFPS;

    @Inject(method = "run", at = @At("HEAD"))
    private void init(CallbackInfo callbackInfo) {
        if (displayWidth < 1067)
            displayWidth = 1067;

        if (displayHeight < 622)
            displayHeight = 622;
    }
    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void startGame(CallbackInfo callbackInfo) {
        CrossSine.INSTANCE.initClient();
    }

    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void createDisplay(CallbackInfo callbackInfo) {
        Display.setTitle(CrossSine.CLIENT_LOADING);
    }


    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = At.Shift.AFTER))
    private void displayGuiScreen(CallbackInfo callbackInfo) {
        if (currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))) {
            currentScreen = CrossSine.mainMenu;

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
            skipRenderWorld = false;
        }

        CrossSine.eventManager.callEvent(new ScreenEvent(currentScreen));
    }

    private long lastFrame = getTime();

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(final CallbackInfo callbackInfo) {
        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;

        RenderUtils.deltaTime = deltaTime;
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void runTick(final CallbackInfo callbackInfo) {
        StaticStorage.scaledResolution = new ScaledResolution((Minecraft) (Object) this);
    }

    @Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;thirdPersonView:I", opcode = PUTFIELD))
    public void setThirdPersonView(GameSettings gameSettings, int value) {
        if (FreeLook.perspectiveToggled) {
            FreeLook.resetPerspective();
        } else {
            gameSettings.thirdPersonView = value;
        }
    }

    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = At.Shift.BEFORE))
    private void onTick(final CallbackInfo callbackInfo) {
        CrossSine.eventManager.callEvent(new TickEvent());
    }

    private static final String TARGET = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V";
    @Redirect(method="runTick", at=@At(value="INVOKE", target=TARGET))
    public void runTick_setKeyBindState(int keybind, boolean state) {
        if(CrossSine.moduleManager.getModule(HitDelayFix.class).getState() && keybind == -100 && !state) {
            leftClickCounter = 0;
        }

        KeyBinding.setKeyBindState(keybind, state);
    }

    @Inject(method = "dispatchKeypresses", at = @At(value = "HEAD"))
    private void onKey(CallbackInfo callbackInfo) {
        try {
            if (Keyboard.getEventKeyState() && (currentScreen == null || (SoundModule.INSTANCE.getToggleIgnoreScreenValue().get() && this.currentScreen instanceof GuiContainer)))
                CrossSine.eventManager.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovingObjectPosition;getBlockPos()Lnet/minecraft/util/BlockPos;"))
    private void onClickBlock(CallbackInfo callbackInfo) {
        if (this.leftClickCounter == 0 && theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().getMaterial() != Material.air) {
            CrossSine.eventManager.callEvent(new ClickBlockEvent(objectMouseOver.getBlockPos(), this.objectMouseOver.sideHit));
        }
    }


    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo callbackInfo) {
        CrossSine.INSTANCE.stopClient();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void clickMouse(CallbackInfo callbackInfo) {
        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT);
        if (CrossSine.moduleManager.getModule(AutoClicker.class).getState())
            leftClickCounter = 0; // fix hit delay lol
    }
    @Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = At.Shift.AFTER))
    private void rightClickMouse(final CallbackInfo callbackInfo) {
        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT);
    }
    @Inject(method = "middleClickMouse", at = @At("HEAD"))
    private void middleClickMouse(CallbackInfo ci) {
        CPSCounter.registerClick(CPSCounter.MouseButton.MIDDLE);
    }


    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient p_loadWorld_1_, String p_loadWorld_2_, final CallbackInfo callbackInfo) {
        CrossSine.eventManager.callEvent(new WorldEvent(p_loadWorld_1_));
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;theWorld:Lnet/minecraft/client/multiplayer/WorldClient;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void clearRenderCache(CallbackInfo ci) {
        MinecraftForgeClient.getRenderPass();
        MinecraftForgeClientAccessor.getRegionCache().invalidateAll();
        MinecraftForgeClientAccessor.getRegionCache().cleanUp();
    }
    @Inject(method = "getRenderViewEntity", at = @At("HEAD"))
    public void getRenderViewEntity(CallbackInfoReturnable<Entity> cir) {
        if (RotationUtils.targetRotation != null && thePlayer != null) {
            final HUD clientRender = CrossSine.moduleManager.getModule(HUD.class);
            final float yaw = RotationUtils.targetRotation.getYaw();
            if (clientRender.getRotationMode().equals("Lock")) {
                thePlayer.rotationYawHead = yaw;
            }
            if (clientRender.getRotationMode().equals("Lock")) {
                thePlayer.renderYawOffset = yaw;
            }
        }
    }
    /**
     * @author CCBlueX
     * @reason
     */

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void setWindowIcon(CallbackInfo callbackInfo) {
        try {
            if (Util.getOSType() != Util.EnumOS.OSX) {
                BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("/assets/minecraft/crosssine/misc/icon.png"));
                ByteBuffer bytebuffer = ImageUtils.readImageToBuffer(ImageUtils.resizeImage(image, 16, 16));
                if (bytebuffer == null) {
                    throw new Exception("Error when loading image.");
                } else {
                    Display.setIcon(new ByteBuffer[]{bytebuffer, ImageUtils.readImageToBuffer(image)});
                    callbackInfo.cancel();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/LoadingScreenRenderer;resetProgressAndMessage(Ljava/lang/String;)V"))
    public void loadWorld(LoadingScreenRenderer loadingScreenRenderer, String string) {
    }

    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/LoadingScreenRenderer;displayLoadingString(Ljava/lang/String;)V"))
    public void loadWorld1(LoadingScreenRenderer loadingScreenRenderer, String string) {
    }

    @Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V", remap = false))
    public void loadWorld2() {
    }

    @Inject(method = "toggleFullscreen()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setFullscreen(Z)V", shift = At.Shift.AFTER, remap = false), require = 1, allow = 1)
    private void toggleFullscreen(CallbackInfo callbackInfo) {
        if (!this.fullscreen) {
            Display.setResizable(false);
            Display.setResizable(true);
        }
    }

    @ModifyConstant(method = "getLimitFramerate", constant = @Constant(intValue = 30))
    public int getLimitFramerate(int constant) {
        return 1000;
    }
}
