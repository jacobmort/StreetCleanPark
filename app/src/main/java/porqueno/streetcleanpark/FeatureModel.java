package porqueno.streetcleanpark;

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
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineString;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 7/26/16.
 */
public class FeatureModel {
	private static final String TAG = "FeatureModel";
	private FirebaseDatabase mDatabase;
	private GeoFire mGeoFire;
	private MapsActivity mActivity; // Make sure model dies with this activity otherwise leak memory
	private List<GeoJsonFeature> mNearbyFeatures;
	private int outstandingRequests;

	FeatureModel(MapsActivity mainActivity) {
		mDatabase = FirebaseDatabase.getInstance();
		mGeoFire = new GeoFire(mDatabase.getReference("geofire"));
		mActivity = mainActivity;
		mNearbyFeatures = new ArrayList<>();
	}

	public void importAllData(GeoJsonLayer layer) {
		DatabaseReference featureRef;
		for (GeoJsonFeature feature : layer.getFeatures()) {
			String json = FeatureHelper.serialize(feature);
			String key = FeatureHelper.getUniqueKeyForBlockSide(feature);
			featureRef = mDatabase.getReference(key);
			featureRef.setValue(json);
		}
	}

	public void setAllGeos(GeoJsonLayer layer) {
		for (GeoJsonFeature feature : layer.getFeatures()) {
			GeoJsonLineString geo = (GeoJsonLineString) feature.getGeometry();
			List<LatLng> points = geo.getCoordinates();
			for (LatLng point: points) {
				mGeoFire.setLocation(FeatureHelper.getUniqueKeyForBlockSide(feature), new GeoLocation(point.latitude, point.longitude));
			}
		}
	}

	public void getFeaturesForPoint(double lat, double lng) {
		outstandingRequests = 0;
		GeoQuery geoQuery = mGeoFire.queryAtLocation(new GeoLocation(lat, lng), 2);
		geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
			@Override
			public void onKeyEntered(String key, GeoLocation location) {
				System.out.println(String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));
				addValueListener(key);
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

	public void addValueListener(String key){
		outstandingRequests += 1;
		mDatabase.getReference(key).addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				outstandingRequests -= 1;
				GeoJsonFeature feature = FeatureHelper.deserialize(dataSnapshot.getValue(String.class));
				mActivity.addFeatureToMap(feature);
				if (outstandingRequests == 0){
					// We have all data, we can now iterate to calc colors
					mActivity.calcColorsForFeatures();
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.w(TAG, "Failed to read value.", databaseError.toException());
			}
		});
	}
}
