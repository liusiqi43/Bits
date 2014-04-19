package com.siqi.bits.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;
import com.siqi.bits.swipelistview.BaseSwipeListViewListener;
import com.siqi.bits.swipelistview.SwipeListView;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.io.InputStream;
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
    BitListArrayAdapter mAdapter;
    AnimateDismissAdapter mAnimateDismissAdapter;

    private OnBitListInteractionListener mListener;

    public static BitsListFragment newInstance() {
        BitsListFragment fragment = new BitsListFragment();
        return fragment;
    }

    public BitsListFragment() {
    }

    private class OnBitDismissCallback implements OnDismissCallback {

        @Override
        public void onDismiss(final AbsListView listView, final int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                mAdapter.remove(mAdapter.getItem(position));
            }
        }
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

        mAdapter = new BitListArrayAdapter(getActivity().getApplicationContext(), tm.getAllSortedTasks());
        mAnimateDismissAdapter = new AnimateDismissAdapter(mAdapter, new OnBitDismissCallback());

        // Swing from bottom anim & dismiss anim
        SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mAnimateDismissAdapter);
        swingBottomInAnimationAdapter.setInitialDelayMillis(200);
        swingBottomInAnimationAdapter.setAnimationDurationMillis(400);
        swingBottomInAnimationAdapter.setAbsListView(mBitsListView);
        this.mBitsListView.setAdapter(swingBottomInAnimationAdapter);


        this.mBitsListView.setSwipeListViewListener(new BaseSwipeListViewListener(){
            @Override
            public void onLeftChoiceAction(int position) {
                if (mAdapter.isExpanded(position))
                    mAdapter.toggle(position);
                Task item = mAdapter.getItem(position);
                item.incrementSkipCount();
                tm.updateTask(item);
                // Implicitly calls datasetChanged() method
                mAdapter.remove(item);
                mAdapter.add(item);
            }

            @Override
            public void onRightChoiceAction(int position) {
                if (mAdapter.isExpanded(position))
                    mAdapter.toggle(position);
                Task item = mAdapter.getItem(position);
                item.incrementDoneCount();
                tm.updateTask(item);
                // Implicitly calls datasetChanged() method
                mAdapter.remove(item);
                mAdapter.add(item);
            }

        });


        mBitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.card_viewswitcher);
                viewSwitcher.showNext();

                    new Handler().postDelayed(new Runnable() { public void run() {
                        viewSwitcher.setDisplayedChild(0);
                    }
                    }, getResources().getInteger(R.integer.actionview_timeout));

                return true;
            }
        });

        mAdapter.setLimit(1);

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

    private class BitListArrayAdapter extends ExpandableListItemAdapter<Task> {

        List<Task> mItems;
        PrettyTime prettyTime = new PrettyTime();
        private final LruCache<String, Bitmap> mMemoryCache;


        public BitListArrayAdapter(Context ctx, List<Task> t) {
            super(ctx, R.layout.card_main_layout, R.id.expandable_list_item_card_title, R.id.expandable_list_item_card_content, t);
            this.setActionViewResId(R.id.front);
            final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
                }
            };

            mItems = t;
        }

        @Override
        public View getTitleView(final int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getTitleVlew:"+position);
            View v = convertView;
            BitTitleHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.card_title_layout, parent, false);

                holder = new BitTitleHolder();

                holder.icon = (ImageView) v.findViewById(R.id.taskIcon);
                holder.title = (TextView) v.findViewById(R.id.taskTitle);
                holder.timeAgo = (TextView) v.findViewById(R.id.timeAgo);
                holder.progressBar = (ProgressBar) v.findViewById(R.id.timeAgoProgressBar);
                holder.doneButton = (Button) v.findViewById(R.id.done_button);
                holder.skipButton = (Button) v.findViewById(R.id.skip_button);
                holder.editButton = (Button) v.findViewById(R.id.edit_button);
                holder.deleteButton = (Button) v.findViewById(R.id.delete_button);
                holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);

                v.setTag(holder);
            } else {
                holder = (BitTitleHolder) v.getTag();
            }

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Task item = mAdapter.getItem(position);
                    item.setDeletedOn(new Date());
                    tm.updateTask(item);
                    // Implicitly calls datasetChanged() method

                    mAnimateDismissAdapter.animateDismiss(position);
                }
            });

            holder.editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("EditButton", "Clicked: " + position);
                }
            });

            final Task t = getItem(position);


//            holder.iconFrameLayout.getHolder().addCallback(new SurfaceHolder.Callback() {
//                @Override
//                public void surfaceCreated(SurfaceHolder surfaceHolder) {
//                    // Do some drawing when surface is ready
//                    Canvas canvas = surfaceHolder.lockCanvas();
//                    SVG svg = null;
//                    try {
//                        svg = SVG.getFromAsset(getActivity().getAssets(), t.getCategory().getIconDrawableName());
//                    } catch (SVGParseException e){
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    if (svg != null)
//                        canvas.drawPicture(svg.renderToPicture(60, 60));
//
//                    surfaceHolder.unlockCanvasAndPost(canvas);
//                }
//
//                @Override
//                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
//
//                }
//
//                @Override
//                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//
//                }
//            });




//            SVGImageView svgImageView = new SVGImageView(getActivity());
//            svgImageView.setImageAsset(t.getCategory().getIconDrawableName());
//
//            holder.iconFrameLayout.removeAllViews();
//            holder.iconFrameLayout.addView(svgImageView,
//                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            Bitmap bitmap = mMemoryCache.get(t.getCategory().getIconDrawableName());
            if (bitmap == null) {
                InputStream is = null;
                try {
                    is = getActivity().getAssets().open(t.getCategory().getIconDrawableName());
                    bitmap = BitmapFactory.decodeStream(is);
                    // Invert bitmap color
                    bitmap = CategoryManager.invertImage(bitmap);
                    if (bitmap != null)
                        mMemoryCache.put(t.getCategory().getIconDrawableName(), bitmap);
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
            }

            holder.icon.setMaxWidth(holder.icon.getHeight());
            holder.icon.setImageBitmap(bitmap);
            holder.title.setText(t.getDescription());
            holder.timeAgo.setText(t.getTimesAgoDescription(getString(R.string.done), getString(R.string.added_recently), prettyTime));
            holder.progressBar.setProgress(t.getProgress());

            holder.viewSwitcher.setInAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left));
            holder.viewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right));

//            if (progress > 100) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Pomegranate));
//            } else {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Emerald));
//            }
//            else if (progress > 90) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Alizarin));
//            }
//            else if (progress > 80) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Pumpkin));
//            } else if (progress > 70) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Carrot));
//            } else if (progress > 60) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Orange));
//            } else if (progress > 50) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Sunflower));
//            } else if (progress > 40) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.BelizeHole));
//            } else if (progress > 30) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.PeterRiver));
//            } else if (progress > 20) {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Nephritis));
//            } else {
//                holder.progressBar.setProgressDrawable(getResources().getDrawable(R.color.Emerald));
//            }
            return v;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getVlew:"+position);
            View v = super.getView(position, convertView, parent);

            ((SwipeListView) parent).recycle(v, position);

            return v;
        }

        @Override
        public View getContentView(int position, View convertView, ViewGroup parent) {
            Log.d("TEST", "getContentVlew:"+position);
            View v = convertView;
            BitContentHolder holder;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.card_content_layout, parent, false);

                holder = new BitContentHolder();

                holder.bitDoneRate = (TextView) v.findViewById(R.id.bit_done_rate);
                holder.othersDoneRate = (TextView) v.findViewById(R.id.others_done_rate);
                holder.timeLine = (GridView) v.findViewById(R.id.timeline_gridview);

                v.setTag(holder);
            } else {
                holder = (BitContentHolder) v.getTag();
            }

            Task t = mAdapter.getItem(position);

            int thisDoneRate = tm.getDoneRate(t);
            int othersDoneRate = tm.getDoneRateExcept(t);

            if (thisDoneRate > othersDoneRate) {
                holder.bitDoneRate.setTextColor(getResources().getColor(R.color.doneColor));
                holder.othersDoneRate.setTextColor(getResources().getColor(R.color.lateColor));
            } else if (thisDoneRate < othersDoneRate) {
                holder.bitDoneRate.setTextColor(getResources().getColor(R.color.lateColor));
                holder.othersDoneRate.setTextColor(getResources().getColor(R.color.doneColor));
            } else {
                holder.bitDoneRate.setTextColor(getResources().getColor(R.color.noneColor));
                holder.othersDoneRate.setTextColor(getResources().getColor(R.color.noneColor));
            }

            holder.bitDoneRate.setText(tm.getDoneRate(t) + " %");
            holder.othersDoneRate.setText(tm.getDoneRateExcept(t) + " %");
            TimeLineAdapter adapter = new TimeLineAdapter(getActivity(), t.getHistoryAsCharArray(getResources().getInteger(R.integer.timeline_rect_per_row)));
            holder.timeLine.setAdapter(adapter);
            return v;
        }

    }


    public interface OnBitListInteractionListener {
        public void startEditBitFragment(Long id);
    }


    private static class BitTitleHolder {
        ImageView icon;
        TextView title;
        TextView timeAgo;
        ProgressBar progressBar;
        Button skipButton, doneButton, editButton, deleteButton;
        ViewSwitcher viewSwitcher;
    }

    private static class BitContentHolder {
        TextView bitDoneRate;
        TextView othersDoneRate;
        GridView timeLine;
    }


    private class TimeLineAdapter extends ArrayAdapter<Character> {
        List<Character> mItems;

        public TimeLineAdapter(Context ctx, List<Character> t) {
            super(ctx, R.layout.timeline_girdview_item, t);
            mItems = t;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater li = getActivity().getLayoutInflater();
                v = li.inflate(R.layout.timeline_girdview_item, parent, false);
            }

            FrameLayout item = (FrameLayout) v.findViewById(R.id.timeline_item);

            switch (mItems.get(position)) {
                case 'l':
                    item.setBackgroundColor(getResources().getColor(R.color.lateColor));
                    break;
                case 'd':
                    item.setBackgroundColor(getResources().getColor(R.color.doneColor));
                    break;
                case 's':
                    item.setBackgroundColor(getResources().getColor(R.color.skipColor));
                    break;
                case 'n':
                    item.setBackgroundColor(getResources().getColor(R.color.noneColor));
                    break;
            }

            v.setTag(mItems.get(position));
            return v;
        }
    }
}
