package com.siqi.bits.app;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import com.siqi.bits.app.ui.StatsFragment;

import utils.Utils;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String CURRENT_FRAGMENT_ID = "CURRENT_FRAGMENT_ID";
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
    private int mActionbarBackgroundColor;

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
        mActionbarBackgroundColor = R.color.Turquoise;

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        if (mCurrentSectionID == NewBitActivity.FRAGMENT_ID) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear all notification
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();

        restoreActionBar();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == mCurrentSectionID)
            return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.zoom_out_fade_in, R.anim.zoom_in_fade_out);

        Fragment dest;
        switch (position) {
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
                mActionbarBackgroundColor = R.color.Turquoise;
                break;
            case AchievementsFragment.FRAGMENT_ID:
                mTitle = getString(R.string.achievements);
                mActionbarIcon = android.R.color.transparent;
                mActionbarBackgroundColor = R.color.Turquoise;
                break;
            case StatsFragment.FRAGMENT_ID:
                mTitle = getString(R.string.statistics);
                mActionbarIcon = android.R.color.transparent;
                mActionbarBackgroundColor = R.color.Turquoise;
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setBackgroundDrawable(getResources().getDrawable(mActionbarBackgroundColor));
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
        Toast.makeText(this, "Click BACK again to quit", Toast.LENGTH_SHORT).show();

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
}
