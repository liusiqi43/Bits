package com.siqi.bits.app;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
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

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        NewBitFragment.OnNewBitInteractionListener, BitsListFragment.OnBitListInteractionListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private int mCurrentSectionID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear all notification
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == mCurrentSectionID)
            return;
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Log.d("Nav", "selected: " + position);

        switch (position) {
            case BitsListFragment.FRAGMENT_ID:
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                transaction
                        .replace(R.id.container, BitsListFragment.newInstance())
                        .commit();
                onSectionAttached(BitsListFragment.FRAGMENT_ID);
                break;
            case AchievementsFragment.FRAGMENT_ID:
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                transaction
                        .replace(R.id.container, AchievementsFragment.newInstance())
                        .commit();
                onSectionAttached(AchievementsFragment.FRAGMENT_ID);
                break;
            case StatsFragment.FRAGMENT_ID:
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                transaction
                        .replace(R.id.container, StatsFragment.newInstance())
                        .commit();
                onSectionAttached(StatsFragment.FRAGMENT_ID);
                break;
        }


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
