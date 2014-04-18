package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.siqi.bits.Category;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.Task;
import com.siqi.bits.TaskDao;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by me on 4/9/14.
 */
public class TaskManager {
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private TaskDao mTaskDao;

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
        Task t = new Task(null, null, new Date(), new Date(), null, 0, 0, 0, 0, 0, 0, c.getId());
        return t;
    }

    public Task newTask() {
        Task t = new Task(null, null, new Date(), new Date(), null, 0, 0, 0, 0, 0, 0, -1);
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

        return tasks;
    }

    public void updateTask(Task t) {
        mTaskDao.update(t);
    }

    public class DuplicatedTaskException extends Throwable {
    }
}
