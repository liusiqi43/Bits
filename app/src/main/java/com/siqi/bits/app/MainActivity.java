package com.siqi.bits.app;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nineoldandroids.view.ViewHelper;
import com.siqi.bits.app.ui.AchievementsFragment;
import com.siqi.bits.app.ui.BitsListFragment;
import com.siqi.bits.app.ui.NewBitActivity;
import com.siqi.bits.app.ui.SettingsActivity;
import com.siqi.bits.app.ui.StatsFragment;

import java.util.List;
import java.util.Stack;

import interfaces.IabSetupActionHandler;
import utils.IabHelper;
import utils.Utils;
import views.AppRater;

public class MainActivity extends ActionBarActivity
    implements NavigationDrawerFragment.NavigationDrawerCallbacks {

  private static final String CURRENT_FRAGMENT_ID = "CURRENT_FRAGMENT_ID";
  private static final int MISC_STARTING_INDEX = 3;
  private static final int SETTING_ITEM_INDEX = 3;
  private static final int HELP_ITEM_INDEX = 4;
  private static final int FEEDBACK_ITEM_INDEX = 5;
  public IabHelper mBillingHelper;
  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */
  private NavigationDrawerFragment mNavigationDrawerFragment;
  /**
   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
   */
  private CharSequence mTitle;
  private int mCurrentSectionID = -1;
  private boolean doubleBackToExitPressedOnce = false;
  private View mContainerView;
  private Integer mActionbarIcon;

  @Override
  protected void onSaveInstanceState(Bundle bundle) {
    super.onSaveInstanceState(bundle);
    bundle.putInt(CURRENT_FRAGMENT_ID, mCurrentSectionID);
  }

  @Override
  protected void onRestoreInstanceState(Bundle bundle) {
    super.onRestoreInstanceState(bundle);

    if (bundle != null) {
      mCurrentSectionID = bundle.getInt(CURRENT_FRAGMENT_ID);
      onSectionAttached(mCurrentSectionID);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Utils.mDisplayMetrics = getResources().getDisplayMetrics();

    setContentView(R.layout.activity_main);
    mContainerView = findViewById(R.id.container);

    mNavigationDrawerFragment = (NavigationDrawerFragment)
        getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
    mTitle = "";
    mActionbarIcon = R.drawable.ic_banner;

    // Set up the drawer.
    mNavigationDrawerFragment.setUp(
        R.id.navigation_drawer,
        (DrawerLayout) findViewById(R.id.drawer_layout));

    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    Utils.setupIabHelper(this, new IabSetupActionHandler() {
      @Override
      public void onSetupDone() {
        Utils.checkIfPremiumPurchased(MainActivity.this);
      }
    });
    AppRater.appLaunched(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Clear all notification
    NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    nMgr.cancelAll();

    restoreActionBar();
    Utils.asyncUploadBitsToDashboard(this);
  }

  private Intent createEmailOnlyChooserIntent(Intent source,
                                              CharSequence chooserTitle) {
    Stack<Intent> intents = new Stack<Intent>();
    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",
        "info@domain.com", null));
    List<ResolveInfo> activities = getPackageManager()
        .queryIntentActivities(i, 0);

    for (ResolveInfo ri : activities) {
      Intent target = new Intent(source);
      target.setPackage(ri.activityInfo.packageName);
      intents.add(target);
    }

    if (!intents.isEmpty()) {
      Intent chooserIntent = Intent.createChooser(intents.remove(0),
          chooserTitle);
      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
          intents.toArray(new Parcelable[intents.size()]));

      return chooserIntent;
    } else {
      return Intent.createChooser(source, chooserTitle);
    }
  }

  @Override
  public void onNavigationDrawerItemSelected(int position) {
    if (position < MISC_STARTING_INDEX && position == mCurrentSectionID)
      return;

    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.setCustomAnimations(R.anim.zoom_out_fade_in, R.anim.zoom_in_fade_out);

    Fragment dest;
    switch (position) {
      case SETTING_ITEM_INDEX:
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        break;
      case HELP_ITEM_INDEX:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View v = getLayoutInflater().inflate(R.layout.help_all, null, false);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, false)) {
          v.findViewById(R.id.view_history).setVisibility(View.VISIBLE);
        }
        builder.setView(v);

        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });

        builder.show();
        break;
      case FEEDBACK_ITEM_INDEX:
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_email_addr)});
        try {
          i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_for_bits_app) + " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
          i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_for_bits_app));
        }

        try {
          startActivity(createEmailOnlyChooserIntent(i, getString(R.string.send_email)));
        } catch (android.content.ActivityNotFoundException ex) {
          Toast.makeText(this, getString(R.string.no_email_client_installed), Toast.LENGTH_SHORT).show();
        }
        break;
      case BitsListFragment.FRAGMENT_ID:
        dest = fragmentManager.findFragmentByTag(BitsListFragment.class.getName());
        if (dest == null) {
          Log.d("TRANSACTION", "instanciating new fragment");
          dest = BitsListFragment.newInstance();
        }
        transaction.replace(R.id.container, dest, BitsListFragment.class.getName());
        transaction.addToBackStack(BitsListFragment.class.getName());
        break;
      case AchievementsFragment.FRAGMENT_ID:
        dest = fragmentManager.findFragmentByTag(AchievementsFragment.class.getName());
        if (dest == null) {
          Log.d("TRANSACTION", "instanciating new fragment");
          dest = AchievementsFragment.newInstance();
        }
        transaction.replace(R.id.container, dest, AchievementsFragment.class.getName());
        transaction.addToBackStack(AchievementsFragment.class.getName());
        break;
      case StatsFragment.FRAGMENT_ID:
        dest = StatsFragment.newInstance();
        transaction.replace(R.id.container, dest, StatsFragment.class.getName());
        transaction.addToBackStack(StatsFragment.class.getName());
        break;
    }

    transaction.commit();
    onSectionAttached(position);
  }

  @Override
  public void onDrawerSlide(View drawerView, float slideOffset) {
    ViewHelper.setScaleX(mContainerView, 1 - slideOffset / 20);
    ViewHelper.setScaleY(mContainerView, 1 - slideOffset / 20);
  }

  // set main activity title correctly
  public void onSectionAttached(int number) {

    mCurrentSectionID = number;

    switch (number) {
      case BitsListFragment.FRAGMENT_ID:
        mTitle = "";
        mActionbarIcon = R.drawable.ic_banner;
        break;
      case AchievementsFragment.FRAGMENT_ID:
        mTitle = getString(R.string.achievements);
        mActionbarIcon = android.R.color.transparent;
        break;
      case StatsFragment.FRAGMENT_ID:
        mTitle = getString(R.string.statistics);
        mActionbarIcon = android.R.color.transparent;
        break;
    }
  }

  public void restoreActionBar() {
    ActionBar actionBar = getSupportActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setTitle(mTitle);
    actionBar.setLogo(mActionbarIcon);
  }

  @Override
  public void onBackPressed() {
    if (mCurrentSectionID == NewBitActivity.FRAGMENT_ID) {
      onSectionAttached(BitsListFragment.FRAGMENT_ID);
      super.onBackPressed();
      return;
    }

    if (doubleBackToExitPressedOnce) {
      finish();
      return;
    }

    doubleBackToExitPressedOnce = true;
    Toast.makeText(this, getString(R.string.click_back_twice_to_quit), Toast.LENGTH_SHORT).show();

    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        doubleBackToExitPressedOnce = false;
      }
    }, 2000);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (!mNavigationDrawerFragment.isDrawerOpen()) {
      // Only show items in the action bar relevant to this screen
      // if the drawer is not showing. Otherwise, let the drawer
      // decide what to show in the action bar.
      restoreActionBar();
      return true;
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mBillingHelper != null) mBillingHelper.dispose();
    mBillingHelper = null;
  }
}
