package porqueno.streetcleanpark;

import com.google.maps.android.geojson.GeoJsonFeature;

/**
 * Created by jacob on 9/13/16.
 * Use for Firebase model to update activity
 */
interface FeatureModelInterface {
	void featureFound(GeoJsonFeature feature);
	void featureLeft(String featureKey);
	void doneFetching();
}
