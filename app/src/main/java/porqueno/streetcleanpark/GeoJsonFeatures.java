package porqueno.streetcleanpark;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLineString;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 9/12/16.
 */
public class GeoJsonFeatures {
	private static final double COORD_ADJUST_AMOUNT = 0.0000003;



	public static GeoJsonLineString getAdjustedGeo(GeoJsonFeature feature) {
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

	public static void setFeatureColor(GeoJsonFeature feature, int color) {
		GeoJsonLineStringStyle lineStyle = new GeoJsonLineStringStyle();
		lineStyle.setColor(color);
		feature.setLineStringStyle(lineStyle);
		feature.notifyObservers();
	}
}
