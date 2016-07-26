package porqueno.streetcleanpark.serialize;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import java.lang.reflect.Type;

/**
 * Created by jacob on 7/26/16.
 */
public class GeoJsonLineStringStyleSerializer implements JsonSerializer<GeoJsonLineStringStyle> {
	public JsonElement serialize(GeoJsonLineStringStyle feature, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}
}
