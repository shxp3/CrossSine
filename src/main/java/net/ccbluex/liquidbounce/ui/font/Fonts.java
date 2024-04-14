package net.ccbluex.liquidbounce.ui.font;

import com.google.gson.*;
import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Fonts {

    @FontDetails(fontName = "Light", fontSize = 32, fileName = "regular.ttf")
    public static GameFontRenderer font32;
    public static TTFFontRenderer fontVerdana;

    @FontDetails(fontName = "Roboto Medium", fontSize = 35)
    public static GameFontRenderer font35;

    @FontDetails(fontName = "Roboto Medium", fontSize = 40)
    public static GameFontRenderer font40;

    @FontDetails(fontName = "Roboto Medium", fontSize = 50)
    public static GameFontRenderer font50;


    @FontDetails(fontName = "Roboto Medium", fontSize = 30)
    public static GameFontRenderer fontSmall;

    @FontDetails(fontName = "Roboto Medium", fontSize = 24)
    public static GameFontRenderer fontTiny;

    @FontDetails(fontName = "Roboto Medium", fontSize = 52)
    public static GameFontRenderer fontLarge;

    @FontDetails(fontName = "SF", fontSize = 35)
    public static GameFontRenderer fontSFUI35;

    @FontDetails(fontName = "SF", fontSize = 40)
    public static GameFontRenderer fontSFUI40;

    public static TTFFontRenderer fontTahomaSmall;

    @FontDetails(fontName = "Bangers", fontSize = 45)
    public static GameFontRenderer fontBangers;

    @FontDetails(fontName = "SFUI35", fontSize = 18)
    public static GameFontRenderer SFUI35;

    @FontDetails(fontName = "Minecraft Font")
    public static final FontRenderer minecraftFont = Minecraft.getMinecraft().fontRendererObj;

    @FontDetails(fontName = "Tenacity35", fontSize = 35)
    public static GameFontRenderer fontTenacity35;

    @FontDetails(fontName = "TenacityBold35", fontSize = 35)
    public static GameFontRenderer fontTenacityBold35;

    @FontDetails(fontName = "tenacity40", fontSize = 40)
    public static GameFontRenderer fontTenacity40;

    @FontDetails(fontName = "tenacityBold40", fontSize = 40)
    public static GameFontRenderer fontTenacityBold40;

    @FontDetails(fontName = "TenacityIcon30", fontSize = 30)
    public static GameFontRenderer fontTenacityIcon30;

    @FontDetails(fontName = "Comfortaa35", fontSize = 35)
    public static GameFontRenderer fontComfortaa35;

    @FontDetails(fontName = "Comfortaa40", fontSize = 40)
    public static GameFontRenderer fontComfortaa40;

    @FontDetails(fontName = "RockoFLFBold35", fontSize = 35)
    public static GameFontRenderer fontRockoFLF35;

    @FontDetails(fontName = "RockoFLFBold40", fontSize = 40)
    public static GameFontRenderer fontRockoFLF40;

    @FontDetails(fontName = "Nunito24", fontSize = 24)
    public static GameFontRenderer Nunito24;

    @FontDetails(fontName = "Nunito30", fontSize = 30)
    public static GameFontRenderer Nunito30;

    @FontDetails(fontName = "Nunito35", fontSize = 35)
    public static GameFontRenderer Nunito35;

    @FontDetails(fontName = "Nunito40", fontSize = 40)
    public static GameFontRenderer Nunito40;

    @FontDetails(fontName = "Nunito50", fontSize = 50)
    public static GameFontRenderer Nunito50;

    @FontDetails(fontName = "Nunito60", fontSize = 60)
    public static GameFontRenderer Nunito60;

    @FontDetails(fontName = "SFApple24", fontSize = 24)
    public static GameFontRenderer SFApple24;
    @FontDetails(fontName = "SFApple30", fontSize = 30)
    public static GameFontRenderer SFApple30;
    @FontDetails(fontName = "SFApple35", fontSize = 35)
    public static GameFontRenderer SFApple35;
    @FontDetails(fontName = "SFApple40", fontSize = 40)
    public static GameFontRenderer SFApple40;
    @FontDetails(fontName = "SFApple50", fontSize = 50)
    public static GameFontRenderer SFApple50;

    @FontDetails(fontName = "Nova24", fontSize = 24)
    public static GameFontRenderer Nova24;
    @FontDetails(fontName = "Nova30", fontSize = 30)
    public static GameFontRenderer Nova30;
    @FontDetails(fontName = "Nova35", fontSize = 35)
    public static GameFontRenderer Nova35;
    @FontDetails(fontName = "Nova40", fontSize = 40)
    public static GameFontRenderer Nova40;
    @FontDetails(fontName = "Nova50", fontSize = 50)
    public static GameFontRenderer Nova50;



    private static final List<GameFontRenderer> CUSTOM_FONT_RENDERERS = new ArrayList<>();

    public static void loadFonts() {
        long l = System.currentTimeMillis();

        ClientUtils.INSTANCE.logInfo("Loading Fonts.");

        font35 = new GameFontRenderer(getFont("Roboto-Medium.ttf", 35));
        font40 = new GameFontRenderer(getFont("Roboto-Medium.ttf", 40));
        font50 = new GameFontRenderer(getFont("Roboto-Medium.ttf", 50));
        fontSmall = new GameFontRenderer(getFont("Roboto-Medium.ttf", 30));
        fontTiny = new GameFontRenderer(getFont("Roboto-Medium.ttf", 24));
        fontLarge = new GameFontRenderer(getFont("Roboto-Medium.ttf", 60));
        fontSFUI35 = new GameFontRenderer(getFont("SF.ttf", 35));
        fontSFUI40 = new GameFontRenderer(getFont("SF.ttf", 40));
        SFUI35 = new GameFontRenderer(getFont("SF.ttf", 18));
        fontSFUI35 = new GameFontRenderer(getFont("SF.ttf", 35));
        fontSFUI40 = new GameFontRenderer(getFont("SF.ttf", 40));
        fontTahomaSmall = new TTFFontRenderer(getFont("Tahoma.ttf", 11));
        fontVerdana = new TTFFontRenderer(getFont("Verdana.ttf", 7));
        // fonts above here may not work as this is a test
        fontBangers = new GameFontRenderer(getFontcustom(45, "Bangers"));
        fontTenacity35 = new GameFontRenderer(getFontcustom(35, "tenacity"));
        fontTenacityBold35 = new GameFontRenderer(getFontcustom(35, "tenacity-bold"));
        fontTenacityIcon30 = new GameFontRenderer(getFontcustom(30, "Tenacityicon"));
        fontTenacity40 = new GameFontRenderer(getFontcustom(40,"tenacity"));
        fontTenacityBold40 = new GameFontRenderer(getFontcustom(40,"tenacity-bold"));
        fontComfortaa35 = new GameFontRenderer(getFontcustom(35, "Comfortaa"));
        fontComfortaa40 = new GameFontRenderer(getFontcustom(40, "Comfortaa"));
        fontRockoFLF35 = new GameFontRenderer(getFontcustom(35, "RockoFLF-Bold"));
        fontRockoFLF40 = new GameFontRenderer(getFontcustom(40, "RockoFLF-Bold"));
        Nunito24 = new GameFontRenderer(getFontcustom(24, "Nunito"));
        Nunito30 = new GameFontRenderer(getFontcustom(30, "Nunito"));
        Nunito35 = new GameFontRenderer(getFontcustom(35, "Nunito"));
        Nunito40 = new GameFontRenderer(getFontcustom(40, "Nunito"));
        Nunito50 = new GameFontRenderer(getFontcustom(50, "Nunito"));
        Nunito60 = new GameFontRenderer(getFontcustom(60, "Nunito"));
        SFApple40 = new GameFontRenderer(getFontcustom(40, "SFApple"));
        SFApple30 = new GameFontRenderer(getFontcustom(30, "SFApple"));
        SFApple35 = new GameFontRenderer(getFontcustom(35, "SFApple"));
        SFApple50 = new GameFontRenderer(getFontcustom(50, "SFApple"));
        SFApple24 = new GameFontRenderer(getFontcustom(24, "SFApple"));
        Nova40 = new GameFontRenderer(getFontcustom(40, "ProximaNovaLight"));
        Nova30 = new GameFontRenderer(getFontcustom(30, "ProximaNovaLight"));
        Nova35 = new GameFontRenderer(getFontcustom(35, "ProximaNovaLight"));
        Nova50 = new GameFontRenderer(getFontcustom(50, "ProximaNovaLight"));
        Nova24 = new GameFontRenderer(getFontcustom(24, "ProximaNovaLight"));


        getCustomFonts();

        initFonts();

        for(final Field field : Fonts.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                final FontDetails fontDetails = field.getAnnotation(FontDetails.class);

                if(fontDetails!=null) {
                    if(!fontDetails.fileName().isEmpty())
                        field.set(null,new GameFontRenderer(getFont(fontDetails.fileName(), fontDetails.fontSize())));
                }
            }catch(final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        try {
            CUSTOM_FONT_RENDERERS.clear();

            final File fontsFile = new File(CrossSine.fileManager.getFontsDir(), "fonts.json");

            if(fontsFile.exists()) {
                final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(fontsFile)));

                if(jsonElement instanceof JsonNull)
                    return;

                final JsonArray jsonArray = (JsonArray) jsonElement;

                for(final JsonElement element : jsonArray) {
                    if(element instanceof JsonNull)
                        return;

                    final JsonObject fontObject = (JsonObject) element;

                    CUSTOM_FONT_RENDERERS.add(new GameFontRenderer(getFont(fontObject.get("fontFile").getAsString(), fontObject.get("fontSize").getAsInt())));
                }
            }else{
                fontsFile.createNewFile();

                final PrintWriter printWriter = new PrintWriter(new FileWriter(fontsFile));
                printWriter.println(new GsonBuilder().setPrettyPrinting().create().toJson(new JsonArray()));
                printWriter.close();
            }
        }catch(final Exception e) {
            e.printStackTrace();
        }

        ClientUtils.INSTANCE.logInfo("Loaded Fonts. (" + (System.currentTimeMillis() - l) + "ms)");
    }

    private static void initFonts() {
        try {
            initSingleFont("regular.ttf", "assets/minecraft/crosssine/font/regular.ttf");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static Font getFontcustom(int size,String fontname) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/"+fontname+".ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("default", 0, size);
        }
        return font;
    }
    private static Font getFont1(int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/icon.ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("default", 0, size);
        }
        return font;
    }
    private static Font getFont2(int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/regular.ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("defualt", 0, size);
        }
        return font;
    }
    private static Font getFont3(int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/SFBOLD.ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("SFBold", 0, size);
        }
        return font;
    }
    private static Font getFont4(int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/tenacity.ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("SFBold", 0, size);
        }
        return font;
    }
    private static Font getFont5(int size) {
        Font font;
        try {
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("crosssine/font/tenacity-bold.ttf")).getInputStream();
            font = Font.createFont(0, is);
            font = font.deriveFont(0, size);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error loading font");
            font = new Font("SFBold", 0, size);
        }
        return font;
    }

    private static void initSingleFont(String name, String resourcePath) throws IOException {
        File file=new File(CrossSine.fileManager.getFontsDir(), name);
        if(!file.exists())
            FileUtils.INSTANCE.unpackFile(file, resourcePath);
    }

    public static FontRenderer getFontRenderer(final String name, final int size) {
        for (final Field field : Fonts.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);

                final Object o = field.get(null);

                if (o instanceof FontRenderer) {
                    final FontDetails fontDetails = field.getAnnotation(FontDetails.class);

                    if (fontDetails.fontName().equals(name) && fontDetails.fontSize() == size)
                        return (FontRenderer) o;
                }
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (final GameFontRenderer liquidFontRenderer : CUSTOM_FONT_RENDERERS) {
            final Font font = liquidFontRenderer.getDefaultFont().getFont();

            if (font.getName().equals(name) && font.getSize() == size)
                return liquidFontRenderer;
        }

        return minecraftFont;
    }

    public static Object[] getFontDetails(final FontRenderer fontRenderer) {
        for (final Field field : Fonts.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);

                final Object o = field.get(null);

                if (o.equals(fontRenderer)) {
                    final FontDetails fontDetails = field.getAnnotation(FontDetails.class);

                    return new Object[] {fontDetails.fontName(), fontDetails.fontSize()};
                }
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (fontRenderer instanceof GameFontRenderer) {
            final Font font = ((GameFontRenderer) fontRenderer).getDefaultFont().getFont();

            return new Object[] {font.getName(), font.getSize()};
        }

        return null;
    }

    public static List<FontRenderer> getFonts() {
        final List<FontRenderer> fonts = new ArrayList<>();

        for(final Field fontField : Fonts.class.getDeclaredFields()) {
            try {
                fontField.setAccessible(true);

                final Object fontObj = fontField.get(null);

                if(fontObj instanceof FontRenderer) fonts.add((FontRenderer) fontObj);
            }catch(final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        fonts.addAll(Fonts.CUSTOM_FONT_RENDERERS);

        return fonts;
    }

    public static List<GameFontRenderer> getCustomFonts() {
        final List<GameFontRenderer> fonts = new ArrayList<>();

        for(final Field fontField : Fonts.class.getDeclaredFields()) {
            try {
                fontField.setAccessible(true);

                final Object fontObj = fontField.get(null);

                if(fontObj instanceof GameFontRenderer) fonts.add((GameFontRenderer) fontObj);
            }catch(final IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        fonts.addAll(Fonts.CUSTOM_FONT_RENDERERS);

        return fonts;
    }

    private static Font getFont(final String fontName, final int size) {
        try {
            final InputStream inputStream = new FileInputStream(new File(CrossSine.fileManager.getFontsDir(), fontName));
            Font awtClientFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            awtClientFont = awtClientFont.deriveFont(Font.PLAIN, size);
            inputStream.close();
            return awtClientFont;
        }catch(final Exception e) {
            e.printStackTrace();

            return new Font("default", Font.PLAIN, size);
        }
    }
}