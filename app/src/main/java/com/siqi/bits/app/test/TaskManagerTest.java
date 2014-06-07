package com.siqi.bits.app.test;

import com.siqi.bits.ActionRecord;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.Task;

import java.util.Date;

import de.greenrobot.dao.test.AbstractDaoSessionTest;
import model.TaskManager;
import utils.FreezedClock;
import utils.SystemClock;
import utils.Utils;

/**
 * Most tests are time-bound, state the need for ClockEntity at the beginning of every test using Utils.setClockEntity()
 */
public class TaskManagerTest extends AbstractDaoSessionTest<DaoMaster, DaoSession> {
    public TaskManagerTest() {
        super(DaoMaster.class);
        try {
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testGetLastActionRecord() throws Exception {
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(new SystemClock());
        Task t = new Task(null, "Test", new Date(), new Date(), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);

        try {
            tm.insertTask(t);
        } catch (TaskManager.DuplicatedTaskException e) {
            e.printStackTrace();
        }


        ActionRecord record;

        record = tm.getLastActiveActionForActiveTask();
        assertNull(record);

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);

        record = tm.getLastActiveActionForActiveTask();
        assertNotNull(record);
        assertEquals(TaskManager.ACTION_TYPE_SKIP, record.getAction());

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);

        record = tm.getLastActiveActionForActiveTask();
        assertNotNull(record);
        assertEquals(TaskManager.ACTION_TYPE_DONE, record.getAction());

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);

        record = tm.getLastActiveActionForActiveTask();
        assertNotNull(record);
        assertEquals(TaskManager.ACTION_TYPE_SKIP, record.getAction());

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_LATE);
        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_LATE);

        record = tm.getLastActiveActionForActiveTask();
        assertNotNull(record);
        assertEquals(TaskManager.ACTION_TYPE_SKIP, record.getAction());
    }

    public void testGetPastMillisOfCurrentPeriod() throws Exception {
        FreezedClock freezedClock = new FreezedClock();
        freezedClock.setCurrentTime(1402941993458l);
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(freezedClock);

        Task t = new Task(null, "Test", new Date(freezedClock.currentTimeMillis()), new Date(freezedClock.currentTimeMillis()), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);

        assertEquals(0, t.getPastMillisOfCurrentPeriod());

        freezedClock.setMoveAheadMillis(5000);
        assertEquals(1402941993458l, freezedClock.currentTimeMillis() - t.getPastMillisOfCurrentPeriod());
        assertEquals(5000, t.getPastMillisOfCurrentPeriod());

        freezedClock.setMoveAheadMillis(t.getPeriod());
        assertEquals(5000, t.getPastMillisOfCurrentPeriod());
    }

    public void testGetActionCountForTaskSinceBeginningOfPeriod() {
        FreezedClock freezedClock = new FreezedClock();
        freezedClock.setCurrentTime(1402941993458l);
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(freezedClock);

        Task t = new Task(null, "Test", new Date(freezedClock.currentTimeMillis()), new Date(freezedClock.currentTimeMillis()), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);

        try {
            tm.insertTask(t);
        } catch (TaskManager.DuplicatedTaskException e) {
            e.printStackTrace();
        }

        assertNotNull(t.getId());
        assertEquals(0, tm.getActionCountForTaskSinceBeginningOfPeriod(t));

        freezedClock.setMoveAheadMillis(5000);


        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);
        assertEquals(1, tm.getActionCountForTaskSinceBeginningOfPeriod(t));

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        assertEquals(2, tm.getActionCountForTaskSinceBeginningOfPeriod(t));

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_LATE);
        assertEquals(2, tm.getActionCountForTaskSinceBeginningOfPeriod(t));

        freezedClock.setMoveAheadMillis(t.getPeriod());
        assertEquals(0, tm.getActionCountForTaskSinceBeginningOfPeriod(t));

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        assertEquals(1, tm.getActionCountForTaskSinceBeginningOfPeriod(t));
    }

    public void testSetNextScheduledTimeForTask() throws Exception {
        FreezedClock freezedClock = new FreezedClock();
        freezedClock.setCurrentTime(1402941993458l);
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(freezedClock);

        Task t = new Task(null, "Test", new Date(freezedClock.currentTimeMillis()), new Date(freezedClock.currentTimeMillis()), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);
        assertEquals(Utils.currentTimeMillis()
                + TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 4, t.getNextScheduledTime());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 4, t.getCurrentInterval());

        try {
            tm.insertTask(t);
        } catch (TaskManager.DuplicatedTaskException e) {
            e.printStackTrace();
        }

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_SKIP);
        assertEquals(0, t.getPastMillisOfCurrentPeriod());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 3, t.getPeriod() / 3);
        assertEquals(Utils.currentTimeMillis(), Utils.currentTimeMillis());
        assertEquals(3, t.getFrequency() - tm.getActionCountForTaskSinceBeginningOfPeriod(t));
        assertEquals(Utils.currentTimeMillis()
                + TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 3, t.getNextScheduledTime());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 3, t.getCurrentInterval());

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        assertEquals(2, t.getFrequency() - tm.getActionCountForTaskSinceBeginningOfPeriod(t));
        assertEquals(Utils.currentTimeMillis()
                + TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 2, t.getNextScheduledTime());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 2, t.getCurrentInterval());

        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        assertEquals(1, t.getFrequency() - tm.getActionCountForTaskSinceBeginningOfPeriod(t));
        assertEquals(Utils.currentTimeMillis()
                + TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS, t.getNextScheduledTime());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS, t.getCurrentInterval());


        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_DONE);
        assertEquals(0, t.getFrequency() - tm.getActionCountForTaskSinceBeginningOfPeriod(t));
        assertEquals(Utils.currentTimeMillis() - t.getPastMillisOfCurrentPeriod()
                + t.getPeriod() + t.getAvgInterval(), t.getNextScheduledTime());
        assertEquals(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS / 4, t.getCurrentInterval());
    }

    public void testGetProgress() throws Exception {
        FreezedClock freezedClock = new FreezedClock();
        freezedClock.setCurrentTime(1402941993458l);
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(freezedClock);

        Task t = new Task(null, "Test", new Date(freezedClock.currentTimeMillis()), new Date(freezedClock.currentTimeMillis()), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);

        try {
            tm.insertTask(t);
        } catch (TaskManager.DuplicatedTaskException e) {
            e.printStackTrace();
        }

        assertEquals(0, tm.getProgressForTask(t));

        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(50, tm.getProgressForTask(t));
        tm.setSkipActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));

        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(50, tm.getProgressForTask(t));
        tm.setDoneActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));

        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(50, tm.getProgressForTask(t));
        tm.setDoneActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));

        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(50, tm.getProgressForTask(t));
        tm.setDoneActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));

        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(0, tm.getProgressForTask(t));
        tm.setDoneActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));
    }

    public void testUndoLastAction() throws Exception {
        /**
         * Set up
         */
        FreezedClock freezedClock = new FreezedClock();
        freezedClock.setCurrentTime(1402941993458l);
        TaskManager tm = TaskManager.getTestInstance(getContext(), db);
        Utils.setClockEntity(freezedClock);

        /**
         * Create dumb task
         */
        Task t = new Task(null, "Test", new Date(freezedClock.currentTimeMillis()), new Date(freezedClock.currentTimeMillis()), null, 0, 0, 0, 0, 0, 0, 0, null, 0);
        t.setPeriod(TaskManager.PeriodStringToDays.get("week") * TaskManager.DAY_IN_MILLIS);
        t.setFrequency(4);
        tm.setNextScheduledTimeForTask(t);

        try {
            tm.insertTask(t);
        } catch (TaskManager.DuplicatedTaskException e) {
            e.printStackTrace();
        }

        /**
         * Set action
         */
        tm.setSkipActionForTask(t);
        freezedClock.setMoveAheadMillis(t.getCurrentInterval() / 2);
        assertEquals(50, tm.getProgressForTask(t));

        long oldNextScheduledTime = t.getNextScheduledTime();
        long oldCurrentInterval = t.getCurrentInterval();

        tm.setDoneActionForTask(t);
        assertEquals(0, tm.getProgressForTask(t));
        tm.setActionRecordForTask(t, TaskManager.ACTION_TYPE_LATE);

        /**
         * Undo last action
         */
        ActionRecord r = tm.getLastActiveActionForActiveTask();
        assertEquals(r.getAction(), TaskManager.ACTION_TYPE_DONE);

        tm.removeActionRecordById(r.getId());
        assertEquals(1, tm.getActionCountForTaskSinceBeginningOfPeriod(t));
        assertEquals(oldCurrentInterval, t.getCurrentInterval());
        assertEquals(oldNextScheduledTime, t.getNextScheduledTime());
        assertEquals(50, tm.getProgressForTask(t));
    }

}
