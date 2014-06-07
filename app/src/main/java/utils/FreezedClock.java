package utils;

/**
 * Proudly powered by me on 6/15/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class FreezedClock implements Clock {

    private long mCurrentTime;


    public FreezedClock() {
        mCurrentTime = System.currentTimeMillis();
    }

    @Override
    public long currentTimeMillis() {
        return mCurrentTime;
    }

    public void setCurrentTime(long currentTime) {
        mCurrentTime = currentTime;
    }

    public void setMoveAheadMillis(long delta) {
        mCurrentTime += delta;
    }
}
