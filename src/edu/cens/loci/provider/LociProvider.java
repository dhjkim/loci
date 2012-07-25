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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import edu.cens.loci.LociConfig;
import edu.cens.loci.provider.LociContract.Data;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.BaseTypes;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Keyword;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Photo;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.StructuredPostal;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;
import edu.cens.loci.provider.LociDatabaseHelper.DataColumns;
import edu.cens.loci.provider.LociDatabaseHelper.MimetypesColumns;
import edu.cens.loci.provider.LociDatabaseHelper.Tables;
import edu.cens.loci.provider.LociDatabaseHelper.Views;
import edu.cens.loci.utils.MyLog;

public class LociProvider extends SQLiteContentProvider {

	private static final String TAG = "LociProvider";
	
	/** Contains the columns from the raw contacts entity view*/
  private static final HashMap<String, String> sPlacesEntityProjectionMap;
  /** Contains columns from the data view */
  private static final HashMap<String, String> sDataProjectionMap;
  /** Contains columns from the data view */
  private static final HashMap<String, String> sDistinctDataProjectionMap;
	
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	
	// uri code
	private static final int PLACES = 1000;
	private static final int PLACES_ID = 1001;
	private static final int PLACES_DATA = 1002;
	private static final int PLACES_WIFI = 1003;
	private static final int PLACES_GPS = 1004;
	private static final int PLACES_KEYWORD = 1005;
	
	private static final int PLACE_ENTITIES = 15001;
	
	private static final int VISITS = 2000;
	private static final int VISITS_ID = 2001;
	private static final int VISITS_PLACEID = 2002;
	
	private static final int TRACKS = 3000;

	private static final int DATA = 4000;
	private static final int DATA_ID = 4001;
	private static final int WIFI_FINGERPRINT = 4002;
	private static final int WIFI_FINGERPRINT_ID = 4003;
	private static final int GPS_CIRCLE = 4004;
	private static final int GPS_CIRCLE_ID = 4005;
	private static final int KEYWORD = 4006;
	private static final int KEYWORD_ID = 4007;
	
	static {
		final UriMatcher matcher = sUriMatcher;
		matcher.addURI(LociContract.AUTHORITY, "places", PLACES);
		matcher.addURI(LociContract.AUTHORITY, "places/#", PLACES_ID);
		matcher.addURI(LociContract.AUTHORITY, "places/#/data", PLACES_DATA);
		matcher.addURI(LociContract.AUTHORITY, "places/#/wifi", PLACES_WIFI);
		matcher.addURI(LociContract.AUTHORITY, "places/#/gps", PLACES_GPS);
		matcher.addURI(LociContract.AUTHORITY, "places/#/keyword", PLACES_KEYWORD);
		
		matcher.addURI(LociContract.AUTHORITY, "data", DATA);
		matcher.addURI(LociContract.AUTHORITY, "data/#", DATA_ID);
		matcher.addURI(LociContract.AUTHORITY, "data/wifi", WIFI_FINGERPRINT);
		matcher.addURI(LociContract.AUTHORITY, "data/wifi/#", WIFI_FINGERPRINT_ID);
		matcher.addURI(LociContract.AUTHORITY, "data/gps", GPS_CIRCLE);
		matcher.addURI(LociContract.AUTHORITY, "data/gps/#", GPS_CIRCLE_ID);
		matcher.addURI(LociContract.AUTHORITY, "data/keyword", KEYWORD);
		matcher.addURI(LociContract.AUTHORITY, "data/keyword/#", KEYWORD_ID);
		
		matcher.addURI(LociContract.AUTHORITY, "place_entities", PLACE_ENTITIES);
		
		matcher.addURI(LociContract.AUTHORITY, "visits", VISITS);
		matcher.addURI(LociContract.AUTHORITY, "visits/#", VISITS_ID);
		matcher.addURI(LociContract.AUTHORITY, "visits/place/#", VISITS_PLACEID);
		
		matcher.addURI(LociContract.AUTHORITY, "tracks", TRACKS);
	}
	
	static {
    sDataProjectionMap = new HashMap<String, String>();
    sDataProjectionMap.put(Data._ID, Data._ID);
    sDataProjectionMap.put(Data.PLACE_ID, Data.PLACE_ID);
    sDataProjectionMap.put(Data.DATA_VERSION, Data.DATA_VERSION);
    //sDataProjectionMap.put(Data.IS_PRIMARY, Data.IS_PRIMARY);
    //sDataProjectionMap.put(Data.IS_SUPER_PRIMARY, Data.IS_SUPER_PRIMARY);
    //sDataProjectionMap.put(Data.RES_PACKAGE, Data.RES_PACKAGE);
    sDataProjectionMap.put(Data.MIMETYPE, Data.MIMETYPE);
    sDataProjectionMap.put(Data.DATA1, Data.DATA1);
    sDataProjectionMap.put(Data.DATA2, Data.DATA2);
    sDataProjectionMap.put(Data.DATA3, Data.DATA3);
    sDataProjectionMap.put(Data.DATA4, Data.DATA4);
    sDataProjectionMap.put(Data.DATA5, Data.DATA5);
    sDataProjectionMap.put(Data.DATA6, Data.DATA6);
    sDataProjectionMap.put(Data.DATA7, Data.DATA7);
    sDataProjectionMap.put(Data.DATA8, Data.DATA8);
    sDataProjectionMap.put(Data.DATA9, Data.DATA9);
    sDataProjectionMap.put(Data.DATA10, Data.DATA10);
    sDataProjectionMap.put(Data.DATA11, Data.DATA11);
    sDataProjectionMap.put(Data.DATA12, Data.DATA12);
    sDataProjectionMap.put(Data.DATA13, Data.DATA13);
    sDataProjectionMap.put(Data.DATA14, Data.DATA14);
    sDataProjectionMap.put(Data.DATA15, Data.DATA15);
    sDataProjectionMap.put(Data.SYNC1, Data.SYNC1);
    sDataProjectionMap.put(Data.SYNC2, Data.SYNC2);
    sDataProjectionMap.put(Data.SYNC3, Data.SYNC3);
    sDataProjectionMap.put(Data.SYNC4, Data.SYNC4);
    //sDataProjectionMap.put(Data.CONTACT_ID, Data.CONTACT_ID);
    sDataProjectionMap.put(Places.ACCOUNT_NAME, Places.ACCOUNT_NAME);
    sDataProjectionMap.put(Places.ACCOUNT_TYPE, Places.ACCOUNT_TYPE);
    sDataProjectionMap.put(Places.SOURCE_ID, RawContacts.SOURCE_ID);
    sDataProjectionMap.put(Places.VERSION, RawContacts.VERSION);
    sDataProjectionMap.put(Places.DIRTY, Places.DIRTY);
    //sDataProjectionMap.put(RawContacts.NAME_VERIFIED, RawContacts.NAME_VERIFIED);

		
    HashMap<String, String> columns;
    columns = new HashMap<String, String>();
    columns.put(Places._ID, Places._ID);
    columns.put(Places.PLACE_NAME, Places.PLACE_NAME);
    columns.put(Places.PLACE_STATE, Places.PLACE_STATE);
    columns.put(Places.PLACE_TYPE, Places.PLACE_TYPE);
    //columns.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
    columns.put(Places.ACCOUNT_NAME, Places.ACCOUNT_NAME);
    columns.put(Places.ACCOUNT_TYPE, Places.ACCOUNT_TYPE);
    columns.put(Places.SOURCE_ID, Places.SOURCE_ID);
    columns.put(Places.VERSION, Places.VERSION);
    columns.put(Places.DIRTY, Places.DIRTY);
    columns.put(Places.DELETED, Places.DELETED);
    //columns.put(RawContacts.IS_RESTRICTED, RawContacts.IS_RESTRICTED);
    columns.put(Places.SYNC1, Places.SYNC1);
    columns.put(Places.SYNC2, Places.SYNC2);
    columns.put(Places.SYNC3, Places.SYNC3);
    columns.put(Places.SYNC4, Places.SYNC4);
    //columns.put(RawContacts.NAME_VERIFIED, RawContacts.NAME_VERIFIED);
    //columns.put(Data.RES_PACKAGE, Data.RES_PACKAGE);
    columns.put(Data.MIMETYPE, Data.MIMETYPE);
    columns.put(Data.DATA1, Data.DATA1);
    columns.put(Data.DATA2, Data.DATA2);
    columns.put(Data.DATA3, Data.DATA3);
    columns.put(Data.DATA4, Data.DATA4);
    columns.put(Data.DATA5, Data.DATA5);
    columns.put(Data.DATA6, Data.DATA6);
    columns.put(Data.DATA7, Data.DATA7);
    columns.put(Data.DATA8, Data.DATA8);
    columns.put(Data.DATA9, Data.DATA9);
    columns.put(Data.DATA10, Data.DATA10);
    columns.put(Data.DATA11, Data.DATA11);
    columns.put(Data.DATA12, Data.DATA12);
    columns.put(Data.DATA13, Data.DATA13);
    columns.put(Data.DATA14, Data.DATA14);
    columns.put(Data.DATA15, Data.DATA15);
    columns.put(Data.SYNC1, Data.SYNC1);
    columns.put(Data.SYNC2, Data.SYNC2);
    columns.put(Data.SYNC3, Data.SYNC3);
    columns.put(Data.SYNC4, Data.SYNC4);
    columns.put(Places.Entity.DATA_ID, Places.Entity.DATA_ID);
    columns.put(Places.STARRED, Places.STARRED);
    columns.put(Data.DATA_VERSION, Data.DATA_VERSION);
    //columns.put(Data.IS_PRIMARY, Data.IS_PRIMARY);
    //columns.put(Data.IS_SUPER_PRIMARY, Data.IS_SUPER_PRIMARY);
    //columns.put(GroupMembership.GROUP_SOURCE_ID, GroupMembership.GROUP_SOURCE_ID);
    sPlacesEntityProjectionMap = columns;
    
    // Projection map for data grouped by contact (not raw contact) and some data field(s)
    sDistinctDataProjectionMap = new HashMap<String, String>();
    sDistinctDataProjectionMap.put(Data._ID,
            "MIN(" + Data._ID + ") AS " + Data._ID);
    sDistinctDataProjectionMap.put(Data.DATA_VERSION, Data.DATA_VERSION);
    //sDistinctDataProjectionMap.put(Data.IS_PRIMARY, Data.IS_PRIMARY);
    //sDistinctDataProjectionMap.put(Data.IS_SUPER_PRIMARY, Data.IS_SUPER_PRIMARY);
    //sDistinctDataProjectionMap.put(Data.RES_PACKAGE, Data.RES_PACKAGE);
    sDistinctDataProjectionMap.put(Data.MIMETYPE, Data.MIMETYPE);
    sDistinctDataProjectionMap.put(Data.DATA1, Data.DATA1);
    sDistinctDataProjectionMap.put(Data.DATA2, Data.DATA2);
    sDistinctDataProjectionMap.put(Data.DATA3, Data.DATA3);
    sDistinctDataProjectionMap.put(Data.DATA4, Data.DATA4);
    sDistinctDataProjectionMap.put(Data.DATA5, Data.DATA5);
    sDistinctDataProjectionMap.put(Data.DATA6, Data.DATA6);
    sDistinctDataProjectionMap.put(Data.DATA7, Data.DATA7);
    sDistinctDataProjectionMap.put(Data.DATA8, Data.DATA8);
    sDistinctDataProjectionMap.put(Data.DATA9, Data.DATA9);
    sDistinctDataProjectionMap.put(Data.DATA10, Data.DATA10);
    sDistinctDataProjectionMap.put(Data.DATA11, Data.DATA11);
    sDistinctDataProjectionMap.put(Data.DATA12, Data.DATA12);
    sDistinctDataProjectionMap.put(Data.DATA13, Data.DATA13);
    sDistinctDataProjectionMap.put(Data.DATA14, Data.DATA14);
    sDistinctDataProjectionMap.put(Data.DATA15, Data.DATA15);
    sDistinctDataProjectionMap.put(Data.SYNC1, Data.SYNC1);
    sDistinctDataProjectionMap.put(Data.SYNC2, Data.SYNC2);
    sDistinctDataProjectionMap.put(Data.SYNC3, Data.SYNC3);
    sDistinctDataProjectionMap.put(Data.SYNC4, Data.SYNC4);
    //sDistinctDataProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);

	}

	public static long checkPlaceIdUri(Uri uri) {
		switch (sUriMatcher.match(uri)) {
  	case PLACES_ID:
  		//Log.d(TAG, LociContract.Places.CONTENT_ITEM_TYPE);
  		return Long.parseLong(uri.getPathSegments().get(1));
  	default:
  		return -1;
		}
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
  
  private interface PlacesQuery {
  	String TABLE = Tables.PLACES;
  	String[] COLUMNS = new String[] {
  			Places.DELETED,
  			Places.ACCOUNT_TYPE,
  			Places.ACCOUNT_NAME,
  	};
  	
  	int DELETED = 0;
  	int ACCOUNT_TYPE = 1;
  	int ACCOUNT_NAME = 2;
  }
	
  /** Sql for updating DIRTY flag on multiple raw contacts */
  private static final String UPDATE_PLACE_SET_DIRTY_SQL =
          "UPDATE " + Tables.PLACES +
          " SET " + Places.DIRTY + "=1" +
          " WHERE " + Places._ID + " IN (";
  
  /** Sql for updating VERSION on multiple raw contacts */
  private static final String UPDATE_PLACE_SET_VERSION_SQL =
          "UPDATE " + Tables.PLACES +
          " SET " + Places.VERSION + " = " + Places.VERSION + " + 1" +
          " WHERE " + Places._ID + " IN (";

  
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

			//Log.d(TAG, "DataRowHandler.insert: placeId=" + placeId);
			
			return dataId;
		}
		
		public boolean update(SQLiteDatabase db, ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
			long dataId = c.getLong(DataUpdateQuery._ID);
			long placeId = c.getLong(DataUpdateQuery.PLACE_ID);

			//Log.d(TAG, "DataRowHandler.update: placeId=" + placeId + " ,dataId=" + dataId);
			
			if (values.size() > 0) {
				mSelectionArgs1[0] = String.valueOf(dataId);
        mDb.update(Tables.DATA, values, Data._ID + "=?", mSelectionArgs1);
			}
			
      if (!callerIsSyncAdapter) {
        setPlaceDirty(placeId);
      }
			
			return true;
		}

    public int delete(SQLiteDatabase db, Cursor c) {
      long dataId = c.getLong(DataDeleteQuery._ID);
      //long placeId = c.getLong(DataDeleteQuery.PLACE_ID);
      
			//Log.d(TAG, "DataRowHandler.delete: dataId=" + dataId);
      
      mSelectionArgs1[0] = String.valueOf(dataId);
      int count = db.delete(Tables.DATA, Data._ID + "=?", mSelectionArgs1);
      return count;
    }

    
    /**
     * Return set of values, using current values at given {@link Data#_ID}
     * as baseline, but augmented with any updates.  Returns null if there is
     * no change.
     */
    public ContentValues getAugmentedValues(SQLiteDatabase db, long dataId,
            ContentValues update) {
        boolean changing = false;
        final ContentValues values = new ContentValues();
        mSelectionArgs1[0] = String.valueOf(dataId);
        final Cursor cursor = db.query(Tables.DATA, null, Data._ID + "=?",
                mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    final String key = cursor.getColumnName(i);
                    final String value = cursor.getString(i);
                    if (!changing && update.containsKey(key)) {
                        Object newValue = update.get(key);
                        String newString = newValue == null ? null : newValue.toString();
                        changing |= !TextUtils.equals(newString, value);
                    }
                    values.put(key, value);
                }
            }
        } finally {
            cursor.close();
        }
        if (!changing) {
            return null;
        }

        values.putAll(update);
        return values;
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
	
	public class GpsCircleAreaRowHandler extends DataRowHandler {
		public GpsCircleAreaRowHandler() {
			super(GpsCircleArea.CONTENT_ITEM_TYPE);
		}
		
		@Override
		public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
			//Log.d(TAG, "gpsRowHandler: insert");
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
			//Log.d(TAG, "keywordRowHandler: insert");
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
	

  public class StructuredPostalRowHandler extends DataRowHandler {
      private PostalSplitter mSplitter;

      public StructuredPostalRowHandler(PostalSplitter splitter) {
          super(StructuredPostal.CONTENT_ITEM_TYPE);
          mSplitter = splitter;
      }

      @Override
      public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
      	//Log.d(TAG, "postalRowHandler: insert");

          fixStructuredPostalComponents(values, values);
          return super.insert(db, rawContactId, values);
      }

      @Override
      public boolean update(SQLiteDatabase db, ContentValues values, Cursor c,
              boolean callerIsSyncAdapter) {
          final long dataId = c.getLong(DataUpdateQuery._ID);
          final ContentValues augmented = getAugmentedValues(db, dataId, values);
          if (augmented == null) {    // No change
              return false;
          }

          fixStructuredPostalComponents(augmented, values);
          super.update(db, values, c, callerIsSyncAdapter);
          return true;
      }

      /**
       * Specific list of structured fields.
       */
      private final String[] STRUCTURED_FIELDS = new String[] {
              StructuredPostal.STREET, StructuredPostal.POBOX, StructuredPostal.NEIGHBORHOOD,
              StructuredPostal.CITY, StructuredPostal.REGION, StructuredPostal.POSTCODE,
              StructuredPostal.COUNTRY,
      };

      /**
       * Prepares the given {@link StructuredPostal} row, building
       * {@link StructuredPostal#FORMATTED_ADDRESS} to match the structured
       * values when missing. When structured components are missing, the
       * unstructured value is assigned to {@link StructuredPostal#STREET}.
       */
      private void fixStructuredPostalComponents(ContentValues augmented, ContentValues update) {
          final String unstruct = update.getAsString(StructuredPostal.FORMATTED_ADDRESS);

          final boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
          final boolean touchedStruct = !areAllEmpty(update, STRUCTURED_FIELDS);

          final PostalSplitter.Postal postal = new PostalSplitter.Postal();

          if (touchedUnstruct && !touchedStruct) {
              mSplitter.split(postal, unstruct);
              postal.toValues(update);
          } else if (!touchedUnstruct
                  && (touchedStruct || areAnySpecified(update, STRUCTURED_FIELDS))) {
              // See comment in
              postal.fromValues(augmented);
              final String joined = mSplitter.join(postal);
              update.put(StructuredPostal.FORMATTED_ADDRESS, joined);
          }
      }
  }

  public class CommonDataRowHandler extends DataRowHandler {

      private final String mTypeColumn;
      private final String mLabelColumn;

      public CommonDataRowHandler(String mimetype, String typeColumn, String labelColumn) {
          super(mimetype);
          mTypeColumn = typeColumn;
          mLabelColumn = labelColumn;
      }

      @Override
      public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
  			//Log.d(TAG, "commonRowHandler: insert");

          enforceTypeAndLabel(values, values);
          return super.insert(db, rawContactId, values);
      }

      @Override
      public boolean update(SQLiteDatabase db, ContentValues values, Cursor c,
              boolean callerIsSyncAdapter) {
          final long dataId = c.getLong(DataUpdateQuery._ID);
          final ContentValues augmented = getAugmentedValues(db, dataId, values);
          if (augmented == null) {        // No change
              return false;
          }
          enforceTypeAndLabel(augmented, values);
          return super.update(db, values, c, callerIsSyncAdapter);
      }

      /**
       * If the given {@link ContentValues} defines {@link #mTypeColumn},
       * enforce that {@link #mLabelColumn} only appears when type is
       * {@link BaseTypes#TYPE_CUSTOM}. Exception is thrown otherwise.
       */
      private void enforceTypeAndLabel(ContentValues augmented, ContentValues update) {
          final boolean hasType = !TextUtils.isEmpty(augmented.getAsString(mTypeColumn));
          final boolean hasLabel = !TextUtils.isEmpty(augmented.getAsString(mLabelColumn));

          if (hasLabel && !hasType) {
              // When label exists, assert that some type is defined
              throw new IllegalArgumentException(mTypeColumn + " must be specified when "
                      + mLabelColumn + " is defined.");
          }
      }
  }

  
  public class PhotoDataRowHandler extends DataRowHandler {

    public PhotoDataRowHandler() {
        super(Photo.CONTENT_ITEM_TYPE);
    }

    @Override
    public long insert(SQLiteDatabase db, long placeId, ContentValues values) {
        long dataId = super.insert(db, placeId, values);
  			//Log.d(TAG, "photoRowHandler: insert");

        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, ContentValues values, Cursor c,
            boolean callerIsSyncAdapter) {
        if (!super.update(db, values, c, callerIsSyncAdapter)) {
            return false;
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, Cursor c) {
        int count = super.delete(db, c);
        return count;
    }
}

	
  public class CustomDataRowHandler extends DataRowHandler {

    public CustomDataRowHandler(String mimetype) {
        super(mimetype);
    }
}

  private HashSet<Long> mDirtyPlaces = new HashSet<Long>();
	
  
  
  private void setPlaceDirty(long placeId) {
    mDirtyPlaces.add(placeId);
  }
	
  private ContentValues mValues = new ContentValues();
  private HashSet<Long> mUpdatedPlaces = new HashSet<Long>();

  private HashMap<String, DataRowHandler> mDataRowHandlers;
  private LociDatabaseHelper mDbHelper;

  private PostalSplitter mPostalSplitter;
  private Locale mCurrentLocale;

  
	@Override
	protected LociDatabaseHelper getDatabaseHelper(Context context) {
		return LociDatabaseHelper.getInstance(context);
	}
  
	@Override
	public boolean onCreate() {
		super.onCreate();
		try {
			return initialize();
		} catch (RuntimeException e) {
			Log.e(TAG, "Cannot start provider", e);
			return false;
		}
	}

  /* Visible for testing */
  protected Locale getLocale() {
      return Locale.getDefault();
  }
  
	
	private boolean initialize() {
		mDbHelper = (LociDatabaseHelper) getDatabaseHelper();
		mDb = mDbHelper.getWritableDatabase();
		
		
		mCurrentLocale = getLocale();
    mPostalSplitter = new PostalSplitter(mCurrentLocale);
		initDataRowHandlers();
		
		mMimeTypeIdWifi = mDbHelper.getMimeTypeId(WifiFingerprint.CONTENT_ITEM_TYPE);
		mMimeTypeIdGps = mDbHelper.getMimeTypeId(GpsCircleArea.CONTENT_ITEM_TYPE);
		mMimeTypeIdKeyword = mDbHelper.getMimeTypeId(Keyword.CONTENT_ITEM_TYPE);
		return (mDb != null);
	}
	
	private void initDataRowHandlers() {
		mDataRowHandlers = new HashMap<String, DataRowHandler>();
		mDataRowHandlers.put(GpsCircleArea.CONTENT_ITEM_TYPE, new GpsCircleAreaRowHandler());
		mDataRowHandlers.put(WifiFingerprint.CONTENT_ITEM_TYPE, new WifiFingerprintRowHandler());
		mDataRowHandlers.put(Keyword.CONTENT_ITEM_TYPE, new KeywordRowHandler());
    mDataRowHandlers.put(StructuredPostal.CONTENT_ITEM_TYPE,
        new StructuredPostalRowHandler(mPostalSplitter));
    mDataRowHandlers.put(Photo.CONTENT_ITEM_TYPE, new PhotoDataRowHandler());

	}
  
  
  public String getType(Uri uri) {
  	
  	//Log.d(TAG, "getType():" + uri.toString());
  	 
  	switch (sUriMatcher.match(uri)) {
  	case PLACES:
  		//Log.d(TAG, LociContract.Places.CONTENT_TYPE);
  		return LociContract.Places.CONTENT_TYPE;
  	case PLACES_ID:
  		//Log.d(TAG, LociContract.Places.CONTENT_ITEM_TYPE);
  		return LociContract.Places.CONTENT_ITEM_TYPE;
  	case PLACES_WIFI:
  		return WifiFingerprint.CONTENT_TYPE;
  	case PLACES_GPS:
  		return GpsCircleArea.CONTENT_TYPE;
  	case PLACES_KEYWORD:
  		return Keyword.CONTENT_TYPE;
  	case VISITS_ID:
  		return LociContract.Visits.CONTENT_ITEM_TYPE;
  	default:
  		//Log.d(TAG, "Unknown uri");
  		throw new IllegalArgumentException("Unknown URI " + uri);
  	}
  		
  }
	

  /**
   * Test all against {@link TextUtils#isEmpty(CharSequence)}.
   */
  private static boolean areAllEmpty(ContentValues values, String[] keys) {
      for (String key : keys) {
          if (!TextUtils.isEmpty(values.getAsString(key))) {
              return false;
          }
      }
      return true;
  }

  /**
   * Returns true if a value (possibly null) is specified for at least one of the supplied keys.
   */
  private static boolean areAnySpecified(ContentValues values, String[] keys) {
      for (String key : keys) {
          if (values.containsKey(key)) {
              return true;
          }
      }
      return false;
  }

  
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
	
		//Log.v(TAG, "query: " + uri);
		
		final SQLiteDatabase db = mDbHelper.getReadableDatabase();
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String groupBy = null;
		String limit = getLimit(uri);
		
		final int match = sUriMatcher.match(uri);
		
		//Log.d(TAG, "query: match=" + match);
		
		switch (match) {
		case PLACES:
			qb.setTables(Tables.PLACES);
			break;
		case PLACES_ID:
			qb.setTables(Tables.PLACES);
			qb.appendWhere(Places._ID + "=" + uri.getPathSegments().get(1));
			break;
		case PLACES_WIFI:
			//Log.d(TAG, "PLACES_WIFI:");
			long placeId = Long.parseLong(uri.getPathSegments().get(1));
			qb.setTables(Views.DATA_ALL);
			selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(placeId));
			qb.appendWhere(Data.MIMETYPE + " = '" + WifiFingerprint.CONTENT_ITEM_TYPE + "'");
			qb.appendWhere(" AND " + Data.PLACE_ID + "=?");

			//Log.d(TAG, "qb: " + qb.toString());
			break;
		case VISITS:
			//Log.d(TAG, "VISITS:");
			break;
		case VISITS_ID:
			//Log.d(TAG, "VISITS_ID:");
			long visitId = Long.parseLong(uri.getPathSegments().get(1));
			qb.setTables(Tables.VISITS);
			qb.appendWhere(Visits._ID + "=" + visitId);
			break;
		case VISITS_PLACEID:
			///Log.d(TAG, "VISITS_PLACEID");
			placeId = Long.parseLong(uri.getPathSegments().get(2));
			qb.setTables(Tables.VISITS);
			qb.appendWhere(Visits.PLACE_ID + "=" + placeId);
			break;
		case TRACKS:
			break;
		case DATA:
			setTablesAndProjectionMapForData(qb, uri, projection, false);
			break;
		case DATA_ID:
			setTablesAndProjectionMapForData(qb, uri, projection, false);
			selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
			qb.appendWhere(" AND " + Data._ID + "=?");
			break;
		case PLACE_ENTITIES:
			setTablesAndProjectionMapForPlaceEntities(qb, uri);
			break;
		default:
			MyLog.e(LociConfig.D.ERROR, TAG, "query: Unsupported Uri.");
		}
		
		Cursor cursor = query(db, qb, projection, selection, selectionArgs, sortOrder, groupBy, limit);
		
		return cursor;
	}
	
  private Cursor query(final SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection,
      String selection, String[] selectionArgs, String sortOrder, String groupBy,
      String limit) {
	  final Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null,
	          sortOrder, limit);
	  if (c != null) {
	      c.setNotificationUri(getContext().getContentResolver(), ContactsContract.AUTHORITY_URI);
	  }
	  return c;
	}
  
  private void setTablesAndProjectionMapForPlaceEntities(SQLiteQueryBuilder qb, Uri uri) {
    // Note: currently, "export only" equals to "restricted", but may not in the future.
    qb.setTables(mDbHelper.getPlaceEntitiesView());
    qb.setProjectionMap(sPlacesEntityProjectionMap);
    //appendAccountFromParameter(qb, uri);
  }
  
  private void setTablesAndProjectionMapForData(SQLiteQueryBuilder qb, Uri uri,
      String[] projection, boolean distinct) {
  StringBuilder sb = new StringBuilder();

  sb.append(mDbHelper.getDataView());
  sb.append(" data");

  qb.setTables(sb.toString());
  qb.setProjectionMap(distinct ? sDistinctDataProjectionMap : sDataProjectionMap);
  appendAccountFromParameter(qb, uri);
}

	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//Log.i(TAG, "insert() " + uri.toString());
		return super.insert(uri, values);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		//Log.i(TAG, "delete() " + uri.toString());
		return super.delete(uri, selection, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		//Log.i(TAG, "update() " + uri.toString());
		return super.update(uri, values, selection, selectionArgs);
	}

	private void clearTransactionalChanges() {
		mDirtyPlaces.clear();
    //mInsertedPlaces.clear();
    mUpdatedPlaces.clear();
    //mUpdatedSyncStates.clear();
	}
	
	private void flushTransactionalChanges() {
		//Log.i(TAG, "flush");
		
		if (!this.mDirtyPlaces.isEmpty()) {
			mSb.setLength(0);
			mSb.append(UPDATE_PLACE_SET_DIRTY_SQL);
			appendIds(mSb, mDirtyPlaces);
			mSb.append(")");
			mDb.execSQL(mSb.toString());
		}
		

    if (!mUpdatedPlaces.isEmpty()) {
        mSb.setLength(0);
        mSb.append(UPDATE_PLACE_SET_VERSION_SQL);
        appendIds(mSb, mUpdatedPlaces);
        mSb.append(")");
        mDb.execSQL(mSb.toString());
    }
		
		clearTransactionalChanges();
	}
	
  private DataRowHandler getDataRowHandler(final String mimeType) {
    DataRowHandler handler = mDataRowHandlers.get(mimeType);
    if (handler == null) {
        handler = new CustomDataRowHandler(mimeType);
        mDataRowHandlers.put(mimeType, handler);
    }
    return handler;
  }
	
	@Override
	protected Uri insertInTransaction(Uri uri, ContentValues values) {
		//MyLog.v(true, TAG, "insertInTransaction: " + uri + " " + values);
		
		boolean callerIsSyncAdapter = false; // don't clear dirty
		
		final int match = sUriMatcher.match(uri);
		long id = 0;
		
		//Log.d(TAG, "match=" + match);
		
		switch (match) {
			case PLACES: {
				id = insertPlace(values);
				break;
			}
			case VISITS:
				break;
			case TRACKS:
				break;
			case DATA: {
				id = insertData(values, callerIsSyncAdapter); 
				mSyncToNetwork |= !callerIsSyncAdapter;
				break;
			}
		}
		if (id < 0)
			return null;
		return ContentUris.withAppendedId(uri, id);
	}
	
	private long insertPlace(ContentValues values) {
		mValues.clear();
		mValues.putAll(values);
		
		Log.d(TAG, mValues.toString());
		
		long placeId = mDb.insert(Tables.PLACES, null, mValues);
		//mInsertedPlaces.
		
		return placeId;
	}
	
  /**
   * Inserts an item in the data table
   *
   * @param values the values for the new row
   * @return the row ID of the newly created row
   */
  private long insertData(ContentValues values, boolean callerIsSyncAdapter) {
      long id = 0;
      mValues.clear();
      mValues.putAll(values);

      long placeId = mValues.getAsLong(Data.PLACE_ID);

      // Replace package with internal mapping
      //final String packageName = mValues.getAsString(Data.RES_PACKAGE);
      //if (packageName != null) {
      //    mValues.put(DataColumns.PACKAGE_ID, mDbHelper.getPackageId(packageName));
      //}
      //mValues.remove(Data.RES_PACKAGE);

      //Log.d(TAG, "insertData: values=" + mValues.toString());
      
      // Replace mimetype with internal mapping
      final String mimeType = mValues.getAsString(Data.MIMETYPE);
      if (TextUtils.isEmpty(mimeType)) {
          throw new IllegalArgumentException(Data.MIMETYPE + " is required");
      }

      mValues.put(DataColumns.MIMETYPE_ID, mDbHelper.getMimeTypeId(mimeType));
      mValues.remove(Data.MIMETYPE);

      DataRowHandler rowHandler = getDataRowHandler(mimeType);
      id = rowHandler.insert(mDb, placeId, mValues);
      if (!callerIsSyncAdapter) {
          setPlaceDirty(placeId);
      }
      mUpdatedPlaces.add(placeId);
      return id;
  }

	
	@Override
	protected int deleteInTransaction(Uri uri, String selection,
			String[] selectionArgs) {
		//MyLog.v(Provider.LOG_EVENT, TAG, "deleteInTransaction: " + uri);
		flushTransactionalChanges();
		final boolean callerIsSyncAdapter = false;
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case PLACES:
		case PLACES_ID:
			final long placeId = ContentUris.parseId(uri);
			return deletePlace(placeId, callerIsSyncAdapter);
		case VISITS:
		case TRACKS:
		case DATA: {
			mSyncToNetwork |= !callerIsSyncAdapter;
			return deleteData(appendAccountToSelection(uri, selection), selectionArgs, callerIsSyncAdapter);
		}
		case DATA_ID:
			long dataId = ContentUris.parseId(uri);
			mSyncToNetwork |= !callerIsSyncAdapter;
			mSelectionArgs1[0] = String.valueOf(dataId);
			return deleteData(Data._ID + "=?", mSelectionArgs1, callerIsSyncAdapter);
		}
		return 0;
	}
	
  public int deletePlace(long placeId, boolean callerIsSyncAdapter) {
    if (callerIsSyncAdapter) {
        int count = mDb.delete(Tables.PLACES, Places._ID + "=" + placeId, null);
        return count;
    } else {
        return markPlaceAsDeleted(placeId);
    }
  }
  
  /**
   * Delete data row by row so that fixing of primaries etc work correctly.
   */
  private int deleteData(String selection, String[] selectionArgs, boolean callerIsSyncAdapter) {
      int count = 0;

      // Note that the query will return data according to the access restrictions,
      // so we don't need to worry about deleting data we don't have permission to read.
      Cursor c = query(Data.CONTENT_URI, DataDeleteQuery.COLUMNS, selection, selectionArgs, null);
      try {
          while(c.moveToNext()) {
              long placeId = c.getLong(DataDeleteQuery.PLACE_ID);
              String mimeType = c.getString(DataDeleteQuery.MIMETYPE);
              DataRowHandler rowHandler = getDataRowHandler(mimeType);
              count += rowHandler.delete(mDb, c);
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
   * Delete a data row provided that it is one of the allowed mime types.
   */
  public int deleteData(long dataId, String[] allowedMimeTypes) {

      // Note that the query will return data according to the access restrictions,
      // so we don't need to worry about deleting data we don't have permission to read.
      mSelectionArgs1[0] = String.valueOf(dataId);
      Cursor c = query(Data.CONTENT_URI, DataDeleteQuery.COLUMNS, Data._ID + "=?",
              mSelectionArgs1, null);

      try {
          if (!c.moveToFirst()) {
              return 0;
          }

          String mimeType = c.getString(DataDeleteQuery.MIMETYPE);
          boolean valid = false;
          for (int i = 0; i < allowedMimeTypes.length; i++) {
              if (TextUtils.equals(mimeType, allowedMimeTypes[i])) {
                  valid = true;
                  break;
              }
          }

          if (!valid) {
              throw new IllegalArgumentException("Data type mismatch: expected .. erased ");
          }

          DataRowHandler rowHandler = getDataRowHandler(mimeType);
          return rowHandler.delete(mDb, c);
      } finally {
          c.close();
      }
  }

  
  private int markPlaceAsDeleted(long placeId) {
    mSyncToNetwork = true;

    mValues.clear();
    mValues.put(Places.DELETED, 1);
    mValues.put(Places.DIRTY, 1);
    return updatePlace(placeId, mValues);
}



	@Override
	protected int updateInTransaction(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {
		//MyLog.v(true, TAG, "updateInTransaction: " + uri + " " + values);
		int count = 0;
		final int match = sUriMatcher.match(uri);
		flushTransactionalChanges();
		final boolean callerIsSyncAdapter = false;
		switch (match) {
			case PLACES:
				selection = appendAccountToSelection(uri, selection);
				count = updatePlaces(values, selection, selectionArgs);
				break;
			case PLACES_ID:
				long placeId = ContentUris.parseId(uri);
				if (selection != null) {
					selectionArgs = insertSelectionArg(selectionArgs, String.valueOf(placeId));
					count = updatePlaces(values, Places._ID + "=?" + "AND(" + selection + ")", selectionArgs);
				} else {
					mSelectionArgs1[0] = String.valueOf(placeId);
					count = updatePlaces(values, Places._ID + "=?", mSelectionArgs1);
				}
			case VISITS:
			case TRACKS:
			case DATA: {
				count = updateData(uri, values, appendAccountToSelection(uri, selection), 
								selectionArgs, callerIsSyncAdapter);
				
				if (count > 0) {
					mSyncToNetwork |= !callerIsSyncAdapter;
				}
				break;
			}
			case DATA_ID: {
				count = updateData(uri, values, selection, selectionArgs, callerIsSyncAdapter);
				if (count > 0) {
					mSyncToNetwork |= !callerIsSyncAdapter;
				}
				break;
			}
		}
		return count;
	}
	
  private int updatePlaces(ContentValues values, String selection, String[] selectionArgs) {

    int count = 0;
    Cursor cursor = mDb.query(mDbHelper.getPlaceView(),
            new String[] { Places._ID }, selection,
            selectionArgs, null, null, null);
    try {
        while (cursor.moveToNext()) {
            long placeId = cursor.getLong(0);
            updatePlace(placeId, values);
            count++;
        }
    } finally {
        cursor.close();
    }

    return count;
  }

	private int updatePlace(long placeId, ContentValues values) {
	    final String selection = Places._ID + " = ?";
	    mSelectionArgs1[0] = Long.toString(placeId);
	    int count = mDb.update(Tables.PLACES, values, selection, mSelectionArgs1);
	    return count;
	}


  private int updateData(Uri uri, ContentValues values, String selection,
          String[] selectionArgs, boolean callerIsSyncAdapter) {
      mValues.clear();
      mValues.putAll(values);
      mValues.remove(Data._ID);
      mValues.remove(Data.PLACE_ID);
      mValues.remove(Data.MIMETYPE);

      //String packageName = values.getAsString(Data.RES_PACKAGE);
      //if (packageName != null) {
      //    mValues.remove(Data.RES_PACKAGE);
      //    mValues.put(DataColumns.PACKAGE_ID, mDbHelper.getPackageId(packageName));
      //}

      //boolean containsIsSuperPrimary = mValues.containsKey(Data.IS_SUPER_PRIMARY);
      //boolean containsIsPrimary = mValues.containsKey(Data.IS_PRIMARY);

      // Remove primary or super primary values being set to 0. This is disallowed by the
      // content provider.
      //if (containsIsSuperPrimary && mValues.getAsInteger(Data.IS_SUPER_PRIMARY) == 0) {
      //    containsIsSuperPrimary = false;
      //    mValues.remove(Data.IS_SUPER_PRIMARY);
      //}
      //if (containsIsPrimary && mValues.getAsInteger(Data.IS_PRIMARY) == 0) {
      //    containsIsPrimary = false;
      //    mValues.remove(Data.IS_PRIMARY);
      //}

      int count = 0;

      // Note that the query will return data according to the access restrictions,
      // so we don't need to worry about updating data we don't have permission to read.
      Cursor c = query(uri, DataUpdateQuery.COLUMNS, selection, selectionArgs, null);
      try {
          while(c.moveToNext()) {
              count += updateData(mValues, c, callerIsSyncAdapter);
          }
      } finally {
          c.close();
      }

      return count;
  }

  private int updateData(ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
      if (values.size() == 0) {
          return 0;
      }

      final String mimeType = c.getString(DataUpdateQuery.MIMETYPE);
      DataRowHandler rowHandler = getDataRowHandler(mimeType);
      if (rowHandler.update(mDb, values, c, callerIsSyncAdapter)) {
          return 1;
      } else {
          return 0;
      }
  }	
	@Override
	protected void notifyChange() {
    notifyChange(mSyncToNetwork);
    mSyncToNetwork = false;
	}
  protected void notifyChange(boolean syncToNetwork) {
    getContext().getContentResolver().notifyChange(ContactsContract.AUTHORITY_URI, null,
            syncToNetwork);
  }
  
  private void appendAccountFromParameter(SQLiteQueryBuilder qb, Uri uri) {
    final String accountName = getQueryParameter(uri, Places.ACCOUNT_NAME);
    final String accountType = getQueryParameter(uri, Places.ACCOUNT_TYPE);

    final boolean partialUri = TextUtils.isEmpty(accountName) ^ TextUtils.isEmpty(accountType);
    if (partialUri) {
        // Throw when either account is incomplete
        throw new IllegalArgumentException(mDbHelper.exceptionMessage(
                "Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
    }

    // Accounts are valid by only checking one parameter, since we've
    // already ruled out partial accounts.
    final boolean validAccount = !TextUtils.isEmpty(accountName);
    if (validAccount) {
        qb.appendWhere(Places.ACCOUNT_NAME + "="
                + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                + Places.ACCOUNT_TYPE + "="
                + DatabaseUtils.sqlEscapeString(accountType));
    } else {
        qb.appendWhere("1");
    }
}

private String appendAccountToSelection(Uri uri, String selection) {
    final String accountName = getQueryParameter(uri, RawContacts.ACCOUNT_NAME);
    final String accountType = getQueryParameter(uri, RawContacts.ACCOUNT_TYPE);

    final boolean partialUri = TextUtils.isEmpty(accountName) ^ TextUtils.isEmpty(accountType);
    if (partialUri) {
        // Throw when either account is incomplete
        throw new IllegalArgumentException(mDbHelper.exceptionMessage(
                "Must specify both or neither of ACCOUNT_NAME and ACCOUNT_TYPE", uri));
    }

    // Accounts are valid by only checking one parameter, since we've
    // already ruled out partial accounts.
    final boolean validAccount = !TextUtils.isEmpty(accountName);
    if (validAccount) {
        StringBuilder selectionSb = new StringBuilder(RawContacts.ACCOUNT_NAME + "="
                + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                + RawContacts.ACCOUNT_TYPE + "="
                + DatabaseUtils.sqlEscapeString(accountType));
        if (!TextUtils.isEmpty(selection)) {
            selectionSb.append(" AND (");
            selectionSb.append(selection);
            selectionSb.append(')');
        }
        return selectionSb.toString();
    } else {
        return selection;
    }
}
  
  /**
   * Gets the value of the "limit" URI query parameter.
   *
   * @return A string containing a non-negative integer, or <code>null</code> if
   *         the parameter is not set, or is set to an invalid value.
   */
  private String getLimit(Uri uri) {
      String limitParam = getQueryParameter(uri, "limit");
      if (limitParam == null) {
          return null;
      }
      // make sure that the limit is a non-negative integer
      try {
          int l = Integer.parseInt(limitParam);
          if (l < 0) {
              Log.w(TAG, "Invalid limit parameter: " + limitParam);
              return null;
          }
          return String.valueOf(l);
      } catch (NumberFormatException ex) {
          Log.w(TAG, "Invalid limit parameter: " + limitParam);
          return null;
      }
  }


  /**
   * A fast re-implementation of {@link Uri#getQueryParameter}
   */
  /* package */ static String getQueryParameter(Uri uri, String parameter) {
      String query = uri.getEncodedQuery();
      if (query == null) {
          return null;
      }

      int queryLength = query.length();
      int parameterLength = parameter.length();

      String value;
      int index = 0;
      while (true) {
          index = query.indexOf(parameter, index);
          if (index == -1) {
              return null;
          }

          index += parameterLength;

          if (queryLength == index) {
              return null;
          }

          if (query.charAt(index) == '=') {
              index++;
              break;
          }
      }

      int ampIndex = query.indexOf('&', index);
      if (ampIndex == -1) {
          value = query.substring(index);
      } else {
          value = query.substring(index, ampIndex);
      }

      return Uri.decode(value);
  }


  /**
   * Appends comma separated ids.
   * @param ids Should not be empty
   */
  private void appendIds(StringBuilder sb, HashSet<Long> ids) {
      for (long id : ids) {
          sb.append(id).append(',');
      }

      sb.setLength(sb.length() - 1); // Yank the last comma
  }
  
  /**
   * Inserts an argument at the beginning of the selection arg list.
   */
  private String[] insertSelectionArg(String[] selectionArgs, String arg) {
      if (selectionArgs == null) {
          return new String[] {arg};
      } else {
          int newLength = selectionArgs.length + 1;
          String[] newSelectionArgs = new String[newLength];
          newSelectionArgs[0] = arg;
          System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
          return newSelectionArgs;
      }
  }
}
