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
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.siqi.bits.Category;
import com.siqi.bits.Task;
import com.siqi.bits.app.R;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

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
    public static final int FRAGMENT_ID = 9;
    private static final String BIT_ID = "bit_id";
    private final ConcurrentHashMap<String, Integer> IntervalToDays = new ConcurrentHashMap<String, Integer>();

    private long mBitID;
    private Task mTask;

    private EditText mBitTitleEditText;
    // n times
    private RadioGroup mFrequencyRBtnGroup;
    // per week
    private RadioGroup mIntervalRBtnGroup;

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

        /**
         * TODO Optimize this so that there is only one copy of hashmap...
         * Well this is in a Frag so there should be only one copy...how bad it can be??
         */
        IntervalToDays.put(getString(R.string.radio_day), 1);
        IntervalToDays.put(getString(R.string.radio_week), 7);
        IntervalToDays.put(getString(R.string.radio_month), 30);
        IntervalToDays.put(getString(R.string.radio_year), 365);

        if (getArguments() != null) {
            mBitID = getArguments().getLong(BIT_ID);
            mTask = tm.getTask(mBitID);
        } else {
            Category c = cm.getDefaultCategory();
            mTask = tm.newTask(c);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_bit, container, false);
        setHasOptionsMenu(true);

        mBitTitleEditText = (EditText) v.findViewById(R.id.bit_title_edittext);
        mFrequencyRBtnGroup = (RadioGroup) v.findViewById(R.id.frequency_radio_group);
        mIntervalRBtnGroup = (RadioGroup) v.findViewById(R.id.interval_radio_group);

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
            if (mBitTitleEditText.getText().toString().trim().length() == 0) {
                mBitTitleEditText.setError(getString(R.string.empty_title_error));
                return true;
            }

            /**
             * Save the model here
             */
            mTask.setLastDone(System.currentTimeMillis());
            mTask.setDescription(mBitTitleEditText.getText().toString());
            mTask.setCreatedOn(new Date());
            mTask.setModifiedOn(new Date());
            mTask.setDoneCount(0);
            mTask.setSkipCount(0);
            mTask.setLateCount(0);

            RadioButton rbFreq = (RadioButton) this.getView().findViewById(this.mFrequencyRBtnGroup.getCheckedRadioButtonId());
            RadioButton rbInterval = (RadioButton) this.getView().findViewById(this.mIntervalRBtnGroup.getCheckedRadioButtonId());

            int daysCount = IntervalToDays.get(rbInterval.getText().toString());

            mTask.setInterval((long) daysCount * 24 * 3600 * 1000/(Integer.parseInt(rbFreq.getText().toString())));
            mTask.setLastDone(System.currentTimeMillis());
            mTask.setNextScheduledTime(mTask.getLastDone() + mTask.getInterval());

            try {
                TaskManager.getInstance(getActivity().getApplicationContext()).insertTask(mTask);
                mListener.onNewDisposeInteraction();
            } catch (TaskManager.DuplicatedTaskException e) {
                mBitTitleEditText.setError(getString(R.string.duplicated_title_error));
            }

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
