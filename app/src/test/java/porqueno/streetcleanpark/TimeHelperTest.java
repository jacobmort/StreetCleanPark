package porqueno.streetcleanpark;

import org.junit.Test;
import java.util.Calendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TimeHelperTest {

	private static void checkDate(Calendar date,
								  int expectedYear,
								  int expectedMonth,
								  int expectedDay,
								  int expectedDayOfWeek,
								  int expectedHour,
								  int expectedMin) {
		assertThat(date.get(Calendar.YEAR), is(expectedYear));
		assertThat(date.get(Calendar.MONTH), is(expectedMonth));
		assertThat(date.get(Calendar.DAY_OF_MONTH), is(expectedDay));
		assertThat(date.get(Calendar.DAY_OF_WEEK), is(expectedDayOfWeek));
		assertThat(date.get(Calendar.HOUR_OF_DAY), is(expectedHour));
		assertThat(date.get(Calendar.MINUTE), is(expectedMin));
	}

	@Test
	public void getNextOccurrence_this_week() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.JULY, 21, 9, 30, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.FRIDAY, 13, 0, 15, 0);
		checkDate(nextCleaning, 2016, Calendar.JULY, 22, Calendar.FRIDAY, 13, 0);
	}

	@Test
	public void getNextOccurrence_today_later() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.JULY, 21, 9, 30, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.THURSDAY, 13, 0, 15, 0);
		checkDate(nextCleaning, 2016, Calendar.JULY, 21, Calendar.THURSDAY, 13, 0);
	}

	@Test
	public void getNextOccurrence_today_now() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.JULY, 21, 13, 30, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.THURSDAY, 13, 0, 15, 0);
		checkDate(nextCleaning, 2016, Calendar.JULY, 21, Calendar.THURSDAY, 13, 0);
	}

	@Test
	public void getNextOccurrence_today_before() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.JULY, 21, 11, 31, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.THURSDAY, 9, 30, 11, 30);
		checkDate(nextCleaning, 2016, Calendar.JULY, 28, Calendar.THURSDAY, 9, 30);
	}

	@Test
	public void getNextOccurrence_rollover_month() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.JULY, 31, 9, 30, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.MONDAY, 13, 0, 15, 0);
		checkDate(nextCleaning, 2016, Calendar.AUGUST, 1, Calendar.MONDAY, 13, 0);
	}

	@Test
	public void getNextOccurrence_rollover_year() throws Exception {
		Calendar startDate = Calendar.getInstance();
		startDate.set(2016, Calendar.DECEMBER, 31, 11, 1, 35);

		Calendar nextCleaning = TimeHelper.getNextOccurrence(startDate, Calendar.SATURDAY, 9, 0, 11, 0);
		checkDate(nextCleaning, 2017, Calendar.JANUARY, 7, Calendar.SATURDAY, 9, 0);
	}
}