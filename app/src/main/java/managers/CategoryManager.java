package managers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.siqi.bits.BitsDevOpenHelper;
import com.siqi.bits.Category;
import com.siqi.bits.CategoryDao;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;
import com.siqi.bits.app.R;

import java.util.Date;
import java.util.List;

import utils.Utils;

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
    private Context mContext;

    private CategoryManager(Context ctx) {
        /**
         * DB init
         */
        BitsDevOpenHelper helper = new BitsDevOpenHelper(
                ctx,
                null);
        mDB = helper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDB);
        mDaoSession = mDaoMaster.newSession();
        mCategoryDao = mDaoSession.getCategoryDao();
        mContext = ctx;

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

            addCategoryWithNameAndIcon(mContext.getString(R.string.reading), "literature-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.love), "two_hearts-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.culture), "university-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.arts), "origami-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.letter), "message-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.study), "student-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.project), "compass2-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.sports), "football2-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.finance), "USD-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.food), "diningroom-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.movie), "film_reel-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.instruments), "french_horn-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.health), "heart_monitor-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.inspiration), "idea-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.music), "music-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.photography), "slr_camera2-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.moments), "stack_of_photos-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.excursion), "mountain_biking-75");
            addCategoryWithNameAndIcon(mContext.getString(R.string.adventure), "treasury_map-75");
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
        Category c = new Category(null, null, null, new Date(Utils.currentTimeMillis()), new Date(Utils.currentTimeMillis()), null);
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
