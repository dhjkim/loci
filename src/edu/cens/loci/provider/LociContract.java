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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.EntityIterator;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import edu.cens.loci.classes.CursorEntityIterator;
import edu.cens.loci.components.ILociManager;
import edu.cens.loci.provider.LociDatabaseHelper.Tables;

public class LociContract {

		private static final String TAG = "LociContract";
		private static ILociManager sIService;
		private static boolean sIsBound = false;

  	public ServiceConnection LociConnection = new ServiceConnection() {
  		public void onServiceConnected(ComponentName className, IBinder service) {
  			Log.d(TAG, "onServiceConnected() <= ILociManager connected");
  			sIService = ILociManager.Stub.asInterface(service);
  			sIsBound = true;
  		}
  		public void onServiceDisconnected(ComponentName className) {
  			Log.d(TAG, "onServiceDisconnected()");
  			sIService = null;
  			sIsBound = false;
  		}
  	};
  	
  	public boolean isConnected() {
  		return sIsBound;
  	}
  	
  	public void addPlaceAlert(long placeid, long expiration, PendingIntent intent) {
  		if (sIsBound) {
  			try {
					sIService.addPlaceAlert(placeid, expiration, intent);
				} catch (RemoteException re) {
					Log.e(TAG, "Remote Exception", re);
				}
  		} else {
  			Log.d(TAG, "Loci service is not connected.");
  		}
  	}
  	
  	public void removePlaceAlert(PendingIntent intent) {
  		if (sIsBound) {
  			try {
					sIService.removePlaceAlert(intent);
				} catch (RemoteException re) {
					Log.e(TAG, "Remote Exception", re);
				}
  		} else {
  			Log.d(TAG, "Loci service is not connected.");
  		}
  	}
  	
  	/** The authority for the loci provider */
  	public static final String AUTHORITY = "edu.cens.loci";
  	/** A content:// style uri to the authority for the loci provider */
  	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
  	
  	public interface BaseColumns {
  		public static final String _ID = "_id";
  		public static final String _COUNT  = "_count";
  	}
  	
  	public static class Visits implements BaseColumns, VisitsColumns, BaseSyncColumns {
  		private Visits() {}
  		//public static final Uri CONTENT_URI = Uri.parse("content://visit_log/visits");
  		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "visits");
  		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.visit";
  		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.visit";
  	}
  	
  	protected interface VisitsColumns {
  		public static final String ENTER 			= "enter";
  		public static final String EXIT 			= "exit";
  		public static final String PLACE_ID 	= "place_id";
  		public static final String TYPE 			= "type";
  		public static final String EXTRA1 		= "extra1"; // Wifi fingerprint
  		public static final String EXTRA2 		= "extra2"; // place recognition result list
  		public static final String EXTRA3			= "extra3"; // comments
  		
  		public static final int TYPE_GPS = 1;
  		public static final int TYPE_WIFI = 2;
  	}
  	
  	public static class Tracks implements BaseColumns, TracksColumns {
  		private Tracks() {}
  		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "tracks");
  		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.track";
  		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.track";
  	}
  	
  	protected interface TracksColumns {
  		public static final String TIME = "time";
  		public static final String LATITUDE = "latitude";
  		public static final String LONGITUDE = "longitude";
  		public static final String ALTITUDE = "altitude";
  		public static final String SPEED = "speed";
  		public static final String ACCURACY = "accuracy";
  		public static final String BEARING = "bearing";
  		public static final String SYNC = "sync";
  	}
  	
  	public static class Places implements BaseColumns, PlacesColumns, SyncColumns, PlaceOptionsColumns {
  		private Places() {}
  		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "places");
  		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.place";
  		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.place";
  		
      /**
       * A sub-directory of a single place that contains all of its
       * {@link LociContract.Data} rows. To access this directory
       * append {@link Data#CONTENT_DIRECTORY} to the contact URI.
       *
       * TODO: deprecate in favor of {@link Places.Entity}.
       */
      public static final class Data implements BaseColumns, DataColumns {
          /**
           * no public constructor since this is a utility class
           */
          private Data() {
          }

          /**
           * The directory twig for this sub-table
           */
          public static final String CONTENT_DIRECTORY = "data";
      }
  		
  		public static final class Entity implements BaseColumns, DataColumns {
        private Entity() {
        }
        /**
         * The directory twig for this sub-table
         */
        public static final String CONTENT_DIRECTORY = "entity";
        /**
         * The ID of the data column. The value will be null if this raw contact has no
         * data rows.
         * <P>Type: INTEGER</P>
         */
        public static final String DATA_ID = "data_id";
  		}
  		
      public static EntityIterator newEntityIterator(Cursor cursor) {
        return new EntityIteratorImpl(cursor);
    }

    private static class EntityIteratorImpl extends CursorEntityIterator {
        private static final String[] DATA_KEYS = new String[]{
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4};

        public EntityIteratorImpl(Cursor cursor) {
            super(cursor);
        }

        @Override
        public android.content.Entity getEntityAndIncrementCursor(Cursor cursor)
                throws RemoteException {
            final int columnPlaceId = cursor.getColumnIndexOrThrow(Places._ID);
            final long placeId = cursor.getLong(columnPlaceId);

            //Log.e(TAG, "getEntityAndIncrementCursor: columnPlaceId=" + columnPlaceId + " placeId=" + placeId);
            
            // we expect the cursor is already at the row we need to read from
            ContentValues cv = new ContentValues();

            DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, _ID);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, PLACE_NAME);
            DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, PLACE_TYPE);
            DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, PLACE_STATE);
            
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, ACCOUNT_NAME);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, ACCOUNT_TYPE);
            DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, DIRTY);
            DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, VERSION);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, SOURCE_ID);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, SYNC1);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, SYNC2);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, SYNC3);
            DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, SYNC4);
            DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, DELETED);
            //DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, CONTACT_ID);
            DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, STARRED);
            
            //DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, IS_RESTRICTED);
            //DatabaseUtils.cursorIntToContentValuesIfPresent(cursor, cv, NAME_VERIFIED);
            android.content.Entity place = new android.content.Entity(cv);
            
            //Log.e(TAG, "android.content.Entity:" + place.toString());

            // read data rows until the place id changes
            do {
                if (placeId != cursor.getLong(columnPlaceId)) {
                	Log.e(TAG, "read data rows: placeId mismatch.");
                  break;
                }
                // add the data to to the contact
                cv = new ContentValues();
                cv.put(Data._ID, cursor.getLong(cursor.getColumnIndexOrThrow(Entity.DATA_ID)));
                //DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                //        Data.RES_PACKAGE);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv, Data.MIMETYPE);
                //DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, Data.IS_PRIMARY);
                //DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv,
                //        Data.IS_SUPER_PRIMARY);
                DatabaseUtils.cursorLongToContentValuesIfPresent(cursor, cv, Data.DATA_VERSION);
                //DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                //        CommonDataKinds.GroupMembership.GROUP_SOURCE_ID);
                DatabaseUtils.cursorStringToContentValuesIfPresent(cursor, cv,
                        Data.DATA_VERSION);
                for (String key : DATA_KEYS) {
                    final int columnIndex = cursor.getColumnIndexOrThrow(key);
                    if (cursor.isNull(columnIndex)) {
                      // don't put anything
                    	//Log.i(TAG, "read data rows: not avail " + key);
                    } else {
                        try {
                            cv.put(key, cursor.getString(columnIndex));
                        } catch (SQLiteException e) {
                            cv.put(key, cursor.getBlob(columnIndex));
                        }
                    }
                    // TODO: go back to this version of the code when bug
                    // http://b/issue?id=2306370 is fixed.
//                    if (cursor.isNull(columnIndex)) {
//                        // don't put anything
//                    } else if (cursor.isLong(columnIndex)) {
//                        values.put(key, cursor.getLong(columnIndex));
//                    } else if (cursor.isFloat(columnIndex)) {
//                        values.put(key, cursor.getFloat(columnIndex));
//                    } else if (cursor.isString(columnIndex)) {
//                        values.put(key, cursor.getString(columnIndex));
//                    } else if (cursor.isBlob(columnIndex)) {
//                        values.put(key, cursor.getBlob(columnIndex));
//                    }
                }
                place.addSubValue(LociContract.Data.CONTENT_URI, cv);
            } while (cursor.moveToNext());

            //Log.e(TAG, "android.content.Entity:" + place.toString());
            
            return place;
        }

    }


  	}
  	
  	protected interface PlacesColumns {
  		public static final String PLACE_NAME			= "name";
  		public static final String PLACE_TYPE			= "type";
  		public static final String PLACE_STATE		= "state";
  		
      public static final String DELETED = "deleted";

  		// to save parameters 
  		public static final String EXTRA1 = "extra1";
  		public static final String EXTRA2 = "extra2";
  		public static final String EXTRA3 = "extra3";
  		
  		public static final String ENTRY 					= "entry";
  		public static final String ENTRY_TIME 		= "entry_time";
  		public static final String REGISTER_TIME 	= "register_time";

  		public static final int TYPE_GPS = 1;
  		public static final int TYPE_WIFI = 2;
  		
  		public static final int STATE_SUGGESTED = 1;
  		public static final int STATE_REGISTERED = 2;
  		public static final int STATE_BLOCKED = 3;
  		public static final int STATE_DELETED = 4;
  		public static final int STATE_MERGED = 5;
  		
  		public static final int ENTRY_WIFI_SUGGESTED = 1;
  		public static final int ENTRY_WIFI_USE_SHORT_VISIT = 2;
  		public static final int ENTRY_WIFI_OVERWRITE_WRONG_RECOGNITION = 3;
  		public static final int ENTRY_GPS_SUGGESTED = 4;
  		public static final int ENTRY_GPS_USER_DEFINED = 5;
  		
  	}
  	
  	public interface MimetypesColumns {
  		public static final String _ID = BaseColumns._ID;
  		public static final String MIMETYPE = "mimetype";
  		
  		public static final String CONCRETE_ID = Tables.MIMETYPES + "." + BaseColumns._ID;
  		public static final String CONCRETE_MIMETYPE = Tables.MIMETYPES + "." + MIMETYPE;
  	}
  	
  	
  	public final static class Data implements BaseColumns, DataColumns {
  		private Data() {}
  		public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "data");
  		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.data";
  	}
  	
  	protected interface DataColumns {
  		public static final String MIMETYPE = "mimetype";
  		public static final String PLACE_ID = "place_id";
  		public static final String DATA_VERSION = "data_version";
  		
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA1 = "data1";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA2 = "data2";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA3 = "data3";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA4 = "data4";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA5 = "data5";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA6 = "data6";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA7 = "data7";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA8 = "data8";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA9 = "data9";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA10 = "data10";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA11 = "data11";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA12 = "data12";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA13 = "data13";
      /** Generic data column, the meaning is {@link #MIMETYPE} specific */
      public static final String DATA14 = "data14";
      /**
       * Generic data column, the meaning is {@link #MIMETYPE} specific. By convention,
       * this field is used to store BLOBs (binary data).
       */
      public static final String DATA15 = "data15";

      /** Generic column for use by sync adapters. */
      public static final String SYNC1 = "data_sync1";
      /** Generic column for use by sync adapters. */
      public static final String SYNC2 = "data_sync2";
      /** Generic column for use by sync adapters. */
      public static final String SYNC3 = "data_sync3";
      /** Generic column for use by sync adapters. */
      public static final String SYNC4 = "data_sync4";
  		
  	}
  	
  	public final static class PlacesEntity implements BaseColumns, DataColumns, PlacesColumns {
  		private PlacesEntity() {}
  		
  		public static final Uri CONTENT_URI = 
  						Uri.withAppendedPath(AUTHORITY_URI, "place_entities");
  		
  		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.place_entity";
  		
  		public static final String DATA_ID = "data_id";
  	}
  	
    /**
     * Generic columns for use by sync adapters. The specific functions of
     * these columns are private to the sync adapter. Other clients of the API
     * should not attempt to either read or write this column.
     *
     */
    protected interface BaseSyncColumns {
    		public static final String VERSION = "version";
    		public static final String DIRTY = "dirty";
    		
        /** Generic column for use by sync adapters. */
        public static final String SYNC1 = "sync1";
        /** Generic column for use by sync adapters. */
        public static final String SYNC2 = "sync2";
        /** Generic column for use by sync adapters. */
        public static final String SYNC3 = "sync3";
        /** Generic column for use by sync adapters. */
        public static final String SYNC4 = "sync4";
    }
    
    /**
     * Columns that appear when each row of a table belongs to a specific
     * account, including sync information that an account may need.
     *
     * @see RawContacts
     * @see Groups
     */
    protected interface SyncColumns extends BaseSyncColumns {
        /**
         * The name of the account instance to which this row belongs, which when paired with
         * {@link #ACCOUNT_TYPE} identifies a specific account.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_NAME = "account_name";

        /**
         * The type of account to which this row belongs, which when paired with
         * {@link #ACCOUNT_NAME} identifies a specific account.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_TYPE = "account_type";

        /**
         * String that uniquely identifies this row to its source account.
         * <P>Type: TEXT</P>
         */
        public static final String SOURCE_ID = "sourceid";

        /**
         * Version number that is updated whenever this row or its related data
         * changes.
         * <P>Type: INTEGER</P>
         */
        public static final String VERSION = "version";

        /**
         * Flag indicating that {@link #VERSION} has changed, and this row needs
         * to be synchronized by its owning account.
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String DIRTY = "dirty";
    }

    /**
     * Columns of {@link LociContract.Places} that track the user's
     * preferences for, or interactions with, the place.
     *
     */
    protected interface PlaceOptionsColumns {
        /**
         * The number of times a contact has been contacted
         * <P>Type: INTEGER</P>
         */
        public static final String TIMES_VISITED = "times_visited";

        /**
         * Is the contact starred?
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String STARRED = "starred";

    }


    public static final class CommonPlaceDataKinds {
    	private CommonPlaceDataKinds() {}
    	
      /**
       * The base types that all "Typed" data kinds support.
       */
      public interface BaseTypes {
          /**
           * A custom type. The custom label should be supplied by user.
           */
          public static int TYPE_CUSTOM = 0;
      }

      /**
       * Columns common across the specific types.
       */
      protected interface CommonColumns extends BaseTypes {
          /**
           * The data for the contact method.
           * <P>Type: TEXT</P>
           */
          public static final String DATA = DataColumns.DATA1;

          /**
           * The type of data, for example Home or Work.
           * <P>Type: INTEGER</P>
           */
          public static final String TYPE = DataColumns.DATA2;

          /**
           * The user defined label for the the contact method.
           * <P>Type: TEXT</P>
           */
          public static final String LABEL = DataColumns.DATA3;
      }
    	
    	public static final class WifiFingerprint implements DataColumns, CommonColumns {
    		private WifiFingerprint() {}
    		
    		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.wififingerprint";
    		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.wififingerprint";
    	
    		public static final String FINGERPRINT = DATA;
    		public static final String TIMESTAMP = DATA4;
    		
    	  public static interface WifiDataQuery {
    	  	String[] COLUMNS = {Data._ID, Data.PLACE_ID, WifiFingerprint.FINGERPRINT, WifiFingerprint.TIMESTAMP};
    	  	int _ID = 0;
    	  	int PLACEID = 1;
    	  	int FINGERPRINT = 2;
    	  	int TIMESTAMP = 3;
    	  }
    	}
    	
    	public static final class GpsCircleArea implements DataColumns, CommonColumns {
    		private GpsCircleArea() {}
    		
    		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.gpsposition";
    		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.gpsposition";
    		
    		public static final String LATITUDE = DATA4;
    		public static final String LONGITUDE = DATA5;
    		public static final String RADIUS = DATA6;
    		
    		public static final String EXTRA1 = DATA7;
    		public static final String EXTRA2 = DATA8;
    		
    		public static final int TYPE_SUGGESTED = 1;
    		public static final int TYPE_USER_DEFINED = 2;
    	}
    	
    	public static final class Keyword implements DataColumns, CommonColumns {
    		private Keyword() {}
    		
    		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.keyword";
    		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.keyword";
    		
    		public static final String TAG = DATA;
    		public static final String CATEGORY = TYPE;
    		
    		public static final int CATEGORY_PERSONAL = 1;
    		public static final int CATEGORY_BUSINESSNAME = 2;

    	}
    	
      public static final class StructuredPostal implements DataColumns, CommonColumns {
        /**
         * This utility class cannot be instantiated
         */
        private StructuredPostal() {
        }

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.cens.loci.address";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * postal addresses.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.address";

        /**
         * The content:// style URI for all data records of the
         * {@link StructuredPostal#CONTENT_ITEM_TYPE} MIME type.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(Data.CONTENT_URI,
                "postals");

        public static final int TYPE_HOME = 1;
        public static final int TYPE_WORK = 2;
        public static final int TYPE_OTHER = 3;

        /**
         * The full, unstructured postal address. <i>This field must be
         * consistent with any structured data.</i>
         * <p>
         * Type: TEXT
         */
        public static final String FORMATTED_ADDRESS = DATA;

        /**
         * Can be street, avenue, road, etc. This element also includes the
         * house number and room/apartment/flat/floor number.
         * <p>
         * Type: TEXT
         */
        public static final String STREET = DATA4;

        /**
         * Covers actual P.O. boxes, drawers, locked bags, etc. This is
         * usually but not always mutually exclusive with street.
         * <p>
         * Type: TEXT
         */
        public static final String POBOX = DATA5;

        /**
         * This is used to disambiguate a street address when a city
         * contains more than one street with the same name, or to specify a
         * small place whose mail is routed through a larger postal town. In
         * China it could be a county or a minor city.
         * <p>
         * Type: TEXT
         */
        public static final String NEIGHBORHOOD = DATA6;

        /**
         * Can be city, village, town, borough, etc. This is the postal town
         * and not necessarily the place of residence or place of business.
         * <p>
         * Type: TEXT
         */
        public static final String CITY = DATA7;

        /**
         * A state, province, county (in Ireland), Land (in Germany),
         * departement (in France), etc.
         * <p>
         * Type: TEXT
         */
        public static final String REGION = DATA8;

        /**
         * Postal code. Usually country-wide, but sometimes specific to the
         * city (e.g. "2" in "Dublin 2, Ireland" addresses).
         * <p>
         * Type: TEXT
         */
        public static final String POSTCODE = DATA9;

        /**
         * The name or code of the country.
         * <p>
         * Type: TEXT
         */
        public static final String COUNTRY = DATA10;

      }

      public static final class Photo implements DataColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Photo() {}

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.photo";

        /**
         * Thumbnail photo of the raw contact. This is the raw bytes of an image
         * that could be inflated using {@link android.graphics.BitmapFactory}.
         * <p>
         * Type: BLOB
         */
        public static final String PHOTO = DATA15;
      }
      
      public static final class Note implements DataColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Note() {}

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.note";

        /**
         * The note text.
         * <P>Type: TEXT</P>
         */
        public static final String NOTE = DATA1;
      }
      
      public static final class Website implements DataColumns, CommonColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Website() {}

        /** MIME type used when storing this in data table. */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.website";

        public static final int TYPE_HOMEPAGE = 1;
        public static final int TYPE_BLOG = 2;
        public static final int TYPE_PROFILE = 3;
        public static final int TYPE_HOME = 4;
        public static final int TYPE_WORK = 5;
        public static final int TYPE_FTP = 6;
        public static final int TYPE_OTHER = 7;

        /**
         * The website URL string.
         * <P>Type: TEXT</P>
         */
        public static final String URL = DATA;
      }
    }
    
    
    /**
     * @see Settings
     */
    protected interface SettingsColumns {
        /**
         * The name of the account instance to which this row belongs.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_NAME = "account_name";

        /**
         * The type of account to which this row belongs, which when paired with
         * {@link #ACCOUNT_NAME} identifies a specific account.
         * <P>Type: TEXT</P>
         */
        public static final String ACCOUNT_TYPE = "account_type";

        /**
         * Depending on the mode defined by the sync-adapter, this flag controls
         * the top-level sync behavior for this data source.
         * <p>
         * Type: INTEGER (boolean)
         */
        public static final String SHOULD_SYNC = "should_sync";

        /**
         * Flag indicating if contacts without any {@link CommonDataKinds.GroupMembership}
         * entries should be visible in any user interface.
         * <p>
         * Type: INTEGER (boolean)
         */
        public static final String UNGROUPED_VISIBLE = "ungrouped_visible";

        /**
         * Read-only flag indicating if this {@link #SHOULD_SYNC} or any
         * {@link Groups#SHOULD_SYNC} under this account have been marked as
         * unsynced.
         */
        public static final String ANY_UNSYNCED = "any_unsynced";

        /**
         * Read-only count of {@link Contacts} from a specific source that have
         * no {@link CommonDataKinds.GroupMembership} entries.
         * <p>
         * Type: INTEGER
         */
        public static final String UNGROUPED_COUNT = "summ_count";

        /**
         * Read-only count of {@link Contacts} from a specific source that have
         * no {@link CommonDataKinds.GroupMembership} entries, and also have phone numbers.
         * <p>
         * Type: INTEGER
         */
        public static final String UNGROUPED_WITH_PHONES = "summ_phones";
    }

    /**
     * <p>
     * Contacts-specific settings for various {@link Account}'s.
     * </p>
     * <h2>Columns</h2>
     * <table class="jd-sumtable">
     * <tr>
     * <th colspan='4'>Settings</th>
     * </tr>
     * <tr>
     * <td>String</td>
     * <td>{@link #ACCOUNT_NAME}</td>
     * <td>read/write-once</td>
     * <td>The name of the account instance to which this row belongs.</td>
     * </tr>
     * <tr>
     * <td>String</td>
     * <td>{@link #ACCOUNT_TYPE}</td>
     * <td>read/write-once</td>
     * <td>The type of account to which this row belongs, which when paired with
     * {@link #ACCOUNT_NAME} identifies a specific account.</td>
     * </tr>
     * <tr>
     * <td>int</td>
     * <td>{@link #SHOULD_SYNC}</td>
     * <td>read/write</td>
     * <td>Depending on the mode defined by the sync-adapter, this flag controls
     * the top-level sync behavior for this data source.</td>
     * </tr>
     * <tr>
     * <td>int</td>
     * <td>{@link #UNGROUPED_VISIBLE}</td>
     * <td>read/write</td>
     * <td>Flag indicating if contacts without any
     * {@link CommonDataKinds.GroupMembership} entries should be visible in any
     * user interface.</td>
     * </tr>
     * <tr>
     * <td>int</td>
     * <td>{@link #ANY_UNSYNCED}</td>
     * <td>read-only</td>
     * <td>Read-only flag indicating if this {@link #SHOULD_SYNC} or any
     * {@link Groups#SHOULD_SYNC} under this account have been marked as
     * unsynced.</td>
     * </tr>
     * <tr>
     * <td>int</td>
     * <td>{@link #UNGROUPED_COUNT}</td>
     * <td>read-only</td>
     * <td>Read-only count of {@link Contacts} from a specific source that have
     * no {@link CommonDataKinds.GroupMembership} entries.</td>
     * </tr>
     * <tr>
     * <td>int</td>
     * <td>{@link #UNGROUPED_WITH_PHONES}</td>
     * <td>read-only</td>
     * <td>Read-only count of {@link Contacts} from a specific source that have
     * no {@link CommonDataKinds.GroupMembership} entries, and also have phone
     * numbers.</td>
     * </tr>
     * </table>
     */
    public static final class Settings implements SettingsColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Settings() {
        }

        /**
         * The content:// style URI for this table
         */
        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, "settings");

        /**
         * The MIME-type of {@link #CONTENT_URI} providing a directory of
         * settings.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cens.loci.setting";

        /**
         * The MIME-type of {@link #CONTENT_URI} providing a single setting.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cens.loci.setting";
    }

    
}
