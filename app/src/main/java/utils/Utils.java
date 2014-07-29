package utils;

import android.app.backup.BackupManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.siqi.bits.Task;
import com.siqi.bits.app.ui.InAppPurchaseActivity;

import org.apache.http.Header;

import java.util.Random;

import interfaces.Clock;
import interfaces.IabSetupActionHandler;

/**
 * Proudly powered by me on 5/21/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class Utils {
    public static final String TAG = "Utils";
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
    public static final String IS_BITS_ADS_SUPPORT_ENABLED = "BITS_ADS_SUPPORT_ENABLED";

    public static boolean GOD_MODE_ON = false;
    public static DisplayMetrics mDisplayMetrics;
    public static IabHelper mIabHelper;
    private static InterstitialAd interstitialAd;

    private static Clock mClock = new SystemClock();
    private static Random mRandomiser = new Random();
    private static BitsRestClient mBitsRestClient = new BitsRestClient();

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

    public static int getRandomInt(int range) {
        return mRandomiser.nextInt(range);
    }

    public static void setupIabHelper(final Context ctx, final IabSetupActionHandler handle) {
        /**
         * In-app billing
         */

        if (Utils.mIabHelper != null && Utils.mIabHelper.isSetUpDone()) {
            handle.onSetupDone();
            return;
        }

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiNVRydS9aTj3bUDSTVt3bPrWt/AAgHF3Gl6rWTVxE+EjyUC09Wnoeh4V8uxjmQGx2DGUHXQzAa9pJli8zwEiTrr4CIjUZrjOPeLrB+K+V7sWTRogpdXpcetSsblPuIKp0mnPkgc6TtucgeilVC5uLMLjWR+XvT7g1XVXxjuKCoU5pL5rLVigGIIOMp937Hkg1L165zrmxbhhRUjizVSfhF/TrW/al5Tp5WK21+Gufx5/p37U0EplepuNx5u0MgX4x5xpJa9bA8NqXAZafetexmc1Jxz+BNf8q+Qj/MCWHuOSvGj9/7EQMUZ7bZN41vHitHUWdeGWy2LH3CLLP516MQIDAQAB";
        mIabHelper = new IabHelper(ctx, base64EncodedPublicKey);
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d("In-App Purchase", "Problem setting up In-app Billing: " + result);
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                    return;
                }
                Log.d("In-App Purchase", "Set-up: Success");
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                handle.onSetupDone();
            }
        });
    }

    public static void checkIfPremiumPurchased(final Context ctx) {
        IabHelper.QueryInventoryFinishedListener gotInventoryListener
                = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result,
                                                 Inventory inventory) {
                if (result.isFailure()) {
                    Log.d("In-App Purchase", "query purchased item failed: " + result);
                } else {
                    if (inventory.hasPurchase(InAppPurchaseActivity.SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK)) {
                        PreferenceManager.getDefaultSharedPreferences(ctx)
                                .edit()
                                .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true)
                                .putBoolean(Utils.IS_BITS_ADS_SUPPORT_ENABLED, false)
                                .commit();
                        Log.d("In-App Purchase", "purchased item restored");
                    } else {
                        // We will potentially face the problem that users could have faked their payment
                        // and then have more than 5 tasks. Here we will reset unlocked to false but not
                        // ads support. Which means users haven't paid but don't get ads
                        // However, users will not be able to add new tasks.
                        boolean adsActivated = PreferenceManager.getDefaultSharedPreferences(ctx)
                                .getBoolean(Utils.IS_BITS_ADS_SUPPORT_ENABLED, false);
                        PreferenceManager.getDefaultSharedPreferences(ctx)
                                .edit()
                                .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, adsActivated)
                                .commit();
                        Log.d("In-App Purchase", "purchased item restored");
                    }
                }
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
            }
        };

        if (mIabHelper != null && mIabHelper.isSetUpDone())
            mIabHelper.queryInventoryAsync(gotInventoryListener);
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

    public static void loadInterstitialAds(Context context) {
        // Create the interstitial.
        interstitialAd = new InterstitialAd(context);
        interstitialAd.setAdUnitId("ca-app-pub-2295213482033436/3191624502");

        // Create ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Begin loading your interstitial.
        interstitialAd.loadAd(adRequest);
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public static void displayInterstitial() {
        Log.d(TAG, "displayInterstitial called, ads loaded: " + interstitialAd.isLoaded());
        if (interstitialAd.isLoaded() && getRandomInt(100) < 20) {
            interstitialAd.show();
        }
    }


    public static void requestBackup(Context ctx) {
        BackupManager bm = new BackupManager(ctx);
        bm.dataChanged();
        Log.d(TAG, "requestBackup sent");
    }

    public static void BitsAsyncUpload(Task t) {
        mBitsRestClient.post(t, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Log.d(TAG, "Post Bit with Success!");
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Log.d(TAG, "Post Bit with Failure code: " + i + throwable.getMessage());
            }
        });
    }
}
