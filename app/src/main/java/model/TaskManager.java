package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.siqi.bits.Category;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.Task;
import com.siqi.bits.TaskDao;
import com.siqi.bits.app.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 4/9/14.
 */
public class TaskManager {
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private TaskDao mTaskDao;

    private final ConcurrentHashMap<String, Integer> IntervalToDays = new ConcurrentHashMap<String, Integer>();

    private static TaskManager INSTANCE = null;

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

        IntervalToDays.put(ctx.getString(R.string.radio_day), 1);
        IntervalToDays.put(ctx.getString(R.string.radio_week), 7);
        IntervalToDays.put(ctx.getString(R.string.radio_month), 30);
        IntervalToDays.put(ctx.getString(R.string.radio_year), 365);
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

    public Task newTask(Category c) {
        Task t = new Task(null, null, "", new Date(), new Date(), null, 0, null, 0, 0, 0, 0, null, c.getId());
        return t;
    }

    public Task newTask() {
        Task t = new Task(null, null, "", new Date(), new Date(), null, 0, null, 0, 0, 0, 0, null, -1);
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
                if (task.getNextScheduledTime() < task2.getNextScheduledTime()) {
                    return -1;
                } else if (task2.getNextScheduledTime() < task.getNextScheduledTime()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (Task t : tasks) {
            Log.d("GETALLSORTEDTASK", t.getDescription() + ":" + t.getNextScheduledTime() + " interval: " + t.getInterval());
        }

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
                sum += e.getDoneCount()+e.getSkipCount();
                total += e.getDoneCount()+e.getSkipCount()+e.getLateCount();
            }
        }

        return ((int) (100 * sum/total));
    }

    public void setIntervalFrequencyForTask(Task t, String interval, String frequency) {
        int daysCount = IntervalToDays.get(interval);
        int freq = Integer.parseInt(frequency);
        t.setInterval((long) daysCount * 24 * 3600 * 1000 / freq);
        // d3 w4 m3 y4 alike
        t.setFrequencyIntervalPair(interval.charAt(0)+frequency);
    }

    public class DuplicatedTaskException extends Throwable {
    }
}
