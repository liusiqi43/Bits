package com.siqi.bits.app.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
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
    private SharedPreferences mPreferences;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_purchase);


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
                    mProgressDialog.cancel();
                    buildFailureDialog();
                    return;
                }

                if (inventory == null || inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK) == null) {
                    mProgressDialog.cancel();
                    buildFailureDialog();
                    return;
                }

                String UnlockPrice =
                        inventory.getSkuDetails(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK).getPrice();

                TextView priceTextView = (TextView) findViewById(R.id.price_tag);
                priceTextView.setText(getString(R.string.all_for_just) + " " + UnlockPrice);
                mProgressDialog.cancel();
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
                        if (result.isFailure()) {
                            buildFailureDialog();
                            return;
                        } else if (purchase.getSku().equals(SKU_ACTIVE_TASKS_COUNT_LIMIT_UNLOCK)) {
                            mPreferences.edit().putBoolean(BitsListFragment.TASKS_COUNT_LIMIT_UNLOCKED, true).commit();
                            buildThankyouDialog();
                            return;
                        }
                    }
                }, deviceId);
            }
        });
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
