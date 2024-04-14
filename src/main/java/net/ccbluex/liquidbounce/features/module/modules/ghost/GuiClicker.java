package net.ccbluex.liquidbounce.features.module.modules.ghost;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.InvocationTargetException;

@ModuleInfo(name = "GuiClicker",  category = ModuleCategory.GHOST)
public class GuiClicker extends Module {
    private final IntegerValue delayValue = new IntegerValue("Delay", 5, 0, 10);
    int mouseDown;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!Mouse.isButtonDown(0) || !Keyboard.isKeyDown(54) && !Keyboard.isKeyDown(42)) {
            mouseDown = 0;
            return;
        }
        mouseDown++;
        inInvClick(mc.currentScreen);
    }
    private void inInvClick(GuiScreen guiScreen) {

        int mouseInGUIPosX = Mouse.getX() * guiScreen.width / mc.displayWidth;
        int mouseInGUIPosY = guiScreen.height - Mouse.getY() * guiScreen.height / mc.displayHeight - 1;

        try {
            if (mouseDown >= delayValue.get()) {
            ReflectionHelper.findMethod(
                    GuiScreen.class,
                    null,
                    new String[]{
                            "func_73864_a",
                            "mouseClicked"
                    },
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE
            ).invoke(guiScreen, mouseInGUIPosX, mouseInGUIPosY, 0);
            mouseDown = 0;
        }
        } catch (IllegalAccessException | InvocationTargetException ignored) {}

    }
}
