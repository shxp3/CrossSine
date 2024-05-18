package net.ccbluex.liquidbounce.ui.client.gui;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.visual.CustomClientColor;
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.*;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.astolfo.AstolfoClickGui;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.NewUi;
import net.ccbluex.liquidbounce.ui.client.gui.options.modernuiLaunchOption;
import net.ccbluex.liquidbounce.features.value.BoolValue;
import net.ccbluex.liquidbounce.features.value.FloatValue;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import net.ccbluex.liquidbounce.features.value.ListValue;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import org.lwjgl.input.Keyboard;

import java.awt.*;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.VISUAL, keyBind = Keyboard.KEY_RSHIFT, canEnable = false)
public class ClickGUIModule extends Module {
    public ListValue styleValue = new ListValue("Style", new String[]{"LB+", "LiquidBounce", "Astolfo"}, "LB+") {
        @Override
        protected void onChanged(final String oldValue, final String newValue) {
            updateStyle();
        }
    };


    public final FloatValue scaleValue = new FloatValue("Scale", 0.70F, 0.7F, 2F);
    public final IntegerValue maxElementsValue = new IntegerValue("MaxElements", 15, 1, 20);
    public final FloatValue scroll = new FloatValue("Scroll", 20f, 0f, 200f);
    public final ListValue animationValue = new ListValue("Animation", new String[]{"Bread", "Slide", "LiquidBounce", "Zoom", "Ziul", "None"}, "Ziul");
    public static final BoolValue fastRenderValue = new BoolValue("FastRender", false);

    public static Color generateColor() {
        if (CustomClientColor.INSTANCE.getState()) {
            return CustomClientColor.INSTANCE.getColor();
        } else {
            return ClientTheme.INSTANCE.getColor(1);
        }
    }

    @Override
    public void onEnable() {
        if (styleValue.get().equalsIgnoreCase("Astolfo")) {
            mc.displayGuiScreen(AstolfoClickGui.Companion.getInstance());
        } else if (styleValue.get().equalsIgnoreCase("LB+")) {
            mc.displayGuiScreen(NewUi.Companion.getInstance());
            this.setState(false);
        }  else {
            updateStyle();
            mc.displayGuiScreen(modernuiLaunchOption.clickGui);
        }

    }

    private void updateStyle() {
        modernuiLaunchOption.clickGui.style = new LiquidBounceStyle();
    }

    @EventTarget(ignoreCondition = true)
    public void onPacket(final PacketEvent event) {
        final Packet packet = event.getPacket();

        if (packet instanceof S2EPacketCloseWindow && mc.currentScreen instanceof ClickGui) {
            event.cancelEvent();
        }
    }
}
