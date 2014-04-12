package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.siqi.bits.Category;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.Task;
import com.siqi.bits.TaskDao;

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

    public void insertTask(Task t) {
        mTaskDao.insert(t);
        Log.d(getClass().getName(), "Task "+t.getDescription()+" inserted in DAO");
    }

    public Task newTask(Category c) {
        Task t = new Task(null, null, new Date(), new Date(), null, 0, 0, 0, 0, 0, 0, c.getId());
        return t;
    }

    public Task getTask(long id) {
        return mTaskDao.load(id);
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = mTaskDao.queryBuilder().where(TaskDao.Properties.DeletedOn.isNull()).list();
        return tasks;
    }
}
