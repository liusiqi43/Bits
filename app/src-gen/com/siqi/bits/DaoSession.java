package com.siqi.bits;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import com.siqi.bits.Task;
import com.siqi.bits.Category;

import com.siqi.bits.TaskDao;
import com.siqi.bits.CategoryDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig taskDaoConfig;
    private final DaoConfig categoryDaoConfig;

    private final TaskDao taskDao;
    private final CategoryDao categoryDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        taskDaoConfig = daoConfigMap.get(TaskDao.class).clone();
        taskDaoConfig.initIdentityScope(type);

        categoryDaoConfig = daoConfigMap.get(CategoryDao.class).clone();
        categoryDaoConfig.initIdentityScope(type);

        taskDao = new TaskDao(taskDaoConfig, this);
        categoryDao = new CategoryDao(categoryDaoConfig, this);

        registerDao(Task.class, taskDao);
        registerDao(Category.class, categoryDao);
    }
    
    public void clear() {
        taskDaoConfig.getIdentityScope().clear();
        categoryDaoConfig.getIdentityScope().clear();
    }

    public TaskDao getTaskDao() {
        return taskDao;
    }

    public CategoryDao getCategoryDao() {
        return categoryDao;
    }

}