package utils;

import interfaces.Clock;

/**
 * Proudly powered by me on 6/14/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class SystemClock implements Clock {

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
