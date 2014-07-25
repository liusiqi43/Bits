package com.siqi.bits.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

import com.siqi.bits.app.R;

import utils.Utils;

/**
 * Proudly powered by me on 6/20/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("IS_AUTO_ROTATE_ENABLED", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.preferences);

        if (!prefs.getBoolean(Utils.REWARD_UNDO_ON_SHAKE_ENABLED, false)) {
            PreferenceCategory category = (PreferenceCategory) findPreference("pref_bits_settings");
            CheckBoxPreference shakeCheckbox = (CheckBoxPreference) findPreference(Utils.IS_BITSLIST_SHAKE_ON);
            category.removePreference(shakeCheckbox);
        }

        PreferenceCategory category = (PreferenceCategory) findPreference("pref_bits_settings");
        Preference optInAds = findPreference(Utils.IS_BITS_ADS_SUPPORT_ENABLED);

        if (!prefs.getBoolean(Utils.TASKS_COUNT_LIMIT_UNLOCKED, false)) {
            category.removePreference(optInAds);
        } else {
            Intent intent = new Intent(this, InAppPurchaseActivity.class);
            optInAds.setIntent(intent);
        }

    }
}