

package net.ccbluex.liquidbounce.font;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayList;

public abstract class FontLoaders {
    public static final CFontRenderer F16 = new CFontRenderer(getFont(16), true, true);
    public static final CFontRenderer F18 = new CFontRenderer(getFont(18), true, true);
    public static final CFontRenderer F24 = new CFontRenderer(getFont(24), true, true);
    public static final CFontRenderer SF16 = new CFontRenderer(getSF(16), true, true);
    public static final CFontRenderer SF18 = new CFontRenderer(getSF(18), true, true);
    public static final CFontRenderer SF24 = new CFontRenderer(getSF(24), true, true);
    public static final CFontRenderer SF30 = new CFontRenderer(getSF(30), true, true);
    public static final CFontRenderer SF35 = new CFontRenderer(getSF(35), true, true);
    public static final ArrayList<CFontRenderer> fonts = new ArrayList<>();


    public static Font getFont(int size) {
        Font font;
        try {
            font = Font.createFont(0, Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("crosssine/font/Urbanist-Medium.ttf")).getInputStream()).deriveFont(Font.PLAIN, (float) size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("default", Font.PLAIN, size);
        }
        return font;
    }

    public static Font getSF(int size) {
        Font font;
        try {
            font = Font.createFont(0, Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("crosssine/font/SFSemiBold.ttf")).getInputStream()).deriveFont(Font.PLAIN, (float) size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("default", Font.PLAIN, size);
        }
        return font;
    }
}
