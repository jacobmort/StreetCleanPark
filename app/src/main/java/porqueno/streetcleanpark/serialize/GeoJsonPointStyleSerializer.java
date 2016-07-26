package porqueno.streetcleanpark.serialize;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import java.lang.reflect.Type;

/**
 * Created by jacob on 7/26/16.
 */
public class GeoJsonPointStyleSerializer implements JsonSerializer<GeoJsonPointStyle> {
	public JsonElement serialize(GeoJsonPointStyle feature, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}
}