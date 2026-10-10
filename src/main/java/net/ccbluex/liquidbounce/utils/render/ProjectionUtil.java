package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ProjectionUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer VECTOR = BufferUtils.createFloatBuffer(4);

    public static float[] project2D(double x, double y, double z) {

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);

        boolean success = GLU.gluProject(
                (float) x,
                (float) y,
                (float) z,
                MODELVIEW,
                PROJECTION,
                VIEWPORT,
                VECTOR
        );

        if (!success) return null;

        float screenX = VECTOR.get(0);
        float screenY = mc.displayHeight - VECTOR.get(1);

        return new float[]{screenX, screenY};
    }
}
