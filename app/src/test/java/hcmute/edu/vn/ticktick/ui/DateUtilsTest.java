package hcmute.edu.vn.ticktick.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

public class DateUtilsTest {

    @Test
    public void getStartOfDay_resetsTimeComponents() {
        Calendar input = Calendar.getInstance();
        input.set(2026, Calendar.MARCH, 8, 15, 42, 11);
        input.set(Calendar.MILLISECOND, 123);

        long result = DateUtils.getStartOfDay(input.getTimeInMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(result);

        assertEquals(2026, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));
        assertEquals(0, calendar.get(Calendar.MILLISECOND));
    }

    @Test
    public void getStartOfNextDay_movesToNextMidnight() {
        Calendar input = Calendar.getInstance();
        input.set(2026, Calendar.MARCH, 8, 22, 5, 0);
        input.set(Calendar.MILLISECOND, 0);

        long result = DateUtils.getStartOfNextDay(input.getTimeInMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(result);

        assertEquals(2026, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH));
        assertEquals(9, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
    }

    @Test
    public void formatDateRange_returnsSingleDateWhenSameDay() {
        Calendar input = Calendar.getInstance();
        input.set(2026, Calendar.MARCH, 8, 0, 0, 0);
        input.set(Calendar.MILLISECOND, 0);
        long value = input.getTimeInMillis();

        assertEquals(DateUtils.formatDate(value), DateUtils.formatDateRange(value, value));
    }
}
