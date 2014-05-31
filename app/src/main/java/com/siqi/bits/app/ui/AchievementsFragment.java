package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.TaskManager;

/**
 * Proudly powered by me on 5/6/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class AchievementsFragment extends BaseFragment {
    public final static int FRAGMENT_ID = 1;

    private ListView mAchievementsListView;
    private SectionedListAdapter mAdapter;
    private TaskManager tm;

    public AchievementsFragment() {
    }

    public static AchievementsFragment newInstance() {
        AchievementsFragment fragment = new AchievementsFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        /**
         * View Binding
         */
        View rootView = inflater.inflate(R.layout.achievement_fragment, container, false);
        mAchievementsListView = (ListView) rootView.findViewById(R.id.achievement_list);

        View emptyView = inflater.inflate(R.layout.placeholder_empty_view, null);
        TextView tv = (TextView) emptyView.findViewById(R.id.message);
        ImageView imageView = (ImageView) emptyView.findViewById(R.id.icon);
        tv.setText(getString(R.string.achievements_empty_message));
        imageView.setImageResource(R.drawable.trophy);
        ((ViewGroup) mAchievementsListView.getParent()).addView(emptyView, mAchievementsListView.getLayoutParams());
        mAchievementsListView.setEmptyView(emptyView);

        /**
         * Data Loading
         */
        tm = TaskManager.getInstance(this.getActivity().getApplicationContext());
        mAdapter = new SectionedListAdapter(new ArrayList<Task>());
        reloadItems();

        // Swing from bottom anim & dismiss anim
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);

        this.mAchievementsListView.setAdapter(swingBottomInAnimationAdapter);
        swingBottomInAnimationAdapter.setAbsListView(mAchievementsListView);

        /**
         * Context Menu
         */
        registerForContextMenu(mAchievementsListView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.achievement_list) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.achievement_listview_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Task t = mAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.unarchive:
                t.setArchieved_on(null);
                t.update();
                reloadItems();
                return true;
            case R.id.delete:
                t.setDeletedOn(new Date());
                t.update();
                reloadItems();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void reloadItems() {
        mAdapter.clear();
        mAdapter.addAll(tm.getAllSortedArchivedTasks());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(FRAGMENT_ID);
    }

    private static class AchievementHolder {
        TextView taskTitle, taskGoal, doneCount, skipCount, lateCount, daysCount, freq, interval, separator;
        ImageView taskIcon;
        View skipCountLayout, skipCountSeparator;
    }

    public class SectionedListAdapter extends com.nhaarman.listviewanimations.ArrayAdapter<Task> {

        private final LruCache<String, Bitmap> mMemoryCache;
        private List<Task> mItems;

        public SectionedListAdapter(List<Task> tasks) {
            super(tasks);
            final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                }
            };
            mItems = tasks;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            final AchievementHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.achievement_item, parent, false);

                holder = new AchievementHolder();

                holder.daysCount = (TextView) v.findViewById(R.id.achievement_day_count);
                holder.doneCount = (TextView) v.findViewById(R.id.achievement_done_count);
                holder.skipCount = (TextView) v.findViewById(R.id.achievement_skip_count);
                holder.freq = (TextView) v.findViewById(R.id.achievement_actual_freq_count);
                holder.interval = (TextView) v.findViewById(R.id.achievement_freq_interval);
                holder.lateCount = (TextView) v.findViewById(R.id.achievement_late_count);
                holder.taskGoal = (TextView) v.findViewById(R.id.taskSubtitle);
                holder.taskTitle = (TextView) v.findViewById(R.id.taskTitle);
                holder.separator = (TextView) v.findViewById(R.id.achievement_seperator);
                holder.taskIcon = (ImageView) v.findViewById(R.id.taskIcon);
                holder.skipCountLayout = v.findViewById(R.id.achievement_skip_count_layout);
                holder.skipCountSeparator = v.findViewById(R.id.achievement_skip_count_separator);
                v.setTag(holder);
            } else {
                holder = (AchievementHolder) v.getTag();
            }

            final Task t = mItems.get(position);

            Bitmap bitmap = mMemoryCache.get(t.getCategory().getIconDrawableName());
            if (bitmap == null) {
                try {
                    bitmap = t.getCategory().getIconBitmap(getActivity());
                    if (bitmap != null) {
                        mMemoryCache.put(t.getCategory().getIconDrawableName(), bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Integer days = (int) Math.ceil((double) (t.getArchieved_on().getTime() - t.getCreatedOn().getTime()) / TaskManager.DAY_IN_MILLIS);
            int periodInDays = (int) (t.getPeriod() / TaskManager.DAY_IN_MILLIS);
            double periodCounts = Math.ceil((double) days / periodInDays);

            Double freq = Math.round(t.getDoneCount() / periodCounts * 100.0) / 100.0;

            Log.d("Achievement", "days: " + days + " periodInDays" + periodInDays + " periodCounts" + periodCounts + " freq:" + freq);

            holder.taskIcon.setImageBitmap(bitmap);
            holder.taskTitle.setText(t.getDescription());
            holder.taskGoal.setText(t.getFrequency() + " times/" + TaskManager.PeriodStringToDays.inverse().get(periodInDays));
            holder.interval.setText("t/" + TaskManager.PeriodStringToDays.inverse().get(periodInDays));
            holder.freq.setText(freq.toString());
            holder.daysCount.setText(days.toString());
            holder.lateCount.setText(Integer.toString(t.getLateCount()));
            holder.doneCount.setText(Integer.toString(t.getDoneCount()));
            holder.skipCount.setText(Integer.toString(t.getSkipCount()));
            holder.separator.setText(tm.getArchivedDescriptionForTask(t));

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.skipCountLayout.setVisibility(View.GONE);
                holder.skipCountSeparator.setVisibility(View.GONE);
            } else {
                holder.skipCountLayout.setVisibility(View.VISIBLE);
                holder.skipCountSeparator.setVisibility(View.VISIBLE);
            }

            return v;
        }

    }

}
