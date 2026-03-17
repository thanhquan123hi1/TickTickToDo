package hcmute.edu.vn.ticktick.ui;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;

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
     * Format a date range for section titles.
     */
    public static String formatDateRange(long startTimestamp, long endTimestamp) {
        if (startTimestamp == 0 || endTimestamp == 0) return "";

        String startText = formatDate(startTimestamp);
        String endText = formatDate(endTimestamp);
        return startText.equals(endText) ? startText : startText + " - " + endText;
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
     * Get start of a specific day (00:00:00.000 in local time).
     */
    public static long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of the next day for a specific timestamp.
     */
    public static long getStartOfNextDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getStartOfDay(timestamp));
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return cal.getTimeInMillis();
    }

    /**
     * Get start of today (00:00:00)
     */
    public static long getStartOfToday() {
        return getStartOfDay(System.currentTimeMillis());
    }

    /**
     * Get the exclusive end of today (start of tomorrow)
     */
    public static long getEndOfToday() {
        return getStartOfNextDay(System.currentTimeMillis());
    }

    /**
     * Get start of tomorrow
     */
    public static long getStartOfTomorrow() {
        return getEndOfToday();
    }

    /**
     * Get the exclusive end of tomorrow (start of the following day)
     */
    public static long getEndOfTomorrow() {
        return getStartOfNextDay(getStartOfTomorrow());
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
     * Get the exclusive end of current week (start of next week)
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

    public static long getDueDateTimeMillis(long dueDate, String dueTime) {
        if (dueDate <= 0) {
            return 0L;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dueDate);

        int hour = 9;
        int minute = 0;
        if (dueTime != null && dueTime.matches("\\d{2}:\\d{2}")) {
            String[] parts = dueTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static String formatReminderOffset(Context context, int minutesBefore) {
        if (minutesBefore % (24 * 60) == 0) {
            int days = minutesBefore / (24 * 60);
            return context.getString(R.string.reminder_days_before, days);
        }
        if (minutesBefore % 60 == 0) {
            int hours = minutesBefore / 60;
            return context.getString(R.string.reminder_hours_before, hours);
        }
        return context.getString(R.string.reminder_minutes_before, minutesBefore);
    }
}
