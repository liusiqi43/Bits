package com.siqi.bits.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.util.DisplayMetrics;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.ActionRecord;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;
import com.siqi.bits.swipelistview.BaseSwipeListViewListener;
import com.siqi.bits.swipelistview.SwipeListView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import model.CategoryManager;
import model.TaskManager;
import utils.ShakeEventListener;

/**
 * Created by me on 4/8/14.
 */
public class BitsListFragment extends Fragment implements ShakeEventListener.OnShakeListener {
    public static final int FRAGMENT_ID = 1;

    public static final int CARD_INFO = 0;
    public static final int CARD_ACTION = 1;

    private CategoryManager cm;
    private TaskManager tm;

    SwipeListView mBitsListView;
    BitListArrayAdapter mAdapter;
    AnimateDismissAdapter mAnimateDismissAdapter;

    private SensorManager mSensorManager;
    private ShakeEventListener mSensorListener;

    private OnBitListInteractionListener mListener;

    private boolean mUndoDialogDisplayed = false;

    // Reordering animation
    HashMap<Task, Integer> mSavedState = new HashMap<Task, Integer>();
    Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public BitsListFragment() {
    }

    public static BitsListFragment newInstance() {
        BitsListFragment fragment = new BitsListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        /**
         * View Binding
         */
        View rootView = inflater.inflate(R.layout.fragment_bitslist, container, false);
        mBitsListView = (SwipeListView) rootView.findViewById(R.id.bitslist);

        /**
         * Data Loading
         */
        tm = TaskManager.getInstance(this.getActivity().getApplicationContext());
        cm = CategoryManager.getInstance(this.getActivity().getApplicationContext());

        mAdapter = new BitListArrayAdapter(getActivity().getApplicationContext(), tm.getAllSortedTasks());
        mAnimateDismissAdapter = new AnimateDismissAdapter(mAdapter, new OnBitDismissCallback());

        // Swing from bottom anim & dismiss anim
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAnimateDismissAdapter);
        swingBottomInAnimationAdapter.setInitialDelayMillis(200);
        swingBottomInAnimationAdapter.setAnimationDurationMillis(400);
        swingBottomInAnimationAdapter.setAbsListView(mBitsListView);
        this.mBitsListView.setAdapter(swingBottomInAnimationAdapter);


        this.mBitsListView.setSwipeListViewListener(new BaseSwipeListViewListener(){
            @Override
            public void onLeftChoiceAction(int position) {
                if (mAdapter.isExpanded(position))
                    mAdapter.toggle(position);
                Task item = mAdapter.getItem(position);
                tm.setActionRecordForTask(item, TaskManager.ACTION_TYPE_SKIP);
                tm.setNextScheduledTimeForTask(item);
                item.update();
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
                tm.setActionRecordForTask(item, TaskManager.ACTION_TYPE_DONE);
                tm.setNextScheduledTimeForTask(item);
                item.update();
                saveState();
                mAdapter.clear();
                mAdapter.addAll(tm.getAllSortedTasks());
                animateNewState();
            }

        });


        mBitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int position, long id) {

                final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.card_viewswitcher);
                viewSwitcher.showNext();

                new Handler().postDelayed(new Runnable() { public void run() {
                    if(viewSwitcher.getDisplayedChild() == CARD_ACTION) {
                        viewSwitcher.setDisplayedChild(CARD_INFO);
                    }
                }
                }, getResources().getInteger(R.integer.actionview_timeout));
                return true;
            }
        });

        mAdapter.setLimit(1);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new ShakeEventListener();

        mSensorListener.setOnShakeListener(this);

        return rootView;
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
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener);
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

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
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
            int padInPx = dpToPx(15);
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
        for(int i=0; i<mAdapter.size(); i++) {
            if( i >= first && i <= last) {
                View v = mBitsListView.getChildAt(i-first);
                int top = v.getTop();
                int dataIdx = i;
                Task dataId = mAdapter.get(dataIdx);
                mSavedState.put(dataId, top);
            } else if( i < first ) {
                int top = mBitsListView.getTop() - mBitsListView.getHeight()/2;
                Task dataId = mAdapter.get(i);
                mSavedState.put(dataId, top);
            } else if( i > last ) {
                int top = mBitsListView.getBottom() + mBitsListView.getHeight()/2;
                Task dataId = mAdapter.get(i);
                mSavedState.put(dataId, top);
            }
        }
        for(int i=0; i < mBitsListView.getChildCount(); i++) {
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
        for(int i=0; i < mBitsListView.getChildCount(); i++) {
            int dataIdx = first + i;
            Task dataId = mAdapter.get(dataIdx);
            if( mSavedState.containsKey(dataId) ) {
                View v = mBitsListView.getChildAt(i);
                int top = v.getTop();
                int oldTop = mSavedState.get(dataId);
                int hDiff = top - oldTop;
                TranslateAnimation anim = new TranslateAnimation(0, 0, -hDiff, 0);
                anim.setInterpolator(mInterpolator);
                anim.setDuration(333);
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
        Button skipButton, doneButton, editButton, deleteButton;
        ViewSwitcher viewSwitcher;
    }

    private static class BitContentHolder {
        TextView bitDoneRate;
        TextView othersDoneRate;
        GridView timeLine;
        LinearLayout globalLayout;
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
            Log.d("TEST", "getTitleVlew:"+position);
            View v = convertView;
            final BitTitleHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.card_title_layout, parent, false);

                holder = new BitTitleHolder();

                holder.icon = (ImageView) v.findViewById(R.id.taskIcon);
                holder.title = (TextView) v.findViewById(R.id.taskTitle);
                holder.timeAgo = (TextView) v.findViewById(R.id.timeAgo);
                holder.progressBar = (ProgressBar) v.findViewById(R.id.timeAgoProgressBar);
                holder.doneButton = (Button) v.findViewById(R.id.done_button);
                holder.skipButton = (Button) v.findViewById(R.id.skip_button);
                holder.editButton = (Button) v.findViewById(R.id.edit_button);
                holder.deleteButton = (Button) v.findViewById(R.id.delete_button);
                holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);

                v.setTag(holder);
            } else {
                holder = (BitTitleHolder) v.getTag();
                holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);
                if (holder.viewSwitcher.getDisplayedChild() == CARD_ACTION) {
                    holder.viewSwitcher.setInAnimation(null);
                    holder.viewSwitcher.setOutAnimation(null);
                    holder.viewSwitcher.setDisplayedChild(CARD_INFO);

                    Animation inAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left); inAnimation.setDuration(300);
                    Animation outAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right); outAnimation.setDuration(300);

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
                InputStream is = null;
                try {
                    is = getActivity().getAssets().open(t.getCategory().getIconDrawableName());
                    bitmap = BitmapFactory.decodeStream(is);
                    // Invert bitmap color
                    bitmap = CategoryManager.invertImage(bitmap);
                    if (bitmap != null)
                        mMemoryCache.put(t.getCategory().getIconDrawableName(), bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(is!=null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            holder.icon.setMaxWidth(holder.icon.getHeight());
            holder.icon.setImageBitmap(bitmap);
            holder.title.setText(t.getDescription());
            holder.timeAgo.setText(tm.getTimesAgoDescriptionForTask(t));

            int progress = tm.getProgressForTask(t);
            if (progress > 100) {
                // Only triggered when t.getFrequency() - actionCountSinceBeginOfInternval > 0, and progress > 100, which means... well, late!
                tm.updateActionRecordForTask(t);
            }
            holder.progressBar.setProgress(progress);
            Log.d("Progress for " + t.getDescription(), progress + "");

            Animation inAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left); inAnimation.setDuration(300);
            Animation outAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right); outAnimation.setDuration(300);

            holder.viewSwitcher.setInAnimation(inAnimation);
            holder.viewSwitcher.setOutAnimation(outAnimation);

            Log.d("BitListFrag", t.getDescription() + " : " + t.getNextScheduledTime());

            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getVlew:"+position);
            View v = super.getView(position, convertView, parent);

            ((SwipeListView) parent).recycle(v, position);

            return v;
        }

        @Override
        public View getContentView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getContentVlew:"+position);
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
