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

public final class LociConfig {
	
	public static final String LOCAL_SEARCH_URL = "http://ajax.googleapis.com/ajax/services/search/local";
	public static final String MAP_KEY = "ABQIAAAAiRfCnvaSdl5LRhxO3W6dkxT2yXp_ZAY8_ufC3CFXhHIE1NvwkxTlgcfIvIl01Y9Dn_2peDQ5z12kqQ";
	
	public static final class Preferences {
		public static final String PREFS_NAME = "loci2_preference_file";	
		public static final String PREFS_KEY_SERVICE_ON = "service_on";
		public static final String PREFS_KEY_SERVICE_POWER_BUTTON_ON = "service_power_button_on";
	}
	
	public static final boolean LOG = true;
	
	public static final class D {
		
		public static final boolean ACC = true;
		public static final boolean WIFI = true;
		public static final boolean GPS = true;

		public static final boolean SYS = true;
		public static final boolean ERROR = true;
		public static final boolean JSON = true;
		
		public static final boolean FILE = true;
		
		public static final class UI {
			public static final boolean MAP = false;
			public static final boolean CALENDAR = false;
			public static final boolean LIST = false;
			public static final boolean OVERLAYS = false;
			public static final boolean STATUS = false;
			public static final boolean DEBUG = true;
			public static final boolean PLACE_SEARCH = false;
		}
		
		public static final class DB {
			public static final boolean DEBUG = true;
			public static final boolean CALL = true;
			public static final boolean UTILS = true;
		}
		
		public static final class MANAGER {
			public static final boolean CALL = true;
			public static final boolean EVENT = true;
		}
		
		public static final class PD {
			public static final boolean CALL = true;
			public static final boolean CHECK = true;
			public static final boolean EVENT = true;
			public static final boolean SCORE = true;
			public static final boolean LOAD = true;
		}
		
		public static final class PT {
			public static final boolean CALL = true;
			public static final boolean EVENT = true;
			public static final boolean SPEED = true;
		}

		public static final class MD {
			public static final boolean CALL = true;
			public static final boolean EVENT = true;
		}
		
		public static final class Service {
			public static final boolean DEBUG = true;
			public static final boolean EVENT = true;
			public static final boolean ALERT = true;
		}
		
		public static final class Reboot {
			public static final boolean DEBUG = true;
			public static final boolean CALL = true;
		}
		
		public static final class Watchdog {
			public static final boolean DEBUG = true;
			public static final boolean CALL = true;
		}
		
		public static final class Classes {
			public static final boolean LOG_CIRCLE = true;
			public static final boolean LOG_LOCATION = false;
			public static final boolean LOG_WIFI = true;
			public static final boolean LOG_VISIT = true;
		}
		
	}
	
	// sensor basic sampling rate
	public static final long 		pWifiMinTime 		= 10000;
	public static final long 		pGpsMinTime 		= 30000;
	public static final int			pAccDuration  	= 2000;
	public static final int 		pAccPeriod  		= 6000;
	
	public static final long 		pWatchDog       = 60000;
	public static final long 		pWifiWatch   		= 300000;
	public static final long    pGpsWatch    		= 300000;
	public static final long    pAccWatch    		= 300000;
	
	// circle
	public static final float 	pMinSuggestedRadius = 30;
	public static final float 	pMaxSuggestedRadius = 100;
	public static final float 	pMaxAccuracy 				= 200;
	public static final float		pMinRadius 					= 15;
	public static final float 	pMaxRadius					= 300;
	
	// place learner
	public final static long 		pMinVisitDurationForSuggestion = 300000; // in msec
	
	public final static float		pSimTh 				= 0.60f;		// similarity threshold
	public final static float 	pSimRecTh 		= 0.70f;
	public final static int  		pConfMax 			= 4; 				// confidence level max

	public final static int			pBufSize			= 2;
	
	public final static float 	pRepStep 			= 0.2f;
	public final static float 	pRepMin 			= 0.5f;
	public final static double	pMinRss 			= -105;
	public final static double	pMaxRss 			= -35;
	
	
	public static final long 		pRecogDelay  	= 60000;
	public static final long 		pPTDelay 			= 120000;
	public static final long 		pMDSmallDelay = 60000;
	public static final long 		pMDDelay 			= 300000;
	
	// movement detector
	public final static float 	pVarThreshold 	= 0.1f;
	public final static long  	pStillTime 			= 30000; // millisec
	public final static long  	pMoveTime 			= 12000; // millisec
	public final static long  	pStaleTime 			= 30000; // used to check movement when #AP = 0
	
	// path tracker
	public final static long 		pSpeedCheckRate = 30000; // 30 sec
	public final static float 	pSpeedTh 				= 2;		 // 2 m/s
	
	// track display
	public final static int 		pMapPathMinDistance = 50; // in meters
}
