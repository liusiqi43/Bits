package model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.siqi.bits.ActionRecord;
import com.siqi.bits.ActionRecordDao;
import com.siqi.bits.BitsDevOpenHelper;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.Task;
import com.siqi.bits.TaskDao;
import com.siqi.bits.app.R;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.dao.query.QueryBuilder;
import service.ReminderScheduleService;

/**
 * Created by me on 4/9/14.
 */
public class TaskManager {
    public static final BiMap<String, Integer> PeriodStringToDays = HashBiMap.create();
    public static final ConcurrentHashMap<Long, Integer> PeriodToDays = new ConcurrentHashMap<Long, Integer>();
    public static final long DAY_IN_MILLIS = 24 * 3600 * 1000;
    public static final int ACTION_TYPE_DONE = 1;
    public static final int ACTION_TYPE_SKIP = 2;
    public static final int ACTION_TYPE_LATE = 3;
    public static final int ACTION_TYPE_ANY = 4;
    public static CachedComparator mBitsComparator;
    private static TaskManager INSTANCE = null;
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private TaskDao mTaskDao;
    private ActionRecordDao mActionRecordDao;
    private PrettyTime mPrettyTime;
    private Context mContext;
    private ArrayList<String> mDoneSlogans = new ArrayList<String>();
    private ArrayList<String> mSkipSlogans = new ArrayList<String>();
    private Random mRandomiser;
    private ReminderScheduleService mScheduleService = null;
    private Toast actionFinishedToast = null;

    private TaskManager(Context ctx) {
        /**
         * DB init
         */
        BitsDevOpenHelper helper = new BitsDevOpenHelper(
                ctx,
                "bits-db",
                null);
        mDB = helper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDB);
        mDaoSession = mDaoMaster.newSession();
        mTaskDao = mDaoSession.getTaskDao();
        mActionRecordDao = mDaoSession.getActionRecordDao();

        PeriodStringToDays.put(ctx.getString(R.string.radio_day), 1);
        PeriodStringToDays.put(ctx.getString(R.string.radio_week), 7);
        PeriodStringToDays.put(ctx.getString(R.string.radio_month), 30);
        PeriodStringToDays.put(ctx.getString(R.string.radio_year), 365);

        PeriodToDays.put((long) 1 * 24 * 60 * 60 * 1000, 1);
        PeriodToDays.put((long) 7 * 24 * 60 * 60 * 1000, 7);
        PeriodToDays.put((long) 30 * 24 * 60 * 60 * 1000, 30);
        PeriodToDays.put((long) 365 * 24 * 60 * 60 * 1000, 365);

        mDoneSlogans.add("Great Job!");
        mDoneSlogans.add("You Rock!");
        mDoneSlogans.add("Awesome!");
        mDoneSlogans.add("Incredible!");
        mDoneSlogans.add("Impressive!");
        mDoneSlogans.add("Bravo!");

        mSkipSlogans.add("Maybe next time!");
        mSkipSlogans.add("Get back to this soon!");
        mSkipSlogans.add("See you soon!");
        mSkipSlogans.add("You can do this!");

        mPrettyTime = new PrettyTime();
        mContext = ctx;
        mRandomiser = new Random();

        mBitsComparator = new CachedComparator();
    }

    public static TaskManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new TaskManager(ctx);
        return INSTANCE;
    }

    public void setScheduleService(ReminderScheduleService mScheduleService) {
        this.mScheduleService = mScheduleService;
    }

    public void insertTask(Task t) throws DuplicatedTaskException {
        if (mTaskDao.queryBuilder()
                .where(TaskDao.Properties.Description.eq(t.getDescription()), TaskDao.Properties.DeletedOn.isNull())
                .list().size() > 0)
            throw new DuplicatedTaskException();
        else
            mTaskDao.insert(t);
    }

    public Task newTask() {
        Task t = new Task(null);
        return t;
    }

    public Task getTask(long id) {
        return mTaskDao.load(id);
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull(), TaskDao.Properties.Archieved_on.isNull()).list();
        return tasks;
    }

    public List<Task> getAllTasks(boolean active, boolean archive) {
        List<Task> tasks = new ArrayList<Task>();

        if (active && archive)
            tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull()).list();
        else if (active && !archive)
            tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull(), TaskDao.Properties.Archieved_on.isNull()).list();
        else if (!active && archive)
            tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull(), TaskDao.Properties.Archieved_on.isNotNull()).list();

        return tasks;
    }

    public List<Task> getAllSortedTasks() {
        long start = System.nanoTime();
        List<Task> tasks = getAllTasks();
        Log.d("TIMING", "SQL: " + (System.nanoTime() - start) / 1000000);

        start = System.nanoTime();
        mBitsComparator.reset();
        Collections.sort(tasks, mBitsComparator);
        Log.d("TIMING", "Sort: " + (System.nanoTime() - start) / 1000000);

        return tasks;
    }

    public List<Task> getAllSortedArchivedTasks() {
        List<Task> tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull(), TaskDao.Properties.Archieved_on.isNotNull()).orderDesc(TaskDao.Properties.Archieved_on).list();
        return tasks;
    }

    public void updateTask(Task t) {
        mTaskDao.update(t);
    }

    public int getDoneRate(Task t) {
        double sum = t.getDoneCount() + t.getSkipCount();
        double total = sum + t.getLateCount();

        return ((int) (100 * sum / total));
    }

    public int getDoneRateExcept(Task t) {
        List<Task> tasks = getAllTasks();

        double sum = 0, total = 0;

        for (Task e : tasks) {
            if (e.getId() != t.getId()) {
                long doneCount = e.getDoneCount();
                long lateCount = e.getLateCount();
                long skipCount = e.getSkipCount();

                sum += doneCount + skipCount;
                total += doneCount + skipCount + lateCount;
            }
        }

        return ((int) (100 * sum / total));
    }

    public long getActionCountForTaskSinceTimestamp(Task t) {
        if (t.getId() == null)
            return 0;

        long ts = System.currentTimeMillis() - t.getPastMillisOfCurrentPeriod();

        QueryBuilder qb = mActionRecordDao.queryBuilder();

        return qb.where(ActionRecordDao.Properties.TaskId.eq(t.getId()),
                ActionRecordDao.Properties.RecordOn.gt(ts),
                qb.or(ActionRecordDao.Properties.Action.eq(ACTION_TYPE_DONE),
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_SKIP))
        ).count();
    }

    public List<ActionRecord> getLastActionForTask(Task t, int ACTION_TYPE) {
        if (t.getId() == null)
            return new ArrayList<ActionRecord>();

        if (ACTION_TYPE != ACTION_TYPE_ANY) {
            return mActionRecordDao.queryBuilder().
                    where(ActionRecordDao.Properties.TaskId.eq(t.getId()),
                            ActionRecordDao.Properties.Action.eq(ACTION_TYPE))
                    .orderDesc(ActionRecordDao.Properties.RecordOn).limit(1).list();
        } else {
            return mActionRecordDao.queryBuilder().
                    where(ActionRecordDao.Properties.TaskId.eq(t.getId()))
                    .orderDesc(ActionRecordDao.Properties.RecordOn).limit(1).list();
        }
    }

    public ActionRecord getLastActionForActiveTask() {
        QueryBuilder qb = mActionRecordDao.queryBuilder();

        List<ActionRecord> records = qb.where(
                qb.or(
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_DONE),
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_SKIP))
        )
                .orderDesc(ActionRecordDao.Properties.RecordOn).list();

        for (ActionRecord r : records) {
            if (r.getTask().getDeletedOn() == null && r.getTask().getArchieved_on() == null) {
                return r;
            }
        }

        return null;
    }

    public void setNextScheduledTimeForTask(Task t) {
        Log.d("ReminderScheduleService", "setNextScheduledTimeForTask");
        if (mScheduleService != null)
            mScheduleService.unScheduleForTask(t);
        else {
            Log.d("ReminderScheduleService", "mScheduleService == null");
        }

        long actionCountSinceBeginOfInternval = getActionCountForTaskSinceTimestamp(t);
        if (t.getFrequency() - actionCountSinceBeginOfInternval > 0) {
            long delta = (t.getPeriod() - t.getPastMillisOfCurrentPeriod()) / (t.getFrequency() - actionCountSinceBeginOfInternval);
            t.setNextScheduledTime(System.currentTimeMillis() + delta);
            t.setCurrentInterval(delta);
        }

        Log.d("ScheduledTimeUpdate", "T:" + t.getDescription() + " freq:" + t.getFrequency() + " actionCount:" + actionCountSinceBeginOfInternval);
        if (mScheduleService != null)
            mScheduleService.scheduleForTask(t);
    }

    public void setActionRecordForTask(Task t, int ACTION_TYPE) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast_action_layout, null);
        View layout = view.findViewById(R.id.toast_layout_root);
        TextView text = (TextView) layout.findViewById(R.id.text);

        if (ACTION_TYPE == ACTION_TYPE_DONE) {
            text.setBackgroundColor(mContext.getResources().getColor(R.color.toast_done_label_background));
            layout.setBackgroundColor(mContext.getResources().getColor(R.color.toast_done_background));
            text.setText(mDoneSlogans.get(mRandomiser.nextInt(mDoneSlogans.size())));
        } else if (ACTION_TYPE == ACTION_TYPE_SKIP) {
            text.setBackgroundColor(mContext.getResources().getColor(R.color.toast_skip_label_background));
            layout.setBackgroundColor(mContext.getResources().getColor(R.color.toast_skip_background));
            text.setText(mSkipSlogans.get(mRandomiser.nextInt(mSkipSlogans.size())));
        }

        if (actionFinishedToast == null) {
            actionFinishedToast = new Toast(mContext.getApplicationContext());
        }
        actionFinishedToast.setView(layout);
        actionFinishedToast.setDuration(Toast.LENGTH_SHORT);
        actionFinishedToast.setGravity(Gravity.FILL, 0, 0);
        actionFinishedToast.show();

        setActionRecordForTaskAtDate(t, ACTION_TYPE, new Date());
    }

    public void setActionRecordForTaskAtDate(Task t, int ACTION_TYPE, Date date) {
        switch (ACTION_TYPE) {
            case ACTION_TYPE_SKIP:
                t.setSkipCount(t.getSkipCount() + 1);
                break;
            case ACTION_TYPE_DONE:
                t.setDoneCount(t.getDoneCount() + 1);
                break;
            case ACTION_TYPE_LATE:
                t.setLateCount(t.getLateCount() + 1);
        }
        t.update();
        ActionRecord record = new ActionRecord(null, ACTION_TYPE, date, t.getId());
        mActionRecordDao.insert(record);
        t.resetActionsRecords();
    }

    /**
     * Called if task's progress is > 100, let's check if
     *
     * @param t
     */
    public void updateActionRecordForTask(Task t) {
        QueryBuilder qb = mActionRecordDao.queryBuilder();
        long now = System.currentTimeMillis();
        // get LateActions whose timestamps are later than nextScheduledTime()
        List<ActionRecord> records = qb.where(
                ActionRecordDao.Properties.TaskId.eq(t.getId()),
                ActionRecordDao.Properties.Action.eq(ACTION_TYPE_LATE),
                qb.or(ActionRecordDao.Properties.RecordOn.gt(t.getNextScheduledTime()),
                        ActionRecordDao.Properties.RecordOn.eq(t.getNextScheduledTime()))
        )
                .orderDesc(ActionRecordDao.Properties.RecordOn).list();

        Long startTime = records.isEmpty() ? t.getNextScheduledTime() : records.get(0).getRecordOn().getTime() + t.getAvgInterval();

        while (startTime < now) {
            setActionRecordForTaskAtDate(t, ACTION_TYPE_LATE, new Date(startTime));
            startTime += t.getAvgInterval();
        }
    }

    public int getProgressForTask(Task t) {
        long actionCountSinceBeginOfInternval = getActionCountForTaskSinceTimestamp(t);
        if (t.getFrequency() - actionCountSinceBeginOfInternval <= 0) {
            // Worked too many times on this task, return 0. No need to work anymore on this task
            return 0;
        } else {
            return t.getProgress();
        }
    }

    public String getTimesAgoDescriptionForTask(Task t) {
        List<ActionRecord> records = getLastActionForTask(t, ACTION_TYPE_DONE);

        StringBuilder builder = new StringBuilder();
        if (records.size() > 0)
            builder.append(mContext.getResources().getString(R.string.done))
                    .append(' ')
                    .append(mPrettyTime.format(records.get(0).getRecordOn()));
        else {
            builder.append(mContext.getResources().getString(R.string.added))
                    .append(' ')
                    .append(mPrettyTime.format(t.getCreatedOn()));
        }
        return builder.toString();
    }

    public String getArchivedDescriptionForTask(Task t) {
        return new StringBuilder()
                .append(mContext.getResources().getString(R.string.achieved))
                .append(' ')
                .append(mPrettyTime.format(t.getArchieved_on()))
                .toString();
    }

    public void removeActionRecordById(long id) {
        ActionRecord record = mActionRecordDao.load(id);

        switch (record.getAction()) {
            case ACTION_TYPE_DONE:
                record.getTask().setDoneCount(record.getTask().getDoneCount() - 1);
                break;
            case ACTION_TYPE_SKIP:
                record.getTask().setSkipCount(record.getTask().getSkipCount() - 1);
                break;
        }

        setNextScheduledTimeForTask(record.getTask());

        record.getTask().update();
        mActionRecordDao.delete(record);
        record.getTask().resetActionsRecords();
    }

    public void setSkipActionForTask(Task t) {
        setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);
        setNextScheduledTimeForTask(t);
        t.update();
    }

    public void setDoneActionForTask(Task t) {
        setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        setNextScheduledTimeForTask(t);
        t.update();
    }

    public HashMap<Long, Integer> getCategoryCountForTasks(boolean active, boolean archive) {
        List<Task> tasks = getAllTasks(active, archive);

        HashMap<Long, Integer> catIdToCount = new HashMap<Long, Integer>();

        for (Task t : tasks) {
            Integer i = catIdToCount.get(t.getCategory().getId());
            if (i == null)
                catIdToCount.put(t.getCategory().getId(), 1);
            else
                catIdToCount.put(t.getCategory().getId(), i + 1);
        }

        return catIdToCount;
    }

    public List<Pair<Long, Integer>> getCategoryWithCount(boolean archieved, boolean active) {
        if (!archieved && !active)
            return null;

        List<Pair<Long, Integer>> pairs = new ArrayList<Pair<Long, Integer>>();

        String cols[] = new String[2];
        cols[0] = "CATEGORY_ID";
        cols[1] = "count(*) as count";

        String whereClause = null;

        if (active && archieved)
            whereClause = "deleted_on IS NULL";
        else if (active && !archieved)
            whereClause = "deleted_on IS NULL AND Archieved_on IS NULL";
        else if (!active && archieved)
            whereClause = "deleted_on IS NULL AND Archieved_on IS NOT NULL";

        String groupBy = "CATEGORY_ID";

        Cursor pairsCursor = mDB.query(mTaskDao.getTablename(), cols, whereClause, null, groupBy, null, null);

        int countColIndex = pairsCursor.getColumnIndex("count");
        int idColIndex = pairsCursor.getColumnIndex("CATEGORY_ID");

        if (pairsCursor.moveToFirst() == true) {
            do {
                int count = pairsCursor.getInt(countColIndex);
                long id = pairsCursor.getLong(idColIndex);
                pairs.add(new Pair<Long, Integer>(id, count));
            } while (pairsCursor.moveToNext());
        }

        return pairs;
    }

    public class CachedComparator implements Comparator<Task> {
        HashMap<Task, Long> taskToCount = new HashMap<Task, Long>();

        public void reset() {
            taskToCount.clear();
        }

        @Override
        public int compare(Task task, Task task2) {
            Long c1 = taskToCount.get(task);
            Long c2 = taskToCount.get(task2);

            if (c1 == null) {
                c1 = getActionCountForTaskSinceTimestamp(task);
                taskToCount.put(task, c1);
            }

            if (c2 == null) {
                c2 = getActionCountForTaskSinceTimestamp(task2);
                taskToCount.put(task2, c2);
            }

            if ((task).getFrequency() <= c1) {
                return 1;
            } else if ((task2).getFrequency() <= c2) {
                return -1;
            }


            if (task.getNextScheduledTime() < task2.getNextScheduledTime()) {
                return -1;
            } else if (task2.getNextScheduledTime() < task.getNextScheduledTime()) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    public class DuplicatedTaskException extends Throwable {
    }
}
