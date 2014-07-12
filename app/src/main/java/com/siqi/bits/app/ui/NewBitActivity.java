package com.siqi.bits.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

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
import utils.Utils;
import views.ExpandingGridView;


public class NewBitActivity extends ActionBarActivity {
    public static final int FRAGMENT_ID = 9;
    public static final String EDITING_BIT_ID = "bit_id";


    private Long mEditingBitID;
    private Task mTask;

    private AutoCompleteTextView mBitTitleEditText;
    // n times
    private RadioGroup mFrequencyRBtnGroup;
    // per week
    private RadioGroup mPeriodRBtnGroup;

    private ExpandingGridView mCategoryGridView;

    private TextSwitcher mCategorySelectedTV;

    private CategoryAdapter mAdapter;

    private CategoryManager cm;
    private TaskManager tm;

    private AdapterView.OnItemClickListener mOnClickListener;
    private View mLastSelected = null;
    private SharedPreferences mPreferences;
    private Handler mIdeasSuggestionRefresherHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (mPreferences.getBoolean("IS_AUTO_ROTATE_ENABLED", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        long id = getIntent().getLongExtra(EDITING_BIT_ID, -1);
        mEditingBitID = id == -1 ? null : id;

        tm = TaskManager.getInstance(this);
        cm = CategoryManager.getInstance(this);

        if (mEditingBitID != null) {
            mTask = tm.getTask(mEditingBitID);
        } else {
            mTask = tm.newTask();
        }

        setContentView(R.layout.new_bit_fragment);
        mBitTitleEditText = (AutoCompleteTextView) findViewById(R.id.bit_title_edittext);
        mFrequencyRBtnGroup = (RadioGroup) findViewById(R.id.frequency_radio_group);
        mPeriodRBtnGroup = (RadioGroup) findViewById(R.id.interval_radio_group);
        mCategorySelectedTV = (TextSwitcher) findViewById(R.id.category_selected);
        mCategoryGridView = (ExpandingGridView) findViewById(R.id.category_gridview);

        mBitTitleEditText.requestFocus();

        mCategorySelectedTV.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView t = new TextView(NewBitActivity.this);
                t.setGravity(Gravity.CENTER_HORIZONTAL);
                t.setBackgroundColor(getResources().getColor(R.color.MidnightBlue));
                t.setPadding(15, 5, 15, 5);
                t.setTextAppearance(NewBitActivity.this, android.R.style.TextAppearance_Holo_Large_Inverse);
                return t;
            }
        });

        mCategorySelectedTV.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.card_flip_top_in));
        mCategorySelectedTV.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.card_flip_bottom_out));

        // Inflate the layout for this fragment
        mAdapter = new CategoryAdapter(this, cm.getAllCategories());
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAdapter);

        swingBottomInAnimationAdapter.setAbsListView(mCategoryGridView);
        swingBottomInAnimationAdapter.setInitialDelayMillis(300);
        mCategoryGridView.setAdapter(swingBottomInAnimationAdapter);

        mOnClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mCategorySelectedTV.setText(mAdapter.getItem(position).getName());
                v.setBackgroundResource(R.color.MidnightBlue);

                if (mLastSelected != null && mLastSelected != v)
                    mLastSelected.setBackgroundResource(android.R.color.transparent);
                mLastSelected = v;

                Log.d("BitListFrag", "Detected Click on View with tag: " + ((Category) mLastSelected.getTag()).getName());
            }
        };

        mCategoryGridView.setOnItemClickListener(mOnClickListener);

        if (mEditingBitID != null) {
            Log.d("BitListFrag", "Loading bit to edit");
            // Editing!!
            mBitTitleEditText.setText(mTask.getDescription());

            long period = mTask.getPeriod();
            switch (TaskManager.PeriodToDays.get(period)) {
                case 1:
                    mPeriodRBtnGroup.check(R.id.radio_day);
                    break;
                case 7:
                    mPeriodRBtnGroup.check(R.id.radio_week);
                    break;
                case 30:
                    mPeriodRBtnGroup.check(R.id.radio_month);
                    break;
                case 365:
                    mPeriodRBtnGroup.check(R.id.radio_year);
                    break;
                default:
                    Log.e("NEW_BIT_FRAGMENT", "Unknown interval code");
            }

            switch (mTask.getFrequency()) {
                case 1:
                    mFrequencyRBtnGroup.check(R.id.radio_1);
                    break;
                case 2:
                    mFrequencyRBtnGroup.check(R.id.radio_2);
                    break;
                case 3:
                    mFrequencyRBtnGroup.check(R.id.radio_3);
                    break;
                case 4:
                    mFrequencyRBtnGroup.check(R.id.radio_4);
                    break;
                case 5:
                    mFrequencyRBtnGroup.check(R.id.radio_5);
                    break;
                case 6:
                    mFrequencyRBtnGroup.check(R.id.radio_6);
                    break;
                default:
                    Log.d("NEW_BIT_FRAGMENT", "Unknown frequency code");
            }
        } else {
            Log.d("BitListFrag", "Handling new bit");
            mTask.setCategory(cm.getDefaultCategory());
        }

        mCategorySelectedTV.setText(cm.getDefaultCategory().getName());

        // Get the string array
        final String[] ideas = getResources().getStringArray(R.array.bits_title_ideas);
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ideas);
        mBitTitleEditText.setAdapter(adapter);

        mIdeasSuggestionRefresherHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBitTitleEditText.setHint(ideas[Utils.getRandomInt(ideas.length)] + "?");
                mIdeasSuggestionRefresherHandler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideSoftKeyBoard();
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.newbits, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_cancel) {
            onBackPressed();
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
            mTask.setModifiedOn(new Date(Utils.currentTimeMillis()));
            if (mEditingBitID == null) {
                mTask.setCreatedOn(new Date(Utils.currentTimeMillis()));
                try {
                    tm.insertTask(mTask);
                } catch (TaskManager.DuplicatedTaskException e) {
                    mBitTitleEditText.setError(getString(R.string.duplicated_title_error));
                    return true;
                }
            }

            mTask.setCategory((Category) mLastSelected.getTag());

            Log.d("BitListFrag", mTask.getDescription() + " in " + ((Category) mLastSelected.getTag()).getName() + " Now ");
            RadioButton rbFreq = (RadioButton) findViewById(this.mFrequencyRBtnGroup.getCheckedRadioButtonId());
            RadioButton rbPeriod = (RadioButton) findViewById(this.mPeriodRBtnGroup.getCheckedRadioButtonId());

            mTask.setPeriod(TaskManager.PeriodStringToDays.get(rbPeriod.getText().toString()) * TaskManager.DAY_IN_MILLIS);
            mTask.setFrequency(Integer.parseInt(rbFreq.getText().toString()));
            tm.setNextScheduledTimeForTask(mTask);

            tm.updateTask(mTask);
            onBackPressed();

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                LayoutInflater li = getLayoutInflater();
                v = li.inflate(R.layout.fragment_new_bit_category_gridview_item, parent, false);
            }

            ImageView icon = (ImageView) v.findViewById(R.id.grid_item_image);

            Category c = getItem(position);


            InputStream is = null;
            try {
                is = getAssets().open(c.getIconDrawableName());
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                icon.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
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
    }
}
