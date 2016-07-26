package porqueno.streetcleanpark;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;

/**
 * Created by jacob on 7/26/16.
 */
public class FeatureModel {
	private FirebaseDatabase mDatabase;

	FeatureModel() {
		mDatabase = FirebaseDatabase.getInstance();
	}

	public void importAllData(GeoJsonLayer layer) {
		DatabaseReference featureRef;
		for (GeoJsonFeature feature : layer.getFeatures()) {
			String key = feature.getProperty("CNN");
			featureRef = mDatabase.getReference(key);
			featureRef.setValue(feature);
		}
	}
}