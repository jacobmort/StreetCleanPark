package porqueno.streetcleanpark;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoJsonLayer.GeoJsonOnFeatureClickListener, GoogleMap.OnCameraChangeListener {
	private static final String TAG = "MapsActivity";
	private static final double COORD_ADJUST_AMOUNT = 0.0000003;
	private static final float DEFAULT_LINE_WIDTH = 10.0f;
	private static final int DEFAULT_DESIRED_PARK_HOURS = 24;
	public final static int NO_TIME = Color.RED;
	public final static int LONG_TIME = Color.GREEN;
	public final static int NO_DATA = Color.BLACK;

	private GoogleMap mMap;
	private GeoJsonLayer mLayer;
	private GeoJsonFeature mLastFeatureActive;
	private Snackbar mSnackbar;
	private Map<String, List<GeoJsonFeature>> mBlockSideFeaturesLookup;
	private FeatureModel mFeatureModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		mBlockSideFeaturesLookup = new HashMap<>();
		mFeatureModel = new FeatureModel(this);
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near sf, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		// Add a marker in sf and move the camera
		LatLng sf = new LatLng(37.751019, -122.506810);
		googleMap.setOnCameraChangeListener(this);
//		try {
//			mFeatureModel.importAllData(new GeoJsonLayer(mMap, R.raw.streetsweep_latlng, this));
//			mFeatureModel.setAllGeos(new GeoJsonLayer(mMap, R.raw.streetsweep_latlng, this));
//			GeoJsonLayer layer = new GeoJsonLayer(googleMap, R.raw.outer_sunset, this);
			mLayer = new GeoJsonLayer(googleMap, new JSONObject());
//			initTheFeatures(layer);
			//mFeatureModel.getFeaturesForPoint(sf.latitude, sf.longitude);
			mLayer.setOnFeatureClickListener(this);
			mLayer.addLayerToMap();
			mMap.moveCamera(CameraUpdateFactory.newLatLng(sf));
			mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
//		} catch (java.io.IOException e) {
//			e.printStackTrace();
//		} catch (org.json.JSONException e) {
//			e.printStackTrace();
//		}
	}

	public void onFeatureClick(GeoJsonFeature feature) {
		if (mLastFeatureActive != null) {
			setFeatureHoverOffStyle(mLastFeatureActive);
		}
		mLastFeatureActive = feature;
		setFeatureHoverStyle(feature);
		if (mSnackbar == null){
			mSnackbar = Snackbar.make(findViewById(R.id.map), getToastText(FeatureModel.getUniqueKeyForBlockSide(feature)), Snackbar.LENGTH_INDEFINITE);
			mSnackbar.show();
		} else {
			mSnackbar.setText(getToastText(FeatureModel.getUniqueKeyForBlockSide(feature)));
		}
	}

	private void initTheFeatures(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			feature.setGeometry(getAdjustedGeo(feature));
			addFeatureToLookup(feature);
		}
		calcColorsForFeatures(layer);
	}

	private void initTheFeature(GeoJsonFeature feature){
		feature.setGeometry(getAdjustedGeo(feature));
		addFeatureToLookup(feature);
	}

	public void calcColorsForFeatures() {
		calcColorsForFeatures(mLayer);
	}

	private void calcColorsForFeatures(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			setFeatureColor(feature, calculateColorForFeature(FeatureModel.getUniqueKeyForBlockSide(feature)));
		}
	}

	private void addFeatureToLookup(GeoJsonFeature feature) {
		String key = FeatureModel.getUniqueKeyForBlockSide(feature);
		List<GeoJsonFeature> blockSideData = mBlockSideFeaturesLookup.get(key);
		if (blockSideData == null) {
			blockSideData = new ArrayList<>();
		}
		blockSideData.add(feature);
		mBlockSideFeaturesLookup.put(key, blockSideData);
	}
	
	private void setFeatureColor(GeoJsonFeature feature, int color) {
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(color);
		feature.setLineStringStyle(lineStyle);
	}

	private int calculateColorForFeature(String featureKey) {
		Calendar now = Calendar.getInstance();
		List<GeoJsonFeature> blockSideData = mBlockSideFeaturesLookup.get(featureKey);
		if (blockSideData != null){
			Calendar next;
			Calendar soonest = null;
			for (GeoJsonFeature feature: blockSideData) {
				next = TimeHelper.getNextOccurrence(
						now,
						FeatureModel.getWeekday(feature),
						FeatureModel.getStartHour(feature),
						FeatureModel.getStartMin(feature),
						FeatureModel.getEndHour(feature),
						FeatureModel.getEndMin(feature),
						FeatureModel.getWeekOne(feature),
						FeatureModel.getWeekTwo(feature),
						FeatureModel.getWeekThree(feature),
						FeatureModel.getWeekFour(feature)

				);

				if (soonest == null || next.getTimeInMillis() < soonest.getTimeInMillis()){
					soonest = next;
				}
			}
			return getColor(now, soonest, DEFAULT_DESIRED_PARK_HOURS);
		}else {
			Log.e(TAG, "No data for:" + featureKey);
			return NO_DATA;
		}
	}

	private GeoJsonLineString getAdjustedGeo(GeoJsonFeature feature) {
		String side = feature.getProperty("BLOCKSIDE");
		GeoJsonLineString geo = (GeoJsonLineString) feature.getGeometry();
		List<LatLng> points = geo.getCoordinates();
		List<LatLng> adjPoints = new ArrayList<LatLng>();
		if (points.size() == 2) {
			// Some features have better specification and don't need the tweaks
			double latAdj, lngAdj;
			latAdj = 1.0;
			lngAdj = 1.0;
			if (side != null) {
				if (side.equals("East")) {
					lngAdj = 1.0 - COORD_ADJUST_AMOUNT;
				} else if (side.equals("West")) {
					lngAdj = 1.0 + COORD_ADJUST_AMOUNT;
				} else if (side.equals("North")) {
					// Lat needs bigger adj than lng
					latAdj = 1.0 + COORD_ADJUST_AMOUNT * 2;
				} else if (side.equals("South")) {
					// South sides already seem off from center
					latAdj = 1.0 - COORD_ADJUST_AMOUNT;
				}
			}

			for (LatLng point : points) {
				adjPoints.add(
						new LatLng(point.latitude * latAdj, point.longitude * lngAdj)
				);
			}
		} else {
			adjPoints = points;
		}
		return new GeoJsonLineString(adjPoints);
	}

	private void setFeatureHoverStyle(GeoJsonFeature feature){
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(Color.BLUE);
		lineStyle.setWidth(DEFAULT_LINE_WIDTH * 1.5f);
		feature.setLineStringStyle(lineStyle);
	}

	private void setFeatureHoverOffStyle(GeoJsonFeature feature){
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(calculateColorForFeature(FeatureModel.getUniqueKeyForBlockSide(feature)));
		lineStyle.setWidth(DEFAULT_LINE_WIDTH);
		feature.setLineStringStyle(lineStyle);
	}

	private String getToastText(String featureKey) {
		List<GeoJsonFeature> blockSideData = mBlockSideFeaturesLookup.get(featureKey);
		String toastText = "";
		if (blockSideData != null) {
			Map<String, List<GeoJsonFeature>> clusteredByDay = FeatureModel.clusterFeaturesByTime(blockSideData);
			for (List<GeoJsonFeature> features: clusteredByDay.values()){
				String daysString = FeatureModel.getDays(features);
				GeoJsonFeature feature = features.get(0); // Clustered by time so any one works
				if (toastText.equals("")){
					// Only put streetname 1st once
					toastText +=
							feature.getProperty("STREETNAME") + " ";
				}
				toastText +=
						daysString +
								" from " +
								feature.getProperty("FROMHOUR") +
								"-" +
								feature.getProperty("TOHOUR");
				toastText += " " + getToastTextWhichWeeks(features);
			}
		}else {
			Log.e(TAG, "Block side data not found for:" + featureKey);
		}
		return toastText;
	}

	private String getToastTextWhichWeeks(List<GeoJsonFeature> features) {
		Set<String> days = new HashSet<>();
		for (GeoJsonFeature feature : features) {
			if (!FeatureModel.getWeekOne(feature)) {
				days.add("1st");
			}
			if (!FeatureModel.getWeekTwo(feature)) {
				days.add("2nd");
			}
			if (!FeatureModel.getWeekThree(feature)) {
				days.add("3rd");
			}

			if (!FeatureModel.getWeekFour(feature)) {
				days.add("4th");
			}
		}
		if (days.size() != 4 && days.size() != 0) {
			String[] daysArr = days.toArray(new String[days.size()]);
			Arrays.sort(daysArr);
			return TextUtils.join(", ", daysArr) + " of the month";
		} else {
			return "";
		}
	}

	public static int getColor(Calendar now, Calendar then, int desiredHoursToPark) {
		long hoursDiff = TimeHelper.getHoursDiff(now, then);
		if (hoursDiff < desiredHoursToPark) {
			return NO_TIME;
		} else {
			return LONG_TIME;
		}
	}

	public void addFeatureToMap(GeoJsonFeature feature){
		mLayer.addFeature(feature);
		initTheFeature(feature);
	}

	private void removeFeaturesFromMap(List<GeoJsonFeature> features) {
		for (GeoJsonFeature feature: features){
			mLayer.removeFeature(feature);
		}
	}

	public void removeFeatureFromMap(String key) {
		if (mBlockSideFeaturesLookup.containsKey(key)){
			removeFeaturesFromMap(mBlockSideFeaturesLookup.get(key));
		}
	}

	public void onCameraChange (CameraPosition position) {
		mFeatureModel.getFeaturesForPoint(position.target.latitude, position.target.longitude);
	}
}
