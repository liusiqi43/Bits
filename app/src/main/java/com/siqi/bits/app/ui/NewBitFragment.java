package com.siqi.bits.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

    // TODO: Rename and change types of parameters
    private long mBitID;
    private Task mTask;

    private Button mCancelBtn;
    private Button mSaveBtn;


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
            mTask.setDescription("What's on your mind");
            mTask.setInterval(24 * 3600 * 1000);
            mTask.setLastDone(System.currentTimeMillis());
            mTask.setNextScheduledTime(mTask.getLastDone() + mTask.getInterval());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_bit, container, false);


        mCancelBtn = (Button) v.findViewById(R.id.cancel_button);

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onNewDisposeInteraction();
            }
        });

        // Inflate the layout for this fragment
        return v;
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
