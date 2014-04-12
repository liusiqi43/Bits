package com.siqi.bits_GDgen;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class BitsDaoGenerator {

    public static void main(String args[]) throws Exception {
        Schema schema = new Schema(3, "com.siqi.bits");

        /**
         * Task
         */
        Entity task = schema.addEntity("Task");
        task.addIdProperty();
        task.addStringProperty("description").notNull().unique();
        task.addDateProperty("createdOn").notNull();
        task.addDateProperty("modifiedOn").notNull();
        task.addDateProperty("deletedOn");
        // if 6 times a day, that's once every 2 hours, interval stores 2 hours in ms
        task.addLongProperty("interval").notNull().getProperty();
        // currentTime when just created
        task.addLongProperty("lastDone").notNull().getProperty();
        Property nextScheduledTime = task.addLongProperty("nextScheduledTime").notNull().getProperty();
        task.addIntProperty("doneCount");
        task.addIntProperty("skipCount");
        task.addIntProperty("lateCount");


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