package net.ccbluex.liquidbounce.ui.client.gui;

public class AnimationUtil {

    public static float animate(float target, float current, double speed) {
        boolean larger = target > current;
        if (speed < 0.0F) {
            speed = 0.0F;
        } else if (speed > 1.0F) {
            speed = 1.0F;
        }
        float dif = Math.max(target, current) - Math.min(target, current);
        float factor = (float) (dif * speed);
        current = larger ? current + factor : current - factor;
        return current;
    }

    public static double animate(double target, double current, double speed) {
        boolean larger = target > current;
        if (speed < 0.0F) {
            speed = 0.0F;
        } else if (speed > 1.0F) {
            speed = 1.0F;
        }
        double dif = Math.max(target, current) - Math.min(target, current);
        double factor = dif * speed;
        if (factor < 0.1F) {
            factor = 0.1F;
        }
        current = larger ? current + factor : current - factor;
        return current;
    }
}

