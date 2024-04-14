package net.ccbluex.liquidbounce.utils.timer;

public final class TimerMS {

    public long time = -1L;

    public boolean hasTimePassed(final long MS) {
        return System.currentTimeMillis() >= time + MS;
    }
    public void reset() {
        time = System.currentTimeMillis();
    }
}
