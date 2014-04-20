package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.Category;
import com.siqi.bits.Task;
import com.siqi.bits.app.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import model.CategoryManager;
import model.TaskManager;
import views.ExpandingGridView;


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
    private static final String EDITING_BIT_ID = "bit_id";


    private Long mEditingBitID;
    private Task mTask;

    private EditText mBitTitleEditText;
    // n times
    private RadioGroup mFrequencyRBtnGroup;
    // per week
    private RadioGroup mIntervalRBtnGroup;

    private ExpandingGridView mCategoryGridView;

    private CategoryAdapter mAdapter;

    private OnNewBitInteractionListener mListener;

    private CategoryManager cm;
    private TaskManager tm;

    private AdapterView.OnItemClickListener mOnClickListener;
    private View mLastSelected = null;

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
            args.putLong(EDITING_BIT_ID, id);
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

        if (getArguments() != null) {
            mEditingBitID = getArguments().getLong(EDITING_BIT_ID);
            mTask = tm.getTask(mEditingBitID);
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
        mCategoryGridView = (ExpandingGridView) v.findViewById(R.id.category_gridview);

        mBitTitleEditText.requestFocus();

        // Inflate the layout for this fragment
        mAdapter = new CategoryAdapter(this.getActivity(), cm.getAllCategories());
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);

        swingBottomInAnimationAdapter.setAbsListView(mCategoryGridView);
        swingBottomInAnimationAdapter.setInitialDelayMillis(300);
        mCategoryGridView.setAdapter(swingBottomInAnimationAdapter);

        mOnClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                v.setBackgroundResource(R.color.MidnightBlue);

                if (mLastSelected != null && mLastSelected != v)
                    mLastSelected.setBackgroundResource(android.R.color.transparent);
                mLastSelected = v;

                Log.d("BitListFrag", "Detected Click on View with tag: "+ ((Category) mLastSelected.getTag()).getName());
            }
        };

        mCategoryGridView.setOnItemClickListener(mOnClickListener);


        if (mEditingBitID != null) {
            Log.d("BitListFrag", "Loading bit to edit");
            // Editing!!
            mBitTitleEditText.setText(mTask.getDescription());

            String frequencyInterval = mTask.getFrequencyIntervalPair();
            switch (frequencyInterval.charAt(0)) {
                case 'd':
                    mIntervalRBtnGroup.check(R.id.radio_day);
                    break;
                case 'w':
                    mIntervalRBtnGroup.check(R.id.radio_week);
                    break;
                case 'm':
                    mIntervalRBtnGroup.check(R.id.radio_month);
                    break;
                case 'y':
                    mIntervalRBtnGroup.check(R.id.radio_year);
                    break;
                default:
                    Log.e("NEW_BIT_FRAGMENT", "Unknown interval code");
            }

            switch (frequencyInterval.charAt(1)) {
                case '1':
                    mFrequencyRBtnGroup.check(R.id.radio_1);
                    break;
                case '2':
                    mFrequencyRBtnGroup.check(R.id.radio_2);
                    break;
                case '3':
                    mFrequencyRBtnGroup.check(R.id.radio_3);
                    break;
                case '4':
                    mFrequencyRBtnGroup.check(R.id.radio_4);
                    break;
                case '5':
                    mFrequencyRBtnGroup.check(R.id.radio_5);
                    break;
                case '6':
                    mFrequencyRBtnGroup.check(R.id.radio_6);
                    break;
                default:
                    Log.d("NEW_BIT_FRAGMENT", "Unknown frequency code");
            }
        } else {
            Log.d("BitListFrag", "Handling new bit");
            mTask.setCategory(cm.getDefaultCategory());
        }

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
             * Save task model here
             */
            mTask.setDescription(mBitTitleEditText.getText().toString().trim());
            mTask.setModifiedOn(new Date());
            mTask.setCategory((Category) mLastSelected.getTag());

            Log.d("BitListFrag", mTask.getDescription() + " in " + ((Category) mLastSelected.getTag()).getName() + " Now ");

            RadioButton rbFreq = (RadioButton) this.getView().findViewById(this.mFrequencyRBtnGroup.getCheckedRadioButtonId());
            RadioButton rbInterval = (RadioButton) this.getView().findViewById(this.mIntervalRBtnGroup.getCheckedRadioButtonId());

            tm.setIntervalFrequencyForTask(mTask, rbInterval.getText().toString(), rbFreq.getText().toString());

            mTask.setNextScheduledTime(System.currentTimeMillis() + mTask.getInterval());

            try {
                if (mEditingBitID == null) {
                    mTask.setCreatedOn(new Date());
                    tm.insertTask(mTask);
                } else {
                    tm.updateTask(mTask);
                }
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
            super(ctx, R.layout.fragment_new_bit_category_gridview_item, t);
            mItems = t;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.fragment_new_bit_category_gridview_item, parent, false);
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

            if (mTask.getCategoryId() != -1 && mLastSelected == null && mAdapter.getItem(position).getId() == mTask.getCategory().getId()) {
                v.setBackgroundColor(getResources().getColor(R.color.MidnightBlue));
                mLastSelected = v;
                Log.d("INIT", "METHOD CALLED FOR CAT " + mTask.getCategory().getName());
            }

            v.setTag(c);
            return v;
        }

        public int getPositionById(Category category) {
            for (int i = 0; i < mItems.size(); ++i) {
                if (category.getId() == mItems.get(i).getId()){
                    return i;
                }
            }
            return -1;
        }
    }
}
