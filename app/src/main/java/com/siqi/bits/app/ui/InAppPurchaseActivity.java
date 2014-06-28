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
    //    public static final String SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK = "android.test.purchased";
    private SharedPreferences mPreferences;
    private ProgressDialog mProgressDialog;

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

        Utils.mIabHelper.queryInventoryAsync(true, additionalSkuList, new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.isFailure()) {
                    Log.d("In-App Purchase", "query failed");
                    mProgressDialog.cancel();
                    buildFailureDialog();
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                    return;
                }

                if (inventory == null || inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) == null) {
                    Log.d("In-App Purchase", "inventory == null || inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) == null");
                    mProgressDialog.cancel();
                    buildFailureDialog();
                    if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                    return;
                }

                String UnlockPrice =
                        inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK).getPrice();

                TextView priceTextView = (TextView) findViewById(R.id.price_tag);
                priceTextView.setText(getString(R.string.all_for_just) + " " + UnlockPrice);
                mProgressDialog.cancel();
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();

//                IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
//                        new IabHelper.OnConsumeFinishedListener() {
//                            public void onConsumeFinished(Purchase purchase, IabResult result) {
//                                if (result.isSuccess()) {
//                                    Log.d("In-App Purchase", "item consumed");
//                                }
//                                else {
//                                    Log.d("In-App Purchase", "item consumption failed");
//                                }
//                            }
//                        };

//                Log.d("In-App Purchase", "inventory.hasPurchase(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) = " + inventory.hasPurchase(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK));
//                Utils.mIabHelper.consumeAsync(inventory.getPurchase(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK),
//                        mConsumeFinishedListener);
            }
        });

        Button purchaseButton = (Button) findViewById(R.id.purchase_button);
        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                            Log.d("In-App Purchase", "purchase done");
                            mPreferences.edit().putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true).commit();
                            buildThankyouDialog();
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        }

                        if (result.isFailure()) {
                            buildFailureDialog();
                            Log.d("In-App Purchase", "purchase failed:" + result);
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        } else if (purchase.getSku().equals(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK)) {
                            Log.d("In-App Purchase", "purchase done");
                            mPreferences.edit().putBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, true).commit();
                            buildThankyouDialog();
                            if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
                            return;
                        }
                        Log.d("In-App Purchase", "purchase.getSku() = " + purchase.getSku());
                    }
                }, deviceId);
                if (Utils.mIabHelper != null) Utils.mIabHelper.flagEndAsync();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("In-App Purchase", "onActivityResult(" + requestCode + "," + resultCode + ","
                + data);

        // Pass on the activity result to the helper for handling
        if (!Utils.mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d("In-App Purchase", "onActivityResult handled by IABUtil.");
        }
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

        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.show();
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

        builder.show();
    }
}
