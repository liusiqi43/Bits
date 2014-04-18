package com.siqi.bits;

import com.siqi.bits.DaoSession;
import de.greenrobot.dao.DaoException;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table TASK.
 */
public class Task {

    private Long id;
    /** Not-null value. */
    private String description;
    /** Not-null value. */
    private java.util.Date createdOn;
    /** Not-null value. */
    private java.util.Date modifiedOn;
    private java.util.Date deletedOn;
    private long interval;
    private long lastDone;
    private long nextScheduledTime;
    private int doneCount;
    private int skipCount;
    private int lateCount;
    private long categoryId;

    /** Used to resolve relations */
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    private transient TaskDao myDao;

    private Category category;
    private Long category__resolvedKey;


    public Task() {
    }

    public Task(Long id) {
        this.id = id;
    }

    public Task(Long id, String description, java.util.Date createdOn, java.util.Date modifiedOn, java.util.Date deletedOn, long interval, long lastDone, long nextScheduledTime, int doneCount, int skipCount, int lateCount, long categoryId) {
        this.id = id;
        this.description = description;
        this.createdOn = createdOn;
        this.modifiedOn = modifiedOn;
        this.deletedOn = deletedOn;
        this.interval = interval;
        this.lastDone = lastDone;
        this.nextScheduledTime = nextScheduledTime;
        this.doneCount = doneCount;
        this.skipCount = skipCount;
        this.lateCount = lateCount;
        this.categoryId = categoryId;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTaskDao() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Not-null value. */
    public String getDescription() {
        return description;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Not-null value. */
    public java.util.Date getCreatedOn() {
        return createdOn;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setCreatedOn(java.util.Date createdOn) {
        this.createdOn = createdOn;
    }

    /** Not-null value. */
    public java.util.Date getModifiedOn() {
        return modifiedOn;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setModifiedOn(java.util.Date modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    public java.util.Date getDeletedOn() {
        return deletedOn;
    }

    public void setDeletedOn(java.util.Date deletedOn) {
        this.deletedOn = deletedOn;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getLastDone() {
        return lastDone;
    }

    public void setLastDone(long lastDone) {
        this.lastDone = lastDone;
    }

    public long getNextScheduledTime() {
        return nextScheduledTime;
    }

    public void setNextScheduledTime(long nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }

    public int getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getLateCount() {
        return lateCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    /** To-one relationship, resolved on first access. */
    public Category getCategory() {
        long __key = this.categoryId;
        if (category__resolvedKey == null || !category__resolvedKey.equals(__key)) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            CategoryDao targetDao = daoSession.getCategoryDao();
            Category categoryNew = targetDao.load(__key);
            synchronized (this) {
                category = categoryNew;
            	category__resolvedKey = __key;
            }
        }
        return category;
    }

    public void setCategory(Category category) {
        if (category == null) {
            throw new DaoException("To-one property 'categoryId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.category = category;
            categoryId = category.getId();
            category__resolvedKey = categoryId;
        }
    }

    /** Convenient call for {@link AbstractDao#delete(Object)}. Entity must attached to an entity context. */
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.delete(this);
    }

    /** Convenient call for {@link AbstractDao#update(Object)}. Entity must attached to an entity context. */
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.update(this);
    }

    /** Convenient call for {@link AbstractDao#refresh(Object)}. Entity must attached to an entity context. */
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.refresh(this);
    }

    // KEEP METHODS - put your custom methods here
    public void setLastDoneAndUpdateNextScheduleTime(long lastDone) {
        this.setLastDone(lastDone);
        this.setNextScheduledTime(lastDone + this.getInterval());
    }

    public void incrementSkipCount() {
        this.setSkipCount(this.getSkipCount() + 1);
    }

    public void incrementLateCount() {
        this.setLateCount(this.getLateCount() + 1);
    }

    public void incrementDoneCount() {
        this.setDoneCount(this.getDoneCount() + 1);
    }
    // KEEP METHODS END

}
