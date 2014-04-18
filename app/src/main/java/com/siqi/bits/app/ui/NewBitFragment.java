package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.Category;
import com.siqi.bits.Task;
import com.siqi.bits.app.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
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

    private GridView mCategoryGridView;

    private CategoryAdapter mAdapter;

    private OnNewBitInteractionListener mListener;

    private CategoryManager cm;
    private TaskManager tm;

    private AdapterView.OnItemClickListener mOnClickListener;
    private View mLastSelected;

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

        tm = TaskManager.getInstance(this.getActivity().getApplicationContext());
        cm = CategoryManager.getInstance(this.getActivity().getApplicationContext());

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
            mTask = tm.newTask();
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
        mCategoryGridView = (GridView) v.findViewById(R.id.category_gridview);

        // Inflate the layout for this fragment
        mAdapter = new CategoryAdapter(this.getActivity(), cm.getAllCategories());
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);

        swingBottomInAnimationAdapter.setAbsListView(mCategoryGridView);
        swingBottomInAnimationAdapter.setInitialDelayMillis(300);
        mCategoryGridView.setAdapter(swingBottomInAnimationAdapter);

        mOnClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(getActivity(), mAdapter.getItem(position).getName(), Toast.LENGTH_SHORT).show();
                v.setBackgroundResource(R.color.MidnightBlue);

                if (mLastSelected != null && mLastSelected != v)
                    mLastSelected.setBackgroundResource(android.R.color.transparent);
                mLastSelected = v;
            }
        };

        mCategoryGridView.setOnItemClickListener(mOnClickListener);

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

            if (mLastSelected == null) {
                Toast.makeText(this.getActivity(), "Please select a category for the Bit", Toast.LENGTH_SHORT).show();
                return true;
            }

            /**
             * Save the model here
             */
            mTask.setLastDone(System.currentTimeMillis());
            mTask.setDescription(mBitTitleEditText.getText().toString().trim());
            mTask.setCreatedOn(new Date());
            mTask.setModifiedOn(new Date());
            mTask.setDoneCount(0);
            mTask.setSkipCount(0);
            mTask.setLateCount(0);
            mTask.setCategory((Category) mLastSelected.getTag());

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

    private class CategoryAdapter extends ArrayAdapter<Category> {
        List<Category> mItems;

        public CategoryAdapter(Context ctx, List<Category> t) {
            super(ctx, R.layout.category_gridview_item, t);
            mItems = t;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.category_gridview_item, parent, false);
            }

            ImageView icon = (ImageView) v.findViewById(R.id.grid_item_image);

            Category c = getItem(position);


            InputStream is = null;
            try {
                is = getActivity().getAssets().open(c.getIconDrawableName());
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                icon.setImageBitmap(CategoryManager.invertImage(bitmap));
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

            v.setTag(c);
            return v;
        }
    }
}
