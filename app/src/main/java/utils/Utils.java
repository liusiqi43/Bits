package utils;

import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Proudly powered by me on 5/21/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class Utils {
    public static DisplayMetrics mDisplayMetrics;

    public static int dpToPx(int dp) {
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics));
        return px;
    }
}