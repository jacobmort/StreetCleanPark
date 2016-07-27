package porqueno.streetcleanpark;

import android.text.TextUtils;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
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
 * Created by jacob on 7/26/16.
 */
public class FeatureModel implements GeoQueryEventListener, ValueEventListener {
	private static final String TAG = "FeatureModel";
	private FirebaseDatabase mDatabase;
	private GeoFire mGeoFire;
	private GeoQuery mGeoQuery;
	private MapsActivity mActivity; // Make sure model dies with this activity otherwise leak memory
	private List<GeoJsonFeature> mNearbyFeatures;
	private int outstandingRequests;

	FeatureModel(MapsActivity mainActivity) {
		mDatabase = FirebaseDatabase.getInstance();
		mGeoFire = new GeoFire(mDatabase.getReference("geofire"));
		mActivity = mainActivity;
		mNearbyFeatures = new ArrayList<>();
		outstandingRequests = 0;
	}

	public void importAllData(GeoJsonLayer layer) {
		DatabaseReference featureRef;
		for (GeoJsonFeature feature : layer.getFeatures()) {
			String json = serialize(feature);
			String key = getUniqueKeyForBlockSide(feature);
			featureRef = mDatabase.getReference(key);
			featureRef.setValue(json);
		}
	}

	public void setAllGeos(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			GeoJsonLineString geo = (GeoJsonLineString) feature.getGeometry();
			List<LatLng> points = geo.getCoordinates();
			for (LatLng point: points) {
				mGeoFire.setLocation(getUniqueKeyForBlockSide(feature), new GeoLocation(point.latitude, point.longitude));
			}
		}
	}

	public void getFeaturesForPoint(double lat, double lng) {
		outstandingRequests = 0;
		if (mGeoQuery == null) {
			setupGeoQuery(lat, lng);
		} else {
			mGeoQuery.setCenter(new GeoLocation(lat, lng));
		}
	}

	private void setupGeoQuery(double lat, double lng) {
		mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(lat, lng), 0.5);
		mGeoQuery.addGeoQueryEventListener(this);
	}

	public void addValueListener(String key){
		outstandingRequests += 1;
		mDatabase.getReference(key).addValueEventListener(this);
	}

	public void removeValueListener(String key) {
		mDatabase.getReference(key).removeEventListener(this);
		mActivity.removeFeatureFromMap(key);
	}

	@Override
	public void onKeyEntered(String key, GeoLocation location) {
		System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
		addValueListener(key);
	}

	@Override
	public void onKeyExited(String key) {
		System.out.println(String.format("Key %s is no longer in the search area", key));
		removeValueListener(key);
	}

	@Override
	public void onKeyMoved(String key, GeoLocation location) {
		System.out.println(String.format("Key %s moved within the search area to [%f,%f]", key, location.latitude, location.longitude));
	}

	@Override
	public void onGeoQueryReady() {
		System.out.println("All initial data has been loaded and events have been fired!");
	}

	@Override
	public void onGeoQueryError(DatabaseError error) {
		System.err.println("There was an error with this query: " + error);
	}

	@Override
	public void onDataChange(DataSnapshot dataSnapshot) {
		outstandingRequests -= 1;
		GeoJsonFeature feature = deserialize(dataSnapshot.getValue(String.class));
		mActivity.addFeatureToMap(feature);
		if (outstandingRequests == 0){
			// We have all data, we can now iterate to calc colors
			mActivity.calcColorsForFeatures();
		}
	}

	@Override
	public void onCancelled(DatabaseError databaseError) {
		outstandingRequests -= 1;
		Log.w(TAG, "Failed to read value.", databaseError.toException());
	}

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
				existing = new ArrayList<>();
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
