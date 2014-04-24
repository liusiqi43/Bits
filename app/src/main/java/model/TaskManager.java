package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.siqi.bits.ActionRecord;
import com.siqi.bits.ActionRecordDao;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.dao.query.QueryBuilder;

/**
 * Created by me on 4/9/14.
 */
public class TaskManager {
    public static final ConcurrentHashMap<String, Integer> PeriodStringToDays = new ConcurrentHashMap<String, Integer>();
    public static final ConcurrentHashMap<Long, Integer> PeriodToDays = new ConcurrentHashMap<Long, Integer>();
    public static final long DAY_TO_MILLIS = 24 * 3600 * 1000;
    public static final int ACTION_TYPE_DONE = 1;
    public static final int ACTION_TYPE_SKIP = 2;
    public static final int ACTION_TYPE_LATE = 3;
    public static final int ACTION_TYPE_ANY = 4;
    private static TaskManager INSTANCE = null;
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private TaskDao mTaskDao;
    private ActionRecordDao mActionRecordDao;
    private PrettyTime mPrettyTime;
    private Context mContext;

    private TaskManager(Context ctx) {
        /**
         * DB init
         */
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(
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

        mPrettyTime = new PrettyTime();
        mContext = ctx;
    }

    public static TaskManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new TaskManager(ctx);
        return INSTANCE;
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
        List<Task> tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull()).list();
        return tasks;
    }

    public List<Task> getAllSortedTasks() {
        List<Task> tasks = getAllTasks();
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task task, Task task2) {
                long c1 = getActionCountForTaskSinceTimestamp(task);
                if (task.getFrequency() <= c1) {
                    return 1;
                }
                if (task.getNextScheduledTime() < task2.getNextScheduledTime()) {
                    return -1;
                } else if (task2.getNextScheduledTime() < task.getNextScheduledTime()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

//        for (Task t : tasks) {
//            Log.d("GETALLSORTEDTASK", t.getDescription() + ":" + t.getNextScheduledTime() + " interval: " + t.getCurrentInterval());
//        }

        return tasks;
    }

    public void updateTask(Task t) {
        mTaskDao.update(t);
    }

    public int getDoneRate(Task t) {
        double sum = t.getDoneCount() + t.getSkipCount();
        double total = sum + t.getLateCount();

        return ((int) (100 * sum/total));
    }

    public int getDoneRateExcept(Task t) {
        List<Task> tasks = getAllTasks();

        double sum = 0, total = 0;

        for (Task e : tasks) {
            if (e.getId() != t.getId()) {
                long doneCount = e.getDoneCount();
                long lateCount = e.getLateCount();
                long skipCount = e.getSkipCount();

                sum += doneCount+skipCount;
                total += doneCount+skipCount+lateCount;
            }
        }

        return ((int) (100 * sum/total));
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
                        ActionRecordDao.Properties.Action.eq(ACTION_TYPE_SKIP)))
                .orderDesc(ActionRecordDao.Properties.RecordOn).list();

        for (ActionRecord r : records) {
            if (r.getTask().getDeletedOn() == null){
                return r;
            }
        }

        return null;
    }

    public void setNextScheduledTimeForTask(Task t) {
        long actionCountSinceBeginOfInternval = getActionCountForTaskSinceTimestamp(t);
        if (t.getFrequency() - actionCountSinceBeginOfInternval > 0) {
            long delta = (t.getPeriod() - t.getPastMillisOfCurrentPeriod()) / (t.getFrequency() - actionCountSinceBeginOfInternval);
            t.setNextScheduledTime(System.currentTimeMillis() + delta);
            t.setCurrentInterval(delta);
        }

        Log.d("ScheduledTimeUpdate", "T:"+t.getDescription()+" freq:"+t.getFrequency() + " actionCount:"+actionCountSinceBeginOfInternval);
    }

    public void setActionRecordForTask(Task t, int ACTION_TYPE) {
        setActionRecordForTaskAtDate(t, ACTION_TYPE, new Date());
    }

    public void setActionRecordForTaskAtDate(Task t, int ACTION_TYPE, Date date) {
        switch (ACTION_TYPE) {
            case ACTION_TYPE_SKIP:
                t.setSkipCount(t.getSkipCount()+1);
                break;
            case ACTION_TYPE_DONE:
                t.setDoneCount(t.getDoneCount()+1);
                break;
            case ACTION_TYPE_LATE:
                t.setLateCount(t.getLateCount()+1);
        }
        t.update();
        ActionRecord record = new ActionRecord(null, ACTION_TYPE, date, t.getId());
        mActionRecordDao.insert(record);
        t.resetActionsRecords();
    }

    /**
     * Called if task's progress is > 100, let's check if
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
                                        ActionRecordDao.Properties.RecordOn.eq(t.getNextScheduledTime())))
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

        if (records.size() > 0)
            return new StringBuilder()
                    .append(mContext.getResources().getString(R.string.done))
                    .append(' ')
                    .append(mPrettyTime.format(records.get(0).getRecordOn())).toString();
        else {
            return mContext.getResources().getString(R.string.added_recently);
        }
    }

    public void removeActionRecordById(long id) {
        ActionRecord record = mActionRecordDao.load(id);

        switch (record.getAction()){
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

    public class DuplicatedTaskException extends Throwable {
    }
}
