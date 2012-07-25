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
package edu.cens.loci;

public final class Constants {
	
	public static final String SERVICE_NAME = "edu.cens.loci.components.LociManagerService";
	
  /**
   * Account type string.
   */
  public static final String ACCOUNT_TYPE = "edu.cens.loci";

  /**
   * Authtoken type string.
   */
  public static final String AUTHTOKEN_TYPE = "edu.cens.loci";
	
	public static final class PlacesList {
		public static final int LIST_ORDER_TYPE_NAME = 0;
		public static final int LIST_ORDER_TYPE_WIFI_SIMILARITY = 1;
		public static final int LIST_ORDER_TYPE_DISTANCE = 2;
	}
	
	public static final class Intents {
		
		public static final class UI {
			
			public static final String ACTION_VIEW_SETTINGS = 
				"edu.cens.loci.action.VIEW_SETTINGS";
			
			public static final String ACTION_VIEW_SUGGESTED_GPS_PLACES = 
				"edu.cens.loci.action.VIEW_SUGGESTED_GPS_PLACES";
		
			public static final String ACTION_CREATE_FROM_SUGGESTED_PLACE = 
				"edu.cens.loci.action.CREATE_FROM_SUGGESTED_PLACE";
			
			public static final String ACTION_ADDTO_FROM_SUGESTED_PLACE =
				"edu.cens.loci.action.ADDTO_FROM_SUGGESTED_PLACE";
			
			public static final String ACTION_CREATE_OR_ADDTO_FROM_SUGGESTED_PLACE =
				"edu.cens.loci.action.CREATE_OR_ADDTO_FROM_SUGGESTED_PLACE";
			
			public static final String ACTION_INSERT = 
				"edu.cens.loci.action.INSERT";
			
			public static final String LIST_ORDER_EXTRA_KEY = 
				"edu.cens.loci.extra.LIST_ORDER_KEY";
			
			public static final String LIST_ORDER_EXTRA_WIFI_KEY =
				"edu.cens.loci.extra.LIST_ORDER_WIFI_KEY";
			
			public static final String LIST_ORDER_EXTRA_WIFI_TIME_KEY =
				"edu.cens.loci.extra.LIST_ORDER_WIFI_TIME_KEY";
			
			public static final String LIST_ORDER_EXTRA_GPS_KEY = 
				"edu.cens.loci.extra.LIST_ORDER_GPS_KEY";
			
			public static final String FILTER_STATE_EXTRA_KEY = 
				"edu.cens.loci.extra.FILTER_STATE";

			public static final int FILTER_STATE_SUGGESTED = 1;
			public static final int FILTER_STATE_REGISTERED = 2;
			public static final int FILTER_STATE_BLOCKED = 4;
			
			public static final String FILTER_TYPE_EXTRA_KEY =
				"edu.cens.loci.extra.FILTER_TYPE";
			
			public static final int FILTER_TYPE_GPS = 1;
			public static final int FILTER_TYPE_WIFI = 2;
			
			public static final String FILTER_TAG_EXTRA_KEY = 
				"edu.cens.loci.extra.FILTER_TAG";
			
			/**
			 * A key for to be used as an intent extra to set the activity
			 * title to a custom String value.
			 */
			public static final String TITLE_EXTRA_KEY = 
				"edu.ucla.cens.android.loci.extra.TITLE_EXTRA";
			
			public static final String SELECTED_PLACE_ID_EXTRA_KEY =
				"edu.ucla.cens.android.loci.extra.SELECTED_PLACE_ID";
			
			public static final String PLACE_ENTRY_TYPE_EXTRA_KEY =
				"edu.ucla.cens.android.loci.extra.PLACE_ENTRY_TYPE";
			
			/**
			 * Intent used by EditMap
			 */
			public static final String MAP_EDIT_CIRCLE_EXTRA_KEY =
				"edu.ucla.cens.android.loci.extra.MAP_EDIT_CIRCLE";
			public static final String MAP_EDIT_MODE_EXTRA_KEY = 
				"edu.ucla.cens.android.loci.extra.MAP_EDIT_MODE";
			public static final String MAP_EDIT_VALUES_EXTRA_KEY = 
				"edu.ucla.cens.android.loci.extra.MAP_EDIT_VALUES";
		}

		public static final class Insert {
			public static final String WIFI_FINTERPRINT = "wifi_fingerprint";
			public static final String GPS_CIRCLE = "gps_circle";
			public static final String TIME = "time";
		}
	}
}
