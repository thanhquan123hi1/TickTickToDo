package hcmute.edu.vn.ticktick.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Task.class, Category.class, TaskReminder.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract TaskReminderDao taskReminderDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ticktick_database")
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Pre-populate with default Vietnamese categories
            databaseWriteExecutor.execute(() -> {
                CategoryDao categoryDao = INSTANCE.categoryDao();
                categoryDao.insert(new Category("Công việc", "ic_work"));
                categoryDao.insert(new Category("Mục tiêu học tập", "ic_study"));
                categoryDao.insert(new Category("Kế hoạch du lịch", "ic_travel"));
                categoryDao.insert(new Category("Việc hàng ngày", "ic_list"));
                categoryDao.insert(new Category("Việc vặt", "ic_list"));
            });
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `task_reminders` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`taskId` INTEGER NOT NULL, "
                    + "`minutesBefore` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_reminders_taskId` ON `task_reminders` (`taskId`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_reminders_taskId_minutesBefore` ON `task_reminders` (`taskId`, `minutesBefore`)");
        }
    };
}
