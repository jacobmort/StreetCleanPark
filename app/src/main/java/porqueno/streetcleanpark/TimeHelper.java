package porqueno.streetcleanpark;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by jacob on 7/20/16.
 */
public class TimeHelper {
	public static Calendar getNextOccurrence(Calendar now, int cleaningWeekday, int startHour, int startMinute, int endHour, int endMinute) {
		now = (Calendar) now.clone();
		int daysDiff = cleaningWeekday - now.get(Calendar.DAY_OF_WEEK);
		if (daysDiff < 0) {
			// We're past the day of week where cleaning happened
			now = advanceDayToNextWeek(now, cleaningWeekday);
		} else if (daysDiff > 0) {
			// Cleaning has yet to happen this week
			now.add(Calendar.DAY_OF_YEAR, daysDiff);
		} else {
			// We're in today
			// Already happened today
			if (isTimePast(now, endHour, endMinute)){
				now = advanceDayToNextWeek(now, cleaningWeekday);
			}
		}
		now.set(Calendar.HOUR_OF_DAY, startHour);
		now.set(Calendar.MINUTE, startMinute);
		return now;
	}

	public static long getHoursDiff(Calendar now, Calendar then){
		return TimeUnit.MILLISECONDS.toHours(then.getTimeInMillis() - now.getTimeInMillis());
	}

	private static boolean isTimePast(Calendar time, int hour, int minute) {
		int timeHour = time.get(Calendar.HOUR_OF_DAY);
		if (timeHour > hour) {
			return true;
		} else if (timeHour == hour && time.get(Calendar.MINUTE) >= minute) {
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
