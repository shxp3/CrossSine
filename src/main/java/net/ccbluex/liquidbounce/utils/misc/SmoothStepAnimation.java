package net.ccbluex.liquidbounce.utils.misc;


import net.ccbluex.liquidbounce.utils.render.AnimationUnit;

public class SmoothStepAnimation extends AnimationUnit
{
    public SmoothStepAnimation(final int ms, final double endPoint) {
        super(ms, endPoint);
    }

    public SmoothStepAnimation(final int ms, final double endPoint, final Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected double getEquation(final double x) {
        final double x2 = x / this.duration;
        return -2.0 * Math.pow(x2, 3.0) + 3.0 * Math.pow(x2, 2.0);
    }
}
