package com.siqi.bits.app;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.siqi.bits.app.ui.AchievementsFragment;
import com.siqi.bits.app.ui.BitsListFragment;
import com.siqi.bits.app.ui.NewBitFragment;
import com.siqi.bits.app.ui.StatsFragment;

import java.util.HashMap;

import utils.Utils;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        NewBitFragment.OnNewBitInteractionListener, BitsListFragment.OnBitListInteractionListener {

    private static final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";
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
    private HashMap<String, Fragment> mCachedFragments = new HashMap<String, Fragment>();
    private Fragment mCurrentFragment = null;

//    @Override
//    protected void () {
//        super.onDestroy();
//        // update the main content by replacing fragments
//
//        Log.d("TRANSACTION", "Destroyed");
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.mDisplayMetrics = getResources().getDisplayMetrics();

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

//        if (savedInstanceState != null) {
//            Log.d("TRANSACTION", "savedInstance!=null");
//            // update the main content by replacing fragments
//            FragmentManager fragmentManager = getSupportFragmentManager();
//
//            mCachedFragments.put(BitsListFragment.class.getName(), fragmentManager.getFragment(savedInstanceState, BitsListFragment.class.getName()));
//            mCachedFragments.put(AchievementsFragment.class.getName(), fragmentManager.getFragment(savedInstanceState, AchievementsFragment.class.getName()));
//            mCachedFragments.put(StatsFragment.class.getName(), fragmentManager.getFragment(savedInstanceState, StatsFragment.class.getName()));
//
//            mCurrentFragment = mCachedFragments.get(savedInstanceState.getString(CURRENT_FRAGMENT));
//            mCurrentSectionID = savedInstanceState.getInt(CURRENT_FRAGMENT_ID);
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear all notification
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
//
//        // update the main content by replacing fragments
//        FragmentManager fragmentManager = getSupportFragmentManager();
//
//        fragmentManager.beginTransaction().remove(mCachedFragments.get(BitsListFragment.class.getName())).commit();
//        fragmentManager.beginTransaction().remove(mCachedFragments.get(AchievementsFragment.class.getName())).commit();
//        fragmentManager.beginTransaction().remove(mCachedFragments.get(StatsFragment.class.getName())).commit();
//
//
//        if (mCachedFragments.get(BitsListFragment.class.getName()) != null)
//            fragmentManager.putFragment(bundle, BitsListFragment.class.getName(), mCachedFragments.get(BitsListFragment.class.getName()));
//        if (mCachedFragments.get(AchievementsFragment.class.getName()) != null)
//            fragmentManager.putFragment(bundle, AchievementsFragment.class.getName(), mCachedFragments.get(AchievementsFragment.class.getName()));
//        if (mCachedFragments.get(StatsFragment.class.getName()) != null)
//            fragmentManager.putFragment(bundle, StatsFragment.class.getName(), mCachedFragments.get(StatsFragment.class.getName()));
//
//        if (mCurrentFragment != null)
//            bundle.putString(CURRENT_FRAGMENT, mCurrentFragment.getClass().getName());
//
//        bundle.putInt(CURRENT_FRAGMENT_ID, mCurrentSectionID);

//        FragmentManager fragmentManager = getSupportFragmentManager();
//        Fragment f = fragmentManager.findFragmentById(R.id.container);
//        while (f != null) {
//            Log.d("TRANSACTION", "remove " + f.getClass().getName());
//            fragmentManager.beginTransaction().(f).commitAllowingStateLoss();
//            f = fragmentManager.findFragmentById(R.id.container);
//        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == mCurrentSectionID)
            return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);

        if (mCurrentFragment != null) {
            Log.d("TRANSACTION", "putting fragment " + mCurrentFragment.getClass().getName());
            mCachedFragments.put(mCurrentFragment.getClass().getName(), mCurrentFragment);
            transaction.hide(mCurrentFragment);
        }
        Log.d("Nav", "selected: " + position);

        Fragment dest = null;
        switch (position) {
            case BitsListFragment.FRAGMENT_ID:
                dest = mCachedFragments.get(BitsListFragment.class.getName());
                if (dest == null) {
                    dest = BitsListFragment.newInstance();
                    transaction
                            .add(R.id.container, dest);
                } else {
                    Log.d("TRANSACTION", "bitlist retrieved");
                    transaction.show(dest);
                }
                onSectionAttached(BitsListFragment.FRAGMENT_ID);
                break;
            case AchievementsFragment.FRAGMENT_ID:
                dest = mCachedFragments.get(AchievementsFragment.class.getName());
                if (dest == null) {
                    dest = AchievementsFragment.newInstance();
                    transaction
                            .add(R.id.container, dest);
                } else {
                    Log.d("TRANSACTION", "achievements retrieved");
                    transaction.show(dest);
                }
                onSectionAttached(AchievementsFragment.FRAGMENT_ID);
                break;
            case StatsFragment.FRAGMENT_ID:
                dest = mCachedFragments.get(StatsFragment.class.getName());
                if (dest == null) {
                    dest = StatsFragment.newInstance();
                    transaction
                            .add(R.id.container, dest);
                } else {
                    Log.d("TRANSACTION", "stats retrieved");
                    transaction.show(dest);
                }
                onSectionAttached(StatsFragment.FRAGMENT_ID);
                break;
        }

        mCurrentFragment = dest;
        transaction.commit();
    }

    // set main activity title correctly
    public void onSectionAttached(int number) {

        mCurrentSectionID = number;

        switch (number) {
            case BitsListFragment.FRAGMENT_ID:
                mTitle = getString(R.string.bits);
                break;
            case AchievementsFragment.FRAGMENT_ID:
                mTitle = getString(R.string.achievements);
                break;
            case StatsFragment.FRAGMENT_ID:
                mTitle = getString(R.string.statistics);
                break;
            case NewBitFragment.FRAGMENT_ID:
                mTitle = getString(R.string.create_new_bit);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()
                && mCurrentSectionID != NewBitFragment.FRAGMENT_ID) {
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
    public void onNewDisposeInteraction() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
        onSectionAttached(BitsListFragment.FRAGMENT_ID);
    }


    @Override
    public void startEditBitFragment(Long id) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        NewBitFragment editFragment = NewBitFragment.newInstance(id);

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_top);
        transaction.replace(R.id.container, editFragment);
        transaction.addToBackStack("BitsListFragment");

        onSectionAttached(NewBitFragment.FRAGMENT_ID);


        transaction.commit();
    }
}
