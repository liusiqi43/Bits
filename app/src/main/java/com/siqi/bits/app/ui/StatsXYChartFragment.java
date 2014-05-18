package com.siqi.bits.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.siqi.bits.app.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Collections;
import java.util.List;

import model.ActionRecordManager;

/**
 * Proudly powered by me on 5/17/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsXYChartFragment extends Fragment {

    private ActionRecordManager arm;

    private LinearLayout mChartViewContainer;

    public StatsXYChartFragment() {
    }

    public static StatsXYChartFragment newInstance() {
        StatsXYChartFragment fragment = new StatsXYChartFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        arm = ActionRecordManager.getInstance(getActivity());

        View root = inflater.inflate(R.layout.stats_linechart_fragment, container, false);
        mChartViewContainer = (LinearLayout) root.findViewById(R.id.xychart_container);

        BurndownChart chart = new BurndownChart();
        mChartViewContainer.addView(chart.getView(getActivity(), arm.getBurnRateForLastDays(14)));
        return root;
    }

    public class BurndownChart {

        private static final String DATE_FORMAT = "dd/MM/yyyy";

        public GraphicalView getView(Context context, List<ActionRecordManager.DateBurnratePair> results) {
            String title = context.getString(R.string.chartTitle);

            int[] colors = new int[] { Color.GREEN };
            PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
            XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);

            ActionRecordManager.DateBurnratePair max = Collections.max(results);
            ActionRecordManager.DateBurnratePair min = Collections.min(results);

            if (!results.isEmpty()) {
                setChartSettings(renderer, context.getString(R.string.chartTitle),
                        context.getString(R.string.burndown_chart_datetitle),
                        context.getString(R.string.burndown_chart_counttitle), results.get(0).mDate.getTime(), results.get(results.size() - 1)
                                .mDate.getTime(), min.mBurnrate, max.mBurnrate, Color.GRAY, Color.LTGRAY);
            }
            renderer.setXLabels(5);
            renderer.setYLabels(10);
            SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(0);
            seriesRenderer.setDisplayChartValues(true);

            return ChartFactory.getTimeChartView(context,
                    buildDateDataset(title, results), renderer, DATE_FORMAT);
        }

        protected void setChartSettings(XYMultipleSeriesRenderer renderer,
                                        String title, String xTitle, String yTitle, double xMin,
                                        double xMax, double yMin, double yMax, int axesColor,
                                        int labelsColor) {
            renderer.setChartTitle(title);
            renderer.setXTitle(xTitle);
            renderer.setYTitle(yTitle);
            renderer.setXAxisMin(xMin);
            renderer.setXAxisMax(xMax);
            renderer.setYAxisMin(yMin);
            renderer.setYAxisMax(yMax);
            renderer.setAxesColor(axesColor);
            renderer.setLabelsColor(labelsColor);
        }

        protected XYMultipleSeriesRenderer buildRenderer(int[] colors,
                                                         PointStyle[] styles) {
            XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
            setRendererProperties(renderer, colors, styles);
            return renderer;
        }

        protected XYMultipleSeriesDataset buildDateDataset(String title,
                                                           List<ActionRecordManager.DateBurnratePair> results) {
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            TimeSeries series = new TimeSeries(title);
            for (ActionRecordManager.DateBurnratePair result : results) {
                series.add(result.mDate, result.mBurnrate);
            }
            dataset.addSeries(series);
            return dataset;
        }

        protected void setRendererProperties(XYMultipleSeriesRenderer renderer, int[] colors,
                                             PointStyle[] styles) {
            renderer.setAxisTitleTextSize(16);
            renderer.setChartTitleTextSize(20);
            renderer.setLabelsTextSize(15);
            renderer.setLegendTextSize(15);
            renderer.setPointSize(5f);
            renderer.setMargins(new int[] { 20, 30, 15, 20 });
            int length = colors.length;
            for (int i = 0; i < length; i++) {
                XYSeriesRenderer r = new XYSeriesRenderer();
                r.setLineWidth(5f);
                r.setPointStrokeWidth(25f);
                r.setFillPoints(false);
                r.setColor(colors[i]);
                r.setPointStyle(styles[i]);
                renderer.addSeriesRenderer(r);
            }
        }
    }


}
