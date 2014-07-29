package managers;

/**
 * Proudly powered by me on 5/17/14.
 * Part of android productivity application Bits
 * A tool that helps you to architect your life to
 * its fullness!
 */

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.siqi.bits.ActionRecordDao;
import com.siqi.bits.BitsDevOpenHelper;
import com.siqi.bits.DaoMaster;
import com.siqi.bits.DaoSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by me on 4/9/14.
 */
public class ActionRecordManager {
    public static final int ACTION_TYPE_DONE = 1;
    public static final int ACTION_TYPE_SKIP = 2;
    public static final int ACTION_TYPE_LATE = 3;
    public static final int ACTION_TYPE_ANY = 4;
    private static ActionRecordManager INSTANCE = null;
    private SQLiteDatabase mDB;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private ActionRecordDao mActionRecordDao;
    private Context mContext;

    private ActionRecordManager(Context ctx) {
        /**
         * DB init
         */
        BitsDevOpenHelper helper = new BitsDevOpenHelper(
                ctx,
                null);
        mDB = helper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDB);
        mDaoSession = mDaoMaster.newSession();
        mActionRecordDao = mDaoSession.getActionRecordDao();

        mContext = ctx;
    }

    public static ActionRecordManager getInstance(Context ctx) {
        if (INSTANCE == null)
            INSTANCE = new ActionRecordManager(ctx);
        return INSTANCE;
    }


    /**
     * Raw query here, pay attention to hard-coded part
     *
     * @param days
     * @return
     */
    public List<DateBurnratePair> getActionRateForLastDays(int action, int days) {
        List<DateBurnratePair> pairs = new ArrayList<DateBurnratePair>();

        String cols[] = new String[2];
        cols[0] = "strftime('%Y-%m-%d', record_on/1000,'unixepoch', 'localtime') as date";
        cols[1] = "count(*) as count";

        String whereClause = "action = " + action + " AND date(record_on/1000, 'unixepoch', 'localtime') > date('now','-" + days + " day')";
        String groupBy = "strftime('%Y-%m-%d', record_on/1000,'unixepoch', 'localtime')";
        String orderBy = "record_on";

        Cursor burnrateCursor = mDB.query(mActionRecordDao.getTablename(), cols, whereClause, null, groupBy, null, orderBy);

        int countColIndex = burnrateCursor.getColumnIndex("count");
        int dateColIndex = burnrateCursor.getColumnIndex("date");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getDefault());

        if (burnrateCursor.moveToFirst() == true) {
            do {
                int count = burnrateCursor.getInt(countColIndex);
                String date = burnrateCursor.getString(dateColIndex);
                try {
                    pairs.add(new DateBurnratePair(formatter.parse(date), count));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } while (burnrateCursor.moveToNext());
        }

        return pairs;
    }


    public class DateBurnratePair implements Comparable<DateBurnratePair> {
        public Date mDate;
        public int mBurnrate;

        public DateBurnratePair(Date mDate, int mBurnrate) {
            this.mDate = mDate;
            this.mBurnrate = mBurnrate;
        }


        @Override
        public int compareTo(DateBurnratePair o) {
            return mBurnrate - o.mBurnrate;
        }
    }
}
