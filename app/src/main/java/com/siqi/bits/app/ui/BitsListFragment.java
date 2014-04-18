package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.caverock.androidsvg.SVGImageView;
import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;
import com.siqi.bits.swipelistview.BaseSwipeListViewListener;
import com.siqi.bits.swipelistview.SwipeListView;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;

import model.CategoryManager;
import model.TaskManager;

/**
 * Created by me on 4/8/14.
 */
public class BitsListFragment extends Fragment {
    public static final int FRAGMENT_ID = 1;

    private CategoryManager cm;
    private TaskManager tm;

    SwipeListView mBitsListView;
    BitListArrayAdapter mAdapter;

    private OnBitListInteractionListener mListener;

    public static BitsListFragment newInstance() {
        BitsListFragment fragment = new BitsListFragment();
        return fragment;
    }

    public BitsListFragment() {
    }

    private class OnBitDismissCallback implements OnDismissCallback {

        @Override
        public void onDismiss(final AbsListView listView, final int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                mAdapter.remove(mAdapter.getItem(position));
            }
        }
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
        final AnimateDismissAdapter animateDismissAdapter = new AnimateDismissAdapter(mAdapter, new OnBitDismissCallback());

        // Swing from bottom anim & dismiss anim
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(animateDismissAdapter);
        swingBottomInAnimationAdapter.setInitialDelayMillis(200);
        swingBottomInAnimationAdapter.setAnimationDurationMillis(400);
        swingBottomInAnimationAdapter.setAbsListView(mBitsListView);
        this.mBitsListView.setAdapter(swingBottomInAnimationAdapter);


        this.mBitsListView.setSwipeListViewListener(new BaseSwipeListViewListener(){
            @Override
            public void onLeftChoiceAction(int position) {
                Task item = mAdapter.getItem(position);
                item.setNextScheduledTime(System.currentTimeMillis() + item.getInterval());
                item.incrementSkipCount();
                tm.updateTask(item);
                // Implicitly calls datasetChanged() method
                mAdapter.remove(item);
                mAdapter.add(item);
            }

            @Override
            public void onRightChoiceAction(int position) {
                Task item = mAdapter.getItem(position);
                item.setLastDoneAndUpdateNextScheduleTime(System.currentTimeMillis());
                item.incrementDoneCount();
                tm.updateTask(item);
                // Implicitly calls datasetChanged() method
                mAdapter.remove(item);
                mAdapter.add(item);
            }
        });

        mBitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d("LongClick", "Called");
                Task item = mAdapter.getItem(position);
                item.setDeletedOn(new Date());
                tm.updateTask(item);
                // Implicitly calls datasetChanged() method

                animateDismissAdapter.animateDismiss(position);
                return true;
            }
        });

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

    private class BitListArrayAdapter extends ArrayAdapter<Task> {

        List<Task> mItems;

        public BitListArrayAdapter(Context ctx, List<Task> t) {
            super(ctx, R.layout.listview_item, t);
            mItems = t;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            BitHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.listview_item, parent, false);

                holder = new BitHolder();

                holder.iconFrameLayout = (FrameLayout) v.findViewById(R.id.taskIcon);
                holder.title = (TextView) v.findViewById(R.id.taskTitle);
                holder.timeAgo = (TextView) v.findViewById(R.id.timeAgo);
                holder.progressBar = (ProgressBar) v.findViewById(R.id.timeAgoProgressBar);
                holder.doneButton = (Button) v.findViewById(R.id.done_button);
                holder.skipButton = (Button) v.findViewById(R.id.skip_button);

                v.setTag(holder);
            } else {
                holder = (BitHolder) v.getTag();
            }

            Task t = getItem(position);
            PrettyTime p = new PrettyTime();

            SVGImageView svgImageView = new SVGImageView(getActivity());
            svgImageView.setImageAsset(t.getCategory().getIconDrawableName());

            holder.iconFrameLayout.removeAllViews();
            holder.iconFrameLayout.addView(svgImageView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            holder.title.setText(t.getDescription());
            holder.timeAgo.setText(p.format(new Date(t.getLastDone())));
            long duration = System.currentTimeMillis()-t.getLastDone();
            holder.progressBar.setProgress((int) (100 * (double) duration / (double) t.getInterval()));

            ((SwipeListView) parent).recycle(v, position);

            return v;
        }

    }


    public interface OnBitListInteractionListener {
        public void startEditBitFragment(Long id);
    }


    private static class BitHolder {
        FrameLayout iconFrameLayout;
        TextView title;
        TextView timeAgo;
        ProgressBar progressBar;
        Button skipButton, doneButton;
    }
}
