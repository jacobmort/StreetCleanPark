package porqueno.streetcleanpark;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoJsonLayer.GeoJsonOnFeatureClickListener, GoogleMap.OnCameraChangeListener, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleApiClient.ConnectionCallbacks, LocationListener, SeekBar.OnSeekBarChangeListener {
	private static final String TAG = "MapsActivity";
	public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1000;
	private static final double COORD_ADJUST_AMOUNT = 0.0000003;
	private static final float DEFAULT_LINE_WIDTH = 10.0f;
	private static final int DEFAULT_DESIRED_PARK_HOURS = 24;
	public final static int NO_TIME = Color.RED;
	public final static int NO_DATA = Color.BLACK;
	private final static long PAN_DEBOUNCE_THRESHOLD_MS = 1000;

	private GoogleMap mMap;
	private Snackbar mSnackbar;
	private SeekBar mSeekBar;
	private TextView mTextView;

	private ProgressBar mProgress;
	private int mDesiredParkHours;

	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;

	private GeoJsonLayer mLayer;
	private GeoJsonFeature mLastFeatureActive;

	// Aggregate features for same block side
	private Map<String, List<GeoJsonFeature>> mBlockSideFeaturesLookup;
	// Feature lookup by unique key
	private Map<String, GeoJsonFeature> mFeaturesOnMap;

	private FeatureModel mFeatureModel;
	private double previousCameraPanMs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		mProgress = (ProgressBar) findViewById(R.id.progress);
		mSeekBar = (SeekBar) findViewById(R.id.seekBar);
		mTextView = (TextView) findViewById(R.id.hours_text);
		mSeekBar.setOnSeekBarChangeListener(this);
		mBlockSideFeaturesLookup = new HashMap<>();
		mFeaturesOnMap = new HashMap<>();
		mFeatureModel = new FeatureModel(this);

		mSeekBar.setProgress(20);
		previousCameraPanMs = 0;
		mDesiredParkHours = DEFAULT_DESIRED_PARK_HOURS;

		updateHoursString();
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addApi(LocationServices.API)
				.build();
	}

	@Override
	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
					mGoogleApiClient, this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mGoogleApiClient.isConnected()) {
			startLocationUpdates();
		}
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
		googleMap.setOnMyLocationButtonClickListener(this);
//		try {
//			GeoJsonLayer layer = new GeoJsonLayer(googleMap, R.raw.streetsweep_latlng, this);
//			mFeatureModel.importAllData(layer);
//			mFeatureModel.setAllGeos(layer);
			mLayer = new GeoJsonLayer(googleMap, new JSONObject());
			mLayer.setOnFeatureClickListener(this);
			mLayer.addLayerToMap();
			mMap.moveCamera(CameraUpdateFactory.newLatLng(sf));
			mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
//		} catch (java.io.IOException e) {
//			e.printStackTrace();
//		} catch (org.json.JSONException e) {
//			e.printStackTrace();
//		}
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			promptForLocationPermissions();
		}else {
			mMap.setMyLocationEnabled(true);
		}
	}

	public void onFeatureClick(GeoJsonFeature feature) {
		if (mLastFeatureActive != null) {
			setFeatureHoverOffStyle(mLastFeatureActive);
		}
		mLastFeatureActive = feature;
		setFeatureHoverStyle(feature);
		showSnackBar(feature);
	}

	private void showSnackBar(GeoJsonFeature feature) {
		if (mSnackbar == null){
			mSnackbar = Snackbar.make(findViewById(R.id.map), getToastText(FeatureModel.getUniqueKeyForBlockSide(feature)), Snackbar.LENGTH_INDEFINITE);
			mSnackbar.show();
		} else {
			mSnackbar.setText(getToastText(FeatureModel.getUniqueKeyForBlockSide(feature)));
		}
	}

	private void initTheFeature(GeoJsonFeature feature){
		feature.setGeometry(getAdjustedGeo(feature));
		addFeatureToLookups(feature);
	}

	public void calcColorsForFeatures() {
		calcColorsForFeatures(mLayer);
		hideProgressBar();
	}

	private void calcColorsForFeatures(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			setFeatureColor(feature, calculateColorForFeature(FeatureModel.getUniqueKeyForBlockSide(feature)));
		}
	}

	public void hideProgressBar() {
		mProgress.setVisibility(View.INVISIBLE);
	}

	private void addFeatureToLookups(GeoJsonFeature feature) {
		String key = FeatureModel.getUniqueKeyForBlockSide(feature);
		List<GeoJsonFeature> blockSideData = mBlockSideFeaturesLookup.get(key);
		if (blockSideData == null) {
			blockSideData = new ArrayList<>();
		}
		blockSideData.add(feature);
		mBlockSideFeaturesLookup.put(key, blockSideData);
		mFeaturesOnMap.put(FeatureModel.getUniqueKey(feature), feature);
	}
	
	private void setFeatureColor(GeoJsonFeature feature, int color) {
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(color);
		feature.setLineStringStyle(lineStyle);
		feature.notifyObservers();
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
			return getColor(now, soonest, mDesiredParkHours);
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
			return getColorForGoodStreet(hoursDiff, desiredHoursToPark);
		}
	}

	public static int getColorForGoodStreet(long hoursDiff, int desiredHoursToPark) {
		double ratio = hoursDiff / (desiredHoursToPark * 2);
		Double greenAmt = 255 * ratio;
		if (greenAmt > 255){
			greenAmt = 255d;
		} else if (greenAmt < 100){
			greenAmt = 100d;
		}
		return Color.rgb(0, greenAmt.intValue(), 0);
	}

	public void addFeatureToMap(GeoJsonFeature feature){
		if (!mFeaturesOnMap.containsKey(FeatureModel.getUniqueKey(feature))){
			initTheFeature(feature);
			mLayer.addFeature(feature);
		} else {
			Log.i(TAG,"tried to add duplicate feature");
		}
	}

	private void removeFeatureFromMap(GeoJsonFeature feature) {
		mLayer.removeFeature(feature);
		mBlockSideFeaturesLookup.remove(FeatureModel.getUniqueKeyForBlockSide(feature));
		mFeaturesOnMap.remove(FeatureModel.getUniqueKey(feature));
	}

	public void removeFeatureFromMap(String key) {
		if (mFeaturesOnMap.containsKey(key)){
			removeFeatureFromMap(mFeaturesOnMap.get(key));
		}
	}

	public void onCameraChange (CameraPosition position) {
		double updatimeMs = SystemClock.uptimeMillis();
		if ((updatimeMs - previousCameraPanMs) > PAN_DEBOUNCE_THRESHOLD_MS){
			previousCameraPanMs = updatimeMs;
			mProgress.setVisibility(View.VISIBLE);
			mFeatureModel.getFeaturesForPoint(position.target.latitude, position.target.longitude);
		}
	}

	public void promptForLocationPermissions() {
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
				mSnackbar.show();
				Snackbar.make(findViewById(R.id.map), "If you would like the map to follow you grant location permissions",
						Snackbar.LENGTH_SHORT).setAction("OK", new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						// Request the permission
						ActivityCompat.requestPermissions(MapsActivity.this,
								new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
								MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
					}
				}).show();
			} else {
				ActivityCompat.requestPermissions(this,
						new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
						MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					try{
						mMap.setMyLocationEnabled(true);
					} catch (SecurityException e){
						Log.w(TAG, "Granted location permissions but don't have somehow");
					}
				}
			}
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		return false; // Let maps handle it
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		startLocationUpdates();
		try {
			mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
					mGoogleApiClient);
		} catch (SecurityException e){
			Log.i(TAG, "location permissions not accepted");
		}

		if (mLastLocation != null) {
			mMap.moveCamera(CameraUpdateFactory.newLatLng(
					new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())
			));
		}
	}

	@Override
	public void onConnectionSuspended(int val) {
	}

	@Override
	public void onLocationChanged(Location location) {
		mLastLocation = location;
		mMap.moveCamera(CameraUpdateFactory.newLatLng(
				new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())
		));
		mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
	}

	private void startLocationUpdates(){
		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		locationRequest.setInterval(5000);
		try{
			LocationServices.FusedLocationApi.requestLocationUpdates(
					mGoogleApiClient, locationRequest, this);
		} catch (SecurityException e) {
			Log.i(TAG, "did not provide location permissions");
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar){
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		int newHours = (int) Math.round(progress * 1.2);
		if (newHours == 0) {
			newHours = 1;
		}
		mDesiredParkHours = newHours;
		updateHoursString();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar){
		mProgress.setVisibility(View.VISIBLE);
		calcColorsForFeatures();
	}

	private void updateHoursString(){
		mTextView.setText(
				Html.fromHtml(
				getString(R.string.hours_desc, mDesiredParkHours)
				)
		);
	}
}
