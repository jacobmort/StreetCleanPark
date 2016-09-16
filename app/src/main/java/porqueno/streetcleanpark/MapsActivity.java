package porqueno.streetcleanpark;

import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

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
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import porqueno.streetcleanpark.databinding.MapsActivityBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
		GeoJsonLayer.GeoJsonOnFeatureClickListener,
		GoogleMap.OnCameraIdleListener,
		ActivityCompat.OnRequestPermissionsResultCallback,
		GoogleMap.OnMyLocationButtonClickListener,
		GoogleApiClient.ConnectionCallbacks,
		LocationListener,
		SeekBar.OnSeekBarChangeListener,
		FeatureModelInterface {
	private static final String TAG = "MapsActivity";
	private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1000;
	private static final float DEFAULT_LINE_WIDTH = 10.0f;
	private static final int DEFAULT_DESIRED_PARK_HOURS = 24;
	private final static long PAN_DEBOUNCE_THRESHOLD_MS = 1000;

	private GoogleMap mMap;
	private MapsActivityBinding mBinding;
	private Snackbar mSnackbar;
	private int mDesiredParkHours;
	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;

	private GeoJsonLayer mLayer;
	private GeoJsonFeature mLastFeatureActive;

	private GeoJsonFeatures mGeoJsonFeatures;

	private FeatureModel mFeatureModel;
	private double mMsWhenPreviousCameraPan;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, R.layout.maps_activity);
		mBinding.seekBar.setOnSeekBarChangeListener(this);
		mBinding.seekBar.setProgress(20);

		mDesiredParkHours = DEFAULT_DESIRED_PARK_HOURS;
		mGeoJsonFeatures = new GeoJsonFeatures();
		mFeatureModel = new FeatureModel(this);
		mMsWhenPreviousCameraPan = 0;
		mBinding.setHoursToPark(String.valueOf(mDesiredParkHours));

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
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mGoogleApiClient.disconnect();
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
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		mMap.setOnCameraIdleListener(this);
		mMap.setOnMyLocationButtonClickListener(this);
		mLayer = new GeoJsonLayer(googleMap, new JSONObject());
		mLayer.setOnFeatureClickListener(this);
		mLayer.addLayerToMap();
		moveMapToSF(mMap);
		setupLocationWatch(mMap);
	}

	private void moveMapToSF(GoogleMap map){
		// Add a marker in sf and move the camera
		LatLng sf = new LatLng(37.7749, -122.4194);
		map.moveCamera(CameraUpdateFactory.newLatLng(sf));
		map.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
	}

	// Note: have to turn on write in Firebase first
	private void saveSweepData() {
		try{
			GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.streetsweep_latlng, this);
			mFeatureModel.writeAllGeoData(layer);
		} catch (java.io.IOException | org.json.JSONException e) {
			e.printStackTrace();
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

	private void addFeaturesToMap(Collection<GeoJsonFeature> features){
		for (GeoJsonFeature feature : features){
			mLayer.addFeature(feature);
		}
	}

	private void setFeatureColors() {
		calcColorsForLayer(mGeoJsonFeatures.getAllFeatures(), mDesiredParkHours);
		hideProgressBar();
	}

	private void calcColorsForLayer(Collection<GeoJsonFeature> features, int desiredParkHours) {
		for (GeoJsonFeature feature : features) {
			setFeatureColor(
					feature,
					mGeoJsonFeatures.calculateColorForFeature(feature, desiredParkHours)
			);
		}
	}


	private void initFeature(GeoJsonFeature feature){
		if (!mGeoJsonFeatures.featuresLookupContains(feature)){
			mGeoJsonFeatures.initTheFeature(feature);
		} else {
			Log.i(TAG,"tried to add duplicate feature");
		}
	}

	private void removeFeatureFromMap(GeoJsonFeature feature) {
		mLayer.removeFeature(feature);
		mGeoJsonFeatures.removeFeatureFromLookups(feature);
	}

	private void removeFeatureFromMap(String key) {
		if (mGeoJsonFeatures.featuresLookupContains(key)){
			removeFeatureFromMap(mGeoJsonFeatures.getFeatureFromLookup(key));
		}
	}

	public void onCameraIdle () {
		double updatimeMs = SystemClock.uptimeMillis();
		if ((updatimeMs - mMsWhenPreviousCameraPan) > PAN_DEBOUNCE_THRESHOLD_MS){
			CameraPosition position = mMap.getCameraPosition();
			mMsWhenPreviousCameraPan = updatimeMs;
			mBinding.progress.setVisibility(View.VISIBLE);
			mFeatureModel.getFeaturesForPoint(position.target.latitude, position.target.longitude);
		}
	}

	// UI Elements
	private void showSnackBar(GeoJsonFeature feature) {
		if (mSnackbar == null){
			mSnackbar = Snackbar.make(findViewById(R.id.map), getToastText(feature), Snackbar.LENGTH_INDEFINITE);
			mSnackbar.show();
		} else {
			mSnackbar.setText(getToastText(feature));
		}
	}

	private void hideProgressBar() {
		mBinding.progress.setVisibility(View.INVISIBLE);
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

	private String getToastText(GeoJsonFeature feature) {
		List<GeoJsonFeature> blockSideData = mGeoJsonFeatures.getBlockSideFeatures(feature);
		String toastText = "";
		if (blockSideData != null) {
			Map<String, List<GeoJsonFeature>> clusteredByDay = FeatureModel.clusterFeaturesByTime(blockSideData);
			for (List<GeoJsonFeature> features: clusteredByDay.values()){
				String daysString = FeatureModel.getDays(features);
				GeoJsonFeature firstBlocksideFeature = features.get(0); // Clustered by time so any one works
				if (toastText.equals("")){
					// Only put streetname 1st once
					toastText +=
							firstBlocksideFeature.getProperty("STREETNAME") + " ";
				}
				toastText +=
						daysString +
								" from " +
								firstBlocksideFeature.getProperty("FROMHOUR") +
								"-" +
								firstBlocksideFeature.getProperty("TOHOUR");
				toastText += " " + getToastTextWhichWeeks(features);
			}
		}else {
			Log.e(TAG, "Block side data not found for:" + feature.getId());
		}
		return toastText;
	}

	private static void setFeatureColor(GeoJsonFeature feature, int color) {
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(color);
		feature.setLineStringStyle(lineStyle);
	}

	private void setFeatureHoverStyle(GeoJsonFeature feature){
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(Color.BLUE);
		lineStyle.setWidth(DEFAULT_LINE_WIDTH * 1.5f);
		feature.setLineStringStyle(lineStyle);
	}

	private void setFeatureHoverOffStyle(GeoJsonFeature feature){
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(mGeoJsonFeatures.calculateColorForFeature(feature, mDesiredParkHours));
		lineStyle.setWidth(DEFAULT_LINE_WIDTH);
		feature.setLineStringStyle(lineStyle);
	}

	// Location handlers
	private void setupLocationWatch(GoogleMap map){
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_COARSE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			promptForLocationPermissions();
		}else {
			map.setMyLocationEnabled(true);
		}
	}

	private void promptForLocationPermissions() {
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
										   @NonNull String permissions[], @NonNull int[] grantResults) {
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

//		if (mLastLocation != null) {
//			mMap.moveCamera(CameraUpdateFactory.newLatLng(
//					new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())
//			));
//		}
	}

	@Override
	public void onConnectionSuspended(int val) {}

	@Override
	public void onLocationChanged(Location location) {
		mLastLocation = location;
//		mMap.moveCamera(CameraUpdateFactory.newLatLng(
//				new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())
//		));
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

	// SeekBar handlers
	@Override
	public void onStartTrackingTouch(SeekBar seekBar){}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
		int newHours = (int) Math.round(progress * 1.2);
		if (newHours == 0) {
			newHours = 1;
		}
		mDesiredParkHours = newHours;
		mBinding.setHoursToPark(String.valueOf(mDesiredParkHours));
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar){
		mBinding.progress.setVisibility(View.VISIBLE);
		setFeatureColors();
	}

	// FeatureModelInterface
	public void featureFound(GeoJsonFeature feature){
		initFeature(feature);
	}

	public void featureLeft(String featureKey){
		removeFeatureFromMap(featureKey);
	}
	public void doneFetching(){
		setFeatureColors();
		addFeaturesToMap(mGeoJsonFeatures.getFeaturesNotOnMap());
		mGeoJsonFeatures.clearFeaturesNotOnMap();
	}
}
