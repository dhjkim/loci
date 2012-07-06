package edu.cens.loci.provider;

import java.util.HashMap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import edu.cens.loci.LociConfig;
import edu.cens.loci.provider.LociContract.BaseColumns;
import edu.cens.loci.provider.LociContract.Data;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Tracks;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.utils.MyLog;

public class LociDatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "PlacesDatabaseHelper";
	
	private static final String DATABASE_NAME = "places.db";
	static final int DATABASE_VERSION = 7;
	
	public interface Tables {
		public static final String PLACES = "places";
		public static final String VISITS = "visits";
		public static final String TRACKS = "tracks";
		public static final String DATA = "data";
		public static final String MIMETYPES = "mimetypes";
		
    public static final String DATA_JOIN_MIMETYPES = "data "
      + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)";
    
    public static final String PLACE_ENTITIES = "place_entities_view";

	}
	
	public interface Views {
		public static final String DATA_ALL = "view_data";
		public static final String PLACES_ALL = "view_places";
	}
	
	protected interface PlacesColumns {
		public static final String CONCRETE_ID = Tables.PLACES + "." + BaseColumns._ID;
		public static final String CONCRETE_NAME = Tables.PLACES + "." + Places.PLACE_NAME;
		public static final String CONCRETE_TYPE = Tables.PLACES + "." + Places.PLACE_TYPE;
		public static final String CONCRETE_STATE = Tables.PLACES + "." + Places.PLACE_STATE;
		public static final String CONCRETE_TIMES_VISITED = Tables.PLACES + "." + Places.TIMES_VISITED;
		public static final String CONCRETE_EXTRA1 = Tables.PLACES + "." + Places.EXTRA1;
		public static final String CONCRETE_EXTRA2 = Tables.PLACES + "." + Places.EXTRA2;
		public static final String CONCRETE_EXTRA3 = Tables.PLACES + "." + Places.EXTRA3;
		public static final String CONCRETE_ENTRY = Tables.PLACES + "." + Places.ENTRY;
		public static final String CONCRETE_ENTRY_TIME = Tables.PLACES + "." + Places.ENTRY_TIME;
		public static final String CONCRETE_REGISTER_TIME	= Tables.PLACES + "." + Places.REGISTER_TIME;
    public static final String CONCRETE_ACCOUNT_NAME = Tables.PLACES + "." + Places.ACCOUNT_NAME;
    public static final String CONCRETE_ACCOUNT_TYPE = Tables.PLACES + "." + Places.ACCOUNT_TYPE;
    public static final String CONCRETE_SOURCE_ID = Tables.PLACES + "." + Places.SOURCE_ID;
    public static final String CONCRETE_VERSION = Tables.PLACES + "." + Places.VERSION;
    public static final String CONCRETE_DIRTY = Tables.PLACES + "." + Places.DIRTY;
    public static final String CONCRETE_DELETED = Tables.PLACES + "." + Places.DELETED;
    public static final String CONCRETE_SYNC1 = Tables.PLACES + "." + Places.SYNC1;
    public static final String CONCRETE_SYNC2 = Tables.PLACES + "." + Places.SYNC2;
    public static final String CONCRETE_SYNC3 = Tables.PLACES + "." + Places.SYNC3;
    public static final String CONCRETE_SYNC4 = Tables.PLACES + "." + Places.SYNC4;
    public static final String CONCRETE_STARRED = Tables.PLACES + "." + Places.STARRED;


	}
	
  public interface DataColumns {
    public static final String MIMETYPE_ID = "mimetype_id";
    public static final String CONCRETE_ID = Tables.DATA + "." + BaseColumns._ID;
    public static final String CONCRETE_MIMETYPE_ID = Tables.DATA + "." + MIMETYPE_ID;
    public static final String CONCRETE_PLACE_ID = Tables.DATA + "." + Data.PLACE_ID;
    public static final String CONCRETE_DATA1 = Tables.DATA + "." + Data.DATA1;
    public static final String CONCRETE_DATA2 = Tables.DATA + "." + Data.DATA2;
    public static final String CONCRETE_DATA3 = Tables.DATA + "." + Data.DATA3;
    public static final String CONCRETE_DATA4 = Tables.DATA + "." + Data.DATA4;
    public static final String CONCRETE_DATA5 = Tables.DATA + "." + Data.DATA5;
    public static final String CONCRETE_DATA6 = Tables.DATA + "." + Data.DATA6;
    public static final String CONCRETE_DATA7 = Tables.DATA + "." + Data.DATA7;
    public static final String CONCRETE_DATA8 = Tables.DATA + "." + Data.DATA8;
    public static final String CONCRETE_DATA9 = Tables.DATA + "." + Data.DATA9;
    public static final String CONCRETE_DATA10 = Tables.DATA + "." + Data.DATA10;
    public static final String CONCRETE_DATA11 = Tables.DATA + "." + Data.DATA11;
    public static final String CONCRETE_DATA12 = Tables.DATA + "." + Data.DATA12;
    public static final String CONCRETE_DATA13 = Tables.DATA + "." + Data.DATA13;
    public static final String CONCRETE_DATA14 = Tables.DATA + "." + Data.DATA14;
    public static final String CONCRETE_DATA15 = Tables.DATA + "." + Data.DATA15;
  }
	
  public interface MimetypesColumns {
    public static final String _ID = BaseColumns._ID;
    public static final String MIMETYPE = "mimetype";

    public static final String CONCRETE_ID = Tables.MIMETYPES + "." + BaseColumns._ID;
    public static final String CONCRETE_MIMETYPE = Tables.MIMETYPES + "." + MIMETYPE;
}
  
	private SQLiteStatement mMimetypeQuery;
	private SQLiteStatement mMimetypeInsert;
	private SQLiteStatement mDataMimetypeQuery;
	
	
	private final Context mContext;
	
	private static LociDatabaseHelper sSingleton = null;
	
	public static synchronized LociDatabaseHelper getInstance(Context context) {
		if (sSingleton == null) {
			sSingleton = new LociDatabaseHelper(context);
		}
		return sSingleton;
	}
	
	public LociDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		MyLog.i(LociConfig.D.DB.CALL, TAG, "onCreate : Bootstrapping database");

		// create tables
		// One row per group of places corresponding to the same place
		
		// places
		db.execSQL("CREATE TABLE " + Tables.PLACES + " (" +
				Places._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				Places.PLACE_NAME + " TEXT NOT NULL," +
				Places.PLACE_STATE + " INTEGER NOT NULL," +
				Places.PLACE_TYPE + " INTEGER NOT NULL," +
				Places.ENTRY + " INTEGER NOT NULL," +
				Places.ENTRY_TIME + " LONG NOT NULL," +
				Places.REGISTER_TIME + " LONG NOT NULL," +
				Places.TIMES_VISITED + " INTEGER NOT NULL DEFAULT 0," +
				Places.ACCOUNT_NAME + " STRING DEFAULT NULL," +
				Places.ACCOUNT_TYPE + " STRING DEFAULT NULL," +
				Places.SOURCE_ID + " TEXT," +
				Places.VERSION + " INTEGER NOT NULL DEFAULT 1," +
				Places.DIRTY + " INTEGER NOT NULL DEFAULT 0," +
				Places.DELETED + " INTEGER NOT NULL DEFAULT 0," +
				Places.STARRED + " INTEGER NOT NULL DEFAULT 0," +
				Places.SYNC1 + " TEXT," +
				Places.SYNC2 + " TEXT," +
				Places.SYNC3 + " TEXT," +
				Places.SYNC4 + " TEXT" +
		");");
		
		// visits
		db.execSQL("CREATE TABLE " + Tables.VISITS + " (" +
				Visits._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				Visits.ENTER + " LONG," + 
				Visits.EXIT + " LONG," +
				Visits.PLACE_ID + " INTEGER REFERENCES places(_id)," +
				Visits.TYPE + " INTEGER," +
				Visits.EXTRA1 + " TEXT," +
				Visits.EXTRA2 + " TEXT," +
				Visits.EXTRA3 + " TEXT," +
				Places.VERSION + " INTEGER NOT NULL DEFAULT 1," +
				Places.DIRTY + " INTEGER NOT NULL DEFAULT 0," +
				Visits.SYNC1 + " TEXT," +
				Visits.SYNC2 + " TEXT," +
				Visits.SYNC3 + " TEXT," +
				Visits.SYNC4 + " TEXT" +
		");");
		
		db.execSQL("CREATE TABLE " + Tables.TRACKS + "(" +
				Tracks._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				Tracks.TIME + " LONG NOT NULL," +
				Tracks.LATITUDE + " DOUBLE NOT NULL," +
				Tracks.LONGITUDE + " DOUBLE NOT NULL," +
				Tracks.ALTITUDE +	" DOUBLE," +
				Tracks.SPEED + " FLOAT," +
				Tracks.ACCURACY + " FLOAT," +
				Tracks.BEARING + " FLOAT," +
				Tracks.SYNC + " TEXT" +		
		");");
		
	
    // Mimetype mapping table
    db.execSQL("CREATE TABLE " + Tables.MIMETYPES + " (" +
            MimetypesColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            MimetypesColumns.MIMETYPE + " TEXT NOT NULL" +
    ");");

    // Mimetype table requires an index on mime type
    db.execSQL("CREATE UNIQUE INDEX mime_type ON " + Tables.MIMETYPES + " (" +
            MimetypesColumns.MIMETYPE +
    ");");
    String dataTable = "CREATE TABLE " + Tables.DATA + "(" +
			Data._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			DataColumns.MIMETYPE_ID + " INTEGER REFERENCES mimetype(_id) NOT NULL," +
			Data.PLACE_ID + " INTEGER REFERENCES places(_id) NOT NULL," +
			Data.DATA_VERSION + " INTEGER NOT NULL DEFAULT 0," +
	    Data.DATA1 + " TEXT," +
	    Data.DATA2 + " TEXT," +
	    Data.DATA3 + " TEXT," +
	    Data.DATA4 + " TEXT," +
	    Data.DATA5 + " TEXT," +
	    Data.DATA6 + " TEXT," +
	    Data.DATA7 + " TEXT," +
	    Data.DATA8 + " TEXT," +
	    Data.DATA9 + " TEXT," +
	    Data.DATA10 + " TEXT," +
	    Data.DATA11 + " TEXT," +
	    Data.DATA12 + " TEXT," +
	    Data.DATA13 + " TEXT," +
	    Data.DATA14 + " TEXT," +
	    Data.DATA15 + " TEXT," +
	    Data.SYNC1 + " TEXT, " +
	    Data.SYNC2 + " TEXT, " +
	    Data.SYNC3 + " TEXT, " +
	    Data.SYNC4 + " TEXT " +
    ");";
		
    db.execSQL(dataTable); 
    
    db.execSQL("CREATE INDEX data_place_id_index ON " + Tables.DATA + " (" +
        		Data.PLACE_ID +
    ");");
    
    createPlacesViews(db);
    createPlacesTriggers(db);
    createPlaceEntitiesView(db);
	}
	
	private static void createPlacesTriggers(SQLiteDatabase db) {
		db.execSQL("DROP TRIGGER IF EXISTS " + Tables.PLACES + "_deleted;");
		db.execSQL("CREATE TRIGGER " + Tables.PLACES + "_deleted "
        		+ "   BEFORE DELETE ON " + Tables.PLACES
        		+ " BEGIN "
        		+ "   DELETE FROM " + Tables.DATA
        		+ "     WHERE " + Data.PLACE_ID + "=OLD." + Places._ID + ";"
        		+ "   DELETE FROM " + Tables.VISITS
        		+ "     WHERE " + Visits.PLACE_ID + "=OLD." + Places._ID + ";" 
            + " END");
		
    db.execSQL("DROP TRIGGER IF EXISTS " + Tables.DATA + "_updated;");
    db.execSQL("CREATE TRIGGER " + Tables.DATA + "_updated " 
    				+ "AFTER UPDATE ON " + Tables.DATA
            + " BEGIN "
            + "   UPDATE " + Tables.DATA
            + "     SET " + Data.DATA_VERSION + "=OLD." + Data.DATA_VERSION + "+1 "
            + "     WHERE " + Data._ID + "=OLD." + Data._ID + ";"
            + "   UPDATE " + Tables.PLACES
            + "     SET " + Places.VERSION + "=" + Places.VERSION + "+1 "
            + "     WHERE " + Places._ID + "=OLD." + Data.PLACE_ID + ";"
            + " END");

    db.execSQL("DROP TRIGGER IF EXISTS " + Tables.DATA + "_deleted;");
    db.execSQL("CREATE TRIGGER " + Tables.DATA + "_deleted "
    				+ "BEFORE DELETE ON " + Tables.DATA
            + " BEGIN "
            + "   UPDATE " + Tables.PLACES
            + "     SET " + Places.VERSION + "=" + Places.VERSION + "+1 "
            + "     WHERE " + Places._ID + "=OLD." + Data.PLACE_ID + ";"
            + " END");
	}
	
	private static void createPlacesViews(SQLiteDatabase db) {
		db.execSQL("DROP VIEW IF EXISTS " + Views.DATA_ALL + ";");
		db.execSQL("DROP VIEW IF EXISTS " + Views.PLACES_ALL + ";");
		
		
    String dataColumns =
      Data.DATA_VERSION + ", "
      + MimetypesColumns.MIMETYPE + " AS " + Data.MIMETYPE + ", "
      + Data.DATA1 + ", "
      + Data.DATA2 + ", "
      + Data.DATA3 + ", "
      + Data.DATA4 + ", "
      + Data.DATA5 + ", "
      + Data.DATA6 + ", "
      + Data.DATA7 + ", "
      + Data.DATA8 + ", "
      + Data.DATA9 + ", "
      + Data.DATA10 + ", "
      + Data.DATA11 + ", "
      + Data.DATA12 + ", "
      + Data.DATA13 + ", "
      + Data.DATA14 + ", "
      + Data.DATA15 + ", "
      + Data.SYNC1 + ", "
      + Data.SYNC2 + ", "
      + Data.SYNC3 + ", "
      + Data.SYNC4;

    String syncColumns = 
    				PlacesColumns.CONCRETE_ACCOUNT_NAME + " AS " + Places.ACCOUNT_NAME + ","
    				+ PlacesColumns.CONCRETE_ACCOUNT_TYPE + " AS " + Places.ACCOUNT_TYPE + ","
    				+ PlacesColumns.CONCRETE_SOURCE_ID + " AS " + Places.SOURCE_ID + ","
    				+ PlacesColumns.CONCRETE_VERSION + " AS " + Places.VERSION + ","
    				+ PlacesColumns.CONCRETE_DIRTY + " AS " + Places.DIRTY + ","
    				+ PlacesColumns.CONCRETE_SYNC1 + " AS " + Places.SYNC1 + ","
    				+ PlacesColumns.CONCRETE_SYNC2 + " AS " + Places.SYNC2 + ","
    				+ PlacesColumns.CONCRETE_SYNC3 + " AS " + Places.SYNC3 + ","
    				+ PlacesColumns.CONCRETE_SYNC4 + " AS " + Places.SYNC4;
    
    String placeOptionColumns = 
    				PlacesColumns.CONCRETE_TIMES_VISITED + " AS " + Places.TIMES_VISITED + ", "
    				+ PlacesColumns.CONCRETE_STARRED + " AS " + Places.STARRED;
    
    String dataSelect = "SELECT " 
							+ DataColumns.CONCRETE_ID + " AS " + Data._ID + ","
							+ Data.PLACE_ID + ", "  
							+ syncColumns + ", " 
							+ dataColumns + ", "
							+ placeOptionColumns 
						  + " FROM " + Tables.DATA 
						  + " JOIN " + Tables.MIMETYPES + " ON ("
						  + DataColumns.CONCRETE_MIMETYPE_ID + "=" + MimetypesColumns.CONCRETE_ID + ")"
						  + " JOIN " + Tables.PLACES + " ON ("
						  + DataColumns.CONCRETE_PLACE_ID + "=" + PlacesColumns.CONCRETE_ID + ")"
						  ;
    
    String placeSelect = "SELECT " 
    					+ PlacesColumns.CONCRETE_ID + " AS " + Places._ID + ", "
    					+ Places.PLACE_NAME + ", "
    					+ Places.PLACE_STATE + ", "
    					+ Places.PLACE_TYPE + ", "
    					+ Places.ENTRY + ", "
    					+ Places.ENTRY_TIME + ", "
    					+ Places.REGISTER_TIME + ", "
    					+ Places.DELETED + ", "
    					+ placeOptionColumns + ", "
    					+ syncColumns
    					+ " FROM " + Tables.PLACES;
    
    db.execSQL("CREATE VIEW " + Views.PLACES_ALL + " AS " + placeSelect);
    db.execSQL("CREATE VIEW " + Views.DATA_ALL + " AS " + dataSelect);
	}
	
	private static void createPlaceEntitiesView(SQLiteDatabase db) {
		db.execSQL("DROP VIEW IF EXISTS " + Tables.PLACE_ENTITIES + ";");

		String placeEntitiesSelect = "SELECT "
						+ PlacesColumns.CONCRETE_ID + " AS " + Places._ID + ","
						+ PlacesColumns.CONCRETE_NAME + " AS " + Places.PLACE_NAME + ","
						+ PlacesColumns.CONCRETE_STATE + " AS " + Places.PLACE_STATE + ","
						+ PlacesColumns.CONCRETE_TYPE + " AS " + Places.PLACE_TYPE + ","
						+ PlacesColumns.CONCRETE_ACCOUNT_NAME + " AS " + Places.ACCOUNT_NAME + ","
						+ PlacesColumns.CONCRETE_ACCOUNT_TYPE + " AS " + Places.ACCOUNT_TYPE + ","
						+ PlacesColumns.CONCRETE_SOURCE_ID + " AS " + Places.SOURCE_ID + ","
						+ PlacesColumns.CONCRETE_VERSION + " AS " + Places.VERSION + ","
						+ PlacesColumns.CONCRETE_DIRTY + " AS " + Places.DIRTY + ","
						+ PlacesColumns.CONCRETE_DELETED + " AS " + Places.DELETED + ","
						+ PlacesColumns.CONCRETE_SYNC1 + " AS " + Places.SYNC1 + ","
						+ PlacesColumns.CONCRETE_SYNC2 + " AS " + Places.SYNC2 + ","
						+ PlacesColumns.CONCRETE_SYNC3 + " AS " + Places.SYNC3 + ","
						+ PlacesColumns.CONCRETE_SYNC4 + " AS " + Places.SYNC4 + ","
						+ PlacesColumns.CONCRETE_STARRED + " AS " + Places.STARRED + ","
						+ PlacesColumns.CONCRETE_TIMES_VISITED + " AS " + Places.TIMES_VISITED + ","
						+ Data.MIMETYPE + ", "
						+ Data.DATA1 + ", " 
						+ Data.DATA2 + ", "
            + Data.DATA3 + ", "
            + Data.DATA4 + ", "
            + Data.DATA5 + ", "
            + Data.DATA6 + ", "
            + Data.DATA7 + ", "
            + Data.DATA8 + ", "
            + Data.DATA9 + ", "
            + Data.DATA10 + ", "
            + Data.DATA11 + ", "
            + Data.DATA12 + ", "
            + Data.DATA13 + ", "
            + Data.DATA14 + ", "
            + Data.DATA15 + ", "
            + Data.SYNC1 + ", "
            + Data.SYNC2 + ", "
            + Data.SYNC3 + ", "
            + Data.SYNC4 + ", "
            + Data.DATA_VERSION + ", "
            + DataColumns.CONCRETE_ID + " AS " + Places.Entity.DATA_ID 
            + " FROM " + Tables.PLACES
            + " LEFT OUTER JOIN " + Tables.DATA + " ON ("
            + DataColumns.CONCRETE_PLACE_ID + "=" + PlacesColumns.CONCRETE_ID + ")"
            + " LEFT OUTER JOIN " + Tables.MIMETYPES + " ON (" 
            + DataColumns.CONCRETE_MIMETYPE_ID + "=" + MimetypesColumns.CONCRETE_ID + ")";
		
		db.execSQL("CREATE VIEW " + Tables.PLACE_ENTITIES + " AS " + placeEntitiesSelect);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		MyLog.i(LociConfig.D.DB.CALL, TAG, "onUpgrade(): oldver=" + oldVersion + " newver=" + newVersion);
		
		boolean upgradeViewsAndTriggers = true;
		
		if (upgradeViewsAndTriggers) {
			createPlacesViews(db);
			createPlacesTriggers(db);
			createPlaceEntitiesView(db);
		}
	}

	public void onOpen(SQLiteDatabase db) {
		MyLog.i(LociConfig.D.DB.CALL, TAG, "onOpen : Bootstrapping database");
    // Create compiled statements for package and mimetype lookups
    mMimetypeQuery = db.compileStatement("SELECT " + MimetypesColumns._ID + " FROM "
            + Tables.MIMETYPES + " WHERE " + MimetypesColumns.MIMETYPE + "=?");
    mMimetypeInsert = db.compileStatement("INSERT INTO " + Tables.MIMETYPES + "("
        + MimetypesColumns.MIMETYPE + ") VALUES (?)");
    mDataMimetypeQuery = db.compileStatement("SELECT " + MimetypesColumns.CONCRETE_MIMETYPE + " FROM "
        + Tables.DATA_JOIN_MIMETYPES + " WHERE " + Tables.DATA + "." + Data._ID + "=?");
	}
	
	
	
	
  /** In-memory cache of previously found MIME-type mappings */
  private final HashMap<String, Long> mMimetypeCache = new HashMap<String, Long>();
	
  /**
   * Perform an internal string-to-integer lookup using the compiled
   * {@link SQLiteStatement} provided, using the in-memory cache to speed up
   * lookups. If a mapping isn't found in cache or database, it will be
   * created. All new, uncached answers are added to the cache automatically.
   *
   * @param query Compiled statement used to query for the mapping.
   * @param insert Compiled statement used to insert a new mapping when no
   *            existing one is found in cache or from query.
   * @param value Value to find mapping for.
   * @param cache In-memory cache of previous answers.
   * @return An unique integer mapping for the given value.
   */
  private long getCachedId(SQLiteStatement query, SQLiteStatement insert,
          String value, HashMap<String, Long> cache) {
      // Try an in-memory cache lookup
      if (cache.containsKey(value)) {
          return cache.get(value);
      }

      long id = -1;
      try {
          // Try searching database for mapping
      		DatabaseUtils.bindObjectToProgram(query, 1, value);
          id = query.simpleQueryForLong();
      } catch (SQLiteDoneException e) {
          // Nothing found, so try inserting new mapping
      		DatabaseUtils.bindObjectToProgram(insert, 1, value);
          id = insert.executeInsert();
      }

      if (id != -1) {
          // Cache and return the new answer
          cache.put(value, id);
          return id;
      } else {
          // Otherwise throw if no mapping found or created
          throw new IllegalStateException("Couldn't find or create internal "
                  + "lookup table entry for value " + value);
      }
  }

  /**
   * Convert a mimetype into an integer, using {@link Tables#MIMETYPES} for
   * lookups and possible allocation of new IDs as needed.
   */
  public long getMimeTypeId(String mimetype) {
      // Make sure compiled statements are ready by opening database
      getReadableDatabase();
      return getMimeTypeIdNoDbCheck(mimetype);
  }

  private long getMimeTypeIdNoDbCheck(String mimetype) {
      return getCachedId(mMimetypeQuery, mMimetypeInsert, mimetype, mMimetypeCache);
  }

  /**
   * Find the mimetype for the given {@link Data#_ID}.
   */
  public String getDataMimeType(long dataId) {
      // Make sure compiled statements are ready by opening database
      getReadableDatabase();
      try {
          // Try database query to find mimetype
          DatabaseUtils.bindObjectToProgram(mDataMimetypeQuery, 1, dataId);
          String mimetype = mDataMimetypeQuery.simpleQueryForString();
          return mimetype;
      } catch (SQLiteDoneException e) {
          // No valid mapping found, so return null
          return null;
      }
  }
  
  public String getDataView() {
  	return Views.DATA_ALL;
  }
  
  public String getPlaceView() {
    return Views.PLACES_ALL;
  }

  
  public String getPlaceEntitiesView() {
    return Tables.PLACE_ENTITIES;
  }

  
  /**
   * Returns a detailed exception message for the supplied URI.  It includes the calling
   * user and calling package(s).
   */
  public String exceptionMessage(Uri uri) {
      return exceptionMessage(null, uri);
  }

  /**
   * Returns a detailed exception message for the supplied URI.  It includes the calling
   * user and calling package(s).
   */
  public String exceptionMessage(String message, Uri uri) {
      StringBuilder sb = new StringBuilder();
      if (message != null) {
          sb.append(message).append("; ");
      }
      sb.append("URI: ").append(uri);
      final PackageManager pm = mContext.getPackageManager();
      int callingUid = Binder.getCallingUid();
      sb.append(", calling user: ");
      String userName = pm.getNameForUid(callingUid);
      if (userName != null) {
          sb.append(userName);
      } else {
          sb.append(callingUid);
      }

      final String[] callerPackages = pm.getPackagesForUid(callingUid);
      if (callerPackages != null && callerPackages.length > 0) {
          if (callerPackages.length == 1) {
              sb.append(", calling package:");
              sb.append(callerPackages[0]);
          } else {
              sb.append(", calling package is one of: [");
              for (int i = 0; i < callerPackages.length; i++) {
                  if (i != 0) {
                      sb.append(", ");
                  }
                  sb.append(callerPackages[i]);
              }
              sb.append("]");
          }
      }

      return sb.toString();
  }

}
