package porqueno.streetcleanpark;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLineString;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jacob on 9/12/16.
 */
public class GeoJsonFeatures {
	private static final String TAG = "GeoJsonFeatures";
	private static final double COORD_ADJUST_AMOUNT = 0.0000003;
	public final static int NO_TIME = Color.RED;
	public final static int NO_DATA = Color.BLACK;
	// Aggregate features by same block side
	private Map<String, List<GeoJsonFeature>> mBlockSideFeaturesLookup;
	// Feature lookup by unique key
	private Map<String, GeoJsonFeature> mFeaturesLookup;

	public GeoJsonFeatures(){
		mBlockSideFeaturesLookup = new HashMap<>();
		mFeaturesLookup = new HashMap<>();
	}

	public void addFeatureToLookups(GeoJsonFeature feature) {
		String key = FeatureModel.getUniqueKeyForBlockSide(feature);
		List<GeoJsonFeature> blockSideData = mBlockSideFeaturesLookup.get(key);
		if (blockSideData == null) {
			blockSideData = new ArrayList<>();
		}
		blockSideData.add(feature);
		mBlockSideFeaturesLookup.put(key, blockSideData);
		mFeaturesLookup.put(FeatureModel.getUniqueKey(feature), feature);
	}

	public void removeFeatureFromLookups(GeoJsonFeature feature){
		removeBlockSideFeature(feature);
		removeFeatureFromLookup(feature);
	}

	public List<GeoJsonFeature> getBlockSideFeatures(GeoJsonFeature feature){
		return mBlockSideFeaturesLookup.get(
				FeatureModel.getUniqueKeyForBlockSide(feature)
		);
	}

	public void removeBlockSideFeature(GeoJsonFeature feature){
		mBlockSideFeaturesLookup.remove(
				FeatureModel.getUniqueKeyForBlockSide(feature)
		);
	}

	public boolean featuresLookupContains(GeoJsonFeature feature){
		return mFeaturesLookup.containsKey(
				FeatureModel.getUniqueKey(feature)
		);
	}

	public GeoJsonFeature getFeatureFromLookup(String uniqueFeatureKey){
		return mFeaturesLookup.get(uniqueFeatureKey);
	}

	public boolean featuresLookupContains(String uniqueFeatureKey){
		return mFeaturesLookup.containsKey(uniqueFeatureKey);
	}

//	public Map<String, GeoJsonFeature> getFeaturesLookup() {
//		return mFeaturesLookup;
//	}

	public void removeFeatureFromLookup(GeoJsonFeature feature) {
		mFeaturesLookup.remove(
				FeatureModel.getUniqueKey(feature)
		);
	}

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

	public void initTheFeature(GeoJsonFeature feature){
		feature.setGeometry(GeoJsonFeatures.getAdjustedGeo(feature));
		addFeatureToLookups(feature);
	}

	public int calculateColorForFeature(GeoJsonFeature feature, int desiredHoursToPark) {
		Calendar now = Calendar.getInstance();
		List<GeoJsonFeature> blockSideData = getBlockSideFeatures(feature);
		if (blockSideData != null){
			Calendar next;
			Calendar soonest = null;
			for (GeoJsonFeature blockFeature: blockSideData) {
				next = TimeHelper.getNextOccurrence(
						now,
						FeatureModel.getWeekday(blockFeature),
						FeatureModel.getStartHour(blockFeature),
						FeatureModel.getStartMin(blockFeature),
						FeatureModel.getEndHour(blockFeature),
						FeatureModel.getEndMin(blockFeature),
						FeatureModel.getWeekOne(blockFeature),
						FeatureModel.getWeekTwo(blockFeature),
						FeatureModel.getWeekThree(blockFeature),
						FeatureModel.getWeekFour(blockFeature)

				);

				if (soonest == null || next.getTimeInMillis() < soonest.getTimeInMillis()){
					soonest = next;
				}
			}
			return getColor(now, soonest, desiredHoursToPark);
		}else {
			Log.e(TAG, "No data for:" + feature.getId());
			return NO_DATA;
		}
	}

	public static int getColor(Calendar now, Calendar then, int desiredHoursToPark) {
		long hoursDiff = TimeHelper.getHoursDiff(now, then);
		if (hoursDiff < desiredHoursToPark) {
			return NO_TIME;
		} else {
			return getColorForGoodStreet(hoursDiff, desiredHoursToPark);
		}
	}

	public static int getColorForGoodStreet(long hoursDiff, int desiredHoursToPark) {
		double ratio = hoursDiff / (desiredHoursToPark * 2);
		Double greenAmt = 255 * ratio;
		if (greenAmt > 255){
			greenAmt = 255d;
		} else if (greenAmt < 100){
			greenAmt = 100d;
		}
		return Color.rgb(0, greenAmt.intValue(), 0);
	}
}
