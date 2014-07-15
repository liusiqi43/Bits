package com.siqi.bits.app.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.siqi.bits.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import utils.IabHelper;
import utils.IabResult;
import utils.Inventory;
import utils.Purchase;
import utils.Utils;

public class InAppPurchaseActivity extends ActionBarActivity {

    public static final String SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK = "active_tasks_count_limit_unlock";
    private static final String TAG = "InAppPurchaseActivity";
    //    public static final String SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK = "android.test.purchased";
    private SharedPreferences mPreferences;
    private ProgressDialog mProgressDialog;
    private boolean mPlayStoreConnectionSucceeded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_purchase);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        List additionalSkuList = new ArrayList();
        additionalSkuList.add(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK);

        mProgressDialog = new ProgressDialog(this, R.style.AppTheme_busyspinner);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        mProgressDialog.show();

        if (Utils.mIabHelper != null) {
            Utils.mIabHelper.queryInventoryAsync(true, additionalSkuList, new IabHelper.QueryInventoryFinishedListener() {
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (inventory == null || inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) == null) {
                        Log.d(TAG, "inventory == null || inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) == null");
                        mProgressDialog.cancel();
//                    buildUnexpectedFailureDialog();
                        if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                        mPlayStoreConnectionSucceeded = false;
                        return;
                    } else {
                        Log.d(TAG, "Inventory retrieved");
                        mProgressDialog.cancel();
                        if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                        mPlayStoreConnectionSucceeded = true;
                    }

                    String UnlockPrice =
                            inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK).getPrice();

                    TextView priceTextView = (TextView) findViewById(R.id.price_tag);
                    priceTextView.setText(getString(R.string.all_for_just) + " " + UnlockPrice);
                    priceTextView.setVisibility(View.VISIBLE);
                    mProgressDialog.cancel();
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                    mPlayStoreConnectionSucceeded = true;
                }
            });
        } else {
            mProgressDialog.cancel();
        }

        Button purchaseButton = (Button) findViewById(R.id.purchase_button);
        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayStoreConnectionSucceeded)
                    buildUpgradeChoiceDialog();
                else
                    buildFallbackOptionDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + ","
                + data);

        // Pass on the activity result to the helper for handling
        if (!Utils.mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    private void buildFallbackOptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.unexpected_error));
        subtitleView.setText(getString(R.string.but_you_can_optin_ads));

        builder.setView(v);
        builder.setTitle(getString(R.string.upgrade_now));

        builder.setPositiveButton(R.string.ads_support, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "Enabling ads-support");
                mPreferences.edit()
                        .putBoolean(Utils.BITS_ADS_SUPPORT_ENABLED, true)
                        .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true)
                        .commit();
                dialog.cancel();
                buildAdsSettingInfoDialog();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                onBackPressed();
            }
        });

        builder.show();
    }

    private void buildFailureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.failed_to_connect_to_play_store));
        subtitleView.setText(getString(R.string.make_sure_your_device_is_connected));

        builder.setView(v);
        builder.setTitle(getString(R.string.oops));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.show().setCancelable(false);
    }

    private void buildUnexpectedFailureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.unexpected_error));
        subtitleView.setText(getString(R.string.please_send_us_a_feedback));

        builder.setView(v);
        builder.setTitle(getString(R.string.oops));

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.show().setCancelable(false);
    }


    private void buildThankyouDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.thank_you_for_your_purchase));
        subtitleView.setText(getString(R.string.thanks_to_people_like_you));

        builder.setView(v);
        builder.setTitle(getString(R.string.success));

        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.show().setCancelable(false);
    }

    private void buildUpgradeChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.you_have_a_choice));
        subtitleView.setText(getString(R.string.I_understand));

        builder.setView(v);
        builder.setTitle(getString(R.string.upgrade_now));

        builder.setPositiveButton(R.string.buy_me_a_coffee, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();

                final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

                final String tmDevice, tmSerial, androidId;
                tmDevice = "" + tm.getDeviceId();
                tmSerial = "" + tm.getSimSerialNumber();
                androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

                UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
                String deviceId = deviceUuid.toString();


                Utils.mIabHelper.launchPurchaseFlow(InAppPurchaseActivity.this, SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK, 647, new IabHelper.OnIabPurchaseFinishedListener() {
                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                        if (Utils.GOD_MODE_ON) {
                            Log.d(TAG, "purchase done");
                            mPreferences.edit()
                                    .putBoolean(Utils.BITS_ADS_SUPPORT_ENABLED, false)
                                    .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true)
                                    .commit();
                            buildThankyouDialog();
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        }

                        if (result.isFailure()) {
//                            buildUnexpectedFailureDialog();
                            Log.d(TAG, "purchase failed:" + result);
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        } else if (purchase.getSku().equals(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK)) {
                            Log.d(TAG, "purchase done");
                            mPreferences.edit()
                                    .putBoolean(Utils.BITS_ADS_SUPPORT_ENABLED, false)
                                    .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true)
                                    .commit();
                            buildThankyouDialog();
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        }
                        Log.d(TAG, "purchase.getSku() = " + purchase.getSku());
                    }
                }, deviceId);
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
            }
        });

        builder.setNeutralButton(R.string.ads_support, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "Enabling ads-support");
                mPreferences.edit()
                        .putBoolean(Utils.BITS_ADS_SUPPORT_ENABLED, true)
                        .putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true)
                        .commit();
                dialog.cancel();
                buildAdsSettingInfoDialog();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                onBackPressed();
            }
        });

        builder.show();
    }

    private void buildAdsSettingInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(InAppPurchaseActivity.this);

        View v = getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

        TextView titleView = (TextView) v.findViewById(R.id.title);
        TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

        titleView.setText(getString(R.string.ads_support_enabled));
        subtitleView.setText(getString(R.string.you_can_switch_to_upgrade_package_if_you_change_mind));

        builder.setView(v);
        builder.setTitle(getString(R.string.success));

        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.show().setCancelable(false);
    }
}
