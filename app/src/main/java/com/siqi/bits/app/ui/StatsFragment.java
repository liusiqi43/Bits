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

/**
 * Proudly powered by me on 5/15/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsFragment extends Fragment {
    public final static int FRAGMENT_ID = 2;

    ViewPager mViewPager;

    public StatsFragment() {
        setRetainInstance(true);
    }

    public static StatsFragment newInstance() {
        StatsFragment fragment = new StatsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        View root = inflater.inflate(R.layout.stats_fragment_layout, container, false);

        mViewPager = (ViewPager) root.findViewById(R.id.view_pager);
        /** Important: Must use the child FragmentManager or you will see side effects. */
        mViewPager.setAdapter(new StatsViewPagerAdapter(getChildFragmentManager()));

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
                    return getString(R.string.burndown_chart_title);
                case 1:
                    return getString(R.string.task_category_chart_title);
                default:
                    return null;
            }
        }
    }
}
