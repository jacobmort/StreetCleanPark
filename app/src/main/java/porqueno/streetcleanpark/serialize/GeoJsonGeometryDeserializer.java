package porqueno.streetcleanpark.serialize;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.maps.android.geojson.GeoJsonGeometry;
import com.google.maps.android.geojson.GeoJsonLineString;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 7/26/16.
 */
public class GeoJsonGeometryDeserializer implements JsonDeserializer<GeoJsonGeometry> {
	public GeoJsonGeometry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		JsonArray coords = json.getAsJsonObject().getAsJsonArray("mCoordinates");
		List<LatLng> points = new ArrayList<>();
		for (JsonElement coord : coords){
			points.add(
					new LatLng(
						coord.getAsJsonObject().get("latitude").getAsDouble(),
						coord.getAsJsonObject().get("longitude").getAsDouble()
					)
			);
		}
		return new GeoJsonLineString(points);
	}
}
