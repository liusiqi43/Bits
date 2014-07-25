package utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.siqi.bits.BitsDevOpenHelper;

import java.io.File;
import java.io.IOException;

/**
 * Created by me on 7/24/14.
 */
public class DataBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        FileBackupHelper dbBackupHelper = new FileBackupHelper(this, BitsDevOpenHelper.DBNAME);
        addHelper(getPackageName() + "." + BitsDevOpenHelper.DBNAME, dbBackupHelper);

        SharedPreferencesBackupHelper sharedPrefHelper =
                new SharedPreferencesBackupHelper(this,
                        Utils.IS_BITS_ADS_SUPPORT_ENABLED,
                        Utils.IS_AUTO_ROTATE_ENABLED,
                        Utils.IS_BITSLIST_HELP_ON,
                        Utils.IS_BITSLIST_LONGPRESS_HELP_ON,
                        Utils.IS_BITSLIST_SHAKE_ON,
                        Utils.IS_FIRST_DONE,
                        Utils.IS_FIRST_LATE,
                        Utils.IS_FIRST_SKIP,
                        Utils.IS_FIRST_TASK_ADDED,
                        Utils.REWARD_HISTORY_ON_TAP_ENABLED,
                        Utils.REWARD_UNDO_ON_SHAKE_ENABLED,
                        Utils.TASKS_COUNT_LIMIT_UNLOCKED);

        addHelper(getPackageName() + ".shared_prefs", sharedPrefHelper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) {
        try {
            super.onBackup(oldState, data, newState);
            Log.d(this.getClass().getSimpleName(), "Backing up now");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public File getFilesDir() {
        File path = getDatabasePath(BitsDevOpenHelper.DBNAME);
        return path.getParentFile();
    }
}
