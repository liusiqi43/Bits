package com.siqi.bits.app.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.siqi.bits.app.R;
import com.viewpagerindicator.TabPageIndicator;

/**
 * Proudly powered by me on 5/15/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsFragment extends BaseFragment {
    public final static int FRAGMENT_ID = 2;

    ViewPager mViewPager;

    public StatsFragment() {
    }

    public static StatsFragment newInstance() {
        StatsFragment fragment = new StatsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        View root = inflater.inflate(R.layout.stats_fragment_layout, container, false);

        /**
         * UI binding
         */
        mViewPager = (ViewPager) root.findViewById(R.id.view_pager);
        mViewPager.setAdapter(new StatsViewPagerAdapter(getChildFragmentManager()));

        //Bind the title indicator to the adapter
        TabPageIndicator titleIndicator = (TabPageIndicator) root.findViewById(R.id.pager_indicator);
        titleIndicator.setViewPager(mViewPager);

        return root;
    }

    class StatsViewPagerAdapter extends FragmentPagerAdapter {

        public StatsViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return StatsXYChartFragment.newInstance();
                case 1:
                    return StatsPieChartFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.burndown_viewpagerstrip_title);
                case 1:
                    return getString(R.string.task_category_viewpagerstrip_title);
                default:
                    return null;
            }
        }
    }
}
