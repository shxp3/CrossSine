package net.ccbluex.liquidbounce.features.module.modules.client.impl;

import net.ccbluex.liquidbounce.CrossSine;
import net.ccbluex.liquidbounce.features.special.NotificationUtil;
import net.ccbluex.liquidbounce.ui.font.Fonts.font40SemiBold;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import java.awt.Color;
import java.util.HashMap;

object Notification : MinecraftInstance() {
    private val fadeStates = HashMap<NotificationUtil, Float>();
    private val offsetXStates = HashMap<NotificationUtil, Float>();
    private val offsetYStates = HashMap<NotificationUtil, Float>(); // Added Y animation

    fun draw() {
        val width = ScaledResolution(mc).scaledWidth;
        val height = ScaledResolution(mc).scaledHeight;

        if (CrossSine.notification.list.isNotEmpty()) {
            var accumulatedOffsetY = 0F;

            val iterator = CrossSine.notification.list.iterator();
            while (iterator.hasNext()) {
                val noti = iterator.next();
                val fadeState = fadeStates.getOrDefault(noti, 0F);
                val offsetX = offsetXStates.getOrDefault(noti, 200F);
                val offsetY = offsetYStates.getOrDefault(noti, 50F); // Start from below

                // Update fade state
                val fadeDelta = (0.0075F * 0.7F * RenderUtils.deltaTime * if (System.currentTimeMillis() > noti.system + noti.timer) 1F else -1F);
                val newFadeState = (fadeState + fadeDelta).coerceIn(0F, 1F);
                fadeStates[noti] = newFadeState;

                // Update offsetX with easing
                val targetOffsetX = 0F;
                val offsetXDelta = (targetOffsetX - offsetX) * 0.15F;
                val newOffsetX = offsetX + offsetXDelta;
                offsetXStates[noti] = newOffsetX;

                // Update offsetY with easing (smooth slide-up effect)
                val targetOffsetY = accumulatedOffsetY;
                val offsetYDelta = (targetOffsetY - offsetY) * 0.15F;
                val newOffsetY = offsetY + offsetYDelta;
                offsetYStates[noti] = newOffsetY;

                val percent = EaseUtils.easeInCirc(newFadeState.toDouble()).toFloat();

                if (newFadeState >= 1F && System.currentTimeMillis() > noti.system + noti.timer) {
                    iterator.remove();
                    fadeStates.remove(noti);
                    offsetXStates.remove(noti);
                    offsetYStates.remove(noti);
                    continue;
                }

                val contentWidth = font40SemiBold.getStringWidth(noti.content);
                val x1 = width - 8F - contentWidth - 32F + newOffsetX + ((32F + contentWidth) * percent);
                val x2 = width.toFloat() + newOffsetX + ((8F + contentWidth) * percent);
                val y1 = height - 8F - font40SemiBold.height - 16F - newOffsetY;
                val y2 = height - 4F - newOffsetY;

                RenderUtils.drawBloomRoundedRect(
                    x1, y1, x2, y2, 4F, 2.5F,
                    Color(0, 0, 0, 120 - (120 * newFadeState).toInt()),
                    RenderUtils.ShaderBloom.BOTH
                );
                RenderUtils.drawImage(
                    ResourceLocation("crosssine/ui/notifications/${noti.type.name}.png"),
                    (width - 37 - contentWidth + ((contentWidth + 4) * percent).toInt()) + newOffsetX.toInt(),
                    height - 32 - newOffsetY.toInt(),
                    27, 27,
                    1F - newFadeState
                );
                font40SemiBold.drawString(
                    noti.content,
                    width - 8F - contentWidth + ((contentWidth + 4F) * percent) + newOffsetX,
                    height - 6F - font40SemiBold.height - newOffsetY,
                    Color(255, 255, 255, 255 - (255 * newFadeState).toInt()).rgb
                );
                font40SemiBold.drawString(
                    noti.title,
                    width - 8F - contentWidth + ((contentWidth + 4F) * percent) + newOffsetX,
                    height - 8F - font40SemiBold.height * 2 - newOffsetY,
                    Color(255, 255, 255, 255 - (255 * newFadeState).toInt()).rgb
                );

                // Adjust spacing
                accumulatedOffsetY += 35F;
            }
        }
    }
}
