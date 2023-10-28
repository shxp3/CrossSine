package net.ccbluex.liquidbounce.utils.render;

import net.ccbluex.liquidbounce.injection.access.StaticStorage;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.astolfo.geom.Rectangle;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MathUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.particles.Particle;
import net.ccbluex.liquidbounce.utils.particles.Vec3;
import net.ccbluex.liquidbounce.utils.render.glu.DirectTessCallback;
import net.ccbluex.liquidbounce.utils.render.glu.VertexData;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.Timer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.*;

import static akka.actor.Nobody.start;
import static akka.actor.Nobody.stop;
import static java.lang.Math.*;
import static net.ccbluex.liquidbounce.utils.render.AnimationUtil.getAnimationState;
import static net.minecraft.client.renderer.GlStateManager.disableBlend;
import static net.minecraft.client.renderer.GlStateManager.enableTexture2D;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public final class RenderUtils extends MinecraftInstance {
    private static final Map<String, Map<Integer, Boolean>> glCapMap = new HashMap<>();
    private static Framebuffer initialFB, frameBuffer, blackBuffer;
    private static ShaderGroup mainShader = null;
    private static float lastWidth = 0, lastHeight = 0, lastStrength = 0;
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation blurDirectory = new ResourceLocation("crosssine/shadow.json");

    public static int deltaTime;
    private static final int[] DISPLAY_LISTS_2D = new int[4];
    private static long startTime;
    private static int animationDuration = 500;


    public static void scaleStart(float x, float y, float scale) {
        glTranslatef(x, y, 0);
        glScalef(scale, scale, 1);
        glTranslatef(-x, -y, 0);
    }
    public static Vec3 to2D(double x, double y, double z) {
        FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, screenCoords);
        if (result) {
            return new Vec3(screenCoords.get(0), Display.getHeight() - screenCoords.get(1), screenCoords.get(2));
        }
        return null;
    }
    public static void drawTracerPointer(float x, float y, float size, float widthDiv, float heightDiv, int color) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glPushMatrix();
        glColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x - size / widthDiv, y + size);
        GL11.glVertex2d(x, y + size / heightDiv);
        GL11.glVertex2d(x + size / widthDiv, y + size);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
        GL11.glColor4f(0, 0, 0, 0.8f);
        GL11.glPopMatrix();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (!blend)
            GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
    public static void drawAxisAlignedBB(final AxisAlignedBB axisAlignedBB, final Color color) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glLineWidth(2F);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glColor(color);
        drawFilledBox(axisAlignedBB);
        GlStateManager.resetColor();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
    }
    static {
        for (int i = 0; i < DISPLAY_LISTS_2D.length; i++) {
            DISPLAY_LISTS_2D[i] = glGenLists(1);
        }

        glNewList(DISPLAY_LISTS_2D[0], GL_COMPILE);

        quickDrawRect(-7F, 2F, -4F, 3F);
        quickDrawRect(4F, 2F, 7F, 3F);
        quickDrawRect(-7F, 0.5F, -6F, 3F);
        quickDrawRect(6F, 0.5F, 7F, 3F);

        glEndList();

        glNewList(DISPLAY_LISTS_2D[1], GL_COMPILE);

        quickDrawRect(-7F, 3F, -4F, 3.3F);
        quickDrawRect(4F, 3F, 7F, 3.3F);
        quickDrawRect(-7.3F, 0.5F, -7F, 3.3F);
        quickDrawRect(7F, 0.5F, 7.3F, 3.3F);

        glEndList();

        glNewList(DISPLAY_LISTS_2D[2], GL_COMPILE);

        quickDrawRect(4F, -20F, 7F, -19F);
        quickDrawRect(-7F, -20F, -4F, -19F);
        quickDrawRect(6F, -20F, 7F, -17.5F);
        quickDrawRect(-7F, -20F, -6F, -17.5F);

        glEndList();

        glNewList(DISPLAY_LISTS_2D[3], GL_COMPILE);

        quickDrawRect(7F, -20F, 7.3F, -17.5F);
        quickDrawRect(-7.3F, -20F, -7F, -17.5F);
        quickDrawRect(4F, -20.3F, 7.3F, -20F);
        quickDrawRect(-7.3F, -20.3F, -4F, -20F);

        glEndList();
    }
    public static void drawClickGuiArrow(final float x, final float y, final float size, final AnimationUnit animation, final int color) {
        GL11.glTranslatef(x, y, 0.0f);
        final double[] interpolation = new double[1];
        setup2DRendering(() -> render(5, () -> {
            color(color);
            interpolation[0] = interpolate(0.0, size / 2.0, animation.getOutput());
            if (animation.getOutput() >= 0.48) {
                GL11.glVertex2d((size / 2.0f), interpolate(size / 2.0, 0.0, animation.getOutput()));
            }
            GL11.glVertex2d(0.0, interpolation[0]);
            if (animation.getOutput() < 0.48) {
                GL11.glVertex2d((size / 2.0f), interpolate(size / 2.0, 0.0, animation.getOutput()));
            }
            GL11.glVertex2d(size, interpolation[0]);
        }));
        GL11.glTranslatef(-x, -y, 0.0f);
    }
    public static Color getGradientOffset(Color color1, Color color2, double offset) {
        double inverse_percent;
        int redPart;
        if(offset > 1.0D) {
            inverse_percent = offset % 1.0D;
            redPart = (int)offset;
            offset = redPart % 2 == 0?inverse_percent:1.0D - inverse_percent;
        }
        inverse_percent = 1.0D - offset;
        redPart = (int)((double)color1.getRed() * inverse_percent + (double)color2.getRed() * offset);
        int greenPart = (int)((double)color1.getGreen() * inverse_percent + (double)color2.getGreen() * offset);
        int bluePart = (int)((double)color1.getBlue() * inverse_percent + (double)color2.getBlue() * offset);
        return new Color(redPart, greenPart, bluePart);
    }
    public static void drawRectBordered(double x, double y, double x1, double y1, double width, int internalColor, int borderColor) {
        rectangle(x + width, y + width, x1 - width, y1 - width, internalColor);
        rectangle(x + width, y, x1 - width, y + width, borderColor);
        rectangle(x, y, x + width, y1, borderColor);
        rectangle(x1 - width, y, x1, y1, borderColor);
        rectangle(x + width, y1 - width, x1 - width, y1, borderColor);
    }
    public static void connectPoints(float xOne, float yOne, float xTwo, float yTwo) {
        glPushMatrix();
        glEnable(GL_LINE_SMOOTH);
        glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glLineWidth(0.5F);
        glBegin(GL_LINES);
        glVertex2f(xOne, yOne);
        glVertex2f(xTwo, yTwo);
        glEnd();
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();
    }
    public static void drawGradientRect(final double left, final double top, final double right, final double bottom, final boolean sideways, final int startColor, final int endColor) {
        GL11.glDisable(3553);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glShadeModel(7425);
        GL11.glBegin(7);
        color(startColor);
        if (sideways) {
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, bottom);
            color(endColor);
            GL11.glVertex2d(right, bottom);
            GL11.glVertex2d(right, top);
        }
        else {
            GL11.glVertex2d(left, top);
            color(endColor);
            GL11.glVertex2d(left, bottom);
            GL11.glVertex2d(right, bottom);
            color(startColor);
            GL11.glVertex2d(right, top);
        }
        GL11.glEnd();
        GL11.glDisable(3042);
        GL11.glShadeModel(7424);
        GL11.glEnable(3553);
    }
    public static int darker(int hexColor, int factor) {
        float alpha = (float) (hexColor >> 24 & 255);
        float red = Math.max((float) (hexColor >> 16 & 255) - (float) (hexColor >> 16 & 255) / (100.0F / (float) factor), 0.0F);
        float green = Math.max((float) (hexColor >> 8 & 255) - (float) (hexColor >> 8 & 255) / (100.0F / (float) factor), 0.0F);
        float blue = Math.max((float) (hexColor & 255) - (float) (hexColor & 255) / (100.0F / (float) factor), 0.0F);
        return (int) ((float) (((int) alpha << 24) + ((int) red << 16) + ((int) green << 8)) + blue);
    }
    public static int darker(final int color, final float factor) {
        final int r = (int)((color >> 16 & 0xFF) * factor);
        final int g = (int)((color >> 8 & 0xFF) * factor);
        final int b = (int)((color & 0xFF) * factor);
        final int a = color >> 24 & 0xFF;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF) | (a & 0xFF) << 24;
    }
    public static void targetHudRect(double x, double y, double x1, double y1, double size) {
        rectangleBordered(x, y + -4.0D, x1 + size, y1 + size, 0.5D, (new Color(60, 60, 60)).getRGB(), (new Color(10, 10, 10)).getRGB());
        rectangleBordered(x + 1.0D, y + -3.0D, x1 + size - 1.0D, y1 + size - 1.0D, 1.0D, (new Color(40, 40, 40)).getRGB(), (new Color(40, 40, 40)).getRGB());
        rectangleBordered(x + 2.5D, y - 1.5D, x1 + size - 2.5D, y1 + size - 2.5D, 0.5D, (new Color(40, 40, 40)).getRGB(), (new Color(60, 60, 60)).getRGB());
        rectangleBordered(x + 2.5D, y - 1.5D, x1 + size - 2.5D, y1 + size - 2.5D, 0.5D, (new Color(22, 22, 22)).getRGB(), (new Color(255, 255, 255, 0)).getRGB());
    }

    public static void targetHudRect1(double x, double y, double x1, double y1, double size) {
        rectangleBordered(x + 4.35D, y + 0.5D, x1 + size - 84.5D, y1 + size - 4.35D, 0.5D, (new Color(48, 48, 48)).getRGB(), (new Color(10, 10, 10)).getRGB());
        rectangleBordered(x + 5.0D, y + 1.0D, x1 + size - 85.0D, y1 + size - 5.0D, 0.5D, (new Color(17, 17, 17)).getRGB(), (new Color(255, 255, 255, 0)).getRGB());
    }
    public static int reAlpha(int color, float alpha) {
        Color c = new Color(color);
        float r = 0.003921569f * (float)c.getRed();
        float g = 0.003921569f * (float)c.getGreen();
        float b = 0.003921569f * (float)c.getBlue();
        return new Color(r, g, b, alpha).getRGB();
    }
    public static void drawRoundedRect(final float x, final float y, final float width, final float height, float edgeRadius, int color, final float borderWidth, int borderColor) {
        if (color == 16777215) {
            color = -65794;
        }
        if (borderColor == 16777215) {
            borderColor = -65794;
        }
        if (edgeRadius < 0.0f) {
            edgeRadius = 0.0f;
        }
        if (edgeRadius > width / 2.0f) {
            edgeRadius = width / 2.0f;
        }
        if (edgeRadius > height / 2.0f) {
            edgeRadius = height / 2.0f;
        }
        drawRDRect(x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0f, height - edgeRadius * 2.0f, color);
        drawRDRect(x + edgeRadius, y, width - edgeRadius * 2.0f, edgeRadius, color);
        drawRDRect(x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0f, edgeRadius, color);
        drawRDRect(x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0f, color);
        drawRDRect(x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0f, color);
        enableRender2D();
        color(color);
        GL11.glBegin(6);
        float centerX = x + edgeRadius;
        float centerY = y + edgeRadius;
        GL11.glVertex2d(centerX, centerY);
        for (int vertices = (int)Math.min(Math.max(edgeRadius, 10.0f), 90.0f), i = 0; i < vertices + 1; ++i) {
            final double angleRadians = 6.283185307179586 * (i + 180) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glEnd();
        GL11.glBegin(6);
        centerX = x + width - edgeRadius;
        centerY = y + edgeRadius;
        GL11.glVertex2d(centerX, centerY);
        for (int vertices = (int)Math.min(Math.max(edgeRadius, 10.0f), 90.0f), i = 0; i < vertices + 1; ++i) {
            final double angleRadians = 6.283185307179586 * (i + 90) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glEnd();
        GL11.glBegin(6);
        centerX = x + edgeRadius;
        centerY = y + height - edgeRadius;
        GL11.glVertex2d(centerX, centerY);
        for (int vertices = (int)Math.min(Math.max(edgeRadius, 10.0f), 90.0f), i = 0; i < vertices + 1; ++i) {
            final double angleRadians = 6.283185307179586 * (i + 270) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glEnd();
        GL11.glBegin(6);
        centerX = x + width - edgeRadius;
        centerY = y + height - edgeRadius;
        GL11.glVertex2d(centerX, centerY);
        for (int vertices = (int)Math.min(Math.max(edgeRadius, 10.0f), 90.0f), i = 0; i < vertices + 1; ++i) {
            final double angleRadians = 6.283185307179586 * i / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glEnd();
        color(borderColor);
        GL11.glLineWidth(borderWidth);
        GL11.glBegin(3);
        centerX = x + edgeRadius;
        centerY = y + edgeRadius;
        int vertices;
        int i;
        for (vertices = (i = (int)Math.min(Math.max(edgeRadius, 10.0f), 90.0f)); i >= 0; --i) {
            final double angleRadians = 6.283185307179586 * (i + 180) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glVertex2d((x + edgeRadius), y);
        GL11.glVertex2d((x + width - edgeRadius), y);
        centerX = x + width - edgeRadius;
        centerY = y + edgeRadius;
        for (i = vertices; i >= 0; --i) {
            final double angleRadians = 6.283185307179586 * (i + 90) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glVertex2d((x + width), (y + edgeRadius));
        GL11.glVertex2d((x + width), (y + height - edgeRadius));
        centerX = x + width - edgeRadius;
        centerY = y + height - edgeRadius;
        for (i = vertices; i >= 0; --i) {
            final double angleRadians = 6.283185307179586 * i / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glVertex2d((x + width - edgeRadius), (y + height));
        GL11.glVertex2d((x + edgeRadius), (y + height));
        centerX = x + edgeRadius;
        centerY = y + height - edgeRadius;
        for (i = vertices; i >= 0; --i) {
            final double angleRadians = 6.283185307179586 * (i + 270) / (vertices * 4);
            GL11.glVertex2d(centerX + Math.sin(angleRadians) * edgeRadius, centerY + Math.cos(angleRadians) * edgeRadius);
        }
        GL11.glVertex2d(x, (y + height - edgeRadius));
        GL11.glVertex2d(x, (y + edgeRadius));
        GL11.glEnd();
        disableRender2D();
    }
    public static void drawRDRect(final float left, final float top, final float width, final float height, final int color) {
        final float f3 = (color >> 24 & 0xFF) / 255.0f;
        final float f4 = (color >> 16 & 0xFF) / 255.0f;
        final float f5 = (color >> 8 & 0xFF) / 255.0f;
        final float f6 = (color & 0xFF) / 255.0f;
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f4, f5, f6, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, (top + height), 0.0).endVertex();
        worldrenderer.pos((left + width), (top + height), 0.0).endVertex();
        worldrenderer.pos((left + width), top, 0.0).endVertex();
        worldrenderer.pos(left, top, 0.0).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    public static void enableRender2D() {
        GL11.glEnable(3042);
        GL11.glDisable(2884);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBlendFunc(770, 771);
        GL11.glLineWidth(1.0f);
    }
    public static void disableRender2D() {
        GL11.glDisable(3042);
        GL11.glEnable(2884);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
    protected static float zLevel = 0F;

    /**
     * Draws a textured rectangle at the stored z-value. Args: x, y, u, v, width, height.
     */
    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height, float zLevel)
    {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((x), (y + height), zLevel).tex(((float)(textureX) * f), ((float)(textureY + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y + height), zLevel).tex(((float)(textureX + width) * f), ((float)(textureY + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y), zLevel).tex(((float)(textureX + width) * f), ((float)(textureY) * f1)).endVertex();
        worldrenderer.pos((x), (y), zLevel).tex(((float)(textureX) * f), ((float)(textureY) * f1)).endVertex();
        tessellator.draw();
    }


    public static double getAnimationStateSmooth(double target, double current, double speed) {
        boolean larger;
        boolean bl = larger = target > current;
        if (speed < 0.0) {
            speed = 0.0;
        } else if (speed > 1.0) {
            speed = 1.0;
        }
        if (target == current){
            return target;
        }
        double dif = Math.max(target, current) - Math.min(target, current);
        double factor = Math.max((dif * speed),1);
        if (factor < 0.1) {
            factor = 0.1;
        }
        if (larger){
            if (current + factor>target){
                current = target;
            }else {
                current += factor;
            }
        }else {
            if (current - factor<target) {
                current = target;
            }else {
                current -= factor;
            }
        }
        return current;
    }
    public static void prepareScissorBox(float x, float y, float x2, float y2) {
        ScaledResolution scale = new ScaledResolution(mc);
        int factor = scale.getScaleFactor();
        GL11.glScissor((int) (x * (float) factor),
                (int) (((float) scale.getScaledHeight() - y2) * (float) factor),
                (int) ((x2 - x) * (float) factor), (int) ((y2 - y) * (float) factor));
    }
    public static void drawOutlinedRect(float x, float y, float width, float height, float lineSize, int lineColor) {
        RenderUtils.drawRect(x, y, width, y + lineSize, lineColor);
        RenderUtils.drawRect(x, height - lineSize, width, height, lineColor);
        RenderUtils.drawRect(x, y + lineSize, x + lineSize, height - lineSize, lineColor);
        RenderUtils.drawRect(width - lineSize, y + lineSize, width, height - lineSize, lineColor);
    }
    public static void drawCircleRect(float x, float y, float x1, float y1, float radius, int color){
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        glColor(color);

        quickRenderCircle(x1-radius,y1-radius,0,90,radius,radius);
        quickRenderCircle(x+radius,y1-radius,90,180,radius,radius);
        quickRenderCircle(x+radius,y+radius,180,270,radius,radius);
        quickRenderCircle(x1-radius,y+radius,270,360,radius,radius);

        quickDrawRect(x+radius,y+radius,x1-radius,y1-radius);
        quickDrawRect(x,y+radius,x+radius,y1-radius);
        quickDrawRect(x1-radius,y+radius,x1,y1-radius);
        quickDrawRect(x+radius,y,x1-radius,y+radius);
        quickDrawRect(x+radius,y1-radius,x1-radius,y1);

        enableTexture2D();
        disableBlend();
    }
    public static int SkyRainbow(int var2, float st, float bright) {
        double v1 = Math.ceil(System.currentTimeMillis() + (long) (var2 * 109)) / 5;
        return Color.getHSBColor( ((float) ((v1 %= 360.0) / 360.0)) < 0.5 ? -((float) (v1 / 360.0)) : (float) (v1 / 360.0), st, bright).getRGB();
    }

    public static Color skyRainbow(int var2, float st, float bright) {
        double v1 = Math.ceil(System.currentTimeMillis() + (long) (var2 * 109)) / 5;
        return Color.getHSBColor( ((float) ((v1 %= 360.0) / 360.0)) < 0.5 ? -((float) (v1 / 360.0)) : (float) (v1 / 360.0), st, bright);
    }
    public static void drawCircle(float x, float y, float radius, int color) {
        glColor(color);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glPushMatrix();
        glLineWidth(1F);
        glBegin(GL_POLYGON);
        for(int i = 0; i <= 360; i++)
            glVertex2d(x + Math.sin(i * Math.PI / 180.0D) * radius, y + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_LINE_SMOOTH);
        glColor4f(1F, 1F, 1F, 1F);
    }

    public static void drawCheck(double x, double y, int lineWidth, int color) {
        start2D();
        GL11.glPushMatrix();
        GL11.glLineWidth(lineWidth);
        setColor(new Color(color));
        GL11.glBegin(GL_LINE_STRIP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + 2, y + 3);
        GL11.glVertex2d(x + 6, y - 2);
        GL11.glEnd();
        GL11.glPopMatrix();
        stop2D();
    }

    public static void setColor(Color color) {
        float alpha = (color.getRGB() >> 24 & 0xFF) / 255.0F;
        float red = (color.getRGB() >> 16 & 0xFF) / 255.0F;
        float green = (color.getRGB() >> 8 & 0xFF) / 255.0F;
        float blue = (color.getRGB() & 0xFF) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }
    public static void start2D() {
        glEnable(3042);
        glDisable(3553);
        glBlendFunc(770, 771);
        glEnable(2848);
    }

    public static void stop2D() {
        glEnable(3553);
        glDisable(3042);
        glDisable(2848);
        enableTexture2D();
        disableBlend();
        glColor4f(1, 1, 1, 1);
    }

    public static void renderCircle(double x, double y, double radius, int color) {
        renderCircle(x, y, 0, 360, radius - 1, color);
    }

    public static void renderCircle(double x, double y, double start, double end, double radius, int color) {
        renderCircle(x, y, start, end, radius, radius, color);
    }

    public static void renderCircle(double x, double y, double start, double end, double w, double h, int color) {
        GlStateManager.color(0, 0, 0);
        GL11.glColor4f(0, 0, 0, 0);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        glColor(color);

        quickRenderCircle(x, y, start, end, w, h);

        enableTexture2D();
        disableBlend();
    }

    public static void quickRenderCircle(double x, double y, double start, double end, double w, double h) {
        if (start > end) {
            double temp = end;
            end = start;
            start = temp;
        }

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(x, y);
        for(double i = end; i >= start; i-=4) {
            double ldx = Math.cos(i * Math.PI / 180.0) * w;
            double ldy = Math.sin(i * Math.PI / 180.0) * h;
            GL11.glVertex2d(x + ldx, y + ldy);
        }
        GL11.glVertex2d(x, y);
        GL11.glEnd();
    }

    private static void quickPolygonCircle(float x, float y, float xRadius, float yRadius, int start, int end) {
        for(int i = end; i >= start; i -= 4) {
            glVertex2d(x + Math.sin(i * Math.PI / 180.0D) * xRadius, y + Math.cos(i * Math.PI / 180.0D) * yRadius);
        }
    }
    
        public static int getRainbowOpaque(int seconds, float saturation, float brightness, int index) {
        float hue = ((System.currentTimeMillis() + index) % (int) (seconds * 1000)) / (float) (seconds * 1000);
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return color;
    }

    public static float smoothAnimation(float ani, float finalState, float speed, float scale) {
        return getAnimationState(ani, finalState, Math.max(10.0F, Math.abs(ani - finalState) * speed) * scale);
    }
    public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
        return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
    }
    
    public static void drawRoundedCornerRect(float x, float y, float x1, float y1, float radius, int color) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        final boolean hasCull = glIsEnabled(GL_CULL_FACE);
        glDisable(GL_CULL_FACE);

        glColor(color);
        drawRoundedCornerRect(x, y, x1, y1, radius);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        setGlState(GL_CULL_FACE, hasCull);
    }
    public static void fastShadowRoundedRect(float x, float y, float x2, float y2, float rad, float width, float r, float g, float b, float al) {
        Stencil.write(true);
        RenderUtils.drawRoundedRect(x, y, x2, y2, rad, new Color(r, g, b, al).getRGB());
        Stencil.erase(false);

        glPushMatrix();
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        glColor4f(r, g, b, al);
        glBegin(GL_QUAD_STRIP);
        glVertex2f(x + width / 2F, y + width / 2F);
        glColor4f(r, g, b, 0F);
        glVertex2f(x - width, y - width);
        glColor4f(r, g, b, al);
        glVertex2f(x2 - width / 2F, y + width / 2F);
        glColor4f(r, g, b, 0F);
        glVertex2f(x2 + width, y - width);
        glColor4f(r, g, b, al);
        glVertex2f(x2 - width / 2F, y2 - width / 2F);
        glColor4f(r, g, b, 0F);
        glVertex2f(x2 + width, y2 + width);
        glColor4f(r, g, b, al);
        glVertex2f(x + width / 2F, y2 - width / 2F);
        glColor4f(r, g, b, 0F);
        glVertex2f(x - width, y2 + width);
        glColor4f(r, g, b, al);
        glVertex2f(x + width / 2F, y + width / 2F);
        glColor4f(r, g, b, 0F);
        glVertex2f(x - width, y - width);
        glColor4f(1f, 1f, 1f, 1f);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(7424);
        glColor4f(1, 1, 1, 1);
        glPopMatrix();

        Stencil.dispose();
    }
    public static void fastShadowRoundedRect(float x, float y, float x2, float y2, float rad, float width, Color color) {
        fastShadowRoundedRect(x, y, x2, y2, rad, width, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }
    public static void initBlur(final ScaledResolution sc, float strength) throws IOException {
        int w = sc.getScaledWidth();
        int h = sc.getScaledHeight();
        int f = sc.getScaleFactor();
        if (lastWidth != w || lastHeight != h || initialFB == null || frameBuffer == null || mainShader == null) {
            initialFB = new Framebuffer(w * f, h * f, true);
            initialFB.setFramebufferColor(0, 0, 0, 0);
            initialFB.setFramebufferFilter(GL_LINEAR);
            mainShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), initialFB, blurDirectory);
            mainShader.createBindFramebuffers(w * f, h * f);
            frameBuffer = mainShader.mainFramebuffer;
            blackBuffer = mainShader.getFramebufferRaw("braindead");
        }

        lastWidth = w;
        lastHeight = h;

        if (strength != lastStrength) {
            lastStrength = strength;
            for (int i = 0; i < 2; i++) {
                mainShader.listShaders.get(i).getShaderManager().getShaderUniform("Radius").set(strength);
            }
        }
    }
    public static void processShadow(boolean begin, float strength) throws IOException {
        if (!OpenGlHelper.isFramebufferEnabled())
            return;

        final ScaledResolution sc = new ScaledResolution(mc);
        initBlur(sc, strength);

        if (begin) {
            mc.getFramebuffer().unbindFramebuffer();
            initialFB.framebufferClear();
            blackBuffer.framebufferClear();
            initialFB.bindFramebuffer(true);
        } else {
            frameBuffer.bindFramebuffer(true);
            mainShader.loadShaderGroup(mc.timer.renderPartialTicks);
            mc.getFramebuffer().bindFramebuffer(true);
            // Variables
            float f = (float)sc.getScaledWidth();
            float f1 = (float)sc.getScaledHeight();
            float f2 = (float)blackBuffer.framebufferWidth / (float)blackBuffer.framebufferTextureWidth;
            float f3 = (float)blackBuffer.framebufferHeight / (float)blackBuffer.framebufferTextureHeight;

            // Enable/Disable required things
            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.colorMask(true, true, true, true);

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            blackBuffer.bindFramebufferTexture();
            glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldrenderer.pos(0.0D, f1, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(f, f1, 0.0D).tex((double)f2, 0.0D).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(f, 0.0D, 0.0D).tex((double)f2, f3).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, f3).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            blackBuffer.unbindFramebufferTexture();

            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();

            GlStateManager.resetColor();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
        }
    }
    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, zLevel).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, zLevel).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, zLevel).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        disableBlend();
        GlStateManager.enableAlpha();
        enableTexture2D();
        GlStateManager.popMatrix();
    }
    public static void drawGradientRoundedRect(int left, int top, int right, int bottom, int radius, int startColor, int endColor) {
            Stencil.write(false);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            fastRoundedRect(left, top, right, bottom, radius);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Stencil.erase(true);
            drawGradientRect(left, top, right, bottom, startColor, endColor);
            Stencil.dispose();
    }

    public static void drawGradientSideways(double left, double top, double right, double bottom, int col1, int col2) {
        float f = (float) (col1 >> 24 & 255) / 255.0f;
        float f1 = (float) (col1 >> 16 & 255) / 255.0f;
        float f2 = (float) (col1 >> 8 & 255) / 255.0f;
        float f3 = (float) (col1 & 255) / 255.0f;
        float f4 = (float) (col2 >> 24 & 255) / 255.0f;
        float f5 = (float) (col2 >> 16 & 255) / 255.0f;
        float f6 = (float) (col2 >> 8 & 255) / 255.0f;
        float f7 = (float) (col2 & 255) / 255.0f;
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glShadeModel(7425);
        GL11.glPushMatrix();
        GL11.glBegin(7);
        GL11.glColor4f(f1, f2, f3, f);
        GL11.glVertex2d(left, top);
        GL11.glVertex2d(left, bottom);
        GL11.glColor4f(f5, f6, f7, f4);
        GL11.glVertex2d(right, bottom);
        GL11.glVertex2d(right, top);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glShadeModel(7424);
    }
    public static void drawAnimatedGradient(double left, double top, double right, double bottom, int col1, int col2) {
        long currentTime = System.currentTimeMillis();
        if (startTime == 0) {
            startTime = currentTime;
        }

        long elapsedTime = currentTime - startTime;
        float progress = (float) (elapsedTime % animationDuration) / animationDuration;

        int color1, color2;

        if (elapsedTime / animationDuration % 2 == 0) {
            // Custom Color 1 to Custom Color 2
            color1 = interpolateColors(col1, col2, progress);
            color2 = interpolateColors(col2, col1, progress);
        } else {
            // Custom Color 2 to Custom Color 1
            color1 = interpolateColors(col2, col1, progress);
            color2 = interpolateColors(col1, col2, progress);
        }

        drawGradientSideways(left, top, right, bottom, color1, color2);

        if (elapsedTime >= 2 * animationDuration) {
            // Reset the start time to continue the loop
            startTime = currentTime;
        }
    }

    public static int interpolateColors(int color1, int color2, float progress) {
        int alpha = (int) ((1.0 - progress) * (color1 >>> 24) + progress * (color2 >>> 24));
        int red = (int) ((1.0 - progress) * ((color1 >> 16) & 0xFF) + progress * ((color2 >> 16) & 0xFF));
        int green = (int) ((1.0 - progress) * ((color1 >> 8) & 0xFF) + progress * ((color2 >> 8) & 0xFF));
        int blue = (int) ((1.0 - progress) * (color1 & 0xFF) + progress * (color2 & 0xFF));

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static void drawShadow(float x, float y, float width, float height) {
        drawTexturedRect(x - 9, y - 9, 9, 9, "paneltopleft");
        drawTexturedRect(x - 9, y + height, 9, 9, "panelbottomleft");
        drawTexturedRect(x + width, y + height, 9, 9, "panelbottomright");
        drawTexturedRect(x + width, y - 9, 9, 9, "paneltopright");
        drawTexturedRect(x - 9, y, 9, height, "panelleft");
        drawTexturedRect(x + width, y, 9, height, "panelright");
        drawTexturedRect(x, y - 9, width, 9, "paneltop");
        drawTexturedRect(x, y + height, width, 9, "panelbottom");
    }

    public static void drawTexturedRect(float x, float y, float width, float height, String image) {
        glPushMatrix();
        final boolean enableBlend = glIsEnabled(GL_BLEND);
        final boolean disableAlpha = !glIsEnabled(GL_ALPHA_TEST);
        if (!enableBlend) glEnable(GL_BLEND);
        if (!disableAlpha) glDisable(GL_ALPHA_TEST);
        mc.getTextureManager().bindTexture(new ResourceLocation("crosssine/ui/shadow/" + image + ".png"));
        GlStateManager.color(1F, 1F, 1F, 1F);
        RenderUtils.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        if (!enableBlend) glDisable(GL_BLEND);
        if (!disableAlpha) glEnable(GL_ALPHA_TEST);
        glPopMatrix();
    }

    
    public static void drawModalRectWithCustomSizedTexture(float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight)
    {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, (y + height), 0.0D).tex((u * f), ((v + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y + height), 0.0D).tex(((u + width) * f), ((v + height) * f1)).endVertex();
        worldrenderer.pos((x + width), y, 0.0D).tex(((u + width) * f), (v * f1)).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex((u * f), (v * f1)).endVertex();
        tessellator.draw();
    }

    public static void drawExhiEnchants(ItemStack stack, float x, float y) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        disableBlend();
        GlStateManager.resetColor();
        final int darkBorder = 0xFF000000;
        if (stack.getItem() instanceof ItemArmor) {
            int prot = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
            int unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            int thorn = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack);
            if (prot > 0) {
                drawExhiOutlined(prot + "", drawExhiOutlined("P", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(prot), getMainColor(prot), true);
                y += 4;
            }
            if (unb > 0) {
                drawExhiOutlined(unb + "", drawExhiOutlined("U", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(unb),getMainColor(unb), true);
                y += 4;
            }
            if (thorn > 0) {
                drawExhiOutlined(thorn + "", drawExhiOutlined("T", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(thorn), getMainColor(thorn), true);
                y += 4;
            }
        }
        if (stack.getItem() instanceof ItemBow) {
            int power = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            int punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
            int flame = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
            int unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (power > 0) {
                drawExhiOutlined(power + "", drawExhiOutlined("Pow", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(power), getMainColor(power), true);
                y += 4;
            }
            if (punch > 0) {
                drawExhiOutlined(punch + "", drawExhiOutlined("Pun", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(punch), getMainColor(punch), true);
                y += 4;
            }
            if (flame > 0) {
                drawExhiOutlined(flame + "", drawExhiOutlined("F", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(flame), getMainColor(flame), true);
                y += 4;
            }
            if (unb > 0) {
                drawExhiOutlined(unb + "", drawExhiOutlined("U", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(unb), getMainColor(unb), true);
                y += 4;
            }
        }
        if (stack.getItem() instanceof ItemSword) {
            int sharp = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            int kb = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
            int fire = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
            int unb = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (sharp > 0) {
                drawExhiOutlined(sharp + "", drawExhiOutlined("S", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(sharp), getMainColor(sharp), true);
                y += 4;
            }
            if (kb > 0) {
                drawExhiOutlined(kb + "", drawExhiOutlined("K", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(kb), getMainColor(kb), true);
                y += 4;
            }
            if (fire > 0) {
                drawExhiOutlined(fire + "", drawExhiOutlined("F", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(fire), getMainColor(fire), true);
                y += 4;
            }
            if (unb > 0) {
                drawExhiOutlined(unb + "", drawExhiOutlined("U", x, y, 0.35F, darkBorder, -1, true), y, 0.35F, getBorderColor(unb), getMainColor(unb), true);
                y += 4;
            }
        }
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
    }

    private static float drawExhiOutlined(String text, float x, float y, float borderWidth, int borderColor, int mainColor, boolean drawText) {
        Fonts.fontTahomaSmall.drawString(text, x, y - borderWidth, borderColor);
        Fonts.fontTahomaSmall.drawString(text, x, y + borderWidth, borderColor);
        Fonts.fontTahomaSmall.drawString(text, x - borderWidth, y, borderColor);
        Fonts.fontTahomaSmall.drawString(text, x + borderWidth, y, borderColor);
        if (drawText)
            Fonts.fontTahomaSmall.drawString(text, x, y, mainColor);
        return x + Fonts.fontTahomaSmall.getWidth(text) - 2F;
    }

    public static void drawFilledCircle(int xx, int yy, float radius, int col) {
        float f = (float) (col >> 24 & 255) / 255.0f;
        float f1 = (float) (col >> 16 & 255) / 255.0f;
        float f2 = (float) (col >> 8 & 255) / 255.0f;
        float f3 = (float) (col & 255) / 255.0f;
        int sections = 50;
        double dAngle = 6.283185307179586 / sections;
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glBlendFunc(770, 771);
        GL11.glBegin(6);
        int i = 0;
        while (i < sections) {
            float x = (float) (radius * Math.sin(i * dAngle));
            float y = (float) (radius * Math.cos(i * dAngle));
            GL11.glColor4f(f1, f2, f3, f);
            GL11.glVertex2f((float) xx + x, (float) yy + y);
            ++i;
        }
        GlStateManager.color(0.0f, 0.0f, 0.0f);
        GL11.glEnd();
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glPopMatrix();
    }

    public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)right, (double)top, 0.0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double)left, (double)top, 0.0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos((double)left, (double)bottom, 0.0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos((double)right, (double)bottom, 0.0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    private static int getMainColor(int level) {
        if (level == 4)
            return 0xFFAA0000;
        return -1;
    }

    private static int getBorderColor(int level) {
        if (level == 2)
            return 0x7055FF55;
        if (level == 3)
            return 0x7000AAAA;
        if (level == 4)
            return 0x70AA0000;
        if (level >= 5)
            return 0x70FFAA00;
        return 0x70FFFFFF;
    }
    public static void customRounded(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float rTL, float rTR, float rBR, float rBL, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float z;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double xTL = paramXStart + rTL;
        double yTL = paramYStart + rTL;

        double xTR = paramXEnd - rTR;
        double yTR = paramYStart + rTR;

        double xBR = paramXEnd - rBR;
        double yBR = paramYEnd - rBR;

        double xBL = paramXStart + rBL;
        double yBL = paramYEnd - rBL;

        glPushMatrix();
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);

        glColor4f(red, green, blue, alpha);
        glBegin(GL_POLYGON);

        double degree = Math.PI / 180;
        for (double i = 0; i <= 90; i += 0.25)
            glVertex2d(xBR + Math.sin(i * degree) * rBR, yBR + Math.cos(i * degree) * rBR);
        for (double i = 90; i <= 180; i += 0.25)
            glVertex2d(xTR + Math.sin(i * degree) * rTR, yTR + Math.cos(i * degree) * rTR);
        for (double i = 180; i <= 270; i += 0.25)
            glVertex2d(xTL + Math.sin(i * degree) * rTL, yTL + Math.cos(i * degree) * rTL);
        for (double i = 270; i <= 360; i += 0.25)
            glVertex2d(xBL + Math.sin(i * degree) * rBL, yBL + Math.cos(i * degree) * rBL);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glPopMatrix();
    }

    public static void drawRoundedCornerRect(float x, float y, float x1, float y1, float radius) {
        glBegin(GL_POLYGON);

        float xRadius = (float) Math.min((x1 - x) * 0.5, radius);
        float yRadius = (float) Math.min((y1 - y) * 0.5, radius);
        quickPolygonCircle(x + xRadius,y + yRadius, xRadius, yRadius,180,270);
        quickPolygonCircle(x1 - xRadius,y + yRadius, xRadius, yRadius,90,180);
        quickPolygonCircle(x1 - xRadius,y1 - yRadius, xRadius, yRadius,0,90);
        quickPolygonCircle(x + xRadius,y1 - yRadius, xRadius, yRadius,270,360);

        glEnd();
    }

       // rTL = radius top left, rTR = radius top right, rBR = radius bottom right, rBL = radius bottom left
    public static void customRoundedinf(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float rTL, float rTR, float rBR, float rBL, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float z = 0;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double xTL = paramXStart + rTL;
        double yTL = paramYStart + rTL;

        double xTR = paramXEnd - rTR;
        double yTR = paramYStart + rTR;

        double xBR = paramXEnd - rBR;
        double yBR = paramYEnd - rBR;

        double xBL = paramXStart + rBL;
        double yBL = paramYEnd - rBL;

        glPushMatrix();
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);

    	glColor4f(red, green, blue, alpha);
        glBegin(GL_POLYGON);
    
        double degree = Math.PI / 180;
        if (rBR <= 0)
            glVertex2d(xBR, yBR);
        else for (double i = 0; i <= 90; i += 1)
            glVertex2d(xBR + Math.sin(i * degree) * rBR, yBR + Math.cos(i * degree) * rBR);

        if (rTR <= 0)
            glVertex2d(xTR, yTR);
        else for (double i = 90; i <= 180; i += 1)
            glVertex2d(xTR + Math.sin(i * degree) * rTR, yTR + Math.cos(i * degree) * rTR);

        if (rTL <= 0)
            glVertex2d(xTL, yTL);
        else for (double i = 180; i <= 270; i += 1)
            glVertex2d(xTL + Math.sin(i * degree) * rTL, yTL + Math.cos(i * degree) * rTL);

        if (rBL <= 0)
            glVertex2d(xBL, yBL);
        else for (double i = 270; i <= 360; i += 1)
            glVertex2d(xBL + Math.sin(i * degree) * rBL, yBL + Math.cos(i * degree) * rBL);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glPopMatrix();
    }

    public static void drawRoundedRect2(float x, float y, float x2, float y2, final float round, final int color) {
        x += (float) (round / 2.0f + 0.5);
        y += (float) (round / 2.0f + 0.5);
        x2 -= (float) (round / 2.0f + 0.5);
        y2 -= (float) (round / 2.0f + 0.5);
        drawRect((int) x, (int) y, (int) x2, (int) y2, color);
        circle(x2 - round / 2.0f, y + round / 2.0f, round, color);
        circle(x + round / 2.0f, y2 - round / 2.0f, round, color);
        circle(x + round / 2.0f, y + round / 2.0f, round, color);
        circle(x2 - round / 2.0f, y2 - round / 2.0f, round, color);
        drawRect((int) (x - round / 2.0f - 0.5f), (int) (y + round / 2.0f), (int) x2, (int) (y2 - round / 2.0f),
                color);
        drawRect((int) x, (int) (y + round / 2.0f), (int) (x2 + round / 2.0f + 0.5f), (int) (y2 - round / 2.0f),
                color);
        drawRect((int) (x + round / 2.0f), (int) (y - round / 2.0f - 0.5f), (int) (x2 - round / 2.0f),
                (int) (y2 - round / 2.0f), color);
        drawRect((int) (x + round / 2.0f), (int) y, (int) (x2 - round / 2.0f), (int) (y2 + round / 2.0f + 0.5f),
                color);
    }

    public static void setupRender(final boolean start) {
        if (start) {
            GlStateManager.enableBlend();
            GL11.glEnable(2848);
            GlStateManager.disableDepth();
            GlStateManager.disableTexture2D();
            GlStateManager.blendFunc(770, 771);
            GL11.glHint(3154, 4354);
        }
        else {
            disableBlend();
            enableTexture2D();
            GL11.glDisable(2848);
            GlStateManager.enableDepth();
        }
        GlStateManager.depthMask(!start);
    }

    public static void drawSuperCircle(final float x, final float y,final float radius,
                                       final int color) {
        final float alpha = (color >> 24 & 0xFF) / 255.0f;
        final float red = (color >> 16 & 0xFF) / 255.0f;
        final float green = (color >> 8 & 0xFF) / 255.0f;
        final float blue = (color & 0xFF) / 255.0f;
        final Tessellator tessellator = Tessellator.getInstance();
        tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        if (alpha > 0.5f) {
            GL11.glEnable(GL_POLYGON_SMOOTH);
            GL11.glEnable(2848);
            GL11.glBlendFunc(770,  771);
            //GL11.glLineWidth(2.0f);
            GL11.glBegin(3);

            int i = 0;
            while (i <= 180) {
                GL11.glVertex2d(
                        ( x + Math.sin( ((double) i * 3.141526 / 180.0)) *  radius),
                        ( y + Math.cos( ((double) i * 3.141526 / 180.0)) * radius));
                GL11.glVertex2d(
                        ( x + Math.sin( ((double) i * 3.141526 / 180.0)) *  radius),
                        ( y + Math.cos( ((double) i * 3.141526 / 180.0)) * radius));
                ++i;
            }
            GL11.glEnd();
            GL11.glDisable(2848);
            GL11.glDisable(GL_POLYGON_SMOOTH);
        }
        GL11.glBegin(6);
        int i = 0;
        while (i <= 180) {
            GL11.glVertex2d(
                    ( x + Math.sin( ((double) i * 3.141526 / 180.0)) *  radius),
                    ( y + Math.cos( ((double) i * 3.141526 / 180.0)) * radius));
            ++i;
        }
        GL11.glEnd();
        enableTexture2D();
        disableBlend();
    }

    public static void circle(final float x, final float y, final float radius, final int fill) {

        arc(x, y, 0.0f, 360.0f, radius, fill);
    } // 1

    public static void circle(final float x, final float y, final float radius, final Color fill) {
        arc(x, y, 0.0f, 360.0f, radius, fill);
    } // 2

    public static void arc(final float x, final float y, final float start, final float end, final float radius,
                           final int color) {
        arcEllipse(x, y, start, end, radius, radius, color);
    } // 1

    public static void arc(final float x, final float y, final float start, final float end, final float radius,
                           final Color color) {
        arcEllipse(x, y, start, end, radius, radius, color);
    } // 2


    public static void arcEllipse(final float x, final float y, float start, float end, final float w, final float h,
                                  final int color) { // 1
        GlStateManager.color(0.0f, 0.0f, 0.0f);
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
        float temp;
        if (start > end) {
            temp = end;
            end = start;
            start = temp;
        }
        final float alpha = (color >> 24 & 0xFF) / 255.0f;
        final float red = (color >> 16 & 0xFF) / 255.0f;
        final float green = (color >> 8 & 0xFF) / 255.0f;
        final float blue = (color & 0xFF) / 255.0f;
        final Tessellator tessellator = Tessellator.getInstance();
        tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        if (alpha > 0.5f) {
            GL11.glEnable(GL_POLYGON_SMOOTH);
            GL11.glEnable(2848);
            GL11.glLineWidth(2.0f);
            GL11.glBegin(3);
            for (float i = end; i >= start; i -= 4.0f) {
                final float ldx = (float) Math.cos(i * Math.PI / 180.0) * w * 1.001f;
                final float ldy = (float) Math.sin(i * Math.PI / 180.0) * h * 1.001f;
                GL11.glVertex2f(x + ldx, y + ldy);
            }
            GL11.glEnd();
            GL11.glDisable(2848);
            GL11.glDisable(GL_POLYGON_SMOOTH);
        }
        GL11.glBegin(6);
        for (float i = end; i >= start; i -= 4.0F) {
            final float ldx = (float) Math.cos(i * Math.PI / 180.0) * w;
            final float ldy = (float) Math.sin(i * Math.PI / 180.0) * h;
            GL11.glVertex2f(x + ldx, y + ldy);
        }
        GL11.glEnd();
        enableTexture2D();
        disableBlend();
    }

    public static void arcEllipse(final float x, final float y, float start, float end, final float w, final float h,
                                  final Color color) { //2
        GlStateManager.color(0.0f, 0.0f, 0.0f);
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
        float temp = 0.0f;
        if (start > end) {
            temp = end;
            end = start;
            start = temp;
        }
        final Tessellator var9 = Tessellator.getInstance();
        var9.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f);
        if (color.getAlpha() > 0.5f) {
            GL11.glEnable(2848);
            GL11.glLineWidth(2.0f);
            GL11.glBegin(3);
            for (float i = end; i >= start; i -= 4.0f) {
                final float ldx = (float) Math.cos(i * Math.PI / 180.0) * w * 1.001f;
                final float ldy = (float) Math.sin(i * Math.PI / 180.0) * h * 1.001f;
                GL11.glVertex2f(x + ldx, y + ldy);
            }
            GL11.glEnd();
            GL11.glDisable(2848);
        }
        GL11.glBegin(6);
        for (float i = end; i >= start; i -= 4.0f) {
            final float ldx = (float) Math.cos(i * Math.PI / 180.0) * w;
            final float ldy = (float) Math.sin(i * Math.PI / 180.0) * h;
            GL11.glVertex2f(x + ldx, y + ldy);
        }
        GL11.glEnd();
        enableTexture2D();
        disableBlend();
    }

    public static void fastRoundedRect(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius) {
        float z = 0;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double x1 = (paramXStart + radius);
        double y1 = (paramYStart + radius);
        double x2 = (paramXEnd - radius);
        double y2 = (paramYEnd - radius);

        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);

        glBegin(GL_POLYGON);

        double degree = Math.PI / 180;
        for (double i = 0; i <= 90; i += 1)
            glVertex2d(x2 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        for (double i = 90; i <= 180; i += 1)
            glVertex2d(x2 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        for (double i = 180; i <= 270; i += 1)
            glVertex2d(x1 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        for (double i = 270; i <= 360; i += 1)
            glVertex2d(x1 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        glEnd();
        glDisable(GL_LINE_SMOOTH);
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }
    public static boolean isInViewFrustrum(Entity entity) {
        return isInViewFrustrum(entity.getEntityBoundingBox()) || entity.ignoreFrustumCheck;
    }
    private static final Frustum frustrum = new Frustum();
    private static boolean isInViewFrustrum(AxisAlignedBB bb) {
        Entity current = mc.getRenderViewEntity();
        frustrum.setPosition(current.posX, current.posY, current.posZ);
        return frustrum.isBoundingBoxInFrustum(bb);
    }

    public static void drawFilledCircleNoGL(final int x, final int y, final double r, final int c, final int quality) {
        final float f = ((c >> 24) & 0xff) / 255F;
        final float f1 = ((c >> 16) & 0xff) / 255F;
        final float f2 = ((c >> 8) & 0xff) / 255F;
        final float f3 = (c & 0xff) / 255F;

        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        for (int i = 0; i <= 360 / quality; i++) {
            final double x2 = Math.sin(((i * quality * Math.PI) / 180)) * r;
            final double y2 = Math.cos(((i * quality * Math.PI) / 180)) * r;
            GL11.glVertex2d(x + x2, y + y2);
        }

        GL11.glEnd();
    }
    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        }
        return framebuffer;
    }

    public static void bindTexture(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    public static void drawEntityBox(final Entity entity, final Color color, final boolean outline) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        enableGlCap(GL_BLEND);
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
        glDepthMask(false);

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX;
        final double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ;

        final AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                entityBox.minX - entity.posX + x - 0.05D,
                entityBox.minY - entity.posY + y,
                entityBox.minZ - entity.posZ + z - 0.05D,
                entityBox.maxX - entity.posX + x + 0.05D,
                entityBox.maxY - entity.posY + y + 0.15D,
                entityBox.maxZ - entity.posZ + z + 0.05D
        );

        if (outline) {
            glLineWidth(1F);
            enableGlCap(GL_LINE_SMOOTH);
            glColor(color.getRed(), color.getGreen(), color.getBlue(), 95);
            drawSelectionBoundingBox(axisAlignedBB);
        }

        glColor(color.getRed(), color.getGreen(), color.getBlue(), outline ? 26 : 35);
        drawFilledBox(axisAlignedBB);
        GlStateManager.resetColor();
        glDepthMask(true);
        resetCaps();
    }

    public static void drawFilledCircle(double x, double y, double r, int c, int id) {
        float f = (float) (c >> 24 & 0xff) / 255F;
        float f1 = (float) (c >> 16 & 0xff) / 255F;
        float f2 = (float) (c >> 8 & 0xff) / 255F;
        float f3 = (float) (c & 0xff) / 255F;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(f1, f2, f3, f);
        GL11.glBegin(GL11.GL_POLYGON);
        if (id == 1) {
            GL11.glVertex2d(x, y);
            for (int i = 0; i <= 90; i++) {
                double x2 = Math.sin((i * 3.141526D / 180)) * r;
                double y2 = Math.cos((i * 3.141526D / 180)) * r;
                GL11.glVertex2d(x - x2, y - y2);
            }
        } else if (id == 2) {
            GL11.glVertex2d(x, y);
            for (int i = 90; i <= 180; i++) {
                double x2 = Math.sin((i * 3.141526D / 180)) * r;
                double y2 = Math.cos((i * 3.141526D / 180)) * r;
                GL11.glVertex2d(x - x2, y - y2);
            }
        } else if (id == 3) {
            GL11.glVertex2d(x, y);
            for (int i = 270; i <= 360; i++) {
                double x2 = Math.sin((i * 3.141526D / 180)) * r;
                double y2 = Math.cos((i * 3.141526D / 180)) * r;
                GL11.glVertex2d(x - x2, y - y2);
            }
        } else if (id == 4) {
            GL11.glVertex2d(x, y);
            for (int i = 180; i <= 270; i++) {
                double x2 = Math.sin((i * 3.141526D / 180)) * r;
                double y2 = Math.cos((i * 3.141526D / 180)) * r;
                GL11.glVertex2d(x - x2, y - y2);
            }
        } else {
            for (int i = 0; i <= 360; i++) {
                double x2 = Math.sin((i * 3.141526D / 180)) * r;
                double y2 = Math.cos((i * 3.141526D / 180)) * r;
                GL11.glVertex2f((float) (x - x2), (float) (y - y2));
            }
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
    public static void renderParticles(final List<Particle> particles) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int i = 0;
        try {
            for (final Particle particle : particles) {
                i++;
                final Vec3 v = particle.position;
                boolean draw = true;

                final double x = v.xCoord - (mc.getRenderManager()).renderPosX;
                final double y = v.yCoord - (mc.getRenderManager()).renderPosY;
                final double z = v.zCoord - (mc.getRenderManager()).renderPosZ;

                final double distanceFromPlayer = mc.thePlayer.getDistance(v.xCoord, v.yCoord - 1, v.zCoord);
                int quality = (int) (distanceFromPlayer * 4 + 10);

                if (quality > 350)
                    quality = 350;

                if (!isInViewFrustrum(new EntityEgg(mc.theWorld, v.xCoord, v.yCoord, v.zCoord)))
                    draw = false;

                if (i % 10 != 0 && distanceFromPlayer > 25)
                    draw = false;

                if (i % 3 == 0 && distanceFromPlayer > 15)
                    draw = false;

                if (draw) {
                    GL11.glPushMatrix();
                    GL11.glTranslated(x, y, z);

                    final float scale = 0.04F;
                    GL11.glScalef(-scale, -scale, -scale);

                    GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0D, 1.0D, 0.0D);
                    GL11.glRotated(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0D : 1.0D, 0.0D, 0.0D);
                    final Color c = new Color(ColorUtils.INSTANCE.getColor(-(1 + 5 * 1.7f), 0.7f, 1));
                    drawFilledCircleNoGL(0, 0, 0.7, c.hashCode(), quality);

                    if (distanceFromPlayer < 4)
                        drawFilledCircleNoGL(0, 0, 1.4, new Color(c.getRed(), c.getGreen(), c.getBlue(), 50).hashCode(), quality);

                    if (distanceFromPlayer < 20)
                        drawFilledCircleNoGL(0, 0, 2.3, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30).hashCode(), quality);

                    GL11.glScalef(0.8F, 0.8F, 0.8F);
                    GL11.glPopMatrix();
                }
            }
        } catch (final ConcurrentModificationException ignored) {
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glColor3d(255, 255, 255);
    }

    public static void drawGradientSidewaysH(double left, double top, double right, double bottom, int col1, int col2) {
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        quickDrawGradientSidewaysH(left, top, right, bottom, col1, col2);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
    }

    public static void quickDrawGradientSidewaysH(double left, double top, double right, double bottom, int col1, int col2) {
        glBegin(GL_QUADS);

        glColor(col1);
        glVertex2d(left, top);
        glVertex2d(left, bottom);
        glColor(col2);
        glVertex2d(right, bottom);
        glVertex2d(right, top);

        glEnd();
    }
    public static void drawGradientSidewaysV(double left, double top, double right, double bottom, int col1, int col2) {
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        quickDrawGradientSidewaysV(left, top, right, bottom, col1, col2);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
    }
    public static void quickDrawGradientSidewaysV(double left, double top, double right, double bottom, int col1, int col2) {
        glBegin(GL_QUADS);

        glColor(col1);
        glVertex2d(right, top);
        glVertex2d(left, top);
        glColor(col2);
        glVertex2d(left, bottom); // TODO: Fix this, this may have been a mistake
        glVertex2d(right, bottom);

        glEnd();
    }
    public static void drawBlockBox(final BlockPos blockPos, final Color color, final boolean outline, final boolean box, final float outlineWidth) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        final double x = blockPos.getX() - renderManager.renderPosX;
        final double y = blockPos.getY() - renderManager.renderPosY;
        final double z = blockPos.getZ() - renderManager.renderPosZ;

        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
        final Block block = BlockUtils.getBlock(blockPos);

        if (block != null) {
            final EntityPlayer player = mc.thePlayer;

            final double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) timer.renderPartialTicks;
            final double posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) timer.renderPartialTicks;
            final double posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) timer.renderPartialTicks;
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                    .expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)
                    .offset(-posX, -posY, -posZ);
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        enableGlCap(GL_BLEND);
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
        glDepthMask(false);

        if(box) {
            glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() != 255 ? color.getAlpha() : outline ? 26 : 35);
            drawFilledBox(axisAlignedBB);
        }

        if (outline) {
            glLineWidth(outlineWidth);
            enableGlCap(GL_LINE_SMOOTH);
            glColor(color);

            drawSelectionBoundingBox(axisAlignedBB);
        }

        GlStateManager.resetColor();
        glDepthMask(true);
        resetCaps();
    }
    public static void drawSelectionBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);

        // Lower Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();

        // Upper Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();

        // Upper Rectangle
        worldrenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();

        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();

        worldrenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        worldrenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();

        tessellator.draw();
    }
    public static void drawEntityBox(final Entity entity, final Color color, final boolean outline, final boolean box, final float outlineWidth) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        enableGlCap(GL_BLEND);
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
        glDepthMask(false);

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX;
        final double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ;

        final AxisAlignedBB entityBox = entity.getEntityBoundingBox();
        final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                entityBox.minX - entity.posX + x - 0.05D,
                entityBox.minY - entity.posY + y,
                entityBox.minZ - entity.posZ + z - 0.05D,
                entityBox.maxX - entity.posX + x + 0.05D,
                entityBox.maxY - entity.posY + y + 0.15D,
                entityBox.maxZ - entity.posZ + z + 0.05D
        );

        if (outline) {
            glLineWidth(outlineWidth);
            enableGlCap(GL_LINE_SMOOTH);
            glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            drawSelectionBoundingBox(axisAlignedBB);
        }

        if(box) {
            glColor(color.getRed(), color.getGreen(), color.getBlue(), outline ? 26 : 50);
            drawFilledBox(axisAlignedBB);
        }
        GlStateManager.resetColor();
        glDepthMask(true);
        resetCaps();
    }

    public static void drawAxisAlignedBB(final AxisAlignedBB axisAlignedBB, final Color color, final boolean outline, final boolean box, final float outlineWidth) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glLineWidth(outlineWidth);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glColor(color);

        if (outline) {
            glLineWidth(outlineWidth);
            enableGlCap(GL_LINE_SMOOTH);
            glColor(color.getRed(), color.getGreen(), color.getBlue(), 95);
            drawSelectionBoundingBox(axisAlignedBB);
        }

        if(box) {
            glColor(color.getRed(), color.getGreen(), color.getBlue(), outline ? 26 : 35);
            drawFilledBox(axisAlignedBB);
        }

        GlStateManager.resetColor();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    public static void drawPlatform(final double y, final Color color, final double size) {
        final RenderManager renderManager = mc.getRenderManager();
        final double renderY = y - renderManager.renderPosY;

        drawAxisAlignedBB(new AxisAlignedBB(size, renderY + 0.02D, size, -size, renderY, -size), color,false,true,2F);
    }

    public static void drawPlatform(final Entity entity, final Color color) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX;
        final double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ;

        final AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox()
                .offset(-entity.posX, -entity.posY, -entity.posZ)
                .offset(x, y, z);

        drawAxisAlignedBB(new AxisAlignedBB(axisAlignedBB.minX, axisAlignedBB.maxY + 0.2, axisAlignedBB.minZ, axisAlignedBB.maxX, axisAlignedBB.maxY + 0.26, axisAlignedBB.maxZ),
                color,false,true,2F);
    }

    public static void drawFilledBox(final AxisAlignedBB axisAlignedBB) {
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();

        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();

        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex();
        tessellator.draw();
    }
    public static void renderOutlines(double x, double y, double z, float width, float height, Color c, float outlinewidth) {
        float halfwidth = width / 2.0F;
        float halfheight = height / 2.0F;
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        y++;
        GL11.glLineWidth(outlinewidth);
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        tessellator.draw();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }
    public static void renderBox(double x, double y, double z, float width, float height, Color c) {
        float halfwidth = width / 2.0F;
        float halfheight = height / 2.0F;
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableDepth();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        y++;
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y + halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x + halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z + halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        worldRenderer.pos(x - halfwidth, y - halfheight, z - halfwidth).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).endVertex();
        tessellator.draw();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawBlockBox(final BlockPos blockPos, final Color color, final boolean outline) {
        final RenderManager renderManager = mc.getRenderManager();
        final Timer timer = mc.timer;

        final double x = blockPos.getX() - renderManager.renderPosX;
        final double y = blockPos.getY() - renderManager.renderPosY;
        final double z = blockPos.getZ() - renderManager.renderPosZ;

        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(x, y, z, x + 1.0, y + 1, z + 1.0);
        final Block block = BlockUtils.getBlock(blockPos);

        if (block != null) {
            final EntityPlayer player = mc.thePlayer;

            final double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) timer.renderPartialTicks;
            final double posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) timer.renderPartialTicks;
            final double posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) timer.renderPartialTicks;
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                    .expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)
                    .offset(-posX, -posY, -posZ);
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        enableGlCap(GL_BLEND);
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST);
        glDepthMask(false);

        glColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() != 255 ? color.getAlpha() : outline ? 26 : 35);
        drawFilledBox(axisAlignedBB);

        if (outline) {
            glLineWidth(1F);
            enableGlCap(GL_LINE_SMOOTH);
            glColor(color);

            drawSelectionBoundingBox(axisAlignedBB);
        }

        GlStateManager.resetColor();
        glDepthMask(true);
        resetCaps();
    }


    // Astolfo
    public static int Astolfo(int var2, float st, float bright) {
        double currentColor = Math.ceil(System.currentTimeMillis() + (long) (var2 * 130)) / 6;
        return Color.getHSBColor((double) ((float) ((currentColor %= 360.0) / 360.0)) < 0.5 ? -((float) (currentColor / 360.0)) : (float) (currentColor / 360.0), st, bright).getRGB();
    }

    public static void quickDrawRect(final float x, final float y, final float x2, final float y2) {
        glBegin(GL_QUADS);

        glVertex2d(x2, y);
        glVertex2d(x, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);

        glEnd();
    }

    public static void quickDrawRect(final float x, final float y, final float x2, final float y2, final int color) {
        glColor(color);
        quickDrawRect(x, y, x2, y2);
    }

    public static void quickDrawRect(final float x, final float y, final float x2, final float y2, final Color color) {
        quickDrawRect(x, y, x2, y2, color.getRGB());
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        enableTexture2D();
        disableBlend();
    }
    public static void drawRect(final double x, final double y, final double x2, final double y2, final int color) {
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);

        glColor(color);
        glBegin(GL_QUADS);

        glVertex2d(x2, y);
        glVertex2d(x, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
    }
    public static void drawRect(Rectangle rect , int color) {
        drawRect(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), color);
    }
    public static void drawRect(final float x, final float y, final float x2, final float y2, final Color color) {
        drawRect(x, y, x2, y2, color.getRGB());
    }
    public static void drawBorderedRect(final double x, final double y, final double x2, final double y2, final double width, final int color1, final int color2) {
        drawBorderedRect((float)x,(float)y,(float)x2,(float)y2,(float)width,color1,color2);
    }
    public static void drawBorderedRect(final float x, final float y, final float x2, final float y2, final float width, final int color1, final int color2) {
        drawRect(x, y, x2, y2, color2);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);

        glColor(color1);
        glLineWidth(width);
        glBegin(1);
        glVertex2d(x, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);
        glVertex2d(x2, y);
        glVertex2d(x, y);
        glVertex2d(x2, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
    }

    public static void newDrawRect(float left, float top, float right, float bottom, int color)
    {
        if (left < right)
        {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom)
        {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        enableTexture2D();
        disableBlend();
    }

    public static void newDrawRect(double left, double top, double right, double bottom, int color)
    {
        if (left < right)
        {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom)
        {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        enableTexture2D();
        disableBlend();
    }

    public static void drawRoundedRect(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius, int color, boolean popPush) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float z = 0;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double x1 = (paramXStart + radius);
        double y1 = (paramYStart + radius);
        double x2 = (paramXEnd - radius);
        double y2 = (paramYEnd - radius);

        if (popPush) glPushMatrix();
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);

        glColor4f(red, green, blue, alpha);
        glBegin(GL_POLYGON);

        double degree = Math.PI / 180;
        for (double i = 0; i <= 90; i += 1)
            glVertex2d(x2 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        for (double i = 90; i <= 180; i += 1)
            glVertex2d(x2 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        for (double i = 180; i <= 270; i += 1)
            glVertex2d(x1 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        for (double i = 270; i <= 360; i += 1)
            glVertex2d(x1 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        if (popPush) glPopMatrix();
    }
    public static void drawRoundedRect(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius, int color, boolean popPush, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float z = 0;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double x1 = paramXStart;
        double y1 = paramYStart;
        double x2 = paramXEnd;
        double y2 = paramYEnd;

        if (popPush) glPushMatrix();
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);

        glColor4f(red, green, blue, alpha);
        glBegin(GL_POLYGON);

        double degree = Math.PI / 180;
        if (roundTopLeft) {
            for (double i = 0; i <= 90; i += 1)
                glVertex2d(x1 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        } else {
            glVertex2d(x1, y1);
        }

        if (roundTopRight) {
            for (double i = 90; i <= 180; i += 1)
                glVertex2d(x2 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius);
        } else {
            glVertex2d(x2, y1);
        }

        if (roundBottomRight) {
            for (double i = 180; i <= 270; i += 1)
                glVertex2d(x2 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        } else {
            glVertex2d(x2, y2);
        }

        if (roundBottomLeft) {
            for (double i = 270; i <= 360; i += 1)
                glVertex2d(x1 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius);
        } else {
            glVertex2d(x1, y2);
        }

        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        if (popPush) glPopMatrix();
    }

    public static void drawLoadingCircle(float x, float y) {
        for (int i = 0; i < 4; i++) {
            int rot = (int) ((System.nanoTime() / 5000000 * i) % 360);
            drawCircle(x, y, i * 10, rot - 180, rot);
        }
    }
    public static void drawCircle(float x, float y, float radius, int start, int end) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor(Color.WHITE);

        glEnable(GL_LINE_SMOOTH);
        glLineWidth(2F);
        glBegin(GL_LINE_STRIP);
        for (float i = end; i >= start; i -= (360 / 90.0f))
            glVertex2f((float) (x + (cos(i * PI / 180) * (radius * 1.001F))), (float) (y + (sin(i * PI / 180) * (radius * 1.001F))));
        glEnd();
        glDisable(GL_LINE_SMOOTH);

        enableTexture2D();
        disableBlend();
    }

    public static void drawLimitedCircle(final float lx, final float ly, final float x2, final float y2,final int xx, final int yy, final float radius, final Color color) {
        int sections = 50;
        double dAngle = 2 * Math.PI / sections;
        float x, y;

        glPushAttrib(GL_ENABLE_BIT);

        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glBegin(GL_TRIANGLE_FAN);

        glColor(color);
        for (int i = 0; i < sections; i++) {
            x = (float) (radius * Math.sin((i * dAngle)));
            y = (float) (radius * Math.cos((i * dAngle)));
            glVertex2f(Math.min(x2,Math.max(xx + x,lx)), Math.min(y2,Math.max(yy + y,ly)));
        }

        GlStateManager.color(0, 0, 0);

        glEnd();

        glPopAttrib();
    }

    public static void drawImage(ResourceLocation image, int x, int y, int width, int height) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
    public static void drawImage(ResourceLocation image, int x, int y, int width, int height, float alpha) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor4f(1.0F, 1.0F, 1.0F, alpha);
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static void drawImage2(ResourceLocation image, float x, float y, int width, int height) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glTranslatef(x, y, x);
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        glTranslatef(-x, -y, -x);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static void drawImage3(ResourceLocation image, float x, float y, int width, int height, float r, float g, float b, float al) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor4f(r, g, b, al);
        glTranslatef(x, y, x);
        mc.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        glTranslatef(-x, -y, -x);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    public static void drawRectBasedBorder(float x, float y, float x2, float y2, float width, int color1) {
        drawRect(x - width / 2F, y - width / 2F, x2 + width / 2F, y + width / 2F, color1);
        drawRect(x - width / 2F, y + width / 2F, x + width / 2F, y2 + width / 2F, color1);
        drawRect(x2 - width / 2F, y + width / 2F, x2 + width / 2F, y2 + width / 2F, color1);
        drawRect(x + width / 2F, y2 - width / 2F, x2 - width / 2F, y2 + width / 2F, color1);
    }

    public static void drawRectBasedBorder(double x, double y, double x2, double y2, double width, int color1) {
        newDrawRect(x - width / 2F, y - width / 2F, x2 + width / 2F, y + width / 2F, color1);
        newDrawRect(x - width / 2F, y + width / 2F, x + width / 2F, y2 + width / 2F, color1);
        newDrawRect(x2 - width / 2F, y + width / 2F, x2 + width / 2F, y2 + width / 2F, color1);
        newDrawRect(x + width / 2F, y2 - width / 2F, x2 - width / 2F, y2 + width / 2F, color1);
    }

    public static void lineWidth(final float width) {
        GL11.glLineWidth(width);
    }
    public static void begin(final int glMode) {
        GL11.glBegin(glMode);
    }
    public static void vertex(final double x, final double y) {
        GL11.glVertex2d(x, y);
    }
    public static void end() {
        GL11.glEnd();
    }

    public static void enable(final int glTarget) {
        GL11.glEnable(glTarget);
    }
    public static void color(Color color) {
        if (color == null)
            color = Color.white;
        color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
    }

    public static void color(final double red, final double green, final double blue, final double alpha) {
        GL11.glColor4d(red, green, blue, alpha);
    }
    public static void color(final int color, final float alpha) {
        final float r = (color >> 16 & 0xFF) / 255.0f;
        final float g = (color >> 8 & 0xFF) / 255.0f;
        final float b = (color & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, alpha);
    }
    public static void color(final int color) {
        color(color, (color >> 24 & 0xFF) / 255.0f);
    }
    public static void disable(final int glTarget) {
        GL11.glDisable(glTarget);
    }

    public static void lineNoGl(final double firstX, final double firstY, final double secondX, final double secondY, final Color color) {
        start();
        if (color != null)
            color(color);
        lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        begin(GL11.GL_LINES);
        {
            vertex(firstX, firstY);
            vertex(secondX, secondY);
        }
        end();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        stop();
    }

    public static void glColor(final int red, final int green, final int blue, final int alpha) {
        GlStateManager.color(red / 255F, green / 255F, blue / 255F, alpha / 255F);
    }

    public static void glColor(final Color color) {
        final float red = color.getRed() / 255F;
        final float green = color.getGreen() / 255F;
        final float blue = color.getBlue() / 255F;
        final float alpha = color.getAlpha() / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glColor(final Color color, final int alpha) {
        glColor(color, alpha/255F);
    }

    public static void glColor(final Color color, final float alpha) {
        final float red = color.getRed() / 255F;
        final float green = color.getGreen() / 255F;
        final float blue = color.getBlue() / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glColor(final int hex) {
        final float alpha = (hex >> 24 & 0xFF) / 255F;
        final float red = (hex >> 16 & 0xFF) / 255F;
        final float green = (hex >> 8 & 0xFF) / 255F;
        final float blue = (hex & 0xFF) / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void glColor(final int hex, final int alpha) {
        final float red = (hex >> 16 & 0xFF) / 255F;
        final float green = (hex >> 8 & 0xFF) / 255F;
        final float blue = (hex & 0xFF) / 255F;

        GlStateManager.color(red, green, blue, alpha / 255F);
    }

    public static void glColor(final int hex, final float alpha) {
        final float red = (hex >> 16 & 0xFF) / 255F;
        final float green = (hex >> 8 & 0xFF) / 255F;
        final float blue = (hex & 0xFF) / 255F;

        GlStateManager.color(red, green, blue, alpha);
    }

    public static void drawTriAngle(float cx, float cy, float r, float n, Color color, boolean polygon) {
        cx *= 2.0;
        cy *= 2.0;
        double b = 6.2831852 / n;
        double p = Math.cos(b);
        double s = Math.sin(b);
        r *= 2.0;
        double x = r;
        double y = 0.0;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        glLineWidth(1F);
        enableGlCap(GL_LINE_SMOOTH);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.resetColor();
        glColor(color);
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        worldrenderer.begin(polygon ? GL_POLYGON : 2, DefaultVertexFormats.POSITION);
        int ii = 0;
        while (ii < n) {
            worldrenderer.pos(x + cx, (double)y + cy, 0.0D).endVertex();
            double t = x;
            x = p * x - s * y;
            y = s * t + p * y;
            ii++;
        }
        tessellator.draw();
        enableTexture2D();
        disableBlend();
        GlStateManager.scale(2f, 2f, 2f);
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawCircle(float x, float y, float radius, float lineWidth, int start, int end, Color color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor(color);

        glEnable(GL_LINE_SMOOTH);
        glLineWidth(lineWidth);
        glBegin(GL_LINE_STRIP);
        for (float i = end; i >= start; i -= (360 / 90.0f)) {
            glVertex2f((float) (x + (cos(i * PI / 180) * (radius * 1.001F))), (float) (y + (sin(i * PI / 180) * (radius * 1.001F))));
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);

        enableTexture2D();
        disableBlend();
    }

    public static void drawCircle(float x, float y, float radius, float lineWidth, int start, int end) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glColor(Color.WHITE);

        glEnable(GL_LINE_SMOOTH);
        glLineWidth(lineWidth);
        glBegin(GL_LINE_STRIP);
        for (float i = end; i >= start; i -= (360 / 90.0f)) {
            glVertex2f((float) (x + (cos(i * PI / 180) * (radius * 1.001F))), (float) (y + (sin(i * PI / 180) * (radius * 1.001F))));
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);

        enableTexture2D();
        disableBlend();
    }

    public static void drawArrow(double x, double y, int lineWidth, int color, double length) {
        start2D();
        GL11.glPushMatrix();
        GL11.glLineWidth(lineWidth);
        setColor(new Color(color));
        GL11.glBegin(GL_LINE_STRIP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + 3, y + length);
        GL11.glVertex2d(x + 3 * 2, y);
        GL11.glEnd();
        GL11.glPopMatrix();
        stop2D();
    }

    public static void draw2D(final EntityLivingBase entity, final double posX, final double posY, final double posZ, final int color, final int backgroundColor) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0F, 1F, 0F);
        GlStateManager.scale(-0.1D, -0.1D, 0.1D);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.depthMask(true);

        glColor(color);

        glCallList(DISPLAY_LISTS_2D[0]);

        glColor(backgroundColor);

        glCallList(DISPLAY_LISTS_2D[1]);

        GlStateManager.translate(0, 21 + -(entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 12, 0);

        glColor(color);
        glCallList(DISPLAY_LISTS_2D[2]);

        glColor(backgroundColor);
        glCallList(DISPLAY_LISTS_2D[3]);

        // Stop render
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        GlStateManager.popMatrix();
    }

    public static void draw2D(final BlockPos blockPos, final int color, final int backgroundColor) {
        final RenderManager renderManager = mc.getRenderManager();

        final double posX = (blockPos.getX() + 0.5) - renderManager.renderPosX;
        final double posY = blockPos.getY() - renderManager.renderPosY;
        final double posZ = (blockPos.getZ() + 0.5) - renderManager.renderPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0F, 1F, 0F);
        GlStateManager.scale(-0.1D, -0.1D, 0.1D);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.depthMask(true);

        glColor(color);

        glCallList(DISPLAY_LISTS_2D[0]);

        glColor(backgroundColor);

        glCallList(DISPLAY_LISTS_2D[1]);

        GlStateManager.translate(0, 9, 0);

        glColor(color);

        glCallList(DISPLAY_LISTS_2D[2]);

        glColor(backgroundColor);

        glCallList(DISPLAY_LISTS_2D[3]);

        // Stop render
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        GlStateManager.popMatrix();
    }

    public static void renderNameTag(final String string, final double x, final double y, final double z) {
        final RenderManager renderManager = mc.getRenderManager();

        glPushMatrix();
        glTranslated(x - renderManager.renderPosX, y - renderManager.renderPosY, z - renderManager.renderPosZ);
        glNormal3f(0F, 1F, 0F);
        glRotatef(-mc.getRenderManager().playerViewY, 0F, 1F, 0F);
        glRotatef(mc.getRenderManager().playerViewX, 1F, 0F, 0F);
        glScalef(-0.05F, -0.05F, 0.05F);
        setGlCap(GL_LIGHTING, false);
        setGlCap(GL_DEPTH_TEST, false);
        setGlCap(GL_BLEND, true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        final int width = Fonts.font35.getStringWidth(string) / 2;

        Gui.drawRect(-width - 1, -1, width + 1, Fonts.font35.FONT_HEIGHT, Integer.MIN_VALUE);
        Fonts.font35.drawString(string, -width, 1.5F, Color.WHITE.getRGB(), true);

        resetCaps();
        glColor4f(1F, 1F, 1F, 1F);
        glPopMatrix();
    }

    public static void makeScissorBox(final float x, final float y, final float x2, final float y2) {
        makeScissorBox(x, y, x2, y2, 1);
    }

    public static void makeScissorBox(final float x, final float y, final float x2, final float y2, final float scaleOffset) {
        final ScaledResolution scaledResolution = StaticStorage.scaledResolution;
        final float factor = scaledResolution.getScaleFactor() * scaleOffset;
        glScissor((int) (x * factor), (int) ((scaledResolution.getScaledHeight() - y2) * factor), (int) ((x2 - x) * factor), (int) ((y2 - y) * factor));
    }

    public static void resetCaps(final String scale) {
        if(!glCapMap.containsKey(scale)) {
            return;
        }
        Map<Integer, Boolean> map = glCapMap.get(scale);
        map.forEach(RenderUtils::setGlState);
        map.clear();
    }

    public static void resetCaps() {
        resetCaps("COMMON");
    }

    public static void clearCaps(final String scale) {
        if(!glCapMap.containsKey(scale)) {
            return;
        }
        Map<Integer, Boolean> map = glCapMap.get(scale);
        if(!map.isEmpty()) {
            ClientUtils.INSTANCE.logWarn("Cap map is not empty! [" + map.size() + "]");
        }
        map.clear();
    }

    public static void clearCaps() {
        clearCaps("COMMON");
    }

    public static void enableGlCap(final int cap, final String scale) {
        setGlCap(cap, true, scale);
    }

    public static void enableGlCap(final int cap) {
        enableGlCap(cap, "COMMON");
    }

    public static void disableGlCap(final int cap, final String scale) {
        setGlCap(cap, false, scale);
    }

    public static void disableGlCap(final int cap) {
        disableGlCap(cap, "COMMON");
    }


    public static void enableGlCap(final int... caps) {
        for(int cap : caps) {
            setGlCap(cap, true, "COMMON");
        }
    }

    public static void disableGlCap(final int... caps) {
        for(int cap : caps) {
            setGlCap(cap, false, "COMMON");
        }
    }

    public static void setGlCap(final int cap, final boolean state, final String scale) {
        if(!glCapMap.containsKey(scale)) {
            glCapMap.put(scale, new HashMap<>());
        }
        glCapMap.get(scale).put(cap, glGetBoolean(cap));
        setGlState(cap, state);
    }

    public static void setGlCap(final int cap, final boolean state) {
        setGlCap(cap, state, "COMMON");
    }

    public static void setGlState(final int cap, final boolean state) {
        if (state)
            glEnable(cap);
        else
            glDisable(cap);
    }

    public static double[] convertTo2D(final double x, final double y, final double z) {
        final FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        final IntBuffer viewport = BufferUtils.createIntBuffer(16);
        final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(2982, modelView);
        GL11.glGetFloat(2983, projection);
        GL11.glGetInteger(2978, viewport);
        final boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport,
                screenCoords);
        return (double[]) (result
                ? new double[] { screenCoords.get(0), Display.getHeight() - screenCoords.get(1), screenCoords.get(2) }
                : null);
    }

    public static void rectangle(double left, double top, double right, double bottom, int color) {
        double var5;
        if (left < right) {
            var5 = left;
            left = right;
            right = var5;
        }
        if (top < bottom) {
            var5 = top;
            top = bottom;
            bottom = var5;
        }
        float var11 = (float) (color >> 24 & 255) / 255.0f;
        float var6 = (float) (color >> 16 & 255) / 255.0f;
        float var7 = (float) (color >> 8 & 255) / 255.0f;
        float var8 = (float) (color & 255) / 255.0f;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate( 770,  771,  1,  0);
        GlStateManager.color( var6,  var7, var8, var11);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(left, bottom, 0.0).endVertex();
        worldRenderer.pos(right, bottom, 0.0).endVertex();
        worldRenderer.pos(right, top, 0.0).endVertex();
        worldRenderer.pos(left, top, 0.0).endVertex();
        tessellator.draw();
        enableTexture2D();
        disableBlend();
        GlStateManager.color( 1.0f,  1.0f,  1.0f, 1.0f);
    }

    public static Color reAlpha(Color cIn, float alpha){
        return new Color(cIn.getRed()/255f,cIn.getGreen()/255f,cIn.getBlue()/255f,cIn.getAlpha() / 255f * alpha);
    }

    public static void rectangleBordered(double x, double y, double x1, double y1, double width, int internalColor,
                                         int borderColor) {
        RenderUtils.rectangle(x + width, y + width, x1 - width, y1 - width, internalColor);
        GlStateManager.color( 1.0f,  1.0f, 1.0f, 1.0f);
        RenderUtils.rectangle(x + width, y, x1 - width, y + width, borderColor);
        GlStateManager.color(1.0f, 1.0f,  1.0f,  1.0f);
        RenderUtils.rectangle(x, y, x + width, y1, borderColor);
        GlStateManager.color( 1.0f,  1.0f,  1.0f,  1.0f);
        RenderUtils.rectangle(x1 - width, y, x1, y1, borderColor);
        GlStateManager.color( 1.0f,  1.0f,  1.0f, 1.0f);
        RenderUtils.rectangle(x + width, y1 - width, x1 - width, y1, borderColor);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void skeetRect(final double x, final double y, final double x1, final double y1, final double size) {
        RenderUtils.rectangleBordered(x, y - 4.0, x1 + size, y1 + size, 0.5, new Color(60, 60, 60).getRGB(), new Color(10, 10, 10).getRGB());
        RenderUtils.rectangleBordered(x + 1.0, y - 3.0, x1 + size - 1.0, y1 + size - 1.0, 1.0, new Color(40, 40, 40).getRGB(), new Color(40, 40, 40).getRGB());
        RenderUtils.rectangleBordered(x + 2.5, y - 1.5, x1 + size - 2.5, y1 + size - 2.5, 0.5, new Color(40, 40, 40).getRGB(), new Color(60, 60, 60).getRGB());
        RenderUtils.rectangleBordered(x + 2.5, y - 1.5, x1 + size - 2.5, y1 + size - 2.5, 0.5, new Color(22, 22, 22).getRGB(), new Color(255, 255, 255, 0).getRGB());
    }

    public static void skeetRectSmall(final double x, final double y, final double x1, final double y1, final double size) {
        RenderUtils.rectangleBordered(x + 4.35, y + 0.5, x1 + size - 84.5, y1 + size - 4.35, 0.5, new Color(48, 48, 48).getRGB(), new Color(10, 10, 10).getRGB());
        RenderUtils.rectangleBordered(x + 5.0, y + 1.0, x1 + size - 85.0, y1 + size - 5.0, 0.5, new Color(17, 17, 17).getRGB(), new Color(255, 255, 255, 0).getRGB());
    }

    public static void drawOutlinedBoundingBox(AxisAlignedBB aa) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(3, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(1, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB aa) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        tessellator.draw();
    }

    public static void originalRoundedRect(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        float z = 0;
        if (paramXStart > paramXEnd) {
            z = paramXStart;
            paramXStart = paramXEnd;
            paramXEnd = z;
        }

        if (paramYStart > paramYEnd) {
            z = paramYStart;
            paramYStart = paramYEnd;
            paramYEnd = z;
        }

        double x1 = paramXStart + radius;
        double y1 = paramYStart + radius;
        double x2 = paramXEnd - radius;
        double y2 = paramYEnd - radius;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        worldrenderer.begin(GL_POLYGON, DefaultVertexFormats.POSITION);

        double degree = Math.PI / 180;
        for (double i = 0; i <= 90; i += 1)
            worldrenderer.pos(x2 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius, 0.0D).endVertex();
        for (double i = 90; i <= 180; i += 1)
            worldrenderer.pos(x2 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius, 0.0D).endVertex();
        for (double i = 180; i <= 270; i += 1)
            worldrenderer.pos(x1 + Math.sin(i * degree) * radius, y1 + Math.cos(i * degree) * radius, 0.0D).endVertex();
        for (double i = 270; i <= 360; i += 1)
            worldrenderer.pos(x1 + Math.sin(i * degree) * radius, y2 + Math.cos(i * degree) * radius, 0.0D).endVertex();

        tessellator.draw();
        enableTexture2D();
        disableBlend();
    }

    public static void drawFilledCircle(final int xx, final int yy, final float radius, final Color color) {
        int sections = 50;
        double dAngle = 2 * Math.PI / sections;
        float x, y;

        glPushAttrib(GL_ENABLE_BIT);

        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glBegin(GL_TRIANGLE_FAN);

        for (int i = 0; i < sections; i++) {
            x = (float) (radius * Math.sin((i * dAngle)));
            y = (float) (radius * Math.cos((i * dAngle)));

            glColor4f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
            glVertex2f(xx + x, yy + y);
        }

        GlStateManager.color(0, 0, 0);

        glEnd();

        glPopAttrib();
    }

    public static void drawFilledCircle(final float xx, final float yy, final float radius, final Color color) {
        int sections = 50;
        double dAngle = 2 * Math.PI / sections;
        float x, y;

        glPushAttrib(GL_ENABLE_BIT);

        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glBegin(GL_TRIANGLE_FAN);

        for (int i = 0; i < sections; i++) {
            x = (float) (radius * Math.sin((i * dAngle)));
            y = (float) (radius * Math.cos((i * dAngle)));

            glColor4f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
            glVertex2f(xx + x, yy + y);
        }

        GlStateManager.color(0, 0, 0);

        glEnd();

        glPopAttrib();
    }

    public static void drawLine(final float x, final float y, final float x1, final float y1, final float width) {
        glDisable(GL_TEXTURE_2D);
        glLineWidth(width);
        glBegin(GL_LINES);
        glVertex2f(x, y);
        glVertex2f(x1, y1);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    public static void drawLine(final double x, final double y, final double x1, final double y1, final float width) {
        glDisable(GL_TEXTURE_2D);
        glLineWidth(width);
        glBegin(GL_LINES);
        glVertex2d(x, y);
        glVertex2d(x1, y1);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    public static void startDrawing() {
        GL11.glEnable(3042);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        Minecraft.getMinecraft().entityRenderer.setupCameraTransform(Minecraft.getMinecraft().timer.renderPartialTicks, 0);
    }

    public static void stopDrawing() {
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
    }

    public static void drawExhiRect(float x, float y, float x2, float y2) {
        drawRect(x - 3.5F, y - 3.5F, x2 + 3.5F, y2 + 3.5F, Color.black.getRGB());
        drawRect(x - 3F, y - 3F, x2 + 3F, y2 + 3F, new Color(50, 50, 50).getRGB());
        //drawBorder(x - 1.5F, y - 1.5F, x2 + 1.5F, y2 + 1.5F, 2.5F, new Color(26, 26, 26).getRGB());
        drawRect(x - 2.5F, y - 2.5F, x2 + 2.5F, y2 + 2.5F, new Color(26, 26, 26).getRGB());
        drawRect(x - 0.5F, y - 0.5F, x2 + 0.5F, y2 + 0.5F, new Color(50, 50, 50).getRGB());
        drawRect(x, y, x2, y2, new Color(18, 18, 18).getRGB());
    }

    public static void drawExhiRect(float x, float y, float x2, float y2, float alpha) {
        drawRect(x - 3.5F, y - 3.5F, x2 + 3.5F, y2 + 3.5F, new Color(0, 0, 0, alpha).getRGB());
        drawRect(x - 3F, y - 3F, x2 + 3F, y2 + 3F, new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha).getRGB());
        drawRect(x - 2.5F, y - 2.5F, x2 + 2.5F, y2 + 2.5F, new Color(26F / 255F, 26F / 255F, 26F / 255F, alpha).getRGB());
        drawRect(x - 0.5F, y - 0.5F, x2 + 0.5F, y2 + 0.5F, new Color(50F / 255F, 50F / 255F, 50F / 255F, alpha).getRGB());
        drawRect(x, y, x2, y2, new Color(18F / 255F, 18 / 255F, 18F / 255F, alpha).getRGB());
    }

    public static void drawRoundedRect(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius, int color) {
        drawRoundedRect(paramXStart, paramYStart, paramXEnd, paramYEnd, radius, color, true);
    }
    public static void drawEntityOnScreen(final double posX, final double posY, final float scale, final EntityLivingBase entity) {
        GlStateManager.pushMatrix();
        GlStateManager.enableColorMaterial();

        GlStateManager.translate(posX, posY, 50.0);
        GlStateManager.scale((-scale), scale, scale);
        GlStateManager.rotate(180F, 0F, 0F, 1F);
        GlStateManager.rotate(135F, 0F, 1F, 0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135F, 0F, 1F, 0F);
        GlStateManager.translate(0.0, 0.0, 0.0);

        RenderManager rendermanager = mc.getRenderManager();
        rendermanager.setPlayerViewY(180F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntityWithPosYaw(entity, 0.0, 0.0, 0.0, 0F, 1F);
        rendermanager.setRenderShadow(true);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static void drawEntityOnScreen(final int posX, final int posY, final int scale, final EntityLivingBase entity) {
        drawEntityOnScreen(posX, posY, (float) scale, entity);
    }
    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight, int width, int height, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(u * f, (v + (float) vHeight) * f1).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex((u + (float) uWidth) * f, (v + (float) vHeight) * f1).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex((u + (float) uWidth) * f, v * f1).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }

    public static void drawScaledCustomSizeModalCircle(int x, int y, float u, float v, int uWidth, int vHeight, int width, int height, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL_POLYGON, DefaultVertexFormats.POSITION_TEX);
        float xRadius = width / 2f;
        float yRadius = height / 2f;
        float uRadius = (((u + (float) uWidth) * f) - (u * f)) / 2f;
        float vRadius = (((v + (float) vHeight) * f1) - (v * f1)) / 2f;
        for(int i = 0; i <= 360; i+=10) {
            double xPosOffset = Math.sin(i * Math.PI / 180.0D);
            double yPosOffset = Math.cos(i * Math.PI / 180.0D);
            worldrenderer.pos(x + xRadius + xPosOffset * xRadius, y + yRadius + yPosOffset * yRadius, 0)
                    .tex(u * f + uRadius + xPosOffset * uRadius, v * f1 + vRadius + yPosOffset * vRadius).endVertex();
        }
        tessellator.draw();
    }

    public static void drawHead(ResourceLocation skin, int x, int y, int width, int height, int color) {
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GlStateManager.color(f, f1, f2, f3);
        mc.getTextureManager().bindTexture(skin);
        RenderUtils.drawScaledCustomSizeModalRect(x, y, 8F, 8F, 8, 8, width, height,
                64F, 64F);
        RenderUtils.drawScaledCustomSizeModalRect(x, y, 40F, 8F, 8, 8, width, height,
                64F, 64F);
    }

    public static void quickDrawBorderedRect(final float x, final float y, final float x2, final float y2, final float width, final int color1, final int color2) {
        quickDrawRect(x, y, x2, y2, color2);

        glColor(color1);
        glLineWidth(width);

        glBegin(GL_LINE_LOOP);

        glVertex2d(x2, y);
        glVertex2d(x, y);
        glVertex2d(x, y2);
        glVertex2d(x2, y2);

        glEnd();
    }
    public static void quickDrawHead(ResourceLocation skin, int x, int y, int width, int height) {
        mc.getTextureManager().bindTexture(skin);
        RenderUtils.drawScaledCustomSizeModalRect(x, y, 8F, 8F, 8, 8, width, height,
                64F, 64F);
        RenderUtils.drawScaledCustomSizeModalRect(x, y, 40F, 8F, 8, 8, width, height,
                64F, 64F);
    }

    // skid in https://github.com/WYSI-Foundation/LiquidBouncePlus/
    public static void drawBorder(float x, float y, float x2, float y2, float width, int color1) {
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);

        glColor(color1);
        glLineWidth(width);

        glBegin(GL_LINE_LOOP);

        glVertex2d(x2 + 1, y - 1);
        glVertex2d(x - 1, y - 1);
        glVertex2d(x - 1, y2 + 1);
        glVertex2d(x2 + 1, y2 + 1);

        glEnd();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
    }

    public static void drawOutLineRect(double x, double y, double x1, double y1, double width, int internalColor, int borderColor) {
        drawRect(x + width, y + width, x1 - width, y1 - width, internalColor);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(x + width, y, x1 - width, y + width, borderColor);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(x, y, x + width, y1, borderColor);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(x1 - width, y, x1, y1, borderColor);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawRect(x + width, y1 - width, x1 - width, y1, borderColor);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void enableSmoothLine(float width) {
        GL11.glDisable(3008);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2884);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glLineWidth(width);
    }

    public static void disableSmoothLine() {
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3008);
        GL11.glDepthMask(true);
        GL11.glCullFace(1029);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glHint(3155, 4352);
    }

    public static void directDrawAWTShape(Shape shape, double epsilon) {
        drawAWTShape(shape, epsilon, DirectTessCallback.INSTANCE);
    }

    public static void startSmooth() {
        glEnable(2848);
        glEnable(2881);
        glEnable(2832);
        glEnable(3042);
        glBlendFunc(770, 771);
        glHint(3154, 4354);
        glHint(3155, 4354);
        glHint(3153, 4354);
    }

    public static void endSmooth() {
        glDisable(2848);
        glDisable(2881);
        glEnable(2832);
    }

    /**
     * Rendering AWT graphics in LWJGL
     * @param shape graphics are ready to render
     * @param epsilon Graphic fineness, pass 0 without processing
     */
    public static void drawAWTShape(Shape shape, double epsilon, GLUtessellatorCallbackAdapter gluTessCallback) {
        PathIterator path=shape.getPathIterator(new AffineTransform());
        Double[] cp=new Double[2]; // Record the point of the last operation used to calculate the curve

        GLUtessellator tess = GLU.gluNewTess(); // Create a GLUtesselator for rendering concave polygons (GL_POLYGON can only render convex polygons)

        tess.gluTessCallback(GLU.GLU_TESS_BEGIN, gluTessCallback);
        tess.gluTessCallback(GLU.GLU_TESS_END, gluTessCallback);
        tess.gluTessCallback(GLU.GLU_TESS_VERTEX, gluTessCallback);
        tess.gluTessCallback(GLU.GLU_TESS_COMBINE, gluTessCallback);

        switch (path.getWindingRule()){
            case PathIterator.WIND_EVEN_ODD:{
                tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
                break;
            }
            case PathIterator.WIND_NON_ZERO:{
                tess.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
                break;
            }
        }

        // Cache points on a single graphics path for refinement to improve performance
        ArrayList<Double[]> pointsCache = new ArrayList<>();

        tess.gluTessBeginPolygon(null);

        while (!path.isDone()){
            double[] segment=new double[6];
            int type=path.currentSegment(segment);
            switch (type){
                case PathIterator.SEG_MOVETO:{
                    tess.gluTessBeginContour();
                    pointsCache.add(new Double[] {segment[0], segment[1]});
                    cp[0] = segment[0];
                    cp[1] = segment[1];
                    break;
                }
                case PathIterator.SEG_LINETO:{
                    pointsCache.add(new Double[] {segment[0], segment[1]});
                    cp[0] = segment[0];
                    cp[1] = segment[1];
                    break;
                }
                case PathIterator.SEG_QUADTO:{
                    Double[][] points=MathUtils.getPointsOnCurve(new Double[][]{new Double[]{cp[0], cp[1]}, new Double[]{segment[0], segment[1]}, new Double[]{segment[2], segment[3]}}, 10);
                    pointsCache.addAll(Arrays.asList(points));
                    cp[0] = segment[2];
                    cp[1] = segment[3];
                    break;
                }
                case PathIterator.SEG_CUBICTO:{
                    Double[][] points=MathUtils.getPointsOnCurve(new Double[][]{new Double[]{cp[0], cp[1]}, new Double[]{segment[0], segment[1]}, new Double[]{segment[2], segment[3]}, new Double[]{segment[4], segment[5]}}, 10);
                    pointsCache.addAll(Arrays.asList(points));
                    cp[0] = segment[4];
                    cp[1] = segment[5];
                    break;
                }
                case PathIterator.SEG_CLOSE:{
                    // Points on a reduced path
                    for(Double[] point : MathUtils.simplifyPoints(pointsCache.toArray(new Double[0][0]), epsilon)){
                        tessVertex(tess, new double[] {point[0], point[1], 0.0, 0.0, 0.0, 0.0});
                    }
                    // Clear the cache to draw the next figure
                    pointsCache.clear();
                    tess.gluTessEndContour();
                    break;
                }
            }
            path.next();
        }

        tess.gluEndPolygon();
        tess.gluDeleteTess();
    }


    public static void drawNewRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }
        float f3 = (float)(color >> 24 & 0xFF) / 255.0f;
        float f = (float)(color >> 16 & 0xFF) / 255.0f;
        float f1 = (float)(color >> 8 & 0xFF) / 255.0f;
        float f2 = (float)(color & 0xFF) / 255.0f;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer vertexbuffer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
        GlStateManager.color(f, f1, f2, f3);
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(left, bottom, 0.0).endVertex();
        vertexbuffer.pos(right, bottom, 0.0).endVertex();
        vertexbuffer.pos(right, top, 0.0).endVertex();
        vertexbuffer.pos(left, top, 0.0).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawCheckeredBackground(final float x, float y, final float x2, final float y2) {
        drawRect(x, y, x2, y2, getColor(16777215));
        boolean offset = false;
        while (y < y2) {
            for (float x3 = x + ((offset = !offset) ? 1 : 0); x3 < x2; x3 += 2.0f) {
                if (x3 <= x2 - 1.0f) {
                    drawRect(x3, y, x3 + 1.0f, y + 1.0f, getColor(8421504));
                }
            }
            ++y;
        }
    }

    public static int getColor(final int color) {
        final int r = color >> 16 & 0xFF;
        final int g = color >> 8 & 0xFF;
        final int b = color & 0xFF;
        final int a = 255;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF) | (a & 0xFF) << 24;
    }

    public static void scissor(final double x, final double y, final double width, final double height) {
        int scaleFactor;
        for (scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor(); scaleFactor < 2 && Minecraft.getMinecraft().displayWidth / (scaleFactor + 1) >= 320 && Minecraft.getMinecraft().displayHeight / (scaleFactor + 1) >= 240; ++scaleFactor) {}
        GL11.glScissor((int)(x * scaleFactor), (int)(Minecraft.getMinecraft().displayHeight - (y + height) * scaleFactor), (int)(width * scaleFactor), (int)(height * scaleFactor));
    }

    public static void drawRoundedRectd(float paramXStart, float paramYStart, float paramXEnd, float paramYEnd, float radius, int color) {
        drawRoundedRect(paramXStart, paramYStart, paramXEnd, paramYEnd, radius, color, true);
    }

    public static void drawGradientRect(final double d, final double e, final double e2, final double g, final int startColor, final int endColor) {
        final float f = (startColor >> 24 & 0xFF) / 255.0f;
        final float f2 = (startColor >> 16 & 0xFF) / 255.0f;
        final float f3 = (startColor >> 8 & 0xFF) / 255.0f;
        final float f4 = (startColor & 0xFF) / 255.0f;
        final float f5 = (endColor >> 24 & 0xFF) / 255.0f;
        final float f6 = (endColor >> 16 & 0xFF) / 255.0f;
        final float f7 = (endColor >> 8 & 0xFF) / 255.0f;
        final float f8 = (endColor & 0xFF) / 255.0f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
        GlStateManager.shadeModel(7425);
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer bufferbuilder = tessellator.getWorldRenderer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(e2, e, (double)RenderUtils.zLevel).color(f2, f3, f4, f).endVertex();
        bufferbuilder.pos(d, e, (double)RenderUtils.zLevel).color(f2, f3, f4, f).endVertex();
        bufferbuilder.pos(d, g, (double)RenderUtils.zLevel).color(f6, f7, f8, f5).endVertex();
        bufferbuilder.pos(e2, g, (double)RenderUtils.zLevel).color(f6, f7, f8, f5).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void render(final int mode, final Runnable render) {
        GL11.glBegin(mode);
        render.run();
        GL11.glEnd();
    }

    public static void setup2DRendering(final Runnable f) {
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        f.run();
        GL11.glEnable(3553);
        GlStateManager.disableBlend();
    }


    public static void tessVertex(GLUtessellator tessellator, double[] coords) {
        tessellator.gluTessVertex(coords, 0, new VertexData(coords));
    }

    public static boolean inArea(float x, float y, float ax1, float ay1, float ax2, float ay2) {
        return x >= ax1 && x <= ax2 && y >= ay1 && y <= ay2;
    }

    public static boolean inArea(int x, int y, int ax1, int ay1, int ax2, int ay2) {
        return x >= ax1 && x <= ax2 && y >= ay1 && y <= ay2;
    }

    /**
     * Reads the image into a texture.
     * @return texture id
     * @author func16
     */
    public static int loadGlTexture(BufferedImage bufferedImage){
        int textureId = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bufferedImage.getWidth(), bufferedImage.getHeight(),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, ImageUtils.readImageToBuffer(bufferedImage));

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return textureId;
    }
    public static void drawModel(final float yaw, final float pitch, final EntityLivingBase entityLivingBase) {
        GlStateManager.resetColor();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0f, 0.0f, 50.0f);
        GlStateManager.scale(-50.0f, 50.0f, 50.0f);
        GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
        final float renderYawOffset = entityLivingBase.renderYawOffset;
        final float rotationYaw = entityLivingBase.rotationYaw;
        final float rotationPitch = entityLivingBase.rotationPitch;
        final float prevRotationYawHead = entityLivingBase.prevRotationYawHead;
        final float rotationYawHead = entityLivingBase.rotationYawHead;
        GlStateManager.rotate(135.0f, 0.0f, 1.0f, 0.0f);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((float) (-Math.atan(pitch / 40.0f) * 20.0), 1.0f, 0.0f, 0.0f);
        entityLivingBase.renderYawOffset = yaw - yaw / yaw * 0.4f;
        entityLivingBase.rotationYaw = yaw - yaw / yaw * 0.2f;
        entityLivingBase.rotationPitch = pitch;
        entityLivingBase.rotationYawHead = entityLivingBase.rotationYaw;
        entityLivingBase.prevRotationYawHead = entityLivingBase.rotationYaw;
        GlStateManager.translate(0.0f, 0.0f, 0.0f);
        final RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0f);
        renderManager.setRenderShadow(false);
        entityLivingBase.getAlwaysRenderNameTag();
        renderManager.renderEntityWithPosYaw(entityLivingBase, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        renderManager.setRenderShadow(true);
        entityLivingBase.renderYawOffset = renderYawOffset;
        entityLivingBase.rotationYaw = rotationYaw;
        entityLivingBase.rotationPitch = rotationPitch;
        entityLivingBase.prevRotationYawHead = prevRotationYawHead;
        entityLivingBase.rotationYawHead = rotationYawHead;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.resetColor();
    }
    public static void drawOutlinedStringCock(FontRenderer fr, String s, float x, float y, int color, int outlineColor) {
        fr.drawString(ColorUtils.stripColor(s), (int) (x - 1.0f), (int) y, outlineColor);
        fr.drawString(ColorUtils.stripColor(s), (int) x, (int) (y - 1.0f), outlineColor);
        fr.drawString(ColorUtils.stripColor(s), (int) (x + 1.0f), (int) y, outlineColor);
        fr.drawString(ColorUtils.stripColor(s), (int) x, (int) (y + 1.0f), outlineColor);
        fr.drawString(s, (int) x, (int) y, color);
    }
    /**
     * Smooth rounded rect
     * @author Shoroa
     * @param x : X pos
     * @param y : Y pos
     * @param x1 : X2 pos
     * @param y1 : Y2 pos
     * @param radius : round of edges;
     * @param colour : color;
     */
    public static void drawSmoothRoundedRect(float x, float y, float x1, float y1, float radius, Color colour) {
        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        ColorUtils.setColour(colour.getRGB());
        glEnable(GL_LINE_SMOOTH);
        glBegin(GL_POLYGON);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glBegin(GL_LINE_LOOP);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_LINE_SMOOTH);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();
        glLineWidth(1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }


    /** rounded rect outline
     * @author Shoroa
     * @param x : X pos
     * @param y : Y pos
     * @param x1 : X2 pos
     * @param y1 : Y2 pos
     * @param radius : round of edges;
     * @param lineWidth : width of outline line;
     * @param colour : color;
     */
    public static void drawRoundedOutline(float x, float y, float x1, float y1, float radius,float lineWidth, int colour) {
        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        ColorUtils.setColour(colour);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(lineWidth);
        glBegin(GL_LINE_LOOP);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();
        glLineWidth(1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }


    /**
     * @author Shoroa
     * @param x : X pos
     * @param y : Y pos
     * @param x1 : X2 pos
     * @param y1 : Y2 pos
     * @param radius : round of edges;
     * @param color : color;
     * @param color2 : color2;
     * @param color3 : color3;
     * @param color4 : color4;
     */
    public static void drawRoundedGradientRectCorner(float x, float y, float x1, float y1, float radius, int color, int color2, int color3, int color4) {
        ColorUtils.setColour(-1);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        ColorUtils.setColour(color);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);
        glBegin(6);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color2);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color3);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        ColorUtils.setColour(color4);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();


        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
        ColorUtils.setColour(-1);
    }
    public static void drawRoundedGradientRectCorner(float x, float y, float x1, float y1, float radius, int color, int color2) {
        ColorUtils.setColour(-1);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        ColorUtils.setColour(color);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);
        glBegin(6);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color2);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        ColorUtils.setColour(color2);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();


        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
        ColorUtils.setColour(-1);
    }
    /**
     * @param x : X pos
     * @param y : Y pos
     * @param x1 : X2 pos
     * @param y1 : Y2 pos
     * @param width : width of line;
     * @param radius : round of edges;
     * @param color : color;
     * @param color2 : color2;
     * @param color3 : color3;
     * @param color4 : color4;
     */
    public static void drawRoundedGradientOutlineCorner(float x, float y, float x1, float y1, float width, float radius, int color, int color2, int color3, int color4) {
        ColorUtils.setColour(-1);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        ColorUtils.setColour(color);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);
        glLineWidth(width);
        glBegin(GL_LINE_LOOP);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color2);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color3);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        ColorUtils.setColour(color4);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glLineWidth(1);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();


        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
        ColorUtils.setColour(-1);
    }
    public static void drawRoundedGradientOutlineCorner(float x, float y, float x1, float y1, float width, float radius, int color, int color2) {
        ColorUtils.setColour(-1);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);

        glPushAttrib(0);
        glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0F;
        y *= 2.0F;
        x1 *= 2.0F;
        y1 *= 2.0F;
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        ColorUtils.setColour(color);
        glEnable(GL_LINE_SMOOTH);
        glShadeModel(GL_SMOOTH);
        glLineWidth(width);
        glBegin(GL_LINE_LOOP);
        int i;
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        ColorUtils.setColour(color2);
        for (i = 0; i <= 90; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        ColorUtils.setColour(color2);
        for (i = 90; i <= 180; i += 3)
            glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);
        glEnd();
        glLineWidth(1);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glScaled(2.0D, 2.0D, 2.0D);
        glPopAttrib();


        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glShadeModel(GL_FLAT);
        ColorUtils.setColour(-1);
    }
    public static void otherDrawOutlinedBoundingBox(float yaw, double x, double y, double z, double width, double height) {
        width *= 1.5D;
        yaw = MathHelper.wrapAngleTo180_float(yaw) + 45.0F;
        float yaw1, yaw2, yaw3, yaw4;
        if(yaw < 0.0F) {
            yaw1 = 0.0F;
            yaw1 += 360.0F - Math.abs(yaw);
        } else {
            yaw1 = yaw;
        }
        yaw1 *= -1.0F;
        yaw1 = (float)(yaw1 * 0.017453292519943295D);

        yaw += 90;
        if(yaw < 0.0F) {
            yaw2 = 0.0F;
            yaw2 += 360.0F - Math.abs(yaw);
        } else {
            yaw2 = yaw;
        }
        yaw2 *= -1.0F;
        yaw2 = (float)(yaw2 * 0.017453292519943295D);

        yaw += 90F;
        if(yaw < 0.0F) {
            yaw3 = 0.0F;
            yaw3 += 360.0F - Math.abs(yaw);
        } else {
            yaw3 = yaw;
        }
        yaw3 *= -1.0F;
        yaw3 = (float)(yaw3 * 0.017453292519943295D);

        yaw += 90F;
        if(yaw < 0.0F) {
            yaw4 = 0.0F;
            yaw4 += 360.0F - Math.abs(yaw);
        } else {
            yaw4 = yaw;
        }
        yaw4 *= -1.0F;
        yaw4 = (float)(yaw4 * 0.017453292519943295D);

        float x1 = (float)(Math.sin(yaw1) * width + x);
        float z1 = (float)(Math.cos(yaw1) * width + z);
        float x2 = (float)(Math.sin(yaw2) * width + x);
        float z2 = (float)(Math.cos(yaw2) * width + z);
        float x3 = (float)(Math.sin(yaw3) * width + x);
        float z3 = (float)(Math.cos(yaw3) * width + z);
        float x4 = (float)(Math.sin(yaw4) * width + x);
        float z4 = (float)(Math.cos(yaw4) * width + z);
        float y2 = (float)(y + height);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1, y, z1).endVertex();
        worldrenderer.pos(x1, y2, z1).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x1, y, z1).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x2, y, z2).endVertex();
        worldrenderer.pos(x3, y, z3).endVertex();
        worldrenderer.pos(x4, y, z4).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x1, y2, z1).endVertex();
        worldrenderer.pos(x1, y, z1).endVertex();
        tessellator.draw();
    }
    public static void otherDrawBoundingBox(float yaw, double x, double y, double z, double width, double height) {
        width *= 1.5D;
        yaw = MathHelper.wrapAngleTo180_float(yaw) + 45.0F;
        float yaw1, yaw2, yaw3, yaw4;
        if(yaw < 0.0F) {
            yaw1 = 0.0F;
            yaw1 += 360.0F - Math.abs(yaw);
        } else {
            yaw1 = yaw;
        }
        yaw1 *= -1.0F;
        yaw1 = (float)((double)yaw1 * 0.017453292519943295D);

        yaw += 90F;
        if(yaw < 0.0F) {
            yaw2 = 0.0F;
            yaw2 += 360.0F - Math.abs(yaw);
        } else {
            yaw2 = yaw;
        }

        yaw2 *= -1.0F;
        yaw2 = (float)((double)yaw2 * 0.017453292519943295D);
        yaw += 90F;
        if(yaw < 0.0F) {
            yaw3 = 0.0F;
            yaw3 += 360.0F - Math.abs(yaw);
        } else {
            yaw3 = yaw;
        }

        yaw3 *= -1.0F;
        yaw3 = (float)((double)yaw3 * 0.017453292519943295D);
        yaw += 90F;
        if(yaw < 0.0F) {
            yaw4 = 0.0F;
            yaw4 += 360.0F - Math.abs(yaw);
        } else {
            yaw4 = yaw;
        }

        yaw4 *= -1.0F;
        yaw4 = (float)((double)yaw4 * 0.017453292519943295D);
        float x1 = (float)(Math.sin(yaw1) * width + x);
        float z1 = (float)(Math.cos(yaw1) * width + z);
        float x2 = (float)(Math.sin(yaw2) * width + x);
        float z2 = (float)(Math.cos(yaw2) * width + z);
        float x3 = (float)(Math.sin(yaw3) * width + x);
        float z3 = (float)(Math.cos(yaw3) * width + z);
        float x4 = (float)(Math.sin(yaw4) * width + x);
        float z4 = (float)(Math.cos(yaw4) * width + z);
        float y1 = (float)y;
        float y2 = (float)(y + height);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x1, y1, z1).endVertex();
        worldrenderer.pos(x1, y2, z1).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x2, y1, z2).endVertex();
        worldrenderer.pos(x2, y1, z2).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x3, y1, z3).endVertex();
        worldrenderer.pos(x3, y1, z3).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x4, y1, z4).endVertex();
        worldrenderer.pos(x4, y1, z4).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        worldrenderer.pos(x1, y2, z1).endVertex();
        worldrenderer.pos(x1, y1, z1).endVertex();
        worldrenderer.pos(x1, y1, z1).endVertex();
        worldrenderer.pos(x2, y1, z2).endVertex();
        worldrenderer.pos(x3, y1, z3).endVertex();
        worldrenderer.pos(x4, y1, z4).endVertex();
        worldrenderer.pos(x1, y2, z1).endVertex();
        worldrenderer.pos(x2, y2, z2).endVertex();
        worldrenderer.pos(x3, y2, z3).endVertex();
        worldrenderer.pos(x4, y2, z4).endVertex();
        tessellator.draw();
    }
    private static void drawEnchantTag(String text, int x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GL11.glScalef(0.5f, 0.5f, 0.5f);
        drawOutlinedStringCock(Minecraft.getMinecraft().fontRendererObj, text, x, y, -1, new Color(0, 0, 0, 220).darker().getRGB());
        GL11.glScalef(1.0f, 1.0f, 1.0f);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
    public static void renderEnchantText(ItemStack stack, int x, float y) {
        RenderHelper.disableStandardItemLighting();
        float enchantmentY = y + 24f;
        if (stack.getItem() instanceof ItemArmor) {
            int protectionLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
            int unbreakingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            int thornLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack);
            if (protectionLevel > 0) {
                RenderUtils.drawEnchantTag("P" + ColorUtils.getColor(protectionLevel) + protectionLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (unbreakingLevel > 0) {
                RenderUtils.drawEnchantTag("U" + ColorUtils.getColor(unbreakingLevel) + unbreakingLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (thornLevel > 0) {
                RenderUtils.drawEnchantTag("T" + ColorUtils.getColor(thornLevel) + thornLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
        }
        if (stack.getItem() instanceof ItemBow) {
            int powerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            int punchLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
            int flameLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack);
            int unbreakingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (powerLevel > 0) {
                RenderUtils.drawEnchantTag("Pow" + ColorUtils.getColor(powerLevel) + powerLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (punchLevel > 0) {
                RenderUtils.drawEnchantTag("Pun" + ColorUtils.getColor(punchLevel) + punchLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (flameLevel > 0) {
                RenderUtils.drawEnchantTag("F" + ColorUtils.getColor(flameLevel) + flameLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (unbreakingLevel > 0) {
                RenderUtils.drawEnchantTag("U" + ColorUtils.getColor(unbreakingLevel) + unbreakingLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
        }
        if (stack.getItem() instanceof ItemSword) {
            int sharpnessLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
            int knockbackLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
            int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);
            int unbreakingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);
            if (sharpnessLevel > 0) {
                RenderUtils.drawEnchantTag("S" +  ColorUtils.getColor(sharpnessLevel) + sharpnessLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (knockbackLevel > 0) {
                RenderUtils.drawEnchantTag("K" + ColorUtils.getColor(knockbackLevel) + knockbackLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (fireAspectLevel > 0) {
                RenderUtils.drawEnchantTag("F" + ColorUtils.getColor(fireAspectLevel) + fireAspectLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
            if (unbreakingLevel > 0) {
                RenderUtils.drawEnchantTag("U" + ColorUtils.getColor(unbreakingLevel) + unbreakingLevel, x * 2, enchantmentY);
                enchantmentY += 8;
            }
        }
        if (stack.getRarity() == EnumRarity.EPIC) {
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            drawOutlinedStringCock(Minecraft.getMinecraft().fontRendererObj, "God", x * 2, enchantmentY, new Color(255, 255, 0).getRGB(), new Color(100, 100, 0, 200).getRGB());
            GL11.glScalef(1.0f, 1.0f, 1.0f);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}
