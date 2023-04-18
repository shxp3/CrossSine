package net.ccbluex.liquidbounce.utils.AutoArmor;

public final class TimeUtil {

    public static long randomDelay(final int minDelay, final int maxDelay) {
        return RandomUtil.nextInt(minDelay, maxDelay);
    }
}