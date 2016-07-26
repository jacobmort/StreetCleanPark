package porqueno.streetcleanpark.serialize;

import com.google.gson.InstanceCreator;
import com.google.maps.android.geojson.GeoJsonFeature;
import java.lang.reflect.Type;

/**
 * Created by jacob on 7/26/16.
 */
public class GeoJsonFeatureInstanceCreator implements InstanceCreator<GeoJsonFeature> {
	public GeoJsonFeature createInstance(Type type) {
		return new GeoJsonFeature(null, null, null, null);
	}
}
