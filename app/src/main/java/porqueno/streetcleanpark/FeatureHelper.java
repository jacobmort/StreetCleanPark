package porqueno.streetcleanpark;

import com.google.maps.android.geojson.GeoJsonFeature;

import java.util.Calendar;

/**
 * Created by jacob on 7/23/16.
 */
public class FeatureHelper {

	public static int getWeekday(GeoJsonFeature feature) {
		String weekday =  feature.getProperty("WEEKDAY");
		switch (weekday){
			case "Mon":
				return Calendar.MONDAY;
			case "Tues":
				return Calendar.TUESDAY;
			case "Wed":
				return Calendar.WEDNESDAY;
			case "Thu":
				return Calendar.THURSDAY;
			case "Fri":
				return Calendar.FRIDAY;
			case "Sat":
				return Calendar.SATURDAY;
			case "Sun":
				return Calendar.SUNDAY;
			default:
				return -1;
		}
	}

	public static int getStartHour(GeoJsonFeature feature) {
		return getHour(feature.getProperty("FROMHOUR"));
	}

	public static int getEndHour(GeoJsonFeature feature) {
		return getHour(feature.getProperty("TOHOUR"));
	}

	public static int getStartMin(GeoJsonFeature feature) {
		return getMin(feature.getProperty("FROMHOUR"));
	}

	public static int getEndMin(GeoJsonFeature feature) {
		return getMin(feature.getProperty("TOHOUR"));
	}

	public static int getHour(String time) {
		return Integer.parseInt(time.substring(0, 2));
	}

	public static int getMin(String time) {
		return Integer.parseInt(time.substring(3, 5));
	}


}
