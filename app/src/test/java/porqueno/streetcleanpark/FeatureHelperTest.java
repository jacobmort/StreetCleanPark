package porqueno.streetcleanpark;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jacob on 7/23/16.
 */
public class FeatureHelperTest {

	@Test
	public void getStartHour() throws Exception {
		assertThat(FeatureHelper.getStartHour("09:00"), is(9));
	}

	@Test
	public void getStartHour_miliatary() throws Exception {
		assertThat(FeatureHelper.getStartHour("13:00"), is(13));
	}

	@Test
	public void getStartMin() throws Exception {
		assertThat(FeatureHelper.getStartMin("09:00"), is(0));
	}

	@Test
	public void getStartMin_miliatary() throws Exception {
		assertThat(FeatureHelper.getStartMin("13:30"), is(30));
	}
}
