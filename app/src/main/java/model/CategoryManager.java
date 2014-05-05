package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.siqi.bits.BitsDevOpenHelper;
import com.siqi.bits.Category;
import com.siqi.bits.CategoryDao;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;

import java.util.Date;
import java.util.List;

/**
 * Created by me on 4/9/14.
 */
public class CategoryManager {
    private static final String CATEGORIES_ICON_EXTENSION = ".png";
    private static CategoryManager INSTANCE = null;
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private CategoryDao mCategoryDao;

    private CategoryManager(Context ctx) {
        /**
         * DB init
         */
        BitsDevOpenHelper helper = new BitsDevOpenHelper(
                ctx,
                "bits-db",
                null);
        mDB = helper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDB);
        mDaoSession = mDaoMaster.newSession();
        mCategoryDao = mDaoSession.getCategoryDao();

        initWithCategories();
    }

    public static CategoryManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new CategoryManager(ctx);
        return INSTANCE;
    }

    private void initWithCategories() {
        if (mCategoryDao.count() < 19) {
            mCategoryDao.deleteAll();

            addCategoryWithNameAndIcon("Reading", "literature-75");
            addCategoryWithNameAndIcon("Love", "two_hearts-75");
            addCategoryWithNameAndIcon("Culture", "university-75");
            addCategoryWithNameAndIcon("Arts", "origami-75");
            addCategoryWithNameAndIcon("Social", "message-75");
            addCategoryWithNameAndIcon("Study", "student-75");
            addCategoryWithNameAndIcon("Project", "compass2-75");
            addCategoryWithNameAndIcon("Sports", "football2-75");
            addCategoryWithNameAndIcon("Finance", "USD-75");
            addCategoryWithNameAndIcon("Food", "diningroom-75");
            addCategoryWithNameAndIcon("Movie", "film_reel-75");
            addCategoryWithNameAndIcon("Concert", "french_horn-75");
            addCategoryWithNameAndIcon("Health", "heart_monitor-75");
            addCategoryWithNameAndIcon("Inspiration", "idea-75");
            addCategoryWithNameAndIcon("Music", "music-75");
            addCategoryWithNameAndIcon("Photography", "slr_camera2-75");
            addCategoryWithNameAndIcon("Memory", "stack_of_photos-75");
            addCategoryWithNameAndIcon("Excursion", "mountain_biking-75");
            addCategoryWithNameAndIcon("Advanture", "treasury_map-75");
        }
    }

    private void addCategoryWithNameAndIcon(String name, String iconName) {
        Category c = newCategory();
        c.setName(name);
        c.setIconDrawableName(iconName + CATEGORIES_ICON_EXTENSION);
        insertCategory(c);
    }

    public Category getCategory(long id) {
        return mCategoryDao.load(id);
    }

    public void insertCategory(Category c) {
        mCategoryDao.insert(c);
        Log.d(getClass().getName(), c.getClass().getName() + ":" + c.getName() + " inserted in DAO");
    }

    public Category newCategory() {
        Category c = new Category(null, null, null, new Date(), new Date(), null);
        return c;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = mCategoryDao.queryBuilder().where(CategoryDao.Properties.DeletedOn.isNull()).list();
        return categories;
    }

    public Category getDefaultCategory() {
        return this.getAllCategories().get(0);
    }
}
