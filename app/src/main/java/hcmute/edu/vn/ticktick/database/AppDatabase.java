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

import hcmute.edu.vn.ticktick.diary.DiaryDao;
import hcmute.edu.vn.ticktick.diary.DiaryEntry;

@Database(entities = {
        Task.class,
        Category.class,
        TaskReminder.class,
        SchoolCalendarEventEntity.class,
        SchoolCalendarTaskLink.class,
        DiaryEntry.class
}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract TaskReminderDao taskReminderDao();
    public abstract SchoolCalendarDao schoolCalendarDao();
    public abstract SchoolCalendarTaskLinkDao schoolCalendarTaskLinkDao();
    public abstract DiaryDao diaryDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

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

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `school_calendar_task_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` TEXT, `taskId` INTEGER NOT NULL, `syncTimestamp` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_school_calendar_task_links_eventId` ON `school_calendar_task_links` (`eventId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_school_calendar_task_links_taskId` ON `school_calendar_task_links` (`taskId`)");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `diary_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `imagePath` TEXT, `caption` TEXT, `createdAt` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "ticktick_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
