package net.ccbluex.liquidbounce.ui.client.gui;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.ClickGui;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.*;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.dropdown.DropdownGUI;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.otcV2.OtcClickGUi;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.tenacity.TenacityClickGUI;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.newVer.NewUi;
import net.ccbluex.liquidbounce.ui.client.gui.options.modernuiLaunchOption;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
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
    public ListValue styleValue = new ListValue("Style", new String[]{"LB+", "LiquidBounce", "Tenacity5", "Null", "Slowly", "OneTap"}, "LB+") {
        @Override
        protected void onChanged(final String oldValue, final String newValue) {
            updateStyle();
        }
    };


    public static final BoolValue backback = new BoolValue("Background Accent", true);

    public static final ListValue scrollMode = new ListValue("Scroll Mode", new String[]{"Screen Height", "Value"}, "Value");

    public static final ListValue colormode = new ListValue("Setting Accent", new String[]{"White", "Color"}, "Color");
    public static final IntegerValue clickHeight = new IntegerValue("Tab Height", 250, 100, 500);
    public final FloatValue scaleValue = new FloatValue("Scale", 0.70F, 0.7F, 2F);
    public final IntegerValue maxElementsValue = new IntegerValue("MaxElements", 15, 1, 20);
    public final ListValue backgroundValue = new ListValue("Background", new String[]{"Default", "Gradient", "None"}, "None");
    public final BoolValue disp = new BoolValue("DisplayValue", false);
    public final BoolValue nao = new BoolValue("Nao", false);
    public final ListValue animationValue = new ListValue("Animation", new String[]{"Bread", "Slide", "LiquidBounce", "Zoom", "Ziul", "None"}, "Ziul");
    public static final BoolValue colorRainbow = new BoolValue("Rainbow", false);
    public static final IntegerValue colorRedValue = (IntegerValue) new IntegerValue("R", 0, 0, 255).displayable(() -> !colorRainbow.get());
    public static final IntegerValue colorGreenValue = (IntegerValue) new IntegerValue("G", 160, 0, 255).displayable(() -> !colorRainbow.get());
    public static final IntegerValue colorBlueValue = (IntegerValue) new IntegerValue("B", 255, 0, 255).displayable(() -> !colorRainbow.get());
    public static final BoolValue fastRenderValue = new BoolValue("FastRender", false);
    public final BoolValue getClosePrevious = (BoolValue) new BoolValue("ClosePrevious", false);


    public static Color generateColor() {
        return colorRainbow.get() ? ColorUtils.INSTANCE.rainbow() : new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get());
    }

    @Override
    public void onEnable() {

        if (styleValue.get().equalsIgnoreCase("Classic")) {
            mc.displayGuiScreen(new DropdownGUI());
        } else if (styleValue.get().equalsIgnoreCase("Tenacity")) {
            mc.displayGuiScreen(new TenacityClickGUI());
        } else if (styleValue.get().equalsIgnoreCase("LB+")) {
            mc.displayGuiScreen(NewUi.getInstance());
            this.setState(false);
        } else if (styleValue.get().equalsIgnoreCase("Onetap")) {
            mc.displayGuiScreen(new OtcClickGUi());
            this.setState(false);
        } else {
            updateStyle();
            mc.displayGuiScreen(modernuiLaunchOption.clickGui);
        }

    }

    private void updateStyle() {
        switch (styleValue.get().toLowerCase()) {
            case "liquidbounce":
                modernuiLaunchOption.clickGui.style = new LiquidBounceStyle();
                break;
            case "null":
                modernuiLaunchOption.clickGui.style = new NullStyle();
                break;
            case "slowly":
                modernuiLaunchOption.clickGui.style = new SlowlyStyle();
                break;
            case "tenacity5":
                modernuiLaunchOption.clickGui.style = new TenacityStyle();
                break;
        }
    }

    @EventTarget(ignoreCondition = true)
    public void onPacket(final PacketEvent event) {
        final Packet packet = event.getPacket();

        if (packet instanceof S2EPacketCloseWindow && mc.currentScreen instanceof ClickGui) {
            event.cancelEvent();
        }
    }
}
