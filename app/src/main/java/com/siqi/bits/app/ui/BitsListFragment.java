package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
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
    List<Task> mBitsList = new ArrayList<Task>();

    private OnBitListInteractionListener mListener;

    public static BitsListFragment newInstance() {
        BitsListFragment fragment = new BitsListFragment();
        return fragment;
    }

    public BitsListFragment() {
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

        mBitsList = tm.getAllTasks();

        if (!mBitsList.isEmpty()) {
            BitListArrayAdapter adapter = new BitListArrayAdapter(getActivity().getApplicationContext(), mBitsList);
            this.mBitsListView.setAdapter(adapter);
        }

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

        public BitListArrayAdapter(Context ctx, List<Task> t) {
            super(ctx, R.layout.listview_item, t);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.listview_item, null);
            }

            ImageView icon = (ImageView) v.findViewById(R.id.taskIcon);
            TextView title = (TextView) v.findViewById(R.id.taskTitle);
            TextView timeAgo = (TextView) v.findViewById(R.id.timeAgo);
            ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.timeAgoProgressBar);

            Task t = getItem(position);
            PrettyTime p = new PrettyTime();

            icon.setImageResource(getResources().getIdentifier(
                    t.getCategory().getIconDrawableName(),
                    "drawable",
                    getContext().getPackageName()));
            title.setText(t.getDescription());
            timeAgo.setText(p.format(new Date(t.getLastDone())));
            long duration = System.currentTimeMillis()-t.getLastDone();
            progressBar.setProgress((int)(duration/t.getInterval()));

            return v;
        }
    }


    public interface OnBitListInteractionListener {
        public void startEditBitFragment(Long id);
    }
}
