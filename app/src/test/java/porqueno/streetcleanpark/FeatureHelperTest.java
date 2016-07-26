package porqueno.streetcleanpark;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLineString;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	@Test
	public void getJsonString_serialize_and_deserialize() throws Exception {
		String expected = "{ \"type\": \"Feature\", \"properties\": { \"CNN\": \"9672000\", \"WEEKDAY\": \"Tues\", \"BLOCKSIDE\": \"North\", \"BLOCKSWEEP\": \"1637569\", \"CNNRIGHTLE\": \"R\", \"CORRIDOR\": \"Noriega St\", \"FROMHOUR\": \"07:00\", \"TOHOUR\": \"08:00\", \"HOLIDAYS\": \"N\", \"WEEK1OFMON\": \"Y\", \"WEEK2OFMON\": \"N\", \"WEEK3OFMON\": \"Y\", \"WEEK4OFMON\": \"N\", \"WEEK5OFMON\": \"N\", \"LF_FADD\": \"3001\", \"LF_TOADD\": \"3099\", \"RT_TOADD\": \"3098\", \"RT_FADD\": \"3000\", \"STREETNAME\": \"NORIEGA ST\", \"ZIP_CODE\": \"94122\", \"NHOOD\": \"Outer Sunset\", \"DISTRICT\": null }, \"geometry\": { \"type\": \"LineString\", \"coordinates\": [ [ -122.495760288740485, 37.753390259911853 ], [ -122.496834942777468, 37.753342819870248 ] ] } }";
		HashMap<String, String> properties = new HashMap<>();
		properties.put("CNN", "9672000");
		properties.put("WEEKDAY", "Tues");
		properties.put("BLOCKSIDE", "North");
		properties.put("BLOCKSWEEP", "1637569");
		properties.put("CNNRIGHTLE", "R");
		properties.put("CORRIDOR", "Noriega St");
		properties.put("FROMHOUR", "07:00");
		properties.put("TOHOUR", "08:00");
		properties.put("HOLIDAYS", "N");
		properties.put("WEEK1OFMON", "Y");
		properties.put("WEEK2OFMON", "N");
		properties.put("WEEK3OFMON", "Y");
		properties.put("WEEK4OFMON", "N");
		properties.put("WEEK5OFMON", "N");
		properties.put("LF_FADD", "3001");
		properties.put("LF_TOADD", "3099");
		properties.put("RT_TOADD", "3098");
		properties.put("RT_FADD", "3000");
		properties.put("STREETNAME", "NORIEGA ST");
		properties.put("ZIP_CODE", "94122");
		properties.put("DISTRICT", null);

		List<LatLng> points = new ArrayList<>();
		points.add(
				new LatLng(37.753390259911853, -122.495760288740485)
		);
		points.add(
				new LatLng(37.753342819870248, -122.496834942777468)
		);
		GeoJsonLineString lineString = new GeoJsonLineString(points);

		GeoJsonFeature feature = new GeoJsonFeature(lineString, null, properties, null);

		String json = FeatureHelper.serialize(feature);

		GeoJsonFeature thawedFeature = FeatureHelper.deserialize(json);

		for (String key : thawedFeature.getPropertyKeys()) {
			assertThat(feature.getProperty(key), is(thawedFeature.getProperty(key)));
		}

		GeoJsonLineString thawedGeo = (GeoJsonLineString) thawedFeature.getGeometry();
		GeoJsonLineString featureGeo = (GeoJsonLineString) feature.getGeometry();
		assertThat(thawedGeo.toString().replaceAll("\\s",""), is(featureGeo.toString().replaceAll("\\s","")));

	}
}
