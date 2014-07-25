package model;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

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
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.dao.query.QueryBuilder;
import service.ReminderScheduleService;
import utils.Utils;

/**
 * Created by me on 4/9/14.
 */
public class TaskManager {
    public static final HashMap<String, Integer> PeriodStringToDays = new HashMap();
    public static final HashMap<Integer, String> DaysToPeriodStrings = new HashMap();
    public static final ConcurrentHashMap<Long, Integer> PeriodToDays = new ConcurrentHashMap<Long, Integer>();
    public static final long DAY_IN_MILLIS = 24 * 3600 * 1000;
    public static final int ACTION_TYPE_DONE = 1;
    public static final int ACTION_TYPE_SKIP = 2;
    public static final int ACTION_TYPE_LATE = 3;
    public static final int ACTION_TYPE_ANY = 4;
    public static final String TOTAL_SKIP_COUNT = "TOTAL_SKIP_COUNT";
    public static final String TOTAL_DONE_COUNT = "TOTAL_DONE_COUNT";
    public static final String TOTAL_LATE_COUNT = "TOTAL_LATE_COUNT";
    public static final String TOTAL_TASK_ADDED = "TOTAL_TASK_ADDED";
    public static CachedComparator mBitsComparator;
    private static TaskManager INSTANCE = null;
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private TaskDao mTaskDao;
    private ActionRecordDao mActionRecordDao;
    private PrettyTime mPrettyTime;
    private Context mContext;
    private String[] mDoneSlogans;
    private ReminderScheduleService mScheduleService = null;
    private SharedPreferences mPreferences;

    private TaskManager(Context ctx) {
        /**
         * DB init
         */
        BitsDevOpenHelper helper = new BitsDevOpenHelper(
                ctx,
                null);
        mDB = helper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDB);
        mDaoSession = mDaoMaster.newSession();
        mTaskDao = mDaoSession.getTaskDao();
        mActionRecordDao = mDaoSession.getActionRecordDao();

        mContext = ctx;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        setUp();
    }

    public static TaskManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new TaskManager(ctx);
        return INSTANCE;
    }

    public static TaskManager getTestInstance(Context ctx, SQLiteDatabase testDB) {
        if (INSTANCE == null)
            INSTANCE = new TaskManager(ctx);

        INSTANCE.mDB = testDB;
        INSTANCE.mDaoMaster = new DaoMaster(INSTANCE.mDB);
        INSTANCE.mDaoSession = INSTANCE.mDaoMaster.newSession();
        INSTANCE.mTaskDao = INSTANCE.mDaoSession.getTaskDao();
        INSTANCE.mActionRecordDao = INSTANCE.mDaoSession.getActionRecordDao();

        return INSTANCE;
    }

    private void setUp() {
        PeriodStringToDays.put(mContext.getString(R.string.radio_day), 1);
        PeriodStringToDays.put(mContext.getString(R.string.radio_week), 7);
        PeriodStringToDays.put(mContext.getString(R.string.radio_month), 30);
        PeriodStringToDays.put(mContext.getString(R.string.radio_year), 365);

        DaysToPeriodStrings.put(1, mContext.getString(R.string.radio_day));
        DaysToPeriodStrings.put(7, mContext.getString(R.string.radio_week));
        DaysToPeriodStrings.put(30, mContext.getString(R.string.radio_month));
        DaysToPeriodStrings.put(365, mContext.getString(R.string.radio_year));

        PeriodToDays.put((long) 1 * 24 * 60 * 60 * 1000, 1);
        PeriodToDays.put((long) 7 * 24 * 60 * 60 * 1000, 7);
        PeriodToDays.put((long) 30 * 24 * 60 * 60 * 1000, 30);
        PeriodToDays.put((long) 365 * 24 * 60 * 60 * 1000, 365);

        mDoneSlogans = mContext.getResources().getStringArray(R.array.done_slogans);

        mPrettyTime = new PrettyTime();

        mBitsComparator = new CachedComparator();
    }

    public void setScheduleService(ReminderScheduleService mScheduleService) {
        this.mScheduleService = mScheduleService;
    }

    public void insertTask(Task t) throws DuplicatedTaskException {
        if (mTaskDao.queryBuilder()
                .where(TaskDao.Properties.Description.eq(t.getDescription()),
                        TaskDao.Properties.DeletedOn.isNull(),
                        TaskDao.Properties.Archieved_on.isNull())
                .list().size() > 0)
            throw new DuplicatedTaskException();
        else {
            mTaskDao.insert(t);
            mPreferences.edit().putInt(TOTAL_TASK_ADDED,
                    mPreferences.getInt(TOTAL_TASK_ADDED, 0) + 1).commit();
        }
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

    public List<Task> getAllSortedTasks() {
        List<Task> tasks = getAllTasks();
        mBitsComparator.reset();
        Collections.sort(tasks, mBitsComparator);

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

    public long getActionCountForTaskSinceBeginningOfPeriod(Task t) {
        if (t.getId() == null)
            return 0;

        long ts = Utils.currentTimeMillis() - t.getPastMillisOfCurrentPeriod();
        QueryBuilder qb = mActionRecordDao.queryBuilder();

        return qb.where(ActionRecordDao.Properties.TaskId.eq(t.getId()),
                qb.or(ActionRecordDao.Properties.RecordOn.gt(ts),
                        ActionRecordDao.Properties.RecordOn.eq(ts)),
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

    private ActionRecord getLastActiveActionForTask(Task t) {
        QueryBuilder qb = mActionRecordDao.queryBuilder();

        List<ActionRecord> records = qb.where(
                ActionRecordDao.Properties.TaskId.eq(t.getId()),
                qb.or(
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_DONE),
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_SKIP))
        ).orderDesc(ActionRecordDao.Properties.RecordOn).list();

        for (ActionRecord r : records) {
            if (r.getTask().getDeletedOn() == null && r.getTask().getArchieved_on() == null) {
                return r;
            }
        }

        return null;
    }

    public ActionRecord getLastActiveActionForActiveTask() {
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

    public void setNextScheduledTimeForTaskAfterUndo(Task t,
                                                     long nextScheduledTime,
                                                     long currentInterval) {
        if (mScheduleService != null)
            mScheduleService.unScheduleForTask(t);
        else {
            Log.d("ReminderScheduleService", "mScheduleService == null");
        }

        t.setNextScheduledTime(nextScheduledTime);
        t.setCurrentInterval(currentInterval);

        if (mScheduleService != null)
            mScheduleService.scheduleForTask(t);
    }

    public void setNextScheduledTimeForTask(Task t) {
        if (mScheduleService != null)
            mScheduleService.unScheduleForTask(t);
        else {
            Log.d("ReminderScheduleService", "mScheduleService == null");
        }

        long actionCountSinceBeginOfInternval = getActionCountForTaskSinceBeginningOfPeriod(t);


        if (t.getFrequency() - actionCountSinceBeginOfInternval > 0) {
            long timeLeftInCurrentPeriod = t.getPeriod() - t.getPastMillisOfCurrentPeriod();
            long delta = timeLeftInCurrentPeriod / (t.getFrequency() - actionCountSinceBeginOfInternval);
            t.setNextScheduledTime(Utils.currentTimeMillis() + delta);
            t.setCurrentInterval(delta);
        } else {
            // already done enough for current period, postpone nextScheduledTime to next period
            long beginningOfCurrentPeriod = Utils.currentTimeMillis() - t.getPastMillisOfCurrentPeriod();
            long beginningOfNextPeriod = beginningOfCurrentPeriod + t.getPeriod();
            t.setNextScheduledTime(beginningOfNextPeriod + t.getAvgInterval());
            t.setCurrentInterval(t.getAvgInterval());
        }

        Log.d("ScheduledTimeUpdate", "T:" + t.getDescription() + " freq:" + t.getFrequency() + " actionCount:" + actionCountSinceBeginOfInternval);
        if (mScheduleService != null)
            mScheduleService.scheduleForTask(t);
    }

    public void setActionRecordForTask(Task t, int ACTION_TYPE) {
        setActionRecordForTaskAtDate(t, ACTION_TYPE, new Date(Utils.currentTimeMillis()));
        setNextScheduledTimeForTask(t);
        t.update();
    }

    private void setActionRecordForTaskAtDate(Task t, int ACTION_TYPE, Date date) {
        switch (ACTION_TYPE) {
            case ACTION_TYPE_SKIP:
                t.setSkipCount(t.getSkipCount() + 1);
                mPreferences.edit().putInt(TOTAL_SKIP_COUNT, mPreferences.getInt(TOTAL_SKIP_COUNT, 0) + 1).commit();
                break;
            case ACTION_TYPE_DONE:
                t.setDoneCount(t.getDoneCount() + 1);
                mPreferences.edit().putInt(TOTAL_DONE_COUNT, mPreferences.getInt(TOTAL_DONE_COUNT, 0) + 1).commit();
                break;
            case ACTION_TYPE_LATE:
                t.setLateCount(t.getLateCount() + 1);
                mPreferences.edit().putInt(TOTAL_LATE_COUNT, mPreferences.getInt(TOTAL_LATE_COUNT, 0) + 1).commit();
        }
        t.update();
        ActionRecord record = new ActionRecord(null, ACTION_TYPE, date, t.getCurrentInterval(), t.getNextScheduledTime(), t.getId());
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
        long now = Utils.currentTimeMillis();
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
        long actionCountSinceBeginOfInternval = getActionCountForTaskSinceBeginningOfPeriod(t);
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
                mPreferences.edit().putInt(TOTAL_DONE_COUNT, Math.max(mPreferences.getInt(TOTAL_DONE_COUNT, 0) - 1, 0)).commit();
                break;
            case ACTION_TYPE_SKIP:
                record.getTask().setSkipCount(record.getTask().getSkipCount() - 1);
                mPreferences.edit().putInt(TOTAL_SKIP_COUNT, Math.max(mPreferences.getInt(TOTAL_SKIP_COUNT, 0) - 1, 0)).commit();
                break;
        }

        Task currentTask = record.getTask();

        mActionRecordDao.delete(record);
        currentTask.resetActionsRecords();
        currentTask.update();

        setNextScheduledTimeForTaskAfterUndo(currentTask, record.getPreviousNextScheduledTime(), record.getPreviousInterval());
    }

    public void setSkipActionForTask(Task t) {
        setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);
    }

    public void setDoneActionForTask(Task t) {
        setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
    }

    public List<Pair<Long, Integer>> getCategoryWithCount(boolean active, boolean archieved) {
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

    public CharSequence getDoneSlogan() {
        return mDoneSlogans[Utils.getRandomInt(mDoneSlogans.length)];
    }

    public Task getTaskOrNew(Long id) {
        if (id != null) {
            return getTask(id);
        } else {
            return newTask();
        }
    }

    public class CachedComparator implements Comparator<Task> {
        HashMap<Task, Long> taskToCount = new HashMap<Task, Long>();

        public void reset() {
            taskToCount.clear();
        }

        @Override
        public int compare(Task t1, Task t2) {
            return getRankingScore(t1) > getRankingScore(t2) ? -1 : 1;
        }

        private double getRankingScore(Task t) {
            double p = getOverdoPenalty(t);
            double f = getFreshness(t);
            double u = getUrgency(t);

            Log.d("TaskManager", "======" + t.getDescription() + "=======");
            Log.d("TaskManager", "p = " + p + " f = " + f + " u = " + u);
            Log.d("TaskManager", "ranking score = " + (0.3 * p + 0.5 * f + 0.2 * u));
            return 0.3 * p + 0.5 * f + 0.2 * u;
        }

        private double getOverdoPenalty(Task t) {
            Long alreadyDone = taskToCount.get(t);

            if (alreadyDone == null) {
                alreadyDone = getActionCountForTaskSinceBeginningOfPeriod(t);
                taskToCount.put(t, alreadyDone);
            }

            long shouldBeDone = t.getFrequency();

            // No penalty when we haven't exceeded our proper share
            if (alreadyDone < shouldBeDone)
                return 1;

            return Math.exp(-(alreadyDone - shouldBeDone + 1));
        }

        private double getFreshness(Task t) {
            ActionRecord lastAction = getLastActiveActionForTask(t);
            long lastActionTime = (lastAction != null)
                    ? lastAction.getRecordOn().getTime()
                    : t.getCreatedOn().getTime();

            long currentTime = Utils.currentTimeMillis();
            // >= 1 if the task is almost late, so totally fresh for users
            // 0 if just done not long ago
            double rawFreshness = Math.min(Math.max((currentTime - lastActionTime) / (double) t.getCurrentInterval(),
                    0), 1);
            // x^0.2 grows super fast when x is small, and finish at 1, so it's normalized
            double freshness = Math.pow(rawFreshness, 0.2);
            return freshness;
        }

        private double getUrgency(Task t) {
            double urgency = (t.getNextScheduledTime() - Utils.currentTimeMillis()) / (double) t.getCurrentInterval();
            return Math.exp(-Math.min(Math.max(urgency, 0), 1));
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

    public class DuplicatedTaskException extends Throwable {
    }
}
