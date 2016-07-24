package porqueno.streetcleanpark;

import android.graphics.Color;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by jacob on 7/20/16.
 */
public class StreetColor {
	public final static int NO_TIME = Color.RED;
	public final static int LONG_TIME = Color.GREEN;

	public static int getColor(Calendar now, Calendar then, int desiredHoursToPark) {
		 long hoursDiff = getHoursDiff(now, then);
		if (hoursDiff < desiredHoursToPark) {
			return NO_TIME;
		} else {
			return LONG_TIME;
		}
	}

	public static Calendar getNextOccurrence(Calendar now, int cleaningWeekday, int startHour, int startMinute) {
		now = (Calendar) now.clone();
		int daysDiff = cleaningWeekday - now.get(Calendar.DAY_OF_WEEK);
		if (daysDiff < 0) {
			// We're past the day of week where cleaning happened
			now = advanceDayToNextWeek(now, cleaningWeekday);
		} else if (daysDiff > 0) {
			// Cleaning has yet to happen this week
			now.add(Calendar.DAY_OF_YEAR, daysDiff);
		} else if (isTimePastStarting(now, startHour, startMinute)) {
			now = advanceDayToNextWeek(now, cleaningWeekday);
		}
		now.set(Calendar.HOUR_OF_DAY, startHour);
		now.set(Calendar.MINUTE, startMinute);
		return now;
	}

	public static long getHoursDiff(Calendar now, Calendar then){
		return TimeUnit.MILLISECONDS.toHours(then.getTimeInMillis() - now.getTimeInMillis());
	}

	private static boolean isTimePastStarting(Calendar time, int startHour, int startMinute) {
		int hour = time.get(Calendar.HOUR_OF_DAY);
		if (hour > startHour) {
			return true;
		} else if (hour == startHour && time.get(Calendar.MINUTE) >= startMinute) {
			return true;
		} else {
			return false;
		}
	}

	private static Calendar advanceDayToNextWeek(Calendar now, int desiredWeekday) {
		int days = 7 - Math.abs(now.get(Calendar.DAY_OF_WEEK) - desiredWeekday);
		now.add(Calendar.DAY_OF_YEAR, days);
		return now;
	}
}
