package model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private CategoryDao mCategoryDao;

    private static CategoryManager INSTANCE = null;

    private CategoryManager(Context ctx) {
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
        mCategoryDao = mDaoSession.getCategoryDao();

        initWithCategories();
    }

    private void initWithCategories() {
        if (mCategoryDao.count() < 3) {
            mCategoryDao.deleteAll();

            Category c;

            c = newCategory();
            c.setName("Entertainment");
            c.setIconDrawableName("icon_6599.svg");
            insertCategory(c);

            c = newCategory();
            c.setName("Sports");
            c.setIconDrawableName("icon_10684.svg");
            insertCategory(c);

            c = newCategory();
            c.setName("Study");
            c.setIconDrawableName("icon_32663.svg");
            insertCategory(c);
        }
    }

    public static CategoryManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new CategoryManager(ctx);
        return INSTANCE;
    }

    public Category getCategory(long id) {
        return mCategoryDao.load(id);
    }

    public void insertCategory(Category c) {
        mCategoryDao.insert(c);
        Log.d(getClass().getName(), c.getClass().getName()+ ":" + c.getName() + " inserted in DAO");
    }

    public Category newCategory() {
        Category c = new Category(null, null, null, new Date(), new Date(), null);
        return c;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = mCategoryDao.queryBuilder().where(CategoryDao.Properties.DeletedOn.isNull()).list();
        return categories;
    }
}
