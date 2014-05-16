package com.siqi.bits.app.ui;

import android.support.v4.app.Fragment;

import model.TaskManager;

/**
 * Proudly powered by me on 5/15/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsFragment extends Fragment {
    public final static int FRAGMENT_ID = 2;

    private TaskManager tm;

    public StatsFragment() {
    }

    public static StatsFragment newInstance() {
        StatsFragment fragment = new StatsFragment();
        return fragment;
    }
}
