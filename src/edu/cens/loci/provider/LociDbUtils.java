/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.cens.loci.provider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import edu.cens.loci.LociConfig;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.classes.LociPlace;
import edu.cens.loci.classes.LociVisit;
import edu.cens.loci.classes.LociVisitWifi;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.LociVisit.RecognitionResult;
import edu.cens.loci.provider.LociContract.Data;
import edu.cens.loci.provider.LociContract.MimetypesColumns;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Tracks;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Keyword;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint.WifiDataQuery;
import edu.cens.loci.provider.LociDatabaseHelper.DataColumns;
import edu.cens.loci.provider.LociDatabaseHelper.Tables;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class LociDbUtils {

	private static final String TAG = "LociDbUtils";
	
	private LociDatabaseHelper mDbHelper = null;
	
	public LociDbUtils(Context context) {
		mDbHelper = getDatabaseHelper(context);
		initDataRowHandler();
	}
	
  /* Visible for testing */
  protected LociDatabaseHelper getDatabaseHelper(final Context context) {
      return LociDatabaseHelper.getInstance(context);
  }
  
  /** Sql for updating DIRTY flag on multiple places */
  private static final String UPDATE_PLACE_SET_DIRTY_SQL =
          "UPDATE " + Tables.PLACES +
          " SET " + Places.DIRTY + "=1" +
          " WHERE " + Places._ID + " IN (";

  /** Sql for updating VERSION on multiple places */
  private static final String UPDATE_PLACE_SET_VERSION_SQL =
          "UPDATE " + Tables.PLACES +
          " SET " + Places.VERSION + " = " + Places.VERSION + " + 1" +
          " WHERE " + Places._ID + " IN (";
  
  /* package */ static final String UPDATE_TIMES_VISITED_PLACES_TABLE =
    "UPDATE " + Tables.PLACES + " SET " + Places.TIMES_VISITED + "=" +
    " CASE WHEN " + Places.TIMES_VISITED + " IS NULL THEN 1 ELSE " +
    " (" + Places.TIMES_VISITED + " + 1) END WHERE " + Places._ID + "=?";
  
  private HashSet<Long> mUpdatedPlaces = new HashSet<Long>();
  private HashSet<Long> mDirtyPlaces = new HashSet<Long>();
  
  public long insertSuggestedWifiPlace(LociVisitWifi visit) {
  	// create a name 
  	int num = countPlacesByEntry(Places.ENTRY_WIFI_SUGGESTED);
  	String name = String.format("New Place #%03d", num+1);
  	long time = System.currentTimeMillis();
  	long placeId = insertPlace(name, Places.STATE_SUGGESTED, Places.TYPE_WIFI, Places.ENTRY_WIFI_SUGGESTED, time, time);
  	insertWifiFingerprint(placeId, visit.wifi);
  	return placeId;
  }
  
  public long insertPlace(String name, int state, int type, int entry, long entryTime, long registerTime) {
		
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	
  	ContentValues args = new ContentValues();
		args.put(Places.PLACE_NAME, name);
		args.put(Places.PLACE_STATE, state);
		args.put(Places.PLACE_TYPE, type);
		args.put(Places.ENTRY, entry);
		args.put(Places.ENTRY_TIME, entryTime);
		args.put(Places.REGISTER_TIME, registerTime);

		return db.insert(Tables.PLACES, null, args);
  }

  public int deletePlace(long placeId) {
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	return db.delete(Tables.PLACES, Places._ID + "=" + String.valueOf(placeId), null);
  }
  
  public int delete(SQLiteDatabase db, Cursor c) {
    long dataId = c.getLong(DataDeleteQuery._ID);
    //long placeId = c.getLong(DataDeleteQuery.PLACE_ID);
    mSelectionArgs1[0] = String.valueOf(dataId);
    int count = db.delete(Tables.DATA, Data._ID + "=?", mSelectionArgs1);
    return count;
  }
  
  public void checkPlaceTable() {
  
  	if (!LociConfig.D.DB.DEBUG)
  		return;
  	
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	
  	Cursor cursor = db.query(Tables.PLACES, null, null, null, null, null, null);

  	MyLog.d(LociConfig.D.DB.DEBUG, TAG, "[DB] checkPlaceTable: #places=" + cursor.getCount());
  	
  	if (cursor.moveToFirst()) {
  		do {
  			long id = cursor.getLong(cursor.getColumnIndex(Places._ID));
  			String name = cursor.getString(cursor.getColumnIndex(Places.PLACE_NAME));
  			int type = cursor.getInt(cursor.getColumnIndex(Places.PLACE_TYPE));
  			int state = cursor.getInt(cursor.getColumnIndex(Places.PLACE_STATE));
  			MyLog.d(LociConfig.D.DB.DEBUG, TAG, String.format("   id=%d name=%s type=%d state=%d", id, name, type, state));
  		} while (cursor.moveToNext());
  	}
  	cursor.close();
  }
  
  public LociPlace getPlace(long placeId) {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	
  	Cursor cursor = db.query(Tables.PLACES, null, Places._ID + "=" + placeId, null, null, null, null);
  	
  	if (cursor.moveToFirst()) {
	  	LociPlace place = new LociPlace();
	  	place.placeId = cursor.getLong(cursor.getColumnIndex(Places._ID));
	  	place.name = cursor.getString(cursor.getColumnIndex(Places.PLACE_NAME));
	  	place.state = cursor.getInt(cursor.getColumnIndex(Places.PLACE_STATE));
	  	place.type = cursor.getInt(cursor.getColumnIndex(Places.PLACE_TYPE));
	  	place.entry = cursor.getInt(cursor.getColumnIndex(Places.ENTRY));
	  	place.entryTime = cursor.getLong(cursor.getColumnIndex(Places.ENTRY_TIME));
	  	place.registerTime = cursor.getLong(cursor.getColumnIndex(Places.REGISTER_TIME));
	  	
	  	place.wifis = getWifiFingerprint(placeId);
	  	place.areas = getGpsCircleArea(placeId);
	  	
	  	if (place.areas == null || place.areas.size() == 0)
	  		place.areas.add(getPlacePositionEstimate(place.placeId));
	  	
	  	cursor.close();
	  	return place;
  	}
  	
  	cursor.close();
  	return null;
  }
  
  public LociCircleArea getPlacePositionEstimate(long placeId) {
		ArrayList<LociVisit> visits = getBaseVisits(placeId, Visits.ENTER + " DESC", null);
		
		LociLocation placeLoc = null;
		
		MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format("[DB] estimating position from %d visits.", visits.size()));
		
		// first, check if we have a good location fix near stay time from all visits
		for (LociVisit visit : visits) {
			long enter = visit.enter;
			long exit = visit.exit;
			
			placeLoc = getAveragePosition(enter-30000, exit+30000, 100);

			if (placeLoc != null && placeLoc.isValid())
				break;
		}
		
		if (placeLoc == null || !placeLoc.isValid()) {
			float minAccuracy = Float.MAX_VALUE;
			LociLocation tmpLoc = null;
			
			MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format("[DB] estimated positions while staying had low accuracy, try bad accuracy."));

			for (LociVisit visit : visits) {
				tmpLoc = getPlaceLocationEstimationWithBestAccuracy(visit.enter-30000, visit.exit+30000);

				if (tmpLoc != null) 
					MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" => lat=%f lon=%f acc=%f", tmpLoc.getLatitude(), tmpLoc.getLongitude(), tmpLoc.getAccuracy()));
				else 
					MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" => no location for this visit.."));				
				
				if (tmpLoc != null && minAccuracy >= tmpLoc.getAccuracy()) {
					placeLoc = tmpLoc;
					minAccuracy = placeLoc.getAccuracy();
				}
			}
			if (placeLoc != null)
				MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" ++ picked lat=%f lon=%f acc=%f", placeLoc.getLatitude(), placeLoc.getLongitude(), placeLoc.getAccuracy()));
			else
				MyLog.i(LociConfig.D.DB.UTILS, TAG, " ++ no location satisfying condition.");
		
		} else {
			MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] (1) got an estimate within a stay with good accuracy.");
			MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" ++ picked lat=%f lon=%f acc=%f", placeLoc.getLatitude(), placeLoc.getLongitude(), placeLoc.getAccuracy()));
			// add to state
			LociCircleArea circle = new LociCircleArea();
			circle.setCenter(placeLoc);
			circle.setRadius(placeLoc.getAccuracy());
			circle.extra = "while staying";
			return circle;
		}
		
		// if not, get the closest location fix from stay time
		if (placeLoc == null || !placeLoc.isValid()) {
			MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] don't have an estimate within a stay, try closest.");
			long minOffTime = Long.MAX_VALUE;
			String finalExtra = "";
			String extra = "";
			long offTime = Long.MAX_VALUE;
			LociLocation tmpLoc = null;
			
			for (LociVisit visit : visits) {
				LociLocation before = getFirstLocationBeforeOrAfterTime(visit.enter, true);
				LociLocation after = getFirstLocationBeforeOrAfterTime(visit.exit, false);
				
				if (before != null && after != null) {
					
					MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] has both before and after.");
					
					if (Math.abs(before.getTime() - visit.enter) < Math.abs(after.getTime() - visit.exit)) {
						tmpLoc = before;
						offTime = visit.enter - before.getTime();
						extra = MyDateUtils.humanReadableDuration(offTime, 2) + "before entering";
					} else {
						tmpLoc = after;
						offTime = after.getTime() - visit.exit;
						extra = MyDateUtils.humanReadableDuration(offTime, 2) + "after exiting";
					}
				} else if (before != null) {
					MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] has before.");

					tmpLoc = before;
					offTime = visit.enter-before.getTime();
					extra = MyDateUtils.humanReadableDuration(offTime, 2) + "before entering";
				} else if (after != null) {
					MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] has after.");
					tmpLoc = after;
					offTime = after.getTime()-visit.exit;
					extra = MyDateUtils.humanReadableDuration(offTime, 2) + "after exiting";
				} else {
					MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] has none.");
					continue;
				}
				
				MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format("[DB] offtime=%d extra=%s", offTime, extra));
				
				if (offTime < minOffTime) {
					minOffTime = offTime;
					finalExtra = extra;
					placeLoc = tmpLoc;
				}
			}
			
			if (placeLoc != null && placeLoc.isValid()) {
				LociCircleArea circle = new LociCircleArea();
				circle.setCenter(placeLoc);
				circle.setRadius(placeLoc.getAccuracy());
				circle.extra = finalExtra;
				MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] (3) got an estimate near stay");
				MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" ++ picked lat=%f lon=%f acc=%f", placeLoc.getLatitude(), placeLoc.getLongitude(), placeLoc.getAccuracy()));
				return circle;
			} else {
				MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] has no gps.");
			}
		
		} else {
			MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] (2) got an estimate within a stay with ok accuracy");
			// add to state
			
			LociCircleArea circle = new LociCircleArea();
			circle.setCenter(placeLoc);
			circle.setRadius(placeLoc.getAccuracy());
			circle.extra = "while staying (low accuracy)";
			
			MyLog.i(LociConfig.D.DB.UTILS, TAG, String.format(" ++ picked lat=%f lon=%f acc=%f", placeLoc.getLatitude(), placeLoc.getLongitude(), placeLoc.getAccuracy()));

			return circle;
		}
		
		//TODO: What should we do when no position?
		MyLog.i(LociConfig.D.DB.UTILS, TAG, "[DB] (4) no estimate");
		LociCircleArea circle = new LociCircleArea();
		circle.setCenter(34.06945, -118.443);
		circle.setRadius(100);
		circle.extra = "location is not available, default location.";
		return null;
  }
  
  public int updatePlaceBasic(LociPlace place) {
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Places.PLACE_NAME, place.name);
  	args.put(Places.PLACE_STATE, place.state);
  	args.put(Places.PLACE_TYPE, place.type);
  	return db.update(Tables.PLACES, args, Places._ID + "=" + place.placeId, null);
  }
  
  public int updatePlaceState(long placeId, int state) {
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Places.PLACE_STATE, state);
  	return db.update(Tables.PLACES, args, Places._ID + "=" + placeId, null);
  }
  
  public ArrayList<LociPlace> getPlaces(String selection) {
  	
  	ArrayList<LociPlace> places = new ArrayList<LociPlace>();
  	
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	
  	String orderBy = Places.PLACE_NAME + " ASC";
  	
  	Cursor cursor = db.query(Tables.PLACES, null, selection, null, null, null, orderBy);
  	
  	if (cursor.moveToFirst()) {
  		do {
  	  	LociPlace place = new LociPlace();
  			place.placeId = cursor.getLong(cursor.getColumnIndex(Places._ID));
  	  	place.name = cursor.getString(cursor.getColumnIndex(Places.PLACE_NAME));
  	  	place.state = cursor.getInt(cursor.getColumnIndex(Places.PLACE_STATE));
  	  	place.type = cursor.getInt(cursor.getColumnIndex(Places.PLACE_TYPE));
  	  	place.entry = cursor.getInt(cursor.getColumnIndex(Places.ENTRY));
  	  	place.entryTime = cursor.getLong(cursor.getColumnIndex(Places.ENTRY_TIME));
  	  	place.registerTime = cursor.getLong(cursor.getColumnIndex(Places.REGISTER_TIME));
  	  	
  	  	place.wifis = getWifiFingerprint(place.placeId);
  	  	place.areas = getGpsCircleArea(place.placeId);

  	  	if (place.state == Places.STATE_DELETED)
  	  		continue;
  	  	
  	  	places.add(place);
  		} while (cursor.moveToNext());
  	}
  	cursor.close();
  	return places;
  }
  
  public ArrayList<LociPlace> sortPlacesByWifiSimilarity(ArrayList<LociPlace> srcPlaces, LociWifiFingerprint targetWifi) {

  	ArrayList<LociPlace> dstPlaces = new ArrayList<LociPlace>();
  	ArrayList<RecognitionResult> results = new ArrayList<RecognitionResult>();
  	
  	int listsize = 0;
  	
  	for (LociPlace place : srcPlaces) {
  		
  		if (place.wifis == null) {
  			MyLog.d(LociConfig.D.DB.DEBUG, TAG, String.format("[DB] sortPlacesByWifiSimilarity: no wifis, skip. (pid=%d)", place.placeId));
  			results.add(new RecognitionResult(-1, place.placeId, -1, 0));
  			place.extra_double = 0;
  			dstPlaces.add(place);
  			continue;
  		}
  		
  		double bestScorePerPlace = 0;
  		
  		for (LociWifiFingerprint wifi: place.wifis) {
  			double score = recognitionAlgorithm(targetWifi, wifi);
  			if (score > bestScorePerPlace) {
  				bestScorePerPlace = score;
  			}
  		}
			place.extra_double = bestScorePerPlace;
  		listsize = results.size();
  		if (listsize == 0) {
  			results.add(new RecognitionResult(-1, place.placeId, -1, bestScorePerPlace));
  			dstPlaces.add(place);
  		} else {
  			for (int i=0; i<listsize; i++) {
  				if (bestScorePerPlace > results.get(i).score) {
  					results.add(i, new RecognitionResult(-1, place.placeId, -1, bestScorePerPlace));
  					dstPlaces.add(i, place);
  					break;
  				}
  			}
  			
  			if (listsize == results.size()) {
  				results.add(new RecognitionResult(-1, place.placeId, -1, bestScorePerPlace));
  				dstPlaces.add(place);
  			}
  			listsize = results.size();
  		}
  	}
  	
  	return dstPlaces;
  }
  
  public int countPlacesByEntry(int entry) {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	Cursor cursor = db.query(Tables.PLACES, new String[] {Places._ID, Places.ENTRY}, Places.ENTRY + "=" + entry, null, null, null, null);
  	if (cursor != null)
  		return cursor.getCount();
  	
  	return 0;
  }
  
  /***
   * Tables.Data related methods
   */
  
  private ContentValues mValues = new ContentValues();
  
  private HashMap<String, DataRowHandler> mDataRowHandlers;
  
  private void initDataRowHandler() {
  	mDataRowHandlers = new HashMap<String, DataRowHandler>();
  	mDataRowHandlers.put(WifiFingerprint.CONTENT_ITEM_TYPE, new WifiFingerprintRowHandler());
  	mDataRowHandlers.put(GpsCircleArea.CONTENT_ITEM_TYPE, new GpsAreaRowHandler());
  	mDataRowHandlers.put(Keyword.CONTENT_ITEM_TYPE, new KeywordRowHandler());
  }
  
  private DataRowHandler getDataRowHandler(final String mimeType) {
  	DataRowHandler handler = mDataRowHandlers.get(mimeType);
  	if (handler == null) {
  		handler = new CustomDataRowHandler(mimeType);
  		mDataRowHandlers.put(mimeType, handler);
  	}
  	return handler;
  }
  
  /**
   * 
   * @param values
   * @param callerIsSyncAdapter
   * @return Data._id of the new row
   */
  private long insertData(ContentValues values, boolean callerIsSyncAdapter) {
  	long id = 0;
  	mValues.clear();
  	mValues.putAll(values);
  	
  	long placeId = mValues.getAsLong(Data.PLACE_ID);
  	
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	
  	// Replace mimetype with internal mapping
  	final String mimeType = mValues.getAsString(Data.MIMETYPE);
  	if (TextUtils.isEmpty(mimeType)) {
  		throw new IllegalArgumentException(Data.MIMETYPE + " is required");
  	}
  	
  	mValues.put(DataColumns.MIMETYPE_ID, mDbHelper.getMimeTypeId(mimeType));
  	mValues.remove(Data.MIMETYPE);
  	
  	DataRowHandler rowHandler = getDataRowHandler(mimeType);
  	id = rowHandler.insert(db, placeId, mValues);
  	if (!callerIsSyncAdapter)
  		setPlaceDirty(placeId);
  	mUpdatedPlaces.add(placeId);
  	
  	return id;
  }
  
  public int updateData(ContentValues values, String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
  	mValues.clear();
  	mValues.putAll(values);
  	mValues.remove(Data._ID);
  	mValues.remove(Data.PLACE_ID);
  	mValues.remove(Data.MIMETYPE);
  	
  	int count = 0;
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	Cursor c = db.query(Tables.DATA, DataUpdateQuery.COLUMNS, selection, selectionArgs, null, null, null);
  	try {
  		while (c.moveToNext()) {
  			count += updateData(mValues, c, callerIsSyncAdapter);
  		}
  	} finally {
  		c.close();
  	}
  	
  	return count;
  }
  
  private int updateData(ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
  	if (values.size() == 0)
  		return 0;

  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	
  	final String mimeType = c.getString(DataUpdateQuery.MIMETYPE);
  	DataRowHandler rowHandler = getDataRowHandler(mimeType);
  	if (rowHandler.update(db, values, c, callerIsSyncAdapter)) {
  		return 1;
  	} else {
  		return 0;
  	}
  }
  
  /**
   * Delete data row by row so that fixing of primaries etc work correctly.
   */
  public int deleteData(String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
      int count = 0;
      final SQLiteDatabase db = mDbHelper.getReadableDatabase();
      // Note that the query will return data according to the access restrictions,
      // so we don't need to worry about deleting data we don't have permission to read.
      Cursor c = db.query(Tables.DATA, DataDeleteQuery.COLUMNS, selection, selectionArgs, null, null, null);
      try {
          while(c.moveToNext()) {
              long placeId = c.getLong(DataDeleteQuery.PLACE_ID);
              String mimeType = c.getString(DataDeleteQuery.MIMETYPE);
              DataRowHandler rowHandler = getDataRowHandler(mimeType);
              count += rowHandler.delete(db, c);
              if (!callerIsSyncAdapter) {
                  setPlaceDirty(placeId);
              }
          }
      } finally {
          c.close();
      }

      return count;
  }
  
  /**
   * 
   * @param visit
   * @return Data._id of the new row
   */
  public long insertWifiFingerprint(long placeId, LociWifiFingerprint wifi) {
  	boolean callerIsSyncAdapter = false;
  	ContentValues values = new ContentValues();
  	values.put(Data.MIMETYPE, WifiFingerprint.CONTENT_ITEM_TYPE);
  	values.put(Data.PLACE_ID, placeId);
  	try {
			values.put(WifiFingerprint.FINGERPRINT, wifi.toJsonObject().toString());
		} catch (JSONException e) {
			MyLog.e(LociConfig.D.JSON, TAG, "insertWifiFingerprint : json error.");
			e.printStackTrace();
			return -1;
		}
  	values.put(WifiFingerprint.TIMESTAMP, wifi.getEnter());
		return insertData(values, callerIsSyncAdapter);
  }
  
  public ArrayList<LociWifiFingerprint> getWifiFingerprint(long placeId) {
  	ArrayList<LociWifiFingerprint> wifi = new ArrayList<LociWifiFingerprint>();
  	Cursor cursor = getPlaceData(placeId, WifiFingerprint.CONTENT_ITEM_TYPE);
  	
  	if (cursor.moveToFirst()) {
  		do {
  			String wifiJson = cursor.getString(cursor.getColumnIndex(WifiFingerprint.FINGERPRINT));
  			
  			try {
					wifi.add(new LociWifiFingerprint(wifiJson));
				} catch (JSONException e) {
					MyLog.e(LociConfig.D.JSON, TAG, "getWifiFingerprint(): json error.");
					e.printStackTrace();
					continue;
				}
  		} while (cursor.moveToNext());
  	}
  	cursor.close();
  	return wifi;
  }

  public Cursor getPlaceData(long placeId, String mimeType) {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	//checkDataTable();
  	String selection = (Data.PLACE_ID + "=" + placeId) + " AND " + DataColumns.MIMETYPE_ID + "=" + mDbHelper.getMimeTypeId(mimeType);
  	Cursor cursor = db.query(Tables.DATA, null, selection, null, null, null, null);
  	return cursor;
  }
  
  public Cursor getPlaceData(String mimeType) {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	//checkDataTable();
  	String selection = DataColumns.MIMETYPE_ID + "=" + mDbHelper.getMimeTypeId(mimeType);
  	Cursor cursor = db.query(Tables.DATA, null, selection, null, null, null, null);
  	return cursor;
  }
  
  public void checkDataTable() {
  	
  	if (!LociConfig.D.DB.DEBUG)
  		return;
  		
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	Cursor cursor = db.query(Tables.DATA, null, null, null, null, null, null);
  	
  	MyLog.d(LociConfig.D.DB.DEBUG, TAG, "[DB] checkDataTable: #rows=" + cursor.getCount());
  	
  	if (cursor.moveToFirst()) {
  		do {
  			long id = cursor.getLong(cursor.getColumnIndex(Data._ID));
  			long pid = cursor.getLong(cursor.getColumnIndex(Data.PLACE_ID));
  			int mimeTypeId = cursor.getInt(cursor.getColumnIndex(DataColumns.MIMETYPE_ID));
  			
  			MyLog.d(LociConfig.D.DB.DEBUG, TAG, String.format("  id=%d pid=%d mimetype=%d", id, pid, mimeTypeId));
  			
  		} while (cursor.moveToNext());
  	}
  	
  	cursor.close();
  }
  
  /**
   * 
   * @param wifi
   * @return Recognition results with score higher than 0, sorted by score
   * 				 Recognition result contains Recognition time, PlaceId, FingerprintId, Score
   */
  public ArrayList<RecognitionResult> getRecogntionScoresWifi(LociWifiFingerprint wifi) {
  	ArrayList<RecognitionResult> results = new ArrayList<RecognitionResult>();
  	
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	
  	checkPlaceTable();
  	
  	//checkDataTable();
  	
  	long mimeTypeId = mDbHelper.getMimeTypeId(WifiFingerprint.CONTENT_ITEM_TYPE);
  	String selection = DataColumns.MIMETYPE_ID + "=" + mimeTypeId;
  	Cursor cursor = db.query(Tables.DATA, WifiDataQuery.COLUMNS, selection, null, null, null, Data.PLACE_ID + " ASC");
  	
  	long recogTime = System.currentTimeMillis();  	
  	if (cursor.moveToFirst()) {
  		
  		do {
  			long placeId = cursor.getLong(WifiDataQuery.PLACEID);
  			LociWifiFingerprint dbWifi;
				try {
					dbWifi = new LociWifiFingerprint(cursor.getString(WifiDataQuery.FINGERPRINT));
				} catch (JSONException e) {
					MyLog.e(LociConfig.D.JSON, TAG, "getRecognitionScoresWifi : json error.");
					e.printStackTrace();
					continue;
				}
  			long timestamp = cursor.getLong(WifiDataQuery.TIMESTAMP);
  			double score = recognitionAlgorithm(dbWifi, wifi);
  			
  			if (score > 0) {
  				RecognitionResult result = new RecognitionResult(recogTime, placeId, timestamp, score);
  				
  				// sort the results by score, index 0 contains the highest score
  				int resultsSize = results.size();
  				for (int i=0; i<resultsSize; i++) {
  					if (score > results.get(i).score) {
  						results.add(i, result);
  						break;
  					}
  				}
  				if (results.size() == resultsSize)
  					results.add(result);
  			}
  		} while (cursor.moveToNext());
  	}
  	cursor.close();

  	MyLog.d(LociConfig.D.PD.SCORE, TAG, "[getRecognitionScoresWifi] #results=" + results.size());
  	for (RecognitionResult result : results) {
  		MyLog.d(LociConfig.D.PD.SCORE, TAG, result.toString());
  	}
  	
  	
  	return results;
  }
  
  private double recognitionAlgorithm(LociWifiFingerprint dbWifi, LociWifiFingerprint wifi) {
  	return LociWifiFingerprint.tanimotoScore(dbWifi, wifi, dbWifi.getRepAPs(), dbWifi.getRepAPs(), 0.3, true);
  }
  
  public long insertGpsCircleArea(long placeId, LociCircleArea area) {
  	boolean callerIsSyncAdapter = false;
  	ContentValues values = new ContentValues();
  	values.put(Data.MIMETYPE, GpsCircleArea.CONTENT_ITEM_TYPE);
  	values.put(Data.PLACE_ID, placeId);
  	values.put(GpsCircleArea.LATITUDE, area.getLatitude());
  	values.put(GpsCircleArea.LONGITUDE, area.getLongitude());
  	values.put(GpsCircleArea.RADIUS, area.getRadius());
  	return insertData(values, callerIsSyncAdapter);
  }
  
  public ArrayList<LociCircleArea> getGpsCircleArea(long placeId) {
  	ArrayList<LociCircleArea> areas = new ArrayList<LociCircleArea>();
  	Cursor cursor = getPlaceData(placeId, GpsCircleArea.CONTENT_ITEM_TYPE);
  	
  	if (cursor.moveToFirst()) {
  		do {
  			double latitude = cursor.getDouble(cursor.getColumnIndex(GpsCircleArea.LATITUDE));
  			double longitude = cursor.getDouble(cursor.getColumnIndex(GpsCircleArea.LONGITUDE));
  			float radius = cursor.getFloat(cursor.getColumnIndex(GpsCircleArea.RADIUS));
  			
  			areas.add(new LociCircleArea(latitude, longitude, radius));
  			
  		} while (cursor.moveToNext());
  	}
  	cursor.close();

  	if (areas.size() <= 0) {
  		
  	}
  	
  	
  	return areas;
  }
  
  public long insertKeyword(long placeId, String keyword) {
  	boolean callerIsSyncAdapter = false;
  	ContentValues values = new ContentValues();
  	values.put(Data.MIMETYPE, GpsCircleArea.CONTENT_ITEM_TYPE);
  	values.put(Data.PLACE_ID, placeId);
  	values.put(Keyword.TAG, keyword);
  	return insertData(values, callerIsSyncAdapter);
  }
  
  
  public ArrayList<String> getSavedKeywords() {
  	Cursor cursor = getPlaceData(Keyword.CONTENT_ITEM_TYPE);
  	
  	HashSet<String> keywordSet = new HashSet<String>();
  	
  	if (cursor.moveToFirst()) {
  		do {
  			String keyword = cursor.getString(cursor.getColumnIndex(Keyword.TAG));
  			if (!keywordSet.contains(keyword)) {
  				keywordSet.add(keyword);
  			}
  		} while (cursor.moveToNext());
  	}
  	cursor.close();

  	Object[] keywordObjArray = keywordSet.toArray();
  	ArrayList<String> keywordArray = new ArrayList<String>();
  	
  	for (Object keywordObj : keywordObjArray) {
  		keywordArray.add((String) keywordObj);
  	}

  	Collections.sort(keywordArray);
  	
  	return keywordArray;
  }
  
  /**
   * 
   * @param visit
   * @return
   */
  public long insertWifiVisit(LociVisitWifi visit) {
  	if (visit.enter == -1 || visit.exit == -1) {
  		MyLog.e(LociConfig.D.ERROR, TAG, "[error] addWifiVisit: enter or exit is -1.");
  		return -1;
  	}
  	
  	if (visit.type != Places.TYPE_WIFI) {
  		MyLog.e(LociConfig.D.ERROR, TAG, "[error] addWifiVisit: place type is not TYPE_WIFI. " + visit.type);
  		return -1;
  	}
  	
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Visits.ENTER, visit.enter);
  	args.put(Visits.EXIT, visit.exit);
  	args.put(Visits.PLACE_ID, visit.placeId);
  	args.put(Visits.TYPE, visit.type);
  	try {
			args.put(Visits.EXTRA1, visit.wifi.toJsonObject().toString());
		} catch (JSONException e) {
			MyLog.e(LociConfig.D.JSON, TAG, "[error] addWifiVisit: json error.");
			e.printStackTrace();
			return -1;
		}
		args.put(Visits.EXTRA2, visit.getRecognitionResults());
		
		return db.insert(Tables.VISITS, null, args);
  }
  
  /**
   * 
   * @param visit Visit information to save
   * @return the number of rows affected
   */
  public int updateWifiVisit(LociVisitWifi visit) {
  	if (visit.enter == -1 || visit.exit == -1) {
  		MyLog.e(LociConfig.D.ERROR, TAG, "[error] updateWifiVisit: (enter or exit is -1)" + String.format("enter=%d exit=%d", visit.enter, visit.exit));
  		return -1;
  	}
  	
  	if (visit.type != Places.TYPE_WIFI) {
  		MyLog.e(LociConfig.D.ERROR, TAG, "[error] updateWifiVisit: place type is not TYPE_WIFI. " + visit.type);
  		return -1;
  	}
  	
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Visits.ENTER, visit.enter);
  	args.put(Visits.EXIT, visit.exit);
  	args.put(Visits.PLACE_ID, visit.placeId);
  	args.put(Visits.TYPE, visit.type);
  	try {
			args.put(Visits.EXTRA1, visit.wifi.toJsonObject().toString());
		} catch (JSONException e) {
			MyLog.e(LociConfig.D.JSON, TAG, "[error] addWifiVisit: json error (parsing wifi to json failed).");
			e.printStackTrace();
			return -1;
		}
		args.put(Visits.EXTRA2, visit.getRecognitionResults());
		
		MyLog.d(LociConfig.D.DB.DEBUG, TAG, String.format("[DB] (updateWifiVisit) visitId=%d, enter=%s, exit=%s, placeId=%d", visit.visitId, MyDateUtils.getTimeFormatMedium(visit.enter), MyDateUtils.getTimeFormatMedium(visit.exit), visit.placeId));
		
		return db.update(Tables.VISITS, args, Visits._ID + "=" + visit.visitId, null);
  }
  
  /**
   * 
   * @return
   */
  public LociVisitWifi getLastWifiVisit() {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	Cursor cursor = db.query(Tables.VISITS, null, Visits.TYPE + "=" + Places.TYPE_WIFI, null, null, null, Visits._ID + " DESC", "1");
  	ArrayList<LociVisitWifi> list = cursor2visitwifi(cursor);
  	
  	if (list != null && list.size() > 0)
  		return list.get(0);
  	else
  		return null;
  }
  
  public ArrayList<LociVisitWifi> getWifiVisits(long start, long end) {
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	
  	String selection = (Visits.EXIT + ">=" + start + " AND " + Visits.ENTER + "<=" + end)
  										+ (" AND " + Visits.TYPE + "=" + Places.TYPE_WIFI);
  	Cursor cursor = db.query(Tables.VISITS, null, selection, null, null, null, null);
  
  	return cursor2visitwifi(cursor);
  }
  
  /**
   * Returns visit basic information including visit_id, type, enter_time, and exit_time
   * @param placeId is place id
   * @param orderBy 
   * @param maxCount maximum number of rows to return. ignored when null.
   * @return
   */
  public ArrayList<LociVisit> getBaseVisits(long placeId, String orderBy, String maxCount) {

  	ArrayList<LociVisit> visits = new ArrayList<LociVisit>();
  	
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	String selection = (Visits.PLACE_ID + "=" + placeId);
  	
  	Cursor cursor;
  	
  	if (maxCount != null)
  		cursor = db.query(Tables.VISITS, null, selection, null, null, null, orderBy, maxCount);
  	else
  		cursor = db.query(Tables.VISITS, null, selection, null, null, null, orderBy);
  	
  	if (cursor.moveToFirst()) {
  		do {
  			long visitId = cursor.getLong(cursor.getColumnIndex(Visits._ID));
  			int type = cursor.getInt(cursor.getColumnIndex(Visits.TYPE));
  			long enter = cursor.getLong(cursor.getColumnIndex(Visits.ENTER));
  			long exit = cursor.getLong(cursor.getColumnIndex(Visits.EXIT));

  			visits.add(new LociVisit(visitId, placeId, type, enter, exit));
  		
  			//Log.d(TAG, String.format("visitId=%ld type=%d enter=%d exit=%d", visitId, type, enter, exit));
  			
  		} while (cursor.moveToNext());
  	}
  	
  	cursor.close();
  	return visits;
  }
  
  public ArrayList<LociVisit> getBaseVisits(long date) {
  	Calendar endTime = Calendar.getInstance();
  	endTime.setTimeInMillis(date);
  	endTime.add(Calendar.DAY_OF_MONTH, 1);
  	return getBaseVisits(date, endTime.getTimeInMillis());
  }
  
  public ArrayList<LociVisit> getBaseVisits(long startTime, long endTime) {
  	ArrayList<LociVisit> visits = new ArrayList<LociVisit>();
  	
  	final SQLiteDatabase db = mDbHelper.getReadableDatabase();
  	String selection = (Visits.EXIT + ">=" + startTime + " AND " + Visits.ENTER + "<=" + endTime);
  	
  	Cursor cursor = db.query(Tables.VISITS, null, selection, null, null, null, null);
  	
  	if (cursor.moveToFirst()) {
  		do {
  			long visitId = cursor.getLong(cursor.getColumnIndex(Visits._ID));
  			long placeId = cursor.getLong(cursor.getColumnIndex(Visits.PLACE_ID));
  			int type = cursor.getInt(cursor.getColumnIndex(Visits.TYPE));
  			long enter = cursor.getLong(cursor.getColumnIndex(Visits.ENTER));
  			long exit = cursor.getLong(cursor.getColumnIndex(Visits.EXIT));
  			
  			//Log.d(TAG, String.format("visitId=%d placeId=%d type=%d enter=%d exit=%d", visitId, placeId, type, enter, exit));
  			
  			visits.add(new LociVisit(visitId, placeId, type, enter, exit));
  		} while (cursor.moveToNext());
  	}
  	cursor.close();
  	return visits;
  }
  /**
   * 
   * @param cursor
   * @return
   */
  private ArrayList<LociVisitWifi> cursor2visitwifi(Cursor cursor) {
  	ArrayList<LociVisitWifi> visits = new ArrayList<LociVisitWifi>();
  	if (cursor.moveToFirst()) {
  		do {
  				long visitId = cursor.getLong(cursor.getColumnIndex(Visits._ID));
	  			long placeId = cursor.getLong(cursor.getColumnIndex(Visits.PLACE_ID));
	  			int type = cursor.getInt(cursor.getColumnIndex(Visits.TYPE));
	  			long enter = cursor.getLong(cursor.getColumnIndex(Visits.ENTER));
	  			long exit = cursor.getLong(cursor.getColumnIndex(Visits.EXIT));
	  			
	  			if (type != Places.TYPE_WIFI) {
	  				MyLog.e(LociConfig.D.ERROR, TAG, "cursor2visitwifi: type is not wifi.");
	  				continue;
	  			}

  				LociWifiFingerprint wifi = null;
	  			try {
	  				wifi = new LociWifiFingerprint(cursor.getString(cursor.getColumnIndex(Visits.EXTRA1)));
	  			} catch (JSONException e) {
	  				MyLog.e(LociConfig.D.JSON, TAG, "cursor2visitwifi : EXTRA1 (wifi fingerprint) json error.");
	  			}
	  			
  				ArrayList<RecognitionResult> recognitions = new ArrayList<RecognitionResult>();
	  			try {
	  				JSONArray jArr = new JSONArray(cursor.getString(cursor.getColumnIndex(Visits.EXTRA2)));
	  				for (int i=0; i<jArr.length(); i++) {
	  					JSONObject jObj = jArr.getJSONObject(i);
	  					RecognitionResult result = new RecognitionResult(jObj.getLong("time"), jObj.getInt("place_id"), jObj.getInt("figerprint_id"), jObj.getDouble("score"));
	  					recognitions.add(result);
	  				}
	  			} catch (JSONException e) {
						MyLog.e(LociConfig.D.JSON, TAG, "cursor2visitwifi : EXTRA2 (recog results) json error.");
					}
  				LociVisitWifi visit = new LociVisitWifi(visitId, placeId, enter, exit, recognitions, wifi);
  				visits.add(visit);

  		} while(cursor.moveToNext());
  	}
  	cursor.close();
  	return visits;
  }
  
  public int updateVisitPlaceId(long visitId, long placeId) {
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Visits.PLACE_ID, placeId);

  	MyLog.d(LociConfig.D.DB.CALL, TAG, String.format("[DB] change visit's placeid : (visitId=%d, placeId=%d)", visitId, placeId));
  	
  	return db.update(Tables.VISITS, args, Visits._ID + "=" + visitId, null);
  }
  
  public int changeVisitPlaceIdAll(long oldId, long newId) {
  	final SQLiteDatabase db = mDbHelper.getWritableDatabase();
  	ContentValues args = new ContentValues();
  	args.put(Visits.PLACE_ID, newId);
  	
  	MyLog.d(LociConfig.D.DB.CALL, TAG, String.format("[DB] change all visits placeid : (oldId=%d, newId=%d)", oldId, newId));
  	
  	return db.update(Tables.VISITS, args, Visits.PLACE_ID + "=" + oldId, null);
  }
  
  
  /**
   * 
   * @param time
   * @param loc
   * @return
   */
	public long insertPosition(long time, Location loc) {

		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentValues args = new ContentValues();
		args.put(Tracks.TIME, time);
		args.put(Tracks.LATITUDE, loc.getLatitude());
		args.put(Tracks.LONGITUDE, loc.getLongitude());
		args.put(Tracks.ALTITUDE, loc.getAltitude());
		args.put(Tracks.SPEED, loc.getSpeed());
		args.put(Tracks.ACCURACY, loc.getAccuracy());
		args.put(Tracks.BEARING, loc.getBearing());
		args.put(Tracks.SYNC, 0);
		
		//MyLog.d(LociConfig.Debug.Provder.DB.LOG_EVENT, TAG, "[Db] insertPosition " + String.format("lat=%7.2f, lon=%7.2f, alt=%7.2f, speed=%7.2f, acc=%7.2f", loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), loc.getSpeed(), loc.getAccuracy()));
			
		return db.insert(Tables.TRACKS, null, args);
	}
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param filter
	 * @return
	 */
	public ArrayList<LociLocation> getTrack(long start, long end, int filter) {
		String [] columns = new String [] {Tracks._ID,
																			 Tracks.TIME,
																			 Tracks.LATITUDE,
																			 Tracks.LONGITUDE,
																			 Tracks.ALTITUDE,
																			 Tracks.SPEED,
																			 Tracks.BEARING,
																			 Tracks.ACCURACY};
		String selection = Tracks.TIME + ">=" + start + " AND " + Tracks.TIME + " <= " + end;
 		
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor cursor = db.query(Tables.TRACKS, columns, selection, null, null, null, null);
		
		ArrayList<LociLocation> track = new ArrayList<LociLocation>();
		
		if (cursor.moveToFirst()) {
			do {
				LociLocation loc = new LociLocation(LocationManager.GPS_PROVIDER);
				loc.setTime(cursor.getLong(cursor.getColumnIndex(Tracks.TIME)));
				loc.setLatitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LATITUDE)));
				loc.setLongitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LONGITUDE)));
				loc.setAltitude(cursor.getDouble(cursor.getColumnIndex(Tracks.ALTITUDE)));
				loc.setSpeed(cursor.getFloat(cursor.getColumnIndex(Tracks.SPEED)));
				loc.setBearing(cursor.getFloat(cursor.getColumnIndex(Tracks.BEARING)));
				loc.setAccuracy(cursor.getFloat(cursor.getColumnIndex(Tracks.ACCURACY)));
				
				track.add(loc);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return track;

	}

	/**
	 * Returns an average position fix
	 * @param start beginning time
	 * @param end ending time
	 * @param accuracy filter position fixes with accuracy value higher than this value
	 * @return average position within the provided time interval, filtered by accuracy
	 */
	public LociLocation getAveragePosition(long start, long end, int accuracy) {
		
		LociLocation placeLoc = null;
		
		String [] columns = new String [] {Tracks._ID,
																			 Tracks.TIME,
																			 Tracks.LATITUDE,
																			 Tracks.LONGITUDE,
																			 Tracks.ALTITUDE,
																			 Tracks.SPEED,
																			 Tracks.BEARING,
																			 Tracks.ACCURACY};
		
		String selection = Tracks.TIME + ">=" + start + " AND " + Tracks.TIME + " <= " + end;
 		
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor cursor = db.query(Tables.TRACKS, columns, selection, null, null, null, null);
		
		ArrayList<LociLocation> track = new ArrayList<LociLocation>();
		
		MyLog.d(LociConfig.D.DB.UTILS, TAG, String.format("[DB] average position between %s-%s ==> #pos=%d (accuracy=%d)", 
				MyDateUtils.getTimeFormatLong(start), MyDateUtils.getTimeFormatLong(end), cursor.getCount(), accuracy));
		
		if (cursor.moveToFirst()) {
			do {
				LociLocation loc = new LociLocation(LocationManager.GPS_PROVIDER);
				loc.setTime(cursor.getLong(cursor.getColumnIndex(Tracks.TIME)));
				loc.setLatitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LATITUDE)));
				loc.setLongitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LONGITUDE)));
				loc.setAltitude(cursor.getDouble(cursor.getColumnIndex(Tracks.ALTITUDE)));
				loc.setSpeed(cursor.getFloat(cursor.getColumnIndex(Tracks.SPEED)));
				loc.setBearing(cursor.getFloat(cursor.getColumnIndex(Tracks.BEARING)));
				loc.setAccuracy(cursor.getFloat(cursor.getColumnIndex(Tracks.ACCURACY)));
				track.add(loc);
			} while (cursor.moveToNext());
		
		  placeLoc = LociLocation.averageLocation(track, accuracy);
		}
		cursor.close();
		return placeLoc;

	}

	public LociLocation getPlaceLocationEstimationWithBestAccuracy(long start, long end) {
		LociLocation placeLoc = null;
		
		String [] columns = new String [] {Tracks._ID,
																			 Tracks.TIME,
																			 Tracks.LATITUDE,
																			 Tracks.LONGITUDE,
																			 Tracks.ALTITUDE,
																			 Tracks.SPEED,
																			 Tracks.BEARING,
																			 Tracks.ACCURACY};
		
		String selection = Tracks.TIME + ">=" + start + " AND " + Tracks.TIME + " <= " + end;
 		
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor cursor = db.query(Tables.TRACKS, columns, selection, null, null, null, null);

		float minAccuracy = Float.MAX_VALUE;
		
		if (cursor.moveToFirst()) {
			do {
				if (minAccuracy > cursor.getFloat(cursor.getColumnIndex(Tracks.ACCURACY))) {
					if (placeLoc == null) placeLoc = new LociLocation(LocationManager.GPS_PROVIDER);
					placeLoc.setTime(cursor.getLong(cursor.getColumnIndex(Tracks.TIME)));
					placeLoc.setLatitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LATITUDE)));
					placeLoc.setLongitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LONGITUDE)));
					placeLoc.setAltitude(cursor.getDouble(cursor.getColumnIndex(Tracks.ALTITUDE)));
					placeLoc.setSpeed(cursor.getFloat(cursor.getColumnIndex(Tracks.SPEED)));
					placeLoc.setBearing(cursor.getFloat(cursor.getColumnIndex(Tracks.BEARING)));
					placeLoc.setAccuracy(cursor.getFloat(cursor.getColumnIndex(Tracks.ACCURACY)));
					minAccuracy = placeLoc.getAccuracy();
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return placeLoc;
	}
	
	/**
	 * @param time
	 * @param before
	 * @return first location before time if before is true. Otherwise, after time. 
	 *         returns null if no location is available.
	 */
	public LociLocation getFirstLocationBeforeOrAfterTime(long time, boolean before) {
		
		LociLocation loc = null;
		
		String [] columns = new String [] {Tracks._ID,
																			 Tracks.TIME,
																			 Tracks.LATITUDE,
																			 Tracks.LONGITUDE,
																			 Tracks.ALTITUDE,
																			 Tracks.SPEED,
																			 Tracks.BEARING,
																			 Tracks.ACCURACY};
		
		String selection = Tracks.TIME + "<=" + time;
		String order  = " DESC";
		
		if (!before) {
			selection = Tracks.TIME + ">=" + time;
			order = " ASC";
		}
			
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor cursor = db.query(Tables.TRACKS, columns, selection, null, null, null, Tracks.TIME + order, "" + 1);
		
		if (cursor.moveToFirst()) {
			loc = new LociLocation(LocationManager.GPS_PROVIDER);
			loc.setTime(cursor.getLong(cursor.getColumnIndex(Tracks.TIME)));
			loc.setLatitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LATITUDE)));
			loc.setLongitude(cursor.getDouble(cursor.getColumnIndex(Tracks.LONGITUDE)));
			loc.setAltitude(cursor.getDouble(cursor.getColumnIndex(Tracks.ALTITUDE)));
			loc.setSpeed(cursor.getFloat(cursor.getColumnIndex(Tracks.SPEED)));
			loc.setBearing(cursor.getFloat(cursor.getColumnIndex(Tracks.BEARING)));
			loc.setAccuracy(cursor.getFloat(cursor.getColumnIndex(Tracks.ACCURACY)));
		} 
		cursor.close();
		return loc;
	}
	
	
	private long mMimeTypeIdWifi;
	private long mMimeTypeIdGps;
	private long mMimeTypeIdKeyword;
	
	private StringBuilder mSb = new StringBuilder();
  private String[] mSelectionArgs1 = new String[1];
  private String[] mSelectionArgs2 = new String[2];
  
  private boolean mSyncToNetwork;
	
  private interface DataUpdateQuery {
    String[] COLUMNS = { Data._ID, Data.PLACE_ID, Data.MIMETYPE };

    int _ID = 0;
    int PLACE_ID = 1;
    int MIMETYPE = 2;
  }
  
  private interface DataDeleteQuery {
    public static final String TABLE = Tables.DATA_JOIN_MIMETYPES;

    public static final String[] CONCRETE_COLUMNS = new String[] {
        MimetypesColumns.MIMETYPE,
        Data.PLACE_ID,
        Data.DATA1,
    };

    public static final String[] COLUMNS = new String[] {
        Data._ID,
        MimetypesColumns.MIMETYPE,
        Data.PLACE_ID,
        Data.DATA1,
    };

    public static final int _ID = 0;
    public static final int MIMETYPE = 1;
    public static final int PLACE_ID = 2;
    public static final int DATA1 = 3;
  }
	
  
	public abstract class DataRowHandler {
		protected final String mMimetype;
		protected long mMimetypeId;
		
		public DataRowHandler(String mimetype) {
			mMimetype = mimetype;
		}
		
		protected long getMimeTypeId() {
			if (mMimetypeId == 0)
				mMimetypeId = mDbHelper.getMimeTypeId(mMimetype);
			return mMimetypeId;
		}
		
		public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
			
			final long dataId = db.insert(Tables.DATA, null, values);
			return dataId;
		}
		
		public boolean update(SQLiteDatabase db, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
			long dataId = c.getLong(DataUpdateQuery._ID);
			long placeId = c.getLong(DataUpdateQuery.PLACE_ID);
			
			if (values.size() > 0) {
				mSelectionArgs1[0] = String.valueOf(dataId);
        db.update(Tables.DATA, values, Data._ID + "=?", mSelectionArgs1);
			}
			
      if (!callerIsSyncAdapter) {
        setPlaceDirty(placeId);
      }
			
			return true;
		}

    public int delete(SQLiteDatabase db, Cursor c) {
      long dataId = c.getLong(DataDeleteQuery._ID);
      //long placeId = c.getLong(DataDeleteQuery.PLACE_ID);
      mSelectionArgs1[0] = String.valueOf(dataId);
      int count = db.delete(Tables.DATA, Data._ID + "=?", mSelectionArgs1);
      return count;
    }

	}
	
  public class CustomDataRowHandler extends DataRowHandler {

    public CustomDataRowHandler(String mimetype) {
        super(mimetype);
    }
}
	
	public class WifiFingerprintRowHandler extends DataRowHandler {
		public WifiFingerprintRowHandler() {
			super(WifiFingerprint.CONTENT_ITEM_TYPE);
		}
		
		@Override
		public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
			
			long dataId = super.insert(db, placeId, values);
			
			return dataId;
		}
		
		@Override
		public boolean update(SQLiteDatabase db, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
			
			super.update(db, values, c, callerIsSyncAdapter);
			
			return true;
		}
		
		@Override
		public int delete(SQLiteDatabase db, Cursor c) {
			int count = super.delete(db, c);
			
			return count;
		}
	}
	
	public class GpsAreaRowHandler extends DataRowHandler {
		public GpsAreaRowHandler() {
			super(GpsCircleArea.CONTENT_ITEM_TYPE);
		}
		
		@Override
		public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
			long dataId = super.insert(db, placeId, values);
			
			return dataId;
		}
		
		@Override
		public boolean update(SQLiteDatabase db, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
			super.update(db, values, c, callerIsSyncAdapter);
			return true;
		}
		
		@Override
		public int delete(SQLiteDatabase db, Cursor c) {
			int count = super.delete(db, c);
			return count;
		}
	}
	
	public class KeywordRowHandler extends DataRowHandler {
		public KeywordRowHandler() {
			super(Keyword.CONTENT_ITEM_TYPE);
		}
		
		@Override
		public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
			long dataId = super.insert(db, placeId, values);
			
			return dataId;
		}
		
		@Override
		public boolean update(SQLiteDatabase db, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
			super.update(db, values, c, callerIsSyncAdapter);
			return true;
		}
		
		@Override
		public int delete(SQLiteDatabase db, Cursor c) {
			int count = super.delete(db, c);
			
			return count;
		}
	}

	
  private void setPlaceDirty(long rawContactId) {
    mDirtyPlaces.add(rawContactId);
  }

}
