package net.ccbluex.liquidbounce.ui.client.gui.paint

import net.ccbluex.liquidbounce.utils.render.Pair
import net.ccbluex.liquidbounce.utils.render.RenderUtils.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.pow


class GuiPaint : GuiScreen() {
    val allCurves = mutableListOf<MutableList<Pair<Float, Float>>>()
    var currentCurve = mutableListOf<Pair<Float, Float>>()

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val width = ScaledResolution(mc).scaledWidth
        val height = ScaledResolution(mc).scaledHeight
        drawRect(width - 50F, height - 50F, 50F, 50F, Color(0,0,0,50))
        drawLine(currentCurve, allCurves)
        drawAllCurves(allCurves, currentCurve)
    }
    private fun drawAllCurves(allCurves: List<List<Pair<Float, Float>>>, currentCurve: List<Pair<Float, Float>>) {
        enableRender2D()

        allCurves.forEach { drawCurve(it, 2f, 0xFFFFFFFF.toInt()) }
        drawCurve(currentCurve, 2f, 0xFFFFFFFF.toInt())

        disableRender2D()
    }

    private fun drawLine(currentCurve: MutableList<Pair<Float, Float>>, allCurves: MutableList<MutableList<Pair<Float, Float>>>) {
        if (Mouse.isButtonDown(0)) {
            val point = Pair(Mouse.getX().toFloat() / 2, (Mouse.getY().toFloat() / 2) * -1)
            currentCurve.add(point)
        } else if (currentCurve.isNotEmpty()) {
            allCurves.add(currentCurve.toList().toMutableList())
            currentCurve.clear()
        }
    }
    private fun drawCurve(points: List<Pair<Float, Float>>, lineWidth: Float, color: Int) {
        if (points.size > 1) {
            GL11.glColor4f((color shr 16 and 0xFF) / 255.0f, (color shr 8 and 0xFF) / 255.0f, (color and 0xFF) / 255.0f, 1.0f)
            GL11.glLineWidth(lineWidth)

            GL11.glBegin(GL11.GL_LINE_STRIP)

            val n = points.size - 1
            for (i in 0 until n) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cp = calculateControlPoint(p0, p1, 0.5f)
                GL11.glVertex2f(p0.first, p0.second)
                GL11.glVertex2f(cp.first, cp.second)
            }

            val lastPoint = points.last()
            GL11.glVertex2f(lastPoint.first, lastPoint.second)

            GL11.glEnd()
        }
    }

    private fun calculateControlPoint(p0: Pair<Float, Float>, p1: Pair<Float, Float>, t: Float): Pair<Float, Float> {
        val x = (1 - t).pow(2) * p0.first + 2 * (1 - t) * t * p0.first + t.pow(2) * p1.first
        val y = (1 - t).pow(2) * p0.second + 2 * (1 - t) * t * p0.second + t.pow(2) * p1.second
        return Pair(x, y)
    }
}