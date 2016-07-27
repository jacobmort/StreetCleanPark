package porqueno.streetcleanpark.serialize;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.maps.android.geojson.GeoJsonPolygonStyle;

import java.lang.reflect.Type;

/**
 * Created by jacob on 7/26/16.
 */
// TODO: any easier way to tell Gson to ignore this?
public class GeoJsonPolygonStyleSerializer implements JsonSerializer<GeoJsonPolygonStyle> {
	public JsonElement serialize(GeoJsonPolygonStyle feature, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}
}
