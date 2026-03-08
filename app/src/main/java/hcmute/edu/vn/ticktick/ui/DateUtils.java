package hcmute.edu.vn.ticktick.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for Vietnamese date formatting and date calculations.
 */
public class DateUtils {

    private static final Locale VIETNAM_LOCALE = Locale.of("vi", "VN");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", VIETNAM_LOCALE);

    /**
     * Format timestamp to dd/MM/yyyy
     */
    public static String formatDate(long timestamp) {
        if (timestamp == 0) return "";
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Get display text for a task's date
     * Shows "07:00" if today, "6 thg 9" if another day
     */
    public static String getDisplayDateOrTime(long dueDate, String dueTime) {
        if (dueDate == 0) return "";

        Calendar taskCal = Calendar.getInstance();
        taskCal.setTimeInMillis(dueDate);

        Calendar todayCal = Calendar.getInstance();

        if (isSameDay(taskCal, todayCal)) {
            // Show time if today
            return (dueTime != null && !dueTime.isEmpty()) ? dueTime : "";
        } else {
            // Show Vietnamese date like "6 thg 9"
            int day = taskCal.get(Calendar.DAY_OF_MONTH);
            int month = taskCal.get(Calendar.MONTH) + 1;
            return day + " thg " + month;
        }
    }

    /**
     * Get start of today (00:00:00)
     */
    public static long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get end of today (23:59:59.999)
     */
    public static long getEndOfToday() {
        return getStartOfToday() + 24 * 60 * 60 * 1000L;
    }

    /**
     * Get start of tomorrow
     */
    public static long getStartOfTomorrow() {
        return getStartOfToday() + 24 * 60 * 60 * 1000L;
    }

    /**
     * Get end of tomorrow
     */
    public static long getEndOfTomorrow() {
        return getStartOfTomorrow() + 24 * 60 * 60 * 1000L;
    }

    /**
     * Get start of day after tomorrow
     */
    public static long getStartOfDayAfterTomorrow() {
        return getStartOfToday() + 2 * 24 * 60 * 60 * 1000L;
    }

    /**
     * Get end of 7 days from today
     */
    public static long getEndOf7Days() {
        return getStartOfToday() + 7 * 24 * 60 * 60 * 1000L;
    }

    /**
     * Get start of current week (Monday)
     */
    public static long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (cal.getTimeInMillis() > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    /**
     * Get end of current week (Sunday 23:59:59)
     */
    public static long getEndOfWeek() {
        return getStartOfWeek() + 7 * 24 * 60 * 60 * 1000L;
    }

    /**
     * Check if two Calendar instances represent the same day
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
