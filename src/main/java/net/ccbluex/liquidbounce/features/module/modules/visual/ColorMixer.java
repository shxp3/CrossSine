package net.ccbluex.liquidbounce.features.module.modules.visual;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.render.BlendUtils;
import net.ccbluex.liquidbounce.features.value.IntegerValue;
import java.awt.Color;

import java.lang.reflect.Field;

@ModuleInfo(name = "ColorMixer", category = ModuleCategory.VISUAL, canEnable = false)
public class ColorMixer extends Module {

    private static float[] lastFraction = new float[]{};
    public static Color[] lastColors = new Color[]{};

    public final IntegerValue blendAmount = new IntegerValue("Mixer-Amount", 2, 2, 10);
    /*
    @Override
    public void onInitialize() {
        regenerateColors();
    }
    */
    public final ColorElement col1RedValue = new ColorElement(1, ColorElement.Material.RED);
    public final ColorElement col1GreenValue = new ColorElement(1, ColorElement.Material.GREEN);
    public final ColorElement col1BlueValue = new ColorElement(1, ColorElement.Material.BLUE);

    public final ColorElement col2RedValue = (ColorElement) new ColorElement(2, ColorElement.Material.RED).displayable(() -> blendAmount.getValue() > 1);
    public final ColorElement col2GreenValue = (ColorElement) new ColorElement(2, ColorElement.Material.GREEN).displayable(() -> blendAmount.getValue() > 1);
    public final ColorElement col2BlueValue = (ColorElement) new ColorElement(2, ColorElement.Material.BLUE).displayable(() -> blendAmount.getValue() > 1);

    public final ColorElement col3RedValue = (ColorElement) new ColorElement(3, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 2);
    public final ColorElement col3GreenValue = (ColorElement) new ColorElement(3, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 2);
    public final ColorElement col3BlueValue = (ColorElement) new ColorElement(3, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 2);

    public final ColorElement col4RedValue = (ColorElement) (ColorElement) new ColorElement(4, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 3);
    public final ColorElement col4GreenValue = (ColorElement) (ColorElement) new ColorElement(4, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 3);
    public final ColorElement col4BlueValue = (ColorElement) (ColorElement) new ColorElement(4, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 3);

    public final ColorElement col5RedValue = (ColorElement) (ColorElement) new ColorElement(5, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 4);
    public final
    ColorElement col5GreenValue = (ColorElement) new ColorElement(5, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 4);
    public final ColorElement col5BlueValue = (ColorElement) (ColorElement) new ColorElement(5, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 4);

    public final ColorElement col6RedValue = (ColorElement) (ColorElement) new ColorElement(6, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 5);
    public final ColorElement col6GreenValue = (ColorElement) (ColorElement) new ColorElement(6, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 5);
    public final ColorElement col6BlueValue = (ColorElement) (ColorElement) new ColorElement(6, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 5);

    public final ColorElement col7RedValue = (ColorElement) (ColorElement) new ColorElement(7, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 6);
    public final ColorElement col7GreenValue = (ColorElement) (ColorElement) new ColorElement(7, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 6);
    public final ColorElement col7BlueValue = (ColorElement) (ColorElement) new ColorElement(7, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 6);

    public final ColorElement col8RedValue = (ColorElement) (ColorElement) new ColorElement(8, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 7);
    public final ColorElement col8GreenValue = (ColorElement) new ColorElement(8, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 7);
    public final ColorElement col8BlueValue = (ColorElement) (ColorElement) new ColorElement(8, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 7);

    public final ColorElement col9RedValue = (ColorElement) (ColorElement) new ColorElement(9, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 8);
    public final ColorElement col9GreenValue = (ColorElement) (ColorElement) new ColorElement(9, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 8);
    public final ColorElement col9BlueValue = (ColorElement) (ColorElement) new ColorElement(9, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 8);

    public final ColorElement col10RedValue = (ColorElement) (ColorElement) (ColorElement) new ColorElement(10, ColorElement.Material.RED, blendAmount).displayable(() -> blendAmount.getValue() > 7);
    public final ColorElement col10GreenValue = (ColorElement) new ColorElement(10, ColorElement.Material.GREEN, blendAmount).displayable(() -> blendAmount.getValue() > 7);
    public final ColorElement col10BlueValue = (ColorElement) new ColorElement(10, ColorElement.Material.BLUE, blendAmount).displayable(() -> blendAmount.getValue() > 7);

    public static Color getMixedColor(int index, int seconds) {
        final ColorMixer colMixer = (ColorMixer) CrossSine.moduleManager.getModule(ColorMixer.class);
        if (colMixer == null) return Color.white;

        if (lastColors.length <= 0 || lastFraction.length <= 0) regenerateColors(true); // just to make sure it won't go white

        return BlendUtils.blendColors(lastFraction, lastColors, (System.currentTimeMillis() + index) % (seconds * 1000) / (float) (seconds * 1000));
    }

    public static void regenerateColors(boolean forceValue) {
        final ColorMixer colMixer = (ColorMixer) CrossSine.moduleManager.getModule(ColorMixer.class);

        if (colMixer == null) return;

        // color generation
        if (forceValue || lastColors.length <= 0 || lastColors.length != (colMixer.blendAmount.get() * 2) - 1) {
            Color[] generator = new Color[(colMixer.blendAmount.get() * 2) - 1];

            // reflection is cool
            for (int i = 1; i <= colMixer.blendAmount.get(); i++) {
                Color result = Color.white;
                try {
                    Field red = ColorMixer.class.getField("col"+i+"RedValue");
                    Field green = ColorMixer.class.getField("col"+i+"GreenValue");
                    Field blue = ColorMixer.class.getField("col"+i+"BlueValue");

                    int r = ((ColorElement)red.get(colMixer)).get();
                    int g = ((ColorElement)green.get(colMixer)).get();
                    int b = ((ColorElement)blue.get(colMixer)).get();

                    result = new Color(Math.max(0, Math.min(r, 255)), Math.max(0, Math.min(g, 255)), Math.max(0, Math.min(b, 255)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                generator[i - 1] = result;
            }

            int h = colMixer.blendAmount.get();
            for (int z = colMixer.blendAmount.get() - 2; z >= 0; z--) {
                generator[h] = generator[z];
                h++;
            }

            lastColors = generator;
        }

        // cache thingy
        if (forceValue || lastFraction.length <= 0 || lastFraction.length != (colMixer.blendAmount.get() * 2) - 1) {
            // color frac regenerate if necessary
            float[] colorFraction = new float[(colMixer.blendAmount.get() * 2) - 1];

            for (int i = 0; i <= (colMixer.blendAmount.get() * 2) - 2; i++)
            {
                colorFraction[i] = (float)i / (float)((colMixer.blendAmount.get() * 2) - 2);
            }

            lastFraction = colorFraction;
        }
    }

}