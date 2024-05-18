package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.TickBase;
import net.ccbluex.liquidbounce.features.module.modules.visual.FreeLook;
import net.ccbluex.liquidbounce.features.module.modules.visual.RenderRotation;
import net.ccbluex.liquidbounce.features.module.modules.world.FPSBoost;
import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.injection.forge.mixins.accessors.MinecraftForgeClientAccessor;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.SpoofItemUtils;
import net.ccbluex.liquidbounce.utils.render.ImageUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.stream.IStream;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.*;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.FutureTask;

import static org.objectweb.asm.Opcodes.PUTFIELD;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    private Entity renderViewEntity;

    @Shadow
    private boolean fullscreen;

    @Shadow
    public boolean skipRenderWorld;
    @Shadow
    protected abstract void runTick();
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

    @Shadow public EntityRenderer entityRenderer;

    @Shadow
    public PlayerControllerMP playerController;

    @Shadow
    public int displayWidth;

    @Shadow
    public int displayHeight;

    @Shadow
    public int rightClickDelayTimer;

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    private Profiler mcProfiler;

    @Shadow
    private boolean isGamePaused;

    @Shadow
    @Final
    public Timer timer;

    @Shadow
    private void rightClickMouse() {}

    @Shadow
    private void clickMouse() {}

    @Shadow
    private void middleClickMouse() {}

    @Shadow
    public long startNanoTime;

    @Shadow
    public boolean inGameHasFocus;

    @Shadow
    public abstract IResourceManager getResourceManager();

    @Shadow
    private PlayerUsageSnooper usageSnooper;

    @Shadow
    private Queue<FutureTask<?>> scheduledTasks;

    @Shadow
    public abstract void shutdown();

    @Shadow
    public GuiAchievement guiAchievement;

    @Shadow
    public int fpsCounter;

    @Shadow
    public long prevFrameTime;

    @Shadow
    private Framebuffer framebufferMc;

    @Shadow
    public abstract void checkGLError(String message);

    @Shadow
    public long debugUpdateTime;

    @Shadow
    private IStream stream;

    @Shadow
    @Final
    public FrameTimer frameTimer;

    @Shadow
    public String debug;

    @Shadow
    private IntegratedServer theIntegratedServer;

    @Shadow
    public abstract boolean isFramerateLimitBelowMax();

    @Shadow
    private static int debugFPS;

    @Shadow
    public abstract void updateDisplay();

    @Shadow
    public abstract boolean isSingleplayer();

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    private void displayDebugInfo(long elapsedTicksTime) {}

    @Shadow
    private long debugCrashKeyPressTime;

    @Shadow
    public GuiIngame ingameGUI;

    @Shadow
    public TextureManager renderEngine;

    @Shadow
    public abstract void refreshResources();

    @Shadow
    private int joinPlayerCounter;

    @Shadow
    public abstract void dispatchKeypresses();

    @Shadow
    public RenderGlobal renderGlobal;

    @Shadow
    private RenderManager renderManager;

    @Shadow
    private NetworkManager myNetworkManager;

    @Shadow
    public long systemTime;

    @Shadow
    public abstract Entity getRenderViewEntity();

    @Shadow
    private SoundHandler mcSoundHandler;

    @Shadow
    private MusicTicker mcMusicTicker;

    @Shadow
    public abstract NetHandlerPlayClient getNetHandler();

    @Shadow
    public abstract void setIngameFocus();

    @Shadow
    private void updateDebugProfilerName(int p_updateDebugProfilerName_1_) {}

    @Shadow
    public abstract void displayInGameMenu();

    @Shadow
    public void displayGuiScreen(GuiScreen p_displayGuiScreen_1_) {}

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
        if (!CrossSine.INSTANCE.getDestruced()) {
            if (currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))) {
                currentScreen = CrossSine.mainMenu;

                ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
                currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
                skipRenderWorld = false;
            }

            CrossSine.eventManager.callEvent(new ScreenEvent(currentScreen));
        }
    }
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }
    private long lastFrame = getTime();

    @Overwrite
    private void runGameLoop() throws IOException {
        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;

        RenderUtils.deltaTime = deltaTime;

        long i = System.nanoTime();
        this.mcProfiler.startSection("root");

        if (Display.isCreated() && Display.isCloseRequested())
        {
            this.shutdown();
        }

        if (this.isGamePaused && this.theWorld != null)
        {
            float f = this.timer.renderPartialTicks;
            this.timer.updateTimer();
            this.timer.renderPartialTicks = f;
        }
        else
        {
            this.timer.updateTimer();
        }

        this.mcProfiler.startSection("scheduledExecutables");

        synchronized (this.scheduledTasks)
        {
            while (!this.scheduledTasks.isEmpty())
            {
                Util.runTask((FutureTask)this.scheduledTasks.poll(), logger);
            }
        }

        this.mcProfiler.endSection();
        long l = System.nanoTime();
        this.mcProfiler.startSection("tick");

        for (int j = 0; j < this.timer.elapsedTicks; ++j)
        {
            if(Minecraft.getMinecraft().thePlayer != null) {
                boolean skip = false;

                if(j == 0) {
                    TickBase tickBase = CrossSine.moduleManager.getModule(TickBase.class);

                    if(tickBase.getState()) {
                        int extraTicks = tickBase.getExtraTicks();

                        if(extraTicks == -1) {
                            skip = true;
                        } else {
                            if(extraTicks > 0) {
                                for(int aa = 0; aa < extraTicks; aa++) {
                                    this.runTick();
                                }

                                tickBase.setFreezing(true);
                            }
                        }
                    }
                }

                if(!skip) {
                    this.runTick();
                }
            } else {
                this.runTick();
            }
        }

        this.mcProfiler.endStartSection("preRenderErrors");
        long i1 = System.nanoTime() - l;
        this.checkGLError("Pre render");
        this.mcProfiler.endStartSection("sound");
        this.mcSoundHandler.setListener(this.thePlayer, this.timer.renderPartialTicks);
        this.mcProfiler.endSection();
        this.mcProfiler.startSection("render");
        GlStateManager.pushMatrix();
        GlStateManager.clear(16640);
        this.framebufferMc.bindFramebuffer(true);
        this.mcProfiler.startSection("display");
        GlStateManager.enableTexture2D();

        if (this.thePlayer != null && this.thePlayer.isEntityInsideOpaqueBlock())
        {
            this.gameSettings.thirdPersonView = 0;
        }

        this.mcProfiler.endSection();

        if (!this.skipRenderWorld)
        {
            this.mcProfiler.endStartSection("gameRenderer");
            this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks, i);
            this.mcProfiler.endSection();
            FMLCommonHandler.instance().onRenderTickEnd(this.timer.renderPartialTicks);
        }

        this.mcProfiler.endSection();

        if (this.gameSettings.showDebugInfo && this.gameSettings.showDebugProfilerChart && !this.gameSettings.hideGUI)
        {
            if (!this.mcProfiler.profilingEnabled)
            {
                this.mcProfiler.clearProfiling();
            }

            this.mcProfiler.profilingEnabled = true;
            this.displayDebugInfo(i1);
        }
        else
        {
            this.mcProfiler.profilingEnabled = false;
            this.prevFrameTime = System.nanoTime();
        }

        this.guiAchievement.updateAchievementWindow();
        this.framebufferMc.unbindFramebuffer();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.framebufferMc.framebufferRender(this.displayWidth, this.displayHeight);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.entityRenderer.renderStreamIndicator(this.timer.renderPartialTicks);
        GlStateManager.popMatrix();
        this.mcProfiler.startSection("root");
        this.updateDisplay();
        Thread.yield();
        this.mcProfiler.startSection("stream");
        this.mcProfiler.startSection("update");
        this.mcProfiler.endStartSection("submit");
        this.mcProfiler.endSection();
        this.mcProfiler.endSection();
        this.checkGLError("Post render");
        ++this.fpsCounter;
        this.isGamePaused = this.isSingleplayer() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame() && !this.theIntegratedServer.getPublic();
        long k = System.nanoTime();
        this.frameTimer.addFrame(k - this.startNanoTime);
        this.startNanoTime = k;

        while (Minecraft.getSystemTime() >= this.debugUpdateTime + 1000L)
        {
            this.debugFPS = this.fpsCounter;
            this.debug = String.format("%d fps (%d chunk update%s) T: %s%s%s%s%s", new Object[] {Integer.valueOf(debugFPS), Integer.valueOf(RenderChunk.renderChunksUpdated), RenderChunk.renderChunksUpdated != 1 ? "s" : "", (float)this.gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : Integer.valueOf(this.gameSettings.limitFramerate), this.gameSettings.enableVsync ? " vsync" : "", this.gameSettings.fancyGraphics ? "" : " fast", this.gameSettings.clouds == 0 ? "" : (this.gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"), OpenGlHelper.useVbo() ? " vbo" : ""});
            RenderChunk.renderChunksUpdated = 0;
            this.debugUpdateTime += 1000L;
            this.fpsCounter = 0;
            this.usageSnooper.addMemoryStatsToSnooper();

            if (!this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.startSnooper();
            }
        }

        if (this.isFramerateLimitBelowMax())
        {
            this.mcProfiler.startSection("fpslimit_wait");
            Display.sync(this.getLimitFramerate());
            this.mcProfiler.endSection();
        }

        this.mcProfiler.endSection();
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
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z", ordinal = 0))
    private void changeItem(CallbackInfo info) {

        for(int k = 0; k < 9; ++k) {
            if (this.gameSettings.keyBindsHotbar[k].isPressed()) {
                if (this.thePlayer.isSpectator()) {
                    this.ingameGUI.getSpectatorGui().func_175260_a(k);
                } else {
                    if(SpoofItemUtils.INSTANCE.getSpoofing()) {
                        SpoofItemUtils.INSTANCE.setSlot(k);
                    } else {
                        this.thePlayer.inventory.currentItem = k;
                    }
                }
            }
        }
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = At.Shift.BEFORE))
    private void onTick(final CallbackInfo callbackInfo) {
        CrossSine.eventManager.callEvent(new TickEvent());
    }

    private static final String TARGET = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V";
    @Redirect(method="runTick", at=@At(value="INVOKE", target=TARGET))
    public void runTick_setKeyBindState(int keybind, boolean state) {
        leftClickCounter = 0;

        KeyBinding.setKeyBindState(keybind, state);
    }
    @Inject(method = "dispatchKeypresses", at = @At(value = "HEAD"))
    private void onKey(CallbackInfo callbackInfo) {
        final KeyBindEvent event = new KeyBindEvent();
        try {
            if (Keyboard.getEventKeyState() && (currentScreen == null))
                CrossSine.eventManager.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        try {
            if (Keyboard.getEventKeyState()) {
                event.setKey(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey());
                CrossSine.eventManager.callEvent(event);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo callbackInfo) {
        CrossSine.INSTANCE.stopClient();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void clickMouse(CallbackInfo callbackInfo) {
        CrossSine.eventManager.callEvent(new ClickEvent());
        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT);
        leftClickCounter = 0;
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

    @Inject(method = "toggleFullscreen()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setFullscreen(Z)V", shift = At.Shift.AFTER, remap = false), require = 1, allow = 1)
    private void toggleFullscreen(CallbackInfo callbackInfo) {
        if (!this.fullscreen) {
            Display.setResizable(false);
            Display.setResizable(true);
        }
    }

    @Overwrite
    public int getLimitFramerate() {
        return this.theWorld == null && this.currentScreen != null && FPSBoost.INSTANCE.getCpuFix().get() ? 5 : this.gameSettings.limitFramerate;
    }
}
