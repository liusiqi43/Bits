package com.siqi.bits.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.siqi.bits.Category;
import com.siqi.bits.Task;
import com.siqi.bits.app.R;

import model.CategoryManager;
import model.TaskManager;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewBitFragment.OnNewBitInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewBitFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class NewBitFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String BIT_ID = "bit_id";
    public static final int FRAGMENT_ID = 9;

    // TODO: Rename and change types of parameters
    private long mBitID;
    private Task mTask;

    private EditText mBitTitle;
    // n times
    private RadioGroup mFrequency;
    // per week
    private RadioGroup mInterval;

    private OnNewBitInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param id current ID of task
     * @return A new instance of fragment NewBitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewBitFragment newInstance(Long id) {
        NewBitFragment fragment = new NewBitFragment();
        if (id != null) {
            Bundle args = new Bundle();
            args.putLong(BIT_ID, id);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public NewBitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TaskManager tm = TaskManager.getInstance(this.getActivity().getApplicationContext());
        CategoryManager cm = CategoryManager.getInstance(this.getActivity().getApplicationContext());

        if (getArguments() != null) {
            mBitID = getArguments().getLong(BIT_ID);
            mTask = tm.getTask(mBitID);
        } else {
            Category c = cm.getDefaultCategory();
            mTask = tm.newTask(c);
            mTask.setInterval(24 * 3600 * 1000);
            mTask.setLastDone(System.currentTimeMillis());
            mTask.setNextScheduledTime(mTask.getLastDone() + mTask.getInterval());
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_bit, container, false);
        setHasOptionsMenu(true);

        mBitTitle = (EditText) v.findViewById(R.id.bit_title_edittext);
        mFrequency = (RadioGroup) v.findViewById(R.id.frequency_radio_group);
        mInterval = (RadioGroup) v.findViewById(R.id.interval_radio_group);

        // Inflate the layout for this fragment
        return v;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.getActivity().getMenuInflater().inflate(R.menu.newbits, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_cancel) {
            this.mListener.onNewDisposeInteraction();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            /**
             * Save the model here
             */
            this.mListener.onNewDisposeInteraction();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnNewBitInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnNewBitInteractionListener {
        // TODO: Update argument type and name
        public void onNewDisposeInteraction();
    }

}
