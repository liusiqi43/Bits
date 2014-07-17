package com.siqi.bits.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siqi.bits.app.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import model.CategoryManager;
import model.TaskManager;
import utils.Utils;

/**
 * Proudly powered by me on 5/17/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsPieChartFragment extends Fragment {

    private static final int PIE_DISPLAY_MIN_PARTS = 3;
    /**
     * Colors to be used for the pie slices.
     */
    private static int[] COLORS = new int[]{R.color.Turquoise, R.color.SeaGreen, R.color.Emerald, R.color.Nephritis, R.color.PeterRiver, R.color.BelizeHole, R.color.Amethyst, R.color.Wisteria, R.color.WetAsphalt, R.color.MidnightBlue, R.color.Sunflower, R.color.Orange, R.color.Carrot, R.color.Pumpkin, R.color.Alizarin, R.color.Pomegranate, R.color.silver, R.color.concrete, R.color.Asbestos};
    private TaskManager tm;
    private CategoryManager cm;
    private List<Pair<Long, Integer>> mCatIdToCount = new ArrayList<Pair<Long, Integer>>();
    /**
     * The main series that will include all the data.
     */
    private CategorySeries mSeries;
    /**
     * The main renderer for the main dataset.
     */
    private DefaultRenderer mRenderer = new DefaultRenderer();
    /**
     * The chart view that displays the data.
     */
    private GraphicalView mChartView;
    private Toast mCurrentToast;
    private CheckBox mActiveCheckbox, mArchivedCheckbox;
    private boolean mActiveTasks = true, mArchivedTasks = true;
    private LinearLayout mChartViewContainer;

    public StatsPieChartFragment() {
    }

    public static StatsPieChartFragment newInstance() {
        StatsPieChartFragment fragment = new StatsPieChartFragment();
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setStartAngle(180);
        mRenderer.setDisplayValues(false);

        mSeries = new CategorySeries(getActivity().getString(R.string.tasks_by_category));

        tm = TaskManager.getInstance(getActivity());
        cm = CategoryManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (tm.getCategoryWithCount(mActiveTasks, mArchivedTasks).size() < PIE_DISPLAY_MIN_PARTS) {
            View emptyView = inflater.inflate(R.layout.placeholder_empty_view, container, false);
            TextView tv = (TextView) emptyView.findViewById(R.id.message);
            ImageView imageView = (ImageView) emptyView.findViewById(R.id.icon);
            tv.setText(getString(R.string.piechart_empty_message));
            imageView.setImageResource(R.drawable.piechart);
            return emptyView;
        } else {
            View root = inflater.inflate(R.layout.stats_piechart_fragment, container, false);
            mChartViewContainer = (LinearLayout) root.findViewById(R.id.piechart_container);
            mActiveCheckbox = (CheckBox) root.findViewById(R.id.active_task_checkbox);
            mArchivedCheckbox = (CheckBox) root.findViewById(R.id.archived_task_checkbox);

            mActiveCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mActiveTasks = ((CheckBox) view).isChecked();
                    if (!CheckValidation((CheckBox) view)) {
                        mActiveTasks = true;
                    }
                    reloadData();
                    mChartView.repaint();
                }
            });

            mArchivedCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mArchivedTasks = ((CheckBox) view).isChecked();
                    if (!CheckValidation((CheckBox) view)) {
                        mArchivedTasks = true;
                    }
                    reloadData();
                    mChartView.repaint();
                }
            });

            reloadData();
            mRenderer.setChartTitle(getString(R.string.task_category_chart_title));

            return root;
        }
    }

    private void reloadData() {
        mSeries.clear();
        mCatIdToCount = tm.getCategoryWithCount(mActiveTasks, mArchivedTasks);
        for (Pair p : mCatIdToCount) {
            Long id = (Long) p.first;
            Integer count = (Integer) p.second;

            mSeries.add(cm.getCategory(id).getName(), count);
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            renderer.setColor(getResources().getColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]));
            mRenderer.addSeriesRenderer(renderer);
        }
    }

    private boolean CheckValidation(CheckBox cb) {
        mCatIdToCount = tm.getCategoryWithCount(mActiveTasks, mArchivedTasks);
        if (mCatIdToCount == null || mCatIdToCount.size() < PIE_DISPLAY_MIN_PARTS) {
            cb.setChecked(true);
            Toast.makeText(getActivity(), getString(R.string.not_enough_data), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (tm.getCategoryWithCount(mActiveTasks, mArchivedTasks).size() >= PIE_DISPLAY_MIN_PARTS) {
            if (mChartView == null) {
                mChartView = ChartFactory.getPieChartView(getActivity(), mSeries, mRenderer);
                mRenderer.setClickEnabled(true);
                mRenderer.setPanEnabled(false);
                mRenderer.setZoomEnabled(true);
                mRenderer.setApplyBackgroundColor(true);
                mChartView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                        if (seriesSelection != null) {
                            for (int i = 0; i < mSeries.getItemCount(); i++) {
                                mRenderer.getSeriesRendererAt(i).setHighlighted(i == seriesSelection.getPointIndex());
                            }
                            mChartView.repaint();

                            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View view = inflater.inflate(R.layout.stats_piechart_toast_layout, null);
                            View layout = view.findViewById(R.id.toast_layout_root);
                            TextView text = (TextView) layout.findViewById(R.id.text);
                            ImageView categoryIcon = (ImageView) layout.findViewById(R.id.categoryIcon);
                            text.setText((int) seriesSelection.getValue() + " " + getString(R.string.task_in_this_category));

                            Iterator<Pair<Long, Integer>> it = mCatIdToCount.iterator();
                            while (it.hasNext()) {
                                Pair<Long, Integer> pairs = it.next();
                                Long id = pairs.first;
                                Log.d("PieChart", cm.getCategory(id).getName() + " == " + mSeries.getCategory(seriesSelection.getPointIndex()) + "? ");
                                if (cm.getCategory(id).getName().equals(mSeries.getCategory(seriesSelection.getPointIndex()))) {
                                    try {
                                        categoryIcon.setImageBitmap(cm.getCategory(id).getIconBitmap(getActivity()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                            }

                            if (mCurrentToast == null) {
                                mCurrentToast = new Toast(getActivity());
                            }
                            mCurrentToast.setView(layout);
                            mCurrentToast.setDuration(Toast.LENGTH_SHORT);
                            mCurrentToast.show();
                        }
                    }
                });
                mChartViewContainer.addView(mChartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                mRenderer.setMargins(new int[]{Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(5), Utils.dpToPx(5)});
                mRenderer.setChartTitleTextSize(Utils.dpToPx(20));
                mRenderer.setLabelsTextSize(Utils.dpToPx(12));
                mRenderer.setLabelsColor(getResources().getColor(R.color.black));
                mRenderer.setShowLegend(false);
            } else {
                mChartView.repaint();
            }
        }
    }

}
