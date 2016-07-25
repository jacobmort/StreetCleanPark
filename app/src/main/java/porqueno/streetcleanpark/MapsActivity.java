package porqueno.streetcleanpark;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoJsonLayer.GeoJsonOnFeatureClickListener {
	private static final double COORD_ADJUST_AMOUNT = 0.0000003;
	private static final float DEFAULT_LINE_WIDTH = 10.0f;
	private static final int DEFAULT_DESIRED_PARK_HOURS = 24;
	public final static int NO_TIME = Color.RED;
	public final static int LONG_TIME = Color.GREEN;

	private GoogleMap mMap;
	private GeoJsonFeature mLastFeatureActive;
	private Snackbar mSnackbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
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

		try {
			GeoJsonLayer layer = new GeoJsonLayer(googleMap, R.raw.outer_sunset, this);
			defaultStyleTheFeatures(layer);
			layer.setOnFeatureClickListener(this);
			layer.addLayerToMap();
			mMap.moveCamera(CameraUpdateFactory.newLatLng(sf));
			mMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );
		} catch (java.io.IOException e) {
			e.printStackTrace();
		} catch (org.json.JSONException e) {
			e.printStackTrace();
		}
	}

	public void onFeatureClick(GeoJsonFeature feature) {
		if (mLastFeatureActive != null) {
			setFeatureHoverOffStyle(mLastFeatureActive);
		}
		mLastFeatureActive = feature;
		setFeatureHoverStyle(feature);
		if (mSnackbar == null){
			mSnackbar = Snackbar.make(findViewById(R.id.map), getToastText(feature), Snackbar.LENGTH_INDEFINITE);
			mSnackbar.show();
		} else {
			mSnackbar.setText(getToastText(feature));
		}
	}

	private void defaultStyleTheFeatures(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			setFeatureColor(feature, calculateColorForFeature(feature));
			feature.setGeometry(getAdjustedGeo(feature));
		}
	}
	
	private void setFeatureColor(GeoJsonFeature feature, int color) {
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(color);
		feature.setLineStringStyle(lineStyle);
	}

	private int calculateColorForFeature(GeoJsonFeature feature) {
		Calendar now = Calendar.getInstance();
		Calendar next = TimeHelper.getNextOccurrence(
				now,
				FeatureHelper.getWeekday(feature),
				FeatureHelper.getStartHour(feature),
				FeatureHelper.getStartMin(feature),
				FeatureHelper.getEndHour(feature),
				FeatureHelper.getEndMin(feature)
		);
		return getColor(now, next, DEFAULT_DESIRED_PARK_HOURS);
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
		lineStyle.setColor(calculateColorForFeature(feature));
		lineStyle.setWidth(DEFAULT_LINE_WIDTH);
		feature.setLineStringStyle(lineStyle);
	}

	private String getToastText(GeoJsonFeature feature) {
		return feature.getProperty("STREETNAME") +
				" " +
				feature.getProperty("WEEKDAY") +
				" from " +
				feature.getProperty("FROMHOUR") +
				"-" +
				feature.getProperty("TOHOUR");
	}

	public static int getColor(Calendar now, Calendar then, int desiredHoursToPark) {
		long hoursDiff = TimeHelper.getHoursDiff(now, then);
		if (hoursDiff < desiredHoursToPark) {
			return NO_TIME;
		} else {
			return LONG_TIME;
		}
	}
}
