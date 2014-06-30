package views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import model.TaskManager;

/**
 * Proudly powered by me on 6/30/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class AppRater {
    private final static String APP_TITLE = "Bits";
    private final static String APP_PACKAGE_NAME = "com.siqi.bits.app";

    private final static int DAYS_UNTIL_PROMPT = 3;
    private final static int DONES_UNTIL_PROMPT = 7;

    private final static String APP_RATING_DIALOG_DISABLED = "APP_RATING_DIALOG_DISABLED";
    private final static String APP_FIRST_LAUNCH_DATE = "APP_FIRST_LAUNCH_DATE";

    public static void appLaunched(Context mContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (prefs.getBoolean(APP_RATING_DIALOG_DISABLED, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        int doneCount = prefs.getInt(TaskManager.TOTAL_DONE_COUNT, 0);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong(APP_FIRST_LAUNCH_DATE, 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong(APP_FIRST_LAUNCH_DATE, date_firstLaunch);
        }

        // Wait at least n days before opening
        if (doneCount >= DONES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, editor);
            }
        }

        editor.commit();
    }

    public static void showRateDialog(final Context mContext,
                                      final SharedPreferences.Editor editor) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String message = "If you enjoy using "
                + APP_TITLE
                + ", please take a moment to rate the app. Thank you for your support!";
        builder.setMessage(message)
                .setTitle("Rate " + APP_TITLE)
                .setIcon(mContext.getApplicationInfo().icon)
                .setCancelable(false)
                .setPositiveButton("Rate Now",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (editor != null) {
                                    editor.putBoolean(APP_RATING_DIALOG_DISABLED, true);
                                    editor.commit();
                                }
                                mContext.startActivity(new Intent(
                                        Intent.ACTION_VIEW, Uri
                                        .parse("market://details?id="
                                                + APP_PACKAGE_NAME)
                                ));
                                dialog.dismiss();
                            }
                        }
                )
                .setNeutralButton("Later",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();

                            }
                        }
                )
                .setNegativeButton("No, Thanks",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (editor != null) {
                                    editor.putBoolean(APP_RATING_DIALOG_DISABLED, true);
                                    editor.commit();
                                }
                                dialog.dismiss();

                            }
                        }
                ).show();
    }
}