package com.siqi.bits_GDgen;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class BitsDaoGenerator {

    public static void main(String args[]) throws Exception {
        Schema schema = new Schema(4, "com.siqi.bits");
        schema.enableKeepSectionsByDefault();

        /**
         * Task
         */
        Entity task = schema.addEntity("Task");
        task.addIdProperty();
        task.addStringProperty("description").notNull();
        task.addDateProperty("createdOn").notNull();
        task.addDateProperty("modifiedOn").notNull();
        task.addDateProperty("deletedOn");
        task.addIntProperty("doneCount").notNull();
        task.addIntProperty("lateCount").notNull();
        task.addIntProperty("skipCount").notNull();
        task.addLongProperty("currentInterval").notNull();
        // if 6 times a day, that's once every 2 hours, interval stores 2 hours in ms
        task.addLongProperty("period").notNull().getProperty();
        Property nextScheduledTime = task.addLongProperty("nextScheduledTime").notNull().getProperty();
        task.addIntProperty("frequency").notNull();
        task.addDateProperty("archieved_on");


        /**
         * Category
         */
        Entity category = schema.addEntity("Category");
        category.addIdProperty();
        category.addStringProperty("name");
        category.addStringProperty("iconDrawableName");
        category.addDateProperty("createdOn").notNull();
        category.addDateProperty("modifiedOn").notNull();
        category.addDateProperty("deletedOn");

        /**
         * HistoryRecord
         */
        Entity actionRecord = schema.addEntity("ActionRecord");
        actionRecord.addIdProperty();

        // Done, Late or Skip as defined in TaskManager
        Property actionType = actionRecord.addIntProperty("action").notNull().getProperty();
        Property recordOn = actionRecord.addDateProperty("recordOn").notNull().getProperty();
        Index recordOnIndex = new Index();
        recordOnIndex.addProperty(actionType);
        recordOnIndex.addProperty(recordOn);
        actionRecord.addIndex(recordOnIndex);


        /**
         * Bi-directional Task -1-n- ActionRecord
         */
        Property taskIdProperty = actionRecord.addLongProperty("taskId").notNull().getProperty();
        actionRecord.addToOne(task, taskIdProperty);
        ToMany taskToActionRecords = task.addToMany(actionRecord, taskIdProperty);
        taskToActionRecords.setName("actionsRecords");
        taskToActionRecords.orderDesc(recordOn);

        /**
         * Bi-directional Category -1-n- Task
         */
        Property categoryIdProperty = task.addLongProperty("categoryId").notNull().getProperty();
        task.addToOne(category, categoryIdProperty);
        ToMany categoryToTasks = category.addToMany(task, categoryIdProperty);
        categoryToTasks.setName("tasks");
        categoryToTasks.orderAsc(nextScheduledTime);

        new DaoGenerator().generateAll(schema, "app/src-gen");
    }
}