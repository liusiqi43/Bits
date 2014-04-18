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
    private static final String CATEGORIES_ICON_EXTENSION = ".png";
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
        if (mCategoryDao.count() < 100) {
            mCategoryDao.deleteAll();

            addCategoryWithNameAndIcon("Music", "music");
            addCategoryWithNameAndIcon("Sports", "bicycle");
            addCategoryWithNameAndIcon("News", "news_paper");
            addCategoryWithNameAndIcon("Reading", "notebook");
            addCategoryWithNameAndIcon("Study", "study");
            addCategoryWithNameAndIcon("Goal", "target");
        }
    }

    private void addCategoryWithNameAndIcon(String name, String iconName) {
        Category c = newCategory();
        c.setName(name);
        c.setIconDrawableName(iconName+CATEGORIES_ICON_EXTENSION);
        insertCategory(c);
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
