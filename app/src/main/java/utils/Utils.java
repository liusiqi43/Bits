package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.siqi.bits.app.ui.InAppPurchaseActivity;

/**
 * Proudly powered by me on 5/21/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class Utils {
    public static final String IS_BITSLIST_HELP_ON = "IS_BITSLIST_HELP_ON";
    public static final String IS_BITSLIST_SHAKE_ON = "IS_BITSLIST_SHAKE_ON";
    public static final String IS_BITSLIST_LONGPRESS_HELP_ON = "IS_BITSLIST_LONGPRESS_HELP_ON";
    public static final String REWARD_HISTORY_ON_TAP_ENABLED = "REWARD_HISTORY_ON_TAP";
    public static final String REWARD_UNDO_ON_SHAKE_ENABLED = "REWARD_UNDO_ON_SHAKE_ENABLED";
    public static final String IS_FIRST_DONE = "IS_FIRST_DONE";
    public static final String IS_FIRST_SKIP = "IS_FIRST_SKIP";
    public static final String IS_FIRST_LATE = "IS_FIRST_LATE";
    public static final String IS_FIRST_TASK_ADDED = "IS_FIRST_TASK_ADDED";
    public static final String TASKS_COUNT_LIMIT_UNLOCKED = "TASKS_COUNT_LIMIT_UNLOCKED";
    public static final String IS_AUTO_ROTATE_ENABLED = "IS_AUTO_ROTATE_ENABLED";
    public static boolean GOD_MODE_ON = false;
    public static DisplayMetrics mDisplayMetrics;
    public static IabHelper mIabHelper;

    private static Clock mClock = new SystemClock();

    public static Bitmap invertImage(Bitmap src) {
        // create new bitmap with the same attributes(width,height)
        //as source bitmap
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        // color info
        int A, R, G, B;
        int pixelColor;
        // image size
        int height = src.getHeight();
        int width = src.getWidth();

        // scan through every pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // get one pixel
                pixelColor = src.getPixel(x, y);
                // saving alpha channel
                A = Color.alpha(pixelColor);
                // inverting byte for each R/G/B channel
                R = 255 - Color.red(pixelColor);
                G = 255 - Color.green(pixelColor);
                B = 255 - Color.blue(pixelColor);
                // set newly-inverted pixel to output image
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        return bmOut;
    }

    public static void setupIabHelper(final Context ctx) {
        /**
         * In-app billing
         */
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn1vS35/bnUlJ+TmB29ZkXcMTLHQmjxhG6rCl5iqRkbhFFk7QnCEyYTPVEQN5nRizs5pi9eyrXXe3Cm2e8Xh/xyH9upCMf9ICPF4TXRPqwmmmh0ghg9b/cnz3w8rrgzBZOCDiDl0agpeo0weiQ11UTdrLGXc7iS4tUx8LN7H9SMux62z6gkaMOOkJOdPTzH+cogE5HqBFGzg1AvR3lnGM+pDGm7L6rJ6omQcmeM2FonnDwzY1Ww+5OVutY4D4IQuwUPmGsOJMJwAY4JKbUiNMwkG1PsfQG/QSJc6dJ8oLq12dXKftB8bCQB3CDt50Nlp8AGp6g3deiC6TCHjqrx/NUwIDAQAB";
        mIabHelper = new IabHelper(ctx, base64EncodedPublicKey);
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d("In-App Purchase", "Problem setting up In-app Billing: " + result);
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                }
                Log.d("In-App Purchase", "Set-up: Success");

                checkIfPremiumPurchased(ctx);
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
            }
        });
    }

    public static void checkIfPremiumPurchased(final Context ctx) {
        IabHelper.QueryInventoryFinishedListener mGotInventoryListener
                = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result,
                                                 Inventory inventory) {
                if (result.isFailure()) {
                    Log.d("In-App Purchase", "query purchased item failed: " + result);
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                } else {
                    PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, inventory.hasPurchase(InAppPurchaseActivity.SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK));
                    Log.d("In-App Purchase", "purchased item restored");
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                }
            }
        };

        mIabHelper.queryInventoryAsync(mGotInventoryListener);
    }

    public static void setClockEntity(Clock clock) {
        mClock = clock;
    }

    public static int dpToPx(int dp) {
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics));
        return px;
    }

    public static long currentTimeMillis() {
        return mClock.currentTimeMillis();
    }
}
