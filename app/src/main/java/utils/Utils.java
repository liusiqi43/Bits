package utils;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.siqi.bits.app.ui.BitsListFragment;
import com.siqi.bits.app.ui.InAppPurchaseActivity;

/**
 * Proudly powered by me on 5/21/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class Utils {
    public static DisplayMetrics mDisplayMetrics;
    public static IabHelper mIabHelper;
    private static Clock mClock = new SystemClock();

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
                    PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(BitsListFragment.TASKS_COUNT_LIMIT_UNLOCKED, inventory.hasPurchase(InAppPurchaseActivity.SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK));
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
