package edu.cens.loci.classes;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cens.loci.LociConfig;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class LociVisit {

	private static final String TAG = "LociVisit";
	
	public long visitId;
	public long placeId;
	public int type;
	public long enter;
	public long exit;
	public ArrayList<RecognitionResult> recognitions;
	
	public LociVisit(int type) {
		this.visitId = -1;
		this.placeId = -1;
		this.type = type;
		this.enter = -1;
		this.exit = -1;
		this.recognitions = new ArrayList<RecognitionResult>();
	}
	
	public LociVisit(long visitId, long placeId, int type, long enter, long exit) {
		this.visitId = visitId;
		this.placeId = placeId;
		this.type = type;
		this.enter = enter;
		this.exit = exit;
		this.recognitions = new ArrayList<RecognitionResult>();
	}
	
	public LociVisit(long visitId, long placeId, int type, long enter, long exit, ArrayList<RecognitionResult> recognitions) {
		this.visitId = visitId;
		this.placeId = placeId;
		this.type = type;
		this.enter = enter;
		this.exit = exit;
		this.recognitions = recognitions;
	}

	public LociVisit(long visitId, long placeId, int type, long enter, long exit, String recognitions) {
		this.visitId = visitId;
		this.placeId = placeId;
		this.type = type;
		this.enter = enter;
		this.exit = exit;
		try {
			JSONArray jArr = new JSONArray(recognitions);
			this.recognitions = new ArrayList<RecognitionResult>();
			for (int i=0; i<jArr.length(); i++) {
				JSONObject jObj = jArr.getJSONObject(i);
				RecognitionResult result = new RecognitionResult(jObj.getLong("time"), jObj.getInt("place_id"), jObj.getInt("fingerprint_id"), jObj.getDouble("score"));
				this.recognitions.add(result);
			}		
		} catch (JSONException e) {
			MyLog.e(LociConfig.D.JSON, TAG, "LociVisit() : json error.");
			e.printStackTrace();
		}
	}
	
	public String toString() {
		return String.format("LociVisit [visitid=%d placeid=%d type=%d enter=%s exit=%s]\n", visitId, placeId, type, ((enter == -1) ? "-1" : MyDateUtils.getTimeFormatMedium(enter)), ((exit == -1) ? "-1" : MyDateUtils.getTimeFormatMedium(exit)));
	}
	
	public void clear() {
		this.visitId = -1;
		this.placeId = -1;
		this.enter = -1;
		this.exit = -1;
		this.recognitions.clear();
	}
	
	public void updateRecognitionResult(long time, long placeId, long fingerprintId, double score) {
		//this.placeId = placeId;
		recognitions.add(new RecognitionResult(time, placeId, fingerprintId, score));
	}

	public long getDuration() {
		if (enter == -1 || exit == -1)
			return -1;
		return exit - enter;
	}
	
	public String getRecognitionResults() {
		
		JSONArray jArr = new JSONArray();
		
		for (RecognitionResult result : this.recognitions) {
			try {
				jArr.put(result.toJsonObject());
			} catch (JSONException e) {
				MyLog.e(LociConfig.D.JSON, TAG, "getRecognitionResults() : Json error.");
				e.printStackTrace();
			}
		}
		
		return jArr.toString();
	}
	
	public static class RecognitionResult {
		
		public static final String KEY_TIME = "time";
		public static final String KEY_PLACE_ID = "place_id";
		public static final String KEY_FINGERPRINT_ID = "fingerprint_id";
		public static final String KEY_SCORE = "score";
		
		public long time;							// time of recognition
		public long placeId = -1;					// matching wifi-fingerprint's placeid
		public long fingerprintId;		// matching wifi-fingerprint's fingerprintId
		public double score;					// matching score
		public String placeName;
		
		public RecognitionResult(long time, long placeId, long fingerprintId, double score) {
			this.time = time;
			this.placeId = placeId;
			this.fingerprintId = fingerprintId;
			this.score = score;
		}
		
		public RecognitionResult(JSONObject jObj) {
			try {
				this.time = jObj.getLong(KEY_TIME);
				this.placeId = jObj.getLong(KEY_PLACE_ID);
				this.fingerprintId = jObj.getLong(KEY_FINGERPRINT_ID);
				this.score = jObj.getDouble(KEY_SCORE);
			} catch (JSONException e) {
				MyLog.e(LociConfig.D.JSON, TAG, "RecognitionResult");
				e.printStackTrace();
			}
		}

		public RecognitionResult setPlaceName(LociDbUtils dbUtils) {
			if (placeId > 0) {
				this.placeName = dbUtils.getPlace(this.placeId).name;
			} else {
				this.placeName = "Unknown";
			}
			return this;
		}
		
		public void setPlaceName(String name) {
			this.placeName = name;
		}
		
		public void clear() {
			this.time = -1;
			this.placeId = -1;
			this.fingerprintId = -1;
			this.score = -1;
		}
		
		public JSONObject toJsonObject() throws JSONException {
			JSONObject jObj = new JSONObject();
			jObj.put(KEY_TIME, time);
			jObj.put(KEY_PLACE_ID, placeId);
			jObj.put(KEY_FINGERPRINT_ID, fingerprintId);
			jObj.put(KEY_SCORE, score);
			return jObj;
		}
		
		public String toString() {
			return String.format("RecognitionResult [time=%s, placeId=%d, fingerprintId=%s, score=%f]", MyDateUtils.getTimeFormatMedium(this.time), this.placeId, MyDateUtils.getTimeFormatMedium(this.fingerprintId), this.score);
		}
	}
}
