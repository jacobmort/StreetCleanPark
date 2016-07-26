package porqueno.streetcleanpark;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.geojson.GeoJsonPointStyle;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import porqueno.streetcleanpark.serialize.GeoJsonFeatureInstanceCreator;
import porqueno.streetcleanpark.serialize.GeoJsonGeometryDeserializer;
import porqueno.streetcleanpark.serialize.GeoJsonLineStringStyleSerializer;
import porqueno.streetcleanpark.serialize.GeoJsonPointStyleSerializer;
import porqueno.streetcleanpark.serialize.GeoJsonPolygonStyleSerializer;

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

	public static boolean getWeekOne(GeoJsonFeature feature) {
		return feature.getProperty("WEEK1OFMON").equals("Y");
	}

	public static boolean getWeekTwo(GeoJsonFeature feature) {
		return feature.getProperty("WEEK2OFMON").equals("Y");
	}

	public static boolean getWeekThree(GeoJsonFeature feature) {
		return feature.getProperty("WEEK3OFMON").equals("Y");
	}

	public static boolean getWeekFour(GeoJsonFeature feature) {
		return feature.getProperty("WEEK4OFMON").equals("Y");
	}

	public static boolean anyWeeksOff(GeoJsonFeature feature){
		return !(getWeekOne(feature) && getWeekTwo(feature) && getWeekThree(feature) && getWeekFour(feature));
	}

	public static int getHour(String time) {
		return Integer.parseInt(time.substring(0, 2));
	}

	public static int getMin(String time) {
		return Integer.parseInt(time.substring(3, 5));
	}

	public static String getUniqueKeyForBlockSide(GeoJsonFeature feature) {
		return feature.getProperty("CNN") + "-" + feature.getProperty("BLOCKSIDE");
	}

	public static Map<String,List<GeoJsonFeature>> clusterFeaturesByDay(List<GeoJsonFeature> features) {
		Map<String, List<GeoJsonFeature>> clustered = new HashMap<>();
		for (GeoJsonFeature feature : features) {
			String day = feature.getProperty("WEEKDAY");
			List<GeoJsonFeature> existing = clustered.get(day);
			if (existing == null){
				existing = new ArrayList<GeoJsonFeature>();
			}
			existing.add(feature);
			clustered.put(day, existing);
		}
		return clustered;
	}

	public static Map<String,List<GeoJsonFeature>> clusterFeaturesByTime(List<GeoJsonFeature> features) {
		Map<String, List<GeoJsonFeature>> clustered = new HashMap<>();
		for (GeoJsonFeature feature : features) {
			String time = feature.getProperty("FROMHOUR") + "-" + feature.getProperty("TOHOUR");
			List<GeoJsonFeature> existing = clustered.get(time);
			if (existing == null){
				existing = new ArrayList<GeoJsonFeature>();
			}
			existing.add(feature);
			clustered.put(time, existing);
		}
		return clustered;
	}

	public static String getDays(List<GeoJsonFeature> features){
		Set<String> days= new HashSet<>();
		for (GeoJsonFeature feature: features){
			days.add(feature.getProperty("WEEKDAY"));
		}
		if (days.size() == 5 && !(days.contains("Sun") || days.contains("Sat"))){
			return "Weekdays";
		} else {
			return TextUtils.join(", ", days.toArray());
		}
	}

	public static String serialize(GeoJsonFeature feature) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(GeoJsonLineStringStyle.class, new GeoJsonLineStringStyleSerializer());
		builder.registerTypeAdapter(GeoJsonPointStyle.class, new GeoJsonPointStyleSerializer());
		builder.registerTypeAdapter(GeoJsonPolygonStyle.class, new GeoJsonPolygonStyleSerializer());
		Gson gson = builder.create();
		return gson.toJson(feature);
	}

	public static GeoJsonFeature deserialize(String json) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(GeoJsonFeature.class, new GeoJsonFeatureInstanceCreator());
		//builder.registerTypeAdapter(GeoJsonGeometry.class, new GeoJsonGeometryInstanceCreator());
		builder.registerTypeAdapter(GeoJsonGeometry.class, new GeoJsonGeometryDeserializer());
		Gson gson = builder.create();
		return gson.fromJson(json, GeoJsonFeature.class);
	}
}
