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
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.List;

import model.ActionRecordManager;

/**
 * Proudly powered by me on 5/17/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class StatsXYChartFragment extends Fragment {

    private static final int PLOT_FOR_LAST_NB_DAYS = 14;
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
        mChartViewContainer.addView(chart.getView(getActivity(), arm.getBurnRateForLastDays(PLOT_FOR_LAST_NB_DAYS)));
        return root;
    }

    public class BurndownChart {

        private static final String DATE_FORMAT = "dd/MM";

        public GraphicalView getView(Context context, List<ActionRecordManager.DateBurnratePair> results) {
            String title = context.getString(R.string.chartTitle);

            int[] colors = new int[]{getResources().getColor(R.color.doneColor), getResources().getColor(R.color.PeterRiver)};
            PointStyle[] styles = new PointStyle[]{PointStyle.CIRCLE, PointStyle.POINT};
            BasicStroke[] strokes = new BasicStroke[]{BasicStroke.SOLID, BasicStroke.DASHED};
            XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles, strokes);

            double avg = 0;
            if (!results.isEmpty()) {
                ActionRecordManager.DateBurnratePair max = results.get(0);
                ActionRecordManager.DateBurnratePair min = results.get(0);

                double sum = 0;
                for (ActionRecordManager.DateBurnratePair p : results) {
                    if (p.mBurnrate > max.mBurnrate)
                        max = p;
                    if (p.mBurnrate < min.mBurnrate)
                        min = p;

                    sum += p.mBurnrate;
                }

                avg = sum / results.size();

                setChartSettings(renderer, context.getString(R.string.chartTitle),
                        context.getString(R.string.burndown_chart_datetitle),
                        context.getString(R.string.burndown_chart_counttitle), results.get(0).mDate.getTime(), results.get(results.size() - 1)
                                .mDate.getTime(), min.mBurnrate, max.mBurnrate, Color.GRAY, Color.LTGRAY);
            }
            renderer.setXLabels(7);
            renderer.setYLabels(5);

            SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(0);
            seriesRenderer.setDisplayChartValues(false);
            seriesRenderer.setShowLegendItem(false);

            SimpleSeriesRenderer avgSeriesRenderer = renderer.getSeriesRendererAt(1);
            avgSeriesRenderer.setDisplayChartValues(false);
            avgSeriesRenderer.setShowLegendItem(false);

            return ChartFactory.getTimeChartView(context,
                    buildDateDataset(title, results, avg), renderer, DATE_FORMAT);
        }

        protected void setChartSettings(XYMultipleSeriesRenderer renderer,
                                        String title, String xTitle, String yTitle, double xMin,
                                        double xMax, double yMin, double yMax, int axesColor,
                                        int labelsColor) {
            renderer.setChartTitle(title);
//            renderer.setXTitle(xTitle);
//            renderer.setYTitle(yTitle);

            renderer.setXAxisMin(xMin - 12 * 3600 * 1000);
            renderer.setXAxisMax(xMax + 12 * 3600 * 1000);
            renderer.setYAxisMin(yMin - 1);
            renderer.setYAxisMax(yMax + 3);
            renderer.setAxesColor(axesColor);
            renderer.setLabelsColor(labelsColor);
            renderer.setZoomEnabled(false, false);
            renderer.setPanEnabled(false, false);
        }

        protected XYMultipleSeriesRenderer buildRenderer(int[] colors,
                                                         PointStyle[] styles,
                                                         BasicStroke[] strokes) {
            XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
            setRendererProperties(renderer, colors, styles, strokes);
            return renderer;
        }

        protected XYMultipleSeriesDataset buildDateDataset(String title,
                                                           List<ActionRecordManager.DateBurnratePair> results,
                                                           double avg) {
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            TimeSeries series = new TimeSeries(title);
            for (ActionRecordManager.DateBurnratePair result : results) {
                series.add(result.mDate, result.mBurnrate);
            }
            dataset.addSeries(series);

            if (avg > 0) {
                TimeSeries avgSeries = new TimeSeries("Average");
                avgSeries.add(results.get(0).mDate, avg);
                avgSeries.add(results.get(results.size() - 1).mDate, avg);
                dataset.addSeries(avgSeries);
            }
            return dataset;
        }

        protected void setRendererProperties(XYMultipleSeriesRenderer renderer, int[] colors,
                                             PointStyle[] styles, BasicStroke[] strokes) {
            renderer.setAxisTitleTextSize(25);
            renderer.setChartTitleTextSize(60);
            renderer.setLabelsTextSize(25);
            renderer.setLegendTextSize(25);
            renderer.setPointSize(5f);
            renderer.setMargins(new int[]{20, 10, 15, 15});
            renderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
            int length = colors.length;
            for (int i = 0; i < length; i++) {
                XYSeriesRenderer r = new XYSeriesRenderer();
                r.setLineWidth(5f);
                r.setPointStrokeWidth(25f);
                r.setColor(colors[i]);
                r.setPointStyle(styles[i]);
                r.setStroke(strokes[i]);
                renderer.addSeriesRenderer(r);
            }
        }
    }


}
