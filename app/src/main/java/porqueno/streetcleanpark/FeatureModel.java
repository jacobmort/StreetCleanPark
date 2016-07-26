package porqueno.streetcleanpark;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;

import java.util.List;

/**
 * Created by jacob on 7/26/16.
 */
public class FeatureModel {
	private FirebaseDatabase mDatabase;
	private GeoFire mGeoFire;
	FeatureModel() {
		mDatabase = FirebaseDatabase.getInstance();
		mGeoFire = new GeoFire(mDatabase.getReference("geofire"));
	}

	public void importAllData(GeoJsonLayer layer) {
		DatabaseReference featureRef;
		for (GeoJsonFeature feature : layer.getFeatures()) {
			String json = FeatureHelper.serialize(feature);
			String key = feature.getProperty("CNN");
			featureRef = mDatabase.getReference(key);
			featureRef.setValue(json);
		}
	}

	public void setAllGeos(GeoJsonLayer layer) {

		for (GeoJsonFeature feature : layer.getFeatures()) {
			GeoJsonLineString geo = (GeoJsonLineString) feature.getGeometry();
			List<LatLng> points = geo.getCoordinates();
			for (LatLng point: points) {
				mGeoFire.setLocation(feature.getProperty("CNN"), new GeoLocation(point.latitude, point.longitude));
			}
		}
	}

	public void getFeaturesForPoint(double lat, double lng) {
		GeoQuery geoQuery = mGeoFire.queryAtLocation(new GeoLocation(lat, lng), 1);
		geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
			@Override
			public void onKeyEntered(String key, GeoLocation location) {
				System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
			}

			@Override
			public void onKeyExited(String key) {
				System.out.println(String.format("Key %s is no longer in the search area", key));
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
		});
	}
}
