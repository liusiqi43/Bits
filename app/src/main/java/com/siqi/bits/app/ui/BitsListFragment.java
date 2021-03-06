package com.siqi.bits.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.siqi.bits.ActionRecord;
import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;
import com.siqi.bits.app.ui.swipelistview.BaseSwipeListViewListener;
import com.siqi.bits.app.ui.swipelistview.SwipeListView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import interfaces.InfoUpdateCallback;
import managers.TaskManager;
import service.ReminderScheduleService;
import utils.ShakeEventListener;
import utils.Utils;

/**
 * Proudly powered by me on 4/8/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */
public class BitsListFragment extends Fragment implements ShakeEventListener.OnShakeListener {
  public static final int FRAGMENT_ID = 0;

  public static final int CARD_INFO = 0;
  public static final int CARD_ACTION = 1;

  private static final int REFRESH_PERIOD = 60 * 1000;
  private static final int FREEMIUM_TASK_COUNT_LIMIT = 5;

  private static String TAG = "BitsListFragment";
  SwipeListView mBitsListView;
  BitListArrayAdapter mAdapter;
  AnimateDismissAdapter mAnimateDismissAdapter;
  // Reordering animation
  HashMap<Long, Integer> mSavedState = new HashMap<Long, Integer>();
  Interpolator mInterpolator = new DecelerateInterpolator();
  TaskManager tm;
  SensorManager mSensorManager;
  ShakeEventListener mSensorListener;
  boolean mUndoDialogDisplayed = false;
  ReminderScheduleService mScheduleService = null;
  ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.v(TAG, "ReminderScheduleService onServiceConnected");
      mScheduleService = ((ReminderScheduleService.LocalBinder) service).getService();
      tm.setScheduleService(mScheduleService);
    }

    public void onServiceDisconnected(ComponentName className) {
      Log.v(TAG, "ReminderScheduleService onServiceDisconnected");
      mScheduleService = null;
      tm.setScheduleService(null);
    }
  };
  TextView popupTextView;
  boolean mIsBound = false;
  Runnable mListReloader;
  Handler mListRefresherHandle = new Handler();
  Handler mBannerTextResetHandle = new Handler();
  private boolean mNeedRearrange = false;
  private MediaPlayer mTaskFinishNotificaiton;
  private TextSwitcher mBanner;
  private SharedPreferences mPreferences;
  private AudioManager mAudioManager;
  private boolean mNeedFinishNotification = false;

  public BitsListFragment() {
  }

  public static BitsListFragment newInstance() {
    BitsListFragment fragment = new BitsListFragment();
    return fragment;
  }

  void doBindService() {
    // Establish a connection with the service.  We use an explicit
    // class name because we want a specific service implementation that
    // we know will be running in our own process (and thus won't be
    // supporting component replacement by other applications).
    getActivity().bindService(new Intent(getActivity(),
        ReminderScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }

  void doUnbindService() {
    if (mIsBound) {
      // Detach our existing connection.
      getActivity().unbindService(mConnection);
      mIsBound = false;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    setHasOptionsMenu(true);

    if (mPreferences.getBoolean(Utils.IS_AUTO_ROTATE_ENABLED, false)) {
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    } else {
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * View Binding
     */
    View rootView = inflater.inflate(R.layout.bitslist_fragment, container, false);
    mBitsListView = (SwipeListView) rootView.findViewById(R.id.bitslist);
    mBanner = (TextSwitcher) rootView.findViewById(R.id.bitslist_banner);

    // Set the ViewFactory of the TextSwitcher that will create TextView object when asked
    mBanner.setFactory(new ViewSwitcher.ViewFactory() {

      public View makeView() {
        TextView bannerTextView = new TextView(getActivity());
        bannerTextView.setGravity(Gravity.CENTER);
        bannerTextView.setTextSize(20);
        bannerTextView.setTypeface(null, Typeface.BOLD);
        bannerTextView.setTextColor(Color.WHITE);
        bannerTextView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        return bannerTextView;
      }

    });

    mBanner.setText(getString(R.string.default_banner_text));

    // Declare the in and out animations and initialize them
    Animation in = AnimationUtils.loadAnimation(getActivity(), R.anim.card_flip_top_in);
    Animation out = AnimationUtils.loadAnimation(getActivity(), R.anim.card_flip_bottom_out);
    mBanner.setInAnimation(in);
    mBanner.setOutAnimation(out);

    mBitsListView.setAdapter(mAnimateDismissAdapter);

    mAnimateDismissAdapter.setAbsListView(mBitsListView);
    // Set the maximum number of items allowed to be expanded.
    mAdapter.setLimit(1);


    mBitsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int position,
                                     long id) {
        mBitsListView.setSwipeMode(SwipeListView.SWIPE_MODE_NONE);
        final ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.card_viewswitcher);
        viewSwitcher.showNext();

        new Handler().postDelayed(new Runnable() {
          public void run() {
            if (viewSwitcher.getDisplayedChild() == CARD_ACTION) {
              viewSwitcher.setDisplayedChild(CARD_INFO);
            }
            mBitsListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
          }
        }, getResources().getInteger(R.integer.actionview_timeout));

        if (mPreferences.getBoolean(Utils.IS_BITSLIST_LONGPRESS_HELP_ON, true)) {
          AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

          builder.setView(getActivity().getLayoutInflater().inflate(R.layout.help_longclick,
              null, false));

          builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.cancel();
            }
          });

          builder.show();
          mPreferences.edit().putBoolean(Utils.IS_BITSLIST_LONGPRESS_HELP_ON, false).commit();
        }

        return true;
      }
    });

    mBitsListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
      @Override
      public void onMove(int position, float x) {
        if (mAdapter.isExpanded(position))
          mAdapter.toggle(position);
      }

      @Override
      public void onLeftChoiceAction(int position) {
        if (mAdapter.isExpanded(position))
          mAdapter.toggle(position);
        Task item = mAdapter.getItem(position);
        tm.setSkipActionForTask(item);

        updateBanner(getResources().getColor(R.color.Orange), getString(R.string.skipped), 3);
        mNeedRearrange = true;
        mNeedFinishNotification = false;

        // loading interstitioal Ads
        if (mPreferences.getBoolean(Utils.IS_BITS_ADS_SUPPORT_ENABLED, false))
          Utils.displayInterstitial();
      }

      @Override
      public void onRightChoiceAction(int position) {
        if (mAdapter.isExpanded(position))
          mAdapter.toggle(position);

        Task item = mAdapter.getItem(position);
        tm.setDoneActionForTask(item);

        mNeedRearrange = true;
        mNeedFinishNotification = true;

        Utils.BitsAsyncUpload(item, new InfoUpdateCallback() {
          @Override
          public void onBitsCountUpdate(int count) {
            if (count - mPreferences.getInt(Utils.LAST_GLOBAL_COUNT, 0) > 50) {
              String counterText = String.format(getString(R.string.bits_done_around_the_word),
                  10 * (count / 10));
              updateBanner(getResources().getColor(R.color.SeaGreen), counterText, 8);
              mPreferences.edit().putInt(Utils.LAST_GLOBAL_COUNT, count).commit();
            } else {
              updateBanner(getResources().getColor(R.color.Emerald),
                  tm.getDoneSlogan());
            }
          }

          @Override
          public void onPostFailed(String json) {
            // If failed, store the pending json to backlog.
            Set<String> set = mPreferences.getStringSet(Utils.FAILED_POST_BACKLOG,
                new HashSet<String>());
            if (set.size() > 20)
              set.clear();
            set.add(json);
            mPreferences.edit().putStringSet(Utils.FAILED_POST_BACKLOG, set).commit();

            updateBanner(getResources().getColor(R.color.Emerald),
                tm.getDoneSlogan());
          }
        });
      }

      @Override
      public void onGeneratedAnimationFinished() {
        Log.d(TAG, "onGeneratedAnimationFinished");
        if (mNeedRearrange) {
          new RearrangeTasks().execute();
          if (mNeedFinishNotification) {
            if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL
                && mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0)
              mTaskFinishNotificaiton.start();
            mNeedFinishNotification = false;
          }
          mNeedRearrange = false;
        }
      }

    });

    mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    mSensorListener = new ShakeEventListener();

    mSensorListener.setOnShakeListener(BitsListFragment.this);

    mListReloader = new Runnable() {
      @Override
      public void run() {
        new ReloadSortedTasks().execute();
        mListRefresherHandle.postDelayed(mListReloader, REFRESH_PERIOD);
      }
    };

    mSensorManager.registerListener(mSensorListener,
        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        SensorManager.SENSOR_DELAY_UI);

    startPeriodicRefresh();
    return rootView;
  }

  private void updateBanner(int color, CharSequence bannerText) {
    updateBanner(color, bannerText, 3);
  }

  private void updateBanner(int color, CharSequence bannerText, int delay) {
    ObjectAnimator anim = ObjectAnimator.ofInt(mBanner, "backgroundColor",
        getActivity().getResources().getColor(R.color.Turquoise));
    anim.setEvaluator(new ArgbEvaluator());
    anim.setInterpolator(new AccelerateInterpolator());
    anim.setDuration(1500);
    anim.setIntValues(color, getActivity().getResources().getColor(R.color.Turquoise));
    mBanner.setText(bannerText);
    anim.start();
    mBannerTextResetHandle.removeCallbacksAndMessages(null);
    mBannerTextResetHandle.postDelayed(new Runnable() {
      @Override
      public void run() {
        mBanner.setText(getString(R.string.default_banner_text));
      }
    }, delay * 1000);
  }


  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    /**
     * Data Loading
     */
    setRetainInstance(true);
    tm = TaskManager.getInstance(this.getActivity());

    mAdapter = new BitListArrayAdapter(getActivity(), new ArrayList<Task>());
    mAnimateDismissAdapter = new AnimateDismissAdapter(mAdapter, new OnBitDismissCallback());
    mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mAudioManager = (AudioManager) getActivity().getSystemService(getActivity().AUDIO_SERVICE);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    ((MainActivity) activity).onSectionAttached(FRAGMENT_ID);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mSensorManager != null) {
      mSensorManager.registerListener(mSensorListener,
          mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
          SensorManager.SENSOR_DELAY_UI);
    }
    /**
     * Service binding
     */
    doBindService();

    /**
     * UI refresher start
     */
    if (mListReloader != null) {
      startPeriodicRefresh();
    }

    mBanner.setText(getString(R.string.default_banner_text));

    mTaskFinishNotificaiton = MediaPlayer.create(getActivity(), R.raw.chance_stage);

    if (mPreferences.getBoolean(Utils.IS_BITS_ADS_SUPPORT_ENABLED, false)) {
      // Look up the AdView as a resource and load a request.
      final AdView adView = (AdView) getActivity().findViewById(R.id.adView);
      adView.setAdListener(new AdListener() {
        @Override
        public void onAdLoaded() {
          super.onAdLoaded();
          adView.setVisibility(View.VISIBLE);
        }
      });
      AdRequest adRequest = new AdRequest.Builder().build();
      adView.loadAd(adRequest);

      // loading interstitioal Ads
      Utils.loadInterstitialAds(getActivity());
    }
  }

  private void startPeriodicRefresh() {
    mListReloader.run();
  }

  private void stopPeriodicRefresh() {
    Log.v("periodic", "Refresh stops now");
    mListRefresherHandle.removeCallbacks(mListReloader);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (mSensorManager != null) {
      mSensorManager.unregisterListener(mSensorListener);
    }
    /**
     * Service Unbinding
     */
    doUnbindService();

    /**
     * UI refresher stops here
     */
    stopPeriodicRefresh();

    /**
     * Stop banner from resetting
     */
    mBannerTextResetHandle.removeCallbacksAndMessages(null);
    mTaskFinishNotificaiton.release();
  }

  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    this.getActivity().getMenuInflater().inflate(R.menu.bitlist, menu);

    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if (item.getItemId() == R.id.action_new) {
      if (mAdapter.getItemCount() < FREEMIUM_TASK_COUNT_LIMIT || mPreferences.getBoolean(Utils
          .TASKS_COUNT_LIMIT_UNLOCKED, false)) {
        Intent intent = new Intent(getActivity(), NewBitActivity.class);
        getActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
        startActivity(intent);
        return true;
      } else {
        Intent intent = new Intent(getActivity(), InAppPurchaseActivity.class);
        startActivity(intent);
      }
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onShake() {
    if (!mPreferences.getBoolean(Utils.IS_BITSLIST_SHAKE_ON, true) || !mPreferences.getBoolean
        (Utils.REWARD_UNDO_ON_SHAKE_ENABLED, false))
      return;

    if (mUndoDialogDisplayed)
      return;

    final ActionRecord record = tm.getLastActiveActionForActiveTask();

    if (record != null) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

      StringBuilder msgBulder = new StringBuilder()
          .append(getString(R.string.last_record_seems_to_be))
          .append(record.getAction() == TaskManager.ACTION_TYPE_DONE ?
              " <font color='#2ecc71'>" + getString(R.string.done) + "</font>"
              : " <font color='#3498db'>" + getString(R.string.skip) + "</font>")
          .append(" ")
          .append(getString(R.string.on_task))
          .append(" <b>")
          .append(record.getTask().getDescription())
          .append("</b>");

      TextView tv = new TextView(getActivity());
      int padInPx = Utils.dpToPx(15);
      tv.setPadding(padInPx, padInPx, padInPx, padInPx);
      tv.setTextAppearance(getActivity(), android.R.style.TextAppearance_Holo_Large);
      tv.setText(Html.fromHtml(msgBulder.toString()), TextView.BufferType.SPANNABLE);
      builder.setView(tv);
      builder.setTitle(getString(R.string.do_you_want_to_undo_it));

      builder.setPositiveButton(R.string.yes_please, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          tm.removeActionRecordById(record.getId());
          new ReloadSortedTasks().execute();
          dialog.cancel();
          mUndoDialogDisplayed = false;
        }
      });

      builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
          mUndoDialogDisplayed = false;
        }
      });

      builder.show();
      mUndoDialogDisplayed = true;
    }
  }

  private void saveState() {
    mSavedState.clear();
    int first = mBitsListView.getFirstVisiblePosition();
    int last = mBitsListView.getLastVisiblePosition();
    for (int i = 0; i < mAdapter.size(); i++) {
      if (i >= first && i <= last) {
        View v = mBitsListView.getChildAt(i - first);
        if (v == null)
          continue;
        int top = v.getTop();
        long dataId = mAdapter.getItem(i).getId();
        mSavedState.put(dataId, top);
      } else if (i < first) {
        int top = mBitsListView.getTop() - mBitsListView.getHeight() / 2;
        long dataId = mAdapter.getItem(i).getId();
        mSavedState.put(dataId, top);
      } else if (i > last) {
        int top = mBitsListView.getBottom() + mBitsListView.getHeight() / 2;
        long dataId = mAdapter.getItem(i).getId();
        mSavedState.put(dataId, top);
      }
    }
    for (int i = 0; i < mBitsListView.getChildCount(); i++) {
      View v = mBitsListView.getChildAt(i);
      if (v == null)
        continue;
      int top = v.getTop();
      int dataIdx = first + i;
      if (dataIdx >= mAdapter.size())
        break;
      long dataId = mAdapter.getItem(dataIdx).getId();
      mSavedState.put(dataId, top);
    }

    Iterator it = mSavedState.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Long, Integer> entry = (Map.Entry<Long, Integer>) it.next();
      Log.d("ANIMATION", "saving : " + tm.getTask(entry.getKey()).getDescription() + ":" + entry
          .getValue());
    }
  }

  private void displayTextualDialog(String title, String summary, String details) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    View v = getActivity().getLayoutInflater().inflate(R.layout.help_textual_dialog, null, false);

    TextView titleView = (TextView) v.findViewById(R.id.title);
    TextView subtitleView = (TextView) v.findViewById(R.id.subtitle);

    titleView.setText(summary);
    subtitleView.setText(details);

    builder.setView(v);
    builder.setTitle(title);

    builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });

    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        builder.show();
      }
    });
  }

  private void updateInstructions() {
    int doneCount = mPreferences.getInt(TaskManager.TOTAL_DONE_COUNT, 0);
    int skipCount = mPreferences.getInt(TaskManager.TOTAL_SKIP_COUNT, 0);
    int lateCount = mPreferences.getInt(TaskManager.TOTAL_LATE_COUNT, 0);
    int newTaskCount = mPreferences.getInt(TaskManager.TOTAL_TASK_ADDED, 0);

    if (newTaskCount > 0 && mPreferences.getBoolean(Utils.IS_FIRST_TASK_ADDED, true)) {
      displayTextualDialog(getString(R.string.first_new_task_title),
          getString(R.string.first_new_task_summary), getString(R.string.first_new_task_details));
      mPreferences.edit().putBoolean(Utils.IS_FIRST_TASK_ADDED, false).commit();
    }

    if (doneCount > 0 && mPreferences.getBoolean(Utils.IS_FIRST_DONE, true)) {
      displayTextualDialog(getString(R.string.first_done), getString(R.string.first_done_summary)
          , getString(R.string.first_done_details));
      mPreferences.edit().putBoolean(Utils.IS_FIRST_DONE, false).commit();
    }

    if (skipCount > 0 && mPreferences.getBoolean(Utils.IS_FIRST_SKIP, true)) {
      displayTextualDialog(getString(R.string.first_skip), getString(R.string.first_skip_summary)
          , getString(R.string.first_skip_details));
      mPreferences.edit().putBoolean(Utils.IS_FIRST_SKIP, false).commit();
    }

    if (lateCount > 0 && mPreferences.getBoolean(Utils.IS_FIRST_LATE, true)) {
      displayTextualDialog(getString(R.string.first_late), getString(R.string.first_late_summary)
          , getString(R.string.first_late_details));
      mPreferences.edit().putBoolean(Utils.IS_FIRST_LATE, false).commit();
    }
  }

  private void updateRewards() {
    int doneCount = mPreferences.getInt(TaskManager.TOTAL_DONE_COUNT, 0);

    if (doneCount >= 3 && !mPreferences.getBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, false)) {
      displayTextualDialog(getString(R.string.reward_history_on_tap),
          getString(R.string.reward_history_on_tap_summary),
          getString(R.string.reward_history_on_tap_details));
      mPreferences.edit().putBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, true).commit();
    } else if (doneCount >= 10 && !mPreferences.getBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED,
        false)) {
      displayTextualDialog(getString(R.string.reward_bitslist_help_off),
          getString(R.string.reward_bitslist_help_off_summary),
          getString(R.string.reward_bitslist_help_off_details));
      mPreferences.edit().putBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, true).commit();
      mPreferences.edit().putBoolean(Utils.IS_BITSLIST_HELP_ON, false).commit();
    } else if (doneCount >= 30 && !mPreferences.getBoolean(Utils.REWARD_UNDO_ON_SHAKE_ENABLED,
        false)) {
      displayTextualDialog(getString(R.string.reward_undo_on_shake),
          getString(R.string.reward_undo_on_shake_summary),
          getString(R.string.reward_undo_on_shake_details));
      mPreferences.edit().putBoolean(Utils.REWARD_UNDO_ON_SHAKE_ENABLED, true).commit();
    }
  }

  private void asyncUpdateForFeedbacks() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        updateInstructions();
        updateRewards();
      }
    }).start();
  }


  private void animateNewState() {
    int first = mBitsListView.getFirstVisiblePosition();
    int last = mBitsListView.getLastVisiblePosition();
    for (int i = 0; i < mBitsListView.getChildCount(); i++) {
      int dataIdx = first + i;
      if (dataIdx >= mAdapter.size())
        break;
      long dataId = mAdapter.getItem(dataIdx).getId();
      if (mSavedState.containsKey(dataId)) {
        View v = mBitsListView.getChildAt(i);
        if (v == null)
          continue;
        int top = v.getTop();
        Log.d("ANIMATION", "loading : " + tm.getTask(dataId).getDescription() + ":" + top);
        int oldTop = mSavedState.get(dataId);
        int hDiff = top - oldTop;
        TranslateAnimation anim = new TranslateAnimation(0, 0, -hDiff, 0);
        Log.d("ANIMATION", "moving : " + tm.getTask(dataId).getDescription() + ":" + (-hDiff));
        anim.setInterpolator(mInterpolator);
        anim.setDuration(300);
        v.startAnimation(anim);
      }
    }
  }

  private static class BitTitleHolder {
    ImageView icon;
    TextView title;
    TextView timeAgo;
    Button skipButton, doneButton;
    ImageButton editButton, deleteButton, achieveButton;
    ViewSwitcher viewSwitcher;
  }

  private static class BitContentHolder {
    TextView bitDoneRate;
    TextView othersDoneRate;
    GridView timeLine;
    LinearLayout globalLayout;
  }

  private class ReloadSortedTasks extends AsyncTask<Void, Integer, List<Task>> {

    @Override
    protected List<Task> doInBackground(Void... voids) {
      saveState();
      return tm.getAllSortedTasks();
    }

    @Override
    protected void onPostExecute(List<Task> l) {
      mAdapter.clear();
      mAdapter.addAll(l);
      animateNewState();
      asyncUpdateForFeedbacks();
    }
  }

  private class RearrangeTasks extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... voids) {
      Log.d("ListView anim", "Reloading now");
      saveState();
      mAdapter.sort();
      return null;
    }

    // This is called when doInBackground() is finished
    protected void onPostExecute(Void v) {
      mAdapter.notifyDataSetChanged();

      animateNewState();
      asyncUpdateForFeedbacks();
      Utils.requestBackup(getActivity());
    }
  }

  private class OnBitDismissCallback implements OnDismissCallback {

    @Override
    public void onDismiss(final AbsListView listView, final int[] reverseSortedPositions) {
      for (int position : reverseSortedPositions) {
        mAdapter.remove(mAdapter.getItem(position));
      }
    }
  }

  public class BitListArrayAdapter extends ExpandableListItemAdapter<Task> {

    public static final int ITEM_TYPE_HELP = 1;
    public static final int ITEM_TYPE_TASK = 0;
    private static final int EXTRA_ITEMS_COUNT = 1;
    private final LruCache<String, Bitmap> mMemoryCache;
    List<Task> mItems;


    public BitListArrayAdapter(Context ctx, List<Task> t) {
      super(ctx, R.layout.card_main_layout, R.id.expandable_list_item_card_title,
          R.id.expandable_list_item_card_content, t);
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

    public int getItemCount() {
      return mItems.size();
    }

    @Override
    public int getCount() {
      return mPreferences.getBoolean(Utils.IS_BITSLIST_HELP_ON, true) || mItems.size() == 0 ?
          mItems.size() + EXTRA_ITEMS_COUNT : mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
      return position >= mItems.size() ? ITEM_TYPE_HELP : ITEM_TYPE_TASK;
    }

    @Override
    public int getViewTypeCount() {
      return EXTRA_ITEMS_COUNT + 1;
    }

    @Override
    public View getTitleView(final int position, View convertView, ViewGroup parent) {
      Log.v(TAG, "getTitleVlew for task with description " + mItems.get(position).getDescription());
      View v = convertView;
      final BitTitleHolder holder;

      if (v == null) {
        LayoutInflater li = getActivity().getLayoutInflater();
        v = li.inflate(R.layout.card_title_layout, parent, false);

        holder = new BitTitleHolder();

        holder.icon = (ImageView) v.findViewById(R.id.taskIcon);
        holder.title = (TextView) v.findViewById(R.id.taskTitle);
        holder.timeAgo = (TextView) v.findViewById(R.id.taskSubtitle);
        holder.doneButton = (Button) v.findViewById(R.id.done_button);
        holder.skipButton = (Button) v.findViewById(R.id.skip_button);
        holder.editButton = (ImageButton) v.findViewById(R.id.edit_button);
        holder.achieveButton = (ImageButton) v.findViewById(R.id.achieve_button);
        holder.deleteButton = (ImageButton) v.findViewById(R.id.delete_button);
        holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);

        v.setTag(holder);
      } else {
        holder = (BitTitleHolder) v.getTag();
        holder.viewSwitcher = (ViewSwitcher) v.findViewById(R.id.card_viewswitcher);
        if (holder.viewSwitcher.getDisplayedChild() == CARD_ACTION) {
          holder.viewSwitcher.setInAnimation(null);
          holder.viewSwitcher.setOutAnimation(null);
          holder.viewSwitcher.setDisplayedChild(CARD_INFO);

          Animation inAnimation = AnimationUtils.loadAnimation(getActivity(),
              android.R.anim.slide_in_left);
          inAnimation.setDuration(300);
          Animation outAnimation = AnimationUtils.loadAnimation(getActivity(),
              android.R.anim.slide_out_right);
          outAnimation.setDuration(300);

          holder.viewSwitcher.setInAnimation(inAnimation);
          holder.viewSwitcher.setOutAnimation(outAnimation);
        }
      }

      holder.deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          holder.viewSwitcher.setDisplayedChild(CARD_INFO);
          Task item = mAdapter.getItem(position);
          item.setDeletedOn(new Date(Utils.currentTimeMillis()));
          tm.updateTask(item);
          if (mScheduleService != null)
            mScheduleService.unScheduleForTask(item);
          else
            Log.d("ReminderScheduleService", "mScheduleService == null, " +
                "deleted item still scheduled");
          // Implicitly calls datasetChanged() method

          mAnimateDismissAdapter.animateDismiss(position);
        }
      });

      holder.achieveButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          holder.viewSwitcher.setDisplayedChild(CARD_INFO);
          Task item = mAdapter.getItem(position);
          item.setArchieved_on(new Date(Utils.currentTimeMillis()));
          tm.updateTask(item);
          if (mScheduleService != null)
            mScheduleService.unScheduleForTask(item);
          else
            Log.d("ReminderScheduleService", "mScheduleService == null, " +
                "deleted item still scheduled");
          // Implicitly calls datasetChanged() method

          mAnimateDismissAdapter.animateDismiss(position);
        }
      });

      holder.editButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = new Intent(getActivity(), NewBitActivity.class);
          intent.putExtra(NewBitActivity.EDITING_BIT_ID, mAdapter.getItem(position).getId());
          getActivity().overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_top);
          startActivity(intent);
        }
      });

      final Task t = getItem(position);

      Bitmap bitmap = mMemoryCache.get(t.getCategory().getIconDrawableName());
      if (bitmap == null) {
        try {
          bitmap = t.getCategory().getIconBitmap(getActivity());
          if (bitmap != null) {
            mMemoryCache.put(t.getCategory().getIconDrawableName(), bitmap);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      holder.icon.setImageBitmap(bitmap);
      holder.title.setText(t.getDescription());
      holder.timeAgo.setText(tm.getTimesAgoDescriptionForTask(t));

      // TODO(liusiqi) Use color instead of progress bar.
      int progress = tm.getProgressForTask(t);
      if (progress > 100) {
        // Only triggered when t.getFrequency() - actionCountSinceBeginOfInternval > 0,
        // and progress > 100, which means... well, late!
        tm.updateActionRecordForTask(t);
      }
      holder.icon.setBackgroundColor(progressToColor(progress));

      Animation inAnimation = AnimationUtils.loadAnimation(getActivity(),
          android.R.anim.slide_in_left);
      inAnimation.setDuration(300);
      Animation outAnimation = AnimationUtils.loadAnimation(getActivity(),
          android.R.anim.slide_out_right);
      outAnimation.setDuration(300);

      holder.viewSwitcher.setInAnimation(inAnimation);
      holder.viewSwitcher.setOutAnimation(outAnimation);

      Log.d("BitListFrag", t.getDescription() + " : " + t.getNextScheduledTime());

      return v;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = null;
      switch (getItemViewType(position)) {
        case ITEM_TYPE_TASK:
          v = super.getView(position, convertView, parent);
          ((SwipeListView) parent).recycle(v, position);
          break;
        case ITEM_TYPE_HELP:
          LayoutInflater li = getActivity().getLayoutInflater();
          if (mItems.size() == 0) {
            v = li.inflate(R.layout.help_new, parent, false);
          } else {
            v = li.inflate(R.layout.help_details, parent, false);
            if (mPreferences.getBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, false)) {
              v.findViewById(R.id.view_history).setVisibility(View.VISIBLE);
            }
          }
          break;
      }
      // Should not happen
      return v;
    }

    @Override
    public View getContentView(int position, View convertView, ViewGroup parent) {
      Log.d("TEST", "getContentVlew:" + position);
      Task t = mAdapter.getItem(position);

      View v = convertView;

      if (!mPreferences.getBoolean(Utils.REWARD_HISTORY_ON_TAP_ENABLED, false)) {
        if (v != null)
          v.setVisibility(View.GONE);
        return v;
      }

      BitContentHolder holder;

      if (v == null) {
        LayoutInflater li = getActivity().getLayoutInflater();
        v = li.inflate(R.layout.card_content_layout, parent, false);

        holder = new BitContentHolder();

        holder.bitDoneRate = (TextView) v.findViewById(R.id.bit_done_rate);
        holder.othersDoneRate = (TextView) v.findViewById(R.id.others_done_rate);
        holder.timeLine = (GridView) v.findViewById(R.id.timeline_gridview);
        holder.globalLayout = (LinearLayout) v.findViewById(R.id.card_content_global_layout);

        v.setTag(holder);
      } else {
        holder = (BitContentHolder) v.getTag();
      }

      if (t.getActionsRecords().isEmpty()) {
        holder.globalLayout.setVisibility(View.GONE);
      } else {
        holder.globalLayout.setVisibility(View.VISIBLE);
        int thisDoneRate = tm.getDoneRate(t);
        int othersDoneRate = tm.getDoneRateExcept(t);

        Log.d("THIS_DONE_RATE", t.getDescription() + ":" + thisDoneRate);

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


        TimeLineAdapter adapter = new TimeLineAdapter(getActivity(), t.getActionsRecords());
        holder.timeLine.setAdapter(adapter);

        holder.timeLine.setOnTouchListener(new View.OnTouchListener() {
          int highlightedItem;

          @Override
          public boolean onTouch(View v, MotionEvent event) {
            GridView gridView = (GridView) v;
            gridView.requestDisallowInterceptTouchEvent(true);

            switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
              case MotionEvent.ACTION_MOVE:
                int[] gridViewCoords = new int[2];
                gridView.getLocationOnScreen(gridViewCoords);

                Rect gridViewHitRect = new Rect();
                gridView.getHitRect(gridViewHitRect);
                if (!gridViewHitRect.contains(v.getLeft() + (int) event.getX(),
                    v.getTop() + (int) event.getY())) {
                  gridView.getChildAt(highlightedItem).findViewById(R.id.highlight).setVisibility
                      (View.GONE);
                  if (popupTextView != null)
                    popupTextView.setVisibility(View.GONE);
                  return false;
                }

                boolean hit = false;
                for (int i = 0; i < gridView.getChildCount(); ++i) {
                  Rect hitRect = new Rect();
                  View child = gridView.getChildAt(i);
                  child.getHitRect(hitRect);
                  if (hitRect.contains((int) event.getRawX() - gridViewCoords[0],
                      (int) event.getRawY() - gridViewCoords[1])) {
                    hit = true;
                    if (popupTextView == null) {
                      popupTextView = new TextView(getActivity());
                      popupTextView.setGravity(Gravity.CENTER);
                      popupTextView.setPadding(Utils.dpToPx(5), Utils.dpToPx(5), Utils.dpToPx(5),
                          Utils.dpToPx(5));
                      popupTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                      popupTextView.setTypeface(null, Typeface.BOLD);
                      popupTextView.setTextColor(getResources().getColor(R.color.white));
                      popupTextView.setVisibility(View.INVISIBLE);
                      RelativeLayout layout = (RelativeLayout) getActivity().getWindow()
                          .findViewById(R.id.bitlist_layout);
                      layout.addView(popupTextView);
                    }

                    child.findViewById(R.id.highlight).setVisibility(View.VISIBLE);
                    highlightedItem = i;

                    ActionRecord record = (ActionRecord) child.getTag();

                    String popUpText;
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd",
                        getResources().getConfiguration().locale);
                    String dateText = format.format(record.getRecordOn());
                    switch (record.getAction()) {
                      case TaskManager.ACTION_TYPE_DONE:
                        popupTextView.setBackgroundColor(getResources().getColor(R.color
                            .doneColor));
                        popUpText = "Done\n";
                        break;
                      case TaskManager.ACTION_TYPE_SKIP:
                        popupTextView.setBackgroundColor(getResources().getColor(R.color
                            .skipColor));
                        popUpText = "Skipped\n";
                        break;
                      case TaskManager.ACTION_TYPE_LATE:
                        popupTextView.setBackgroundColor(getResources().getColor(R.color
                            .lateColor));
                        popUpText = "Late\n";
                        break;
                      default:
                        popUpText = "";
                    }
                    popUpText += dateText;
                    int[] child_loc = new int[2];
                    child.getLocationOnScreen(child_loc);
                    popupTextView.setText(popUpText);
                    popupTextView.setX(child_loc[0] + child.getWidth() / 2 - popupTextView
                        .getWidth() / 2);
                    popupTextView.setY(child_loc[1] - 2 * popupTextView.getHeight() - Utils
                        .dpToPx(40));

                    popupTextView.setVisibility(View.VISIBLE);
                  } else {
                    child.findViewById(R.id.highlight).setVisibility(View.GONE);
                  }
                }

                if (!hit) {
                  gridView.getChildAt(highlightedItem).findViewById(R.id.highlight).setVisibility
                      (View.GONE);
                  if (popupTextView != null)
                    popupTextView.setVisibility(View.GONE);
                }
                return hit;
              default:
                gridView.getChildAt(highlightedItem).findViewById(R.id.highlight).setVisibility
                    (View.GONE);
                if (popupTextView != null)
                  popupTextView.setVisibility(View.GONE);
                return false;
            }
          }
        });
      }

      return v;
    }

    public void sort() {
      TaskManager.mBitsComparator.reset();
      Collections.sort(mItems, TaskManager.mBitsComparator);
    }

    @Override
    public void toggle(int position) {
      super.toggle(position);
    }

    private double bounded(double number) {
      return Math.min(Math.max(0., number), 100.);
    }

    private int progressToColor(int progress) {
      progress = (int) bounded(progress);
      Log.d("rgb for progress:" + progress, getRed(progress) + "," + getGreen(progress) + "," +
          "" + getBlue(progress));
      return Color.rgb(getRed(progress),
          getGreen(progress),
          getBlue(progress));
    }

    private int getRed(int progress) {
      return Math.min(22 + (int) Math.round(1.87f * Math.pow(progress / 10f, 2.7)), 255);
    }

    private int getGreen(int progress) {
      return Math.min(62 + (int) Math.round(1.7f * 10 * Math.sqrt(progress) - 144f * Math.pow
          (progress / 100f, 2)), 255);
    }

    private int getBlue(int progress) {
      return Math.min(80 + (int) Math.round(1.44f * 10 * Math.sqrt(progress) - 164f * Math.pow
          (progress / 100f, 2)), 255);
    }
  }

  private class TimeLineAdapter extends ArrayAdapter<ActionRecord> {
    List<ActionRecord> mItems;


    public TimeLineAdapter(Context ctx, List<ActionRecord> t) {
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

      switch (mItems.get(mItems.size() - position - 1).getAction()) {
        case TaskManager.ACTION_TYPE_LATE:
          item.setBackgroundColor(getResources().getColor(R.color.lateColor));
          break;
        case TaskManager.ACTION_TYPE_DONE:
          item.setBackgroundColor(getResources().getColor(R.color.doneColor));
          break;
        case TaskManager.ACTION_TYPE_SKIP:
          item.setBackgroundColor(getResources().getColor(R.color.skipColor));
          break;
        default:
          item.setBackgroundColor(getResources().getColor(R.color.noneColor));
          break;
      }

      v.setTag(mItems.get(mItems.size() - position - 1));

      return v;
    }
  }
}
