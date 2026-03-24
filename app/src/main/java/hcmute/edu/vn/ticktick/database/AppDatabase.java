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

@Database(entities = {
        Task.class,
        Category.class,
        TaskReminder.class,
        SchoolCalendarEventEntity.class,
        SchoolCalendarTaskLink.class
}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract TaskReminderDao taskReminderDao();
    public abstract SchoolCalendarDao schoolCalendarDao();
    public abstract SchoolCalendarTaskLinkDao schoolCalendarTaskLinkDao();

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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `school_calendar_events` ("
                    + "`uid` TEXT NOT NULL, "
                    + "`title` TEXT, "
                    + "`description` TEXT, "
                    + "`category` TEXT, "
                    + "`startTimeMillis` INTEGER NOT NULL, "
                    + "`endTimeMillis` INTEGER NOT NULL, "
                    + "`referenceTimeMillis` INTEGER NOT NULL, "
                    + "`lastModifiedMillis` INTEGER NOT NULL, "
                    + "`syncedAtMillis` INTEGER NOT NULL, "
                    + "`sourceUrl` TEXT, "
                    + "PRIMARY KEY(`uid`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_school_calendar_events_referenceTimeMillis` ON `school_calendar_events` (`referenceTimeMillis`)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `tasks` ADD COLUMN `linkedSchoolEventUid` TEXT");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("UPDATE `tasks` SET `linkedSchoolEventUid` = NULL "
                    + "WHERE `linkedSchoolEventUid` IS NOT NULL AND `linkedSchoolEventUid` != '' "
                    + "AND `id` NOT IN ("
                    + "SELECT MIN(`id`) FROM `tasks` "
                    + "WHERE `linkedSchoolEventUid` IS NOT NULL AND `linkedSchoolEventUid` != '' "
                    + "GROUP BY `linkedSchoolEventUid`)"
            );

            db.execSQL("CREATE TABLE IF NOT EXISTS `school_calendar_task_links` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`schoolEventUid` TEXT NOT NULL, "
                    + "`taskId` INTEGER NOT NULL, "
                    + "`syncMode` TEXT, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "`updatedAt` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_school_calendar_task_links_schoolEventUid` "
                    + "ON `school_calendar_task_links` (`schoolEventUid`)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_school_calendar_task_links_taskId` "
                    + "ON `school_calendar_task_links` (`taskId`)");

            db.execSQL("INSERT OR IGNORE INTO `school_calendar_task_links`("
                    + "`schoolEventUid`, `taskId`, `syncMode`, `createdAt`, `updatedAt`) "
                    + "SELECT `linkedSchoolEventUid`, `id`, 'MANUAL_EXPORT', `createdAt`, `createdAt` "
                    + "FROM `tasks` WHERE `linkedSchoolEventUid` IS NOT NULL AND `linkedSchoolEventUid` != ''");

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_linkedSchoolEventUid` "
                    + "ON `tasks` (`linkedSchoolEventUid`)");
        }
    };
}
