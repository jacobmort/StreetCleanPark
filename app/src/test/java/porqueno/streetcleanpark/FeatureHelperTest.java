package porqueno.streetcleanpark;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jacob on 7/23/16.
 */
public class FeatureHelperTest {

	@Test
	public void getHour() throws Exception {
		assertThat(FeatureHelper.getHour("09:00"), is(9));
	}

	@Test
	public void getHour_miliatary() throws Exception {
		assertThat(FeatureHelper.getHour("13:00"), is(13));
	}

	@Test
	public void getMin() throws Exception {
		assertThat(FeatureHelper.getMin("09:00"), is(0));
	}

	@Test
	public void getMin_miliatary() throws Exception {
		assertThat(FeatureHelper.getMin("13:30"), is(30));
	}
}
