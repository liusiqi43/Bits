package com.siqi.bits.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.siqi.bits.ActionRecord;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;
import com.siqi.bits.swipelistview.BaseSwipeListViewListener;
import com.siqi.bits.swipelistview.SwipeListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import model.TaskManager;
import service.ReminderScheduleService;
import utils.ShakeEventListener;
import utils.Utils;

/**
 * Proudly powered by me on 4/8/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class BitsListFragment extends Fragment implements ShakeEventListener.OnShakeListener {
    public static final int FRAGMENT_ID = 0;

    public static final int CARD_INFO = 0;
    public static final int CARD_ACTION = 1;
    private static final int REFRESH_PERIOD = 60 * 1000;
    SwipeListView mBitsListView;
    BitListArrayAdapter mAdapter;
    AnimateDismissAdapter mAnimateDismissAdapter;
    // Reordering animation
    HashMap<Task, Integer> mSavedState = new HashMap<Task, Integer>();
    Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    TaskManager tm;
    SensorManager mSensorManager;
    ShakeEventListener mSensorListener;
    OnBitListInteractionListener mListener;
    boolean mUndoDialogDisplayed = false;
    ReminderScheduleService mScheduleService = null;
    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("ReminderScheduleService", "onServiceConnected");
            mScheduleService = ((ReminderScheduleService.LocalBinder) service).getService();
            tm.setScheduleService(mScheduleService);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ReminderScheduleService", "onServiceDisconnected");
            mScheduleService = null;
            tm.setScheduleService(null);
        }
    };
    boolean mIsBound = false;
    Runnable mListReloader;
    Handler mListRefresherHandle = new Handler();

    public BitsListFragment() {
    }

    public static BitsListFragment newInstance() {
        BitsListFragment fragment = new BitsListFragment();
        return fragment;
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        getActivity().bindService(new Intent(getActivity(),
                ReminderScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        /**
         * View Binding
         */
        View rootView = inflater.inflate(R.layout.bitslist_fragment, container, false);
        mBitsListView = (SwipeListView) rootView.findViewById(R.id.bitslist);

        new ReloadSortedTasks().execute();

        mAdapter = new BitListArrayAdapter(getActivity(), new ArrayList<Task>());
        mAnimateDismissAdapter = new AnimateDismissAdapter(mAdapter, new OnBitDismissCallback());
        mBitsListView.setAdapter(mAnimateDismissAdapter);
        mAnimateDismissAdapter.setAbsListView(mBitsListView);
        mAdapter.setLimit(1);


        mBitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int position, long id) {

                final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.card_viewswitcher);
                viewSwitcher.showNext();

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if (viewSwitcher.getDisplayedChild() == CARD_ACTION) {
                            viewSwitcher.setDisplayedChild(CARD_INFO);
                        }
                    }
                }, getResources().getInteger(R.integer.actionview_timeout));

                return true;
            }
        });

        mBitsListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onLeftChoiceAction(int position) {
                if (mAdapter.isExpanded(position))
                    mAdapter.toggle(position);
                Task item = mAdapter.getItem(position);
                tm.setSkipActionForTask(item);
                saveState();
                mAdapter.clear();
                mAdapter.addAll(tm.getAllSortedTasks());
                animateNewState();
            }

            @Override
            public void onRightChoiceAction(int position) {
                if (mAdapter.isExpanded(position))
                    mAdapter.toggle(position);

                Task item = mAdapter.getItem(position);
                tm.setDoneActionForTask(item);
                saveState();
                mAdapter.clear();
                mAdapter.addAll(tm.getAllSortedTasks());


                animateNewState();
            }

        });

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new ShakeEventListener();

        mSensorListener.setOnShakeListener(BitsListFragment.this);

        mListReloader = new Runnable() {
            @Override
            public void run() {
                new ReloadSortedTasks().execute();
                mListRefresherHandle.postDelayed(mListReloader, REFRESH_PERIOD);
            }
        };

        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        startPeriodicRefresh();

        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        /**
         * Data Loading
         */
        setRetainInstance(true);
        tm = TaskManager.getInstance(this.getActivity());


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnBitListInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBitListInteractionListener");
        }

        ((MainActivity) activity).onSectionAttached(FRAGMENT_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSensorManager != null) {
            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI);
        }
        /**
         * Service binding
         */
        doBindService();

        /**
         * UI refresher start
         */
        if (mListReloader != null) {
            startPeriodicRefresh();
        }
    }

    private void startPeriodicRefresh() {
        mListReloader.run();
    }

    private void stopPeriodicRefresh() {
        Log.d("periodic", "Refresh stops now");
        mListRefresherHandle.removeCallbacks(mListReloader);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        /**
         * Service Unbinding
         */
        doUnbindService();

        /**
         * UI refresher stops here
         */
        stopPeriodicRefresh();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.getActivity().getMenuInflater().inflate(R.menu.bitlist, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_new) {
            this.mListener.startEditBitFragment(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShake() {
        if (mUndoDialogDisplayed)
            return;

        final ActionRecord record = tm.getLastActionForActiveTask();

        if (record != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            StringBuilder msgBulder = new StringBuilder()
                    .append(getString(R.string.last_record_seems_to_be))
                    .append(record.getAction() == TaskManager.ACTION_TYPE_DONE ?
                            " <font color='#2ecc71'>" + getString(R.string.done) + "</font>"
                            : " <font color='#3498db'>" + getString(R.string.skip) + "</font>")
                    .append(" ")
                    .append(getString(R.string.on_task))
                    .append(" <b>")
                    .append(record.getTask().getDescription())
                    .append("</b>");

            TextView tv = new TextView(getActivity());
            int padInPx = Utils.dpToPx(15);
            tv.setPadding(padInPx, padInPx, padInPx, padInPx);
            tv.setTextAppearance(getActivity(), android.R.style.TextAppearance_Holo_Large);
            tv.setText(Html.fromHtml(msgBulder.toString()), TextView.BufferType.SPANNABLE);
            builder.setView(tv);
            builder.setTitle(getString(R.string.do_you_want_to_undo_it));

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    tm.removeActionRecordById(record.getId());
                    saveState();
                    mAdapter.clear();
                    mAdapter.addAll(tm.getAllSortedTasks());
                    animateNewState();
                    dialog.cancel();
                    mUndoDialogDisplayed = false;
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    mUndoDialogDisplayed = false;
                }
            });

            builder.show();
            mUndoDialogDisplayed = true;
        }
    }

    private void saveState() {
        mSavedState.clear();

        int first = mBitsListView.getFirstVisiblePosition();
        int last = mBitsListView.getLastVisiblePosition();
        for (int i = 0; i < mAdapter.size(); i++) {
            if (i >= first && i <= last) {
                View v = mBitsListView.getChildAt(i - first);
                int top = v.getTop();
                int dataIdx = i;
                Task dataId = mAdapter.get(dataIdx);
                mSavedState.put(dataId, top);
            } else if (i < first) {
                int top = mBitsListView.getTop() - mBitsListView.getHeight() / 2;
                Task dataId = mAdapter.get(i);
                mSavedState.put(dataId, top);
            } else if (i > last) {
                int top = mBitsListView.getBottom() + mBitsListView.getHeight() / 2;
                Task dataId = mAdapter.get(i);
                mSavedState.put(dataId, top);
            }
        }
        for (int i = 0; i < mBitsListView.getChildCount(); i++) {
            View v = mBitsListView.getChildAt(i);
            int top = v.getTop();
            int dataIdx = first + i;
            Task dataId = mAdapter.get(dataIdx);
            mSavedState.put(dataId, top);
        }
    }

    private void animateNewState() {
        int first = mBitsListView.getFirstVisiblePosition();
        int last = mBitsListView.getLastVisiblePosition();
        for (int i = 0; i < mBitsListView.getChildCount(); i++) {
            int dataIdx = first + i;
            Task dataId = mAdapter.get(dataIdx);
            if (mSavedState.containsKey(dataId)) {
                View v = mBitsListView.getChildAt(i);
                int top = v.getTop();
                int oldTop = mSavedState.get(dataId);
                int hDiff = top - oldTop;
                TranslateAnimation anim = new TranslateAnimation(0, 0, -hDiff, 0);
                anim.setInterpolator(mInterpolator);
                anim.setDuration(330);
                v.startAnimation(anim);
            }
        }
    }

    public interface OnBitListInteractionListener {
        public void startEditBitFragment(Long id);
    }

    private static class BitTitleHolder {
        ImageView icon;
        TextView title;
        TextView timeAgo;
        ProgressBar progressBar;
        Button skipButton, doneButton;
        ImageButton editButton, deleteButton, achieveButton;
        ViewSwitcher viewSwitcher;
    }

    private static class BitContentHolder {
        TextView bitDoneRate;
        TextView othersDoneRate;
        GridView timeLine;
        LinearLayout globalLayout;
    }

    private class ReloadSortedTasks extends AsyncTask<Void, Integer, List<Task>> {
        @Override
        protected List<Task> doInBackground(Void... voids) {
            return tm.getAllSortedTasks();
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(List<Task> result) {
            saveState();
            mAdapter.clear();
            mAdapter.addAll(result);
            animateNewState();
        }
    }

    private class OnBitDismissCallback implements OnDismissCallback {

        @Override
        public void onDismiss(final AbsListView listView, final int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                mAdapter.remove(mAdapter.getItem(position));
            }
        }
    }

    private class BitListArrayAdapter extends ExpandableListItemAdapter<Task> {

        private final LruCache<String, Bitmap> mMemoryCache;
        List<Task> mItems;


        public BitListArrayAdapter(Context ctx, List<Task> t) {
            super(ctx, R.layout.card_main_layout, R.id.expandable_list_item_card_title, R.id.expandable_list_item_card_content, t);
            this.setActionViewResId(R.id.front);
            final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                }
            };

            mItems = t;
        }

        @Override
        public View getTitleView(final int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getTitleVlew:" + position);
            View v = convertView;
            final BitTitleHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.card_title_layout, parent, false);

                holder = new BitTitleHolder();

                holder.icon = (ImageView) v.findViewById(R.id.taskIcon);
                holder.title = (TextView) v.findViewById(R.id.taskTitle);
                holder.timeAgo = (TextView) v.findViewById(R.id.taskSubtitle);
                holder.progressBar = (ProgressBar) v.findViewById(R.id.timeAgoProgressBar);
                holder.doneButton = (Button) v.findViewById(R.id.done_button);
                holder.skipButton = (Button) v.findViewById(R.id.skip_button);
                holder.editButton = (ImageButton) v.findViewById(R.id.edit_button);
                holder.achieveButton = (ImageButton) v.findViewById(R.id.achieve_button);
                holder.deleteButton = (ImageButton) v.findViewById(R.id.delete_button);
                holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);

                v.setTag(holder);
            } else {
                holder = (BitTitleHolder) v.getTag();
                holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);
                if (holder.viewSwitcher.getDisplayedChild() == CARD_ACTION) {
                    holder.viewSwitcher.setInAnimation(null);
                    holder.viewSwitcher.setOutAnimation(null);
                    holder.viewSwitcher.setDisplayedChild(CARD_INFO);

                    Animation inAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
                    inAnimation.setDuration(300);
                    Animation outAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
                    outAnimation.setDuration(300);

                    holder.viewSwitcher.setInAnimation(inAnimation);
                    holder.viewSwitcher.setOutAnimation(outAnimation);
                }
            }

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.viewSwitcher.setDisplayedChild(CARD_INFO);
                    Task item = mAdapter.getItem(position);
                    item.setDeletedOn(new Date());
                    tm.updateTask(item);
                    if (mScheduleService != null)
                        mScheduleService.unScheduleForTask(item);
                    else
                        Log.d("ReminderScheduleService", "mScheduleService == null, deleted item still scheduled");
                    // Implicitly calls datasetChanged() method

                    mAnimateDismissAdapter.animateDismiss(position);
                }
            });

            holder.achieveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.viewSwitcher.setDisplayedChild(CARD_INFO);
                    Task item = mAdapter.getItem(position);
                    item.setArchieved_on(new Date());
                    tm.updateTask(item);
                    if (mScheduleService != null)
                        mScheduleService.unScheduleForTask(item);
                    else
                        Log.d("ReminderScheduleService", "mScheduleService == null, deleted item still scheduled");
                    // Implicitly calls datasetChanged() method

                    mAnimateDismissAdapter.animateDismiss(position);
                }
            });

            holder.editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.startEditBitFragment(mAdapter.getItem(position).getId());
                }
            });

            final Task t = getItem(position);

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

            holder.icon.setImageBitmap(bitmap);
            holder.title.setText(t.getDescription());
            holder.timeAgo.setText(tm.getTimesAgoDescriptionForTask(t));

            int progress = tm.getProgressForTask(t);
            if (progress > 100) {
                // Only triggered when t.getFrequency() - actionCountSinceBeginOfInternval > 0, and progress > 100, which means... well, late!
                tm.updateActionRecordForTask(t);
            }
            holder.progressBar.setProgress(progress);
            if (progress < 50) {
                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_horizontal_success));
            } else if (progress < 80) {
                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_horizontal_warning));
            } else {
                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_horizontal_danger));
            }

            Log.d("Progress for " + t.getDescription(), progress + "");

            Animation inAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
            inAnimation.setDuration(300);
            Animation outAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
            outAnimation.setDuration(300);

            holder.viewSwitcher.setInAnimation(inAnimation);
            holder.viewSwitcher.setOutAnimation(outAnimation);

            Log.d("BitListFrag", t.getDescription() + " : " + t.getNextScheduledTime());

            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getVlew:" + position);
            View v = super.getView(position, convertView, parent);

            ((SwipeListView) parent).recycle(v, position);

            return v;
        }

        @Override
        public View getContentView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getContentVlew:" + position);
            Task t = mAdapter.getItem(position);

            View v = convertView;
            BitContentHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.card_content_layout, parent, false);

                holder = new BitContentHolder();

                holder.bitDoneRate = (TextView) v.findViewById(R.id.bit_done_rate);
                holder.othersDoneRate = (TextView) v.findViewById(R.id.others_done_rate);
                holder.timeLine = (GridView) v.findViewById(R.id.timeline_gridview);
                holder.globalLayout = (LinearLayout) v.findViewById(R.id.card_content_global_layout);

                v.setTag(holder);
            } else {
                holder = (BitContentHolder) v.getTag();
            }

            if (t.getActionsRecords().isEmpty()) {
                holder.globalLayout.setVisibility(View.GONE);
            } else {
                holder.globalLayout.setVisibility(View.VISIBLE);
                int thisDoneRate = tm.getDoneRate(t);
                int othersDoneRate = tm.getDoneRateExcept(t);

                Log.d("THIS_DONE_RATE", t.getDescription() + ":" + thisDoneRate);

                if (thisDoneRate > othersDoneRate) {
                    holder.bitDoneRate.setTextColor(getResources().getColor(R.color.doneColor));
                    holder.othersDoneRate.setTextColor(getResources().getColor(R.color.lateColor));
                } else if (thisDoneRate < othersDoneRate) {
                    holder.bitDoneRate.setTextColor(getResources().getColor(R.color.lateColor));
                    holder.othersDoneRate.setTextColor(getResources().getColor(R.color.doneColor));
                } else {
                    holder.bitDoneRate.setTextColor(getResources().getColor(R.color.noneColor));
                    holder.othersDoneRate.setTextColor(getResources().getColor(R.color.noneColor));
                }

                holder.bitDoneRate.setText(tm.getDoneRate(t) + " %");
                holder.othersDoneRate.setText(tm.getDoneRateExcept(t) + " %");


                TimeLineAdapter adapter = new TimeLineAdapter(getActivity(), t.getActionsRecords());
                holder.timeLine.setAdapter(adapter);
            }

            return v;
        }

    }

    private class TimeLineAdapter extends ArrayAdapter<ActionRecord> {
        List<ActionRecord> mItems;

        public TimeLineAdapter(Context ctx, List<ActionRecord> t) {
            super(ctx, R.layout.timeline_girdview_item, t);
            Collections.reverse(t);
            mItems = t;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.timeline_girdview_item, parent, false);
            }

            FrameLayout item = (FrameLayout) v.findViewById(R.id.timeline_item);

            switch (mItems.get(position).getAction()) {
                case TaskManager.ACTION_TYPE_LATE:
                    item.setBackgroundColor(getResources().getColor(R.color.lateColor));
                    break;
                case TaskManager.ACTION_TYPE_DONE:
                    item.setBackgroundColor(getResources().getColor(R.color.doneColor));
                    break;
                case TaskManager.ACTION_TYPE_SKIP:
                    item.setBackgroundColor(getResources().getColor(R.color.skipColor));
                    break;
                default:
                    item.setBackgroundColor(getResources().getColor(R.color.noneColor));
                    break;
            }

            v.setTag(mItems.get(position));
            return v;
        }
    }
}
