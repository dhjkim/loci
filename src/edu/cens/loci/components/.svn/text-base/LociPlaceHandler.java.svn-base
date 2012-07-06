package edu.cens.loci.components;

import java.util.Calendar;

import android.content.Context;
import edu.cens.loci.LociConfig;
import edu.cens.loci.classes.LociVisitWifi;
import edu.cens.loci.components.PlaceDetector.PlaceDetectorListener;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

/**
 * @author dhjkim
 *	Turns on GPS/Wifi Place Detectors.
 * 	Decides when to signal other components.
 * 
 *  When both WifiPlaceDetector and GpsPlaceDetector are on,
 *  WifiPlaceDetector decides when to signal other components.
 *  Save visit instance to Database.
 *  
 *  GpsPlaceDetector decides only when WifiPlaceDetector is not used.
 * 
 *  1) PlaceDetectorWifi:
 *  		a. After entering a place, decide when to turn off Path Tracker
 *      b. After entering a place, decide when to turn on Movement Detector
 *      c. Pause/Resume Wifi Place Detector (Controlled by the Service Manager)
 *
 * 	2) PlaceDetectorGps:
 * 			a. After entering a place, decide when to turn on Movement Detector
 * 			b. Pause/Resume Gps Place Detector (Controlled by the Service Manager)
 * 
 */
public class LociPlaceHandler {

	private static final String TAG = "LociPlaceHandler";
	
	private Context mCxt;
	
	private PlaceDetectorWifi 	mPlaceDetectorWifi = null;
	private PlaceDetectorGps 		mPlaceDetectorGps = null;
	private LociDbUtils 				mDbUtils = null;
	private PlaceListener 			mListener = null;
	
	private boolean mIsOn = false;
	
	private long mMinTime = -1;
	
	private static final long pMinTime = 1000; 		// msec
	private static final long pMaxTime = 60000; 	// msec
	
	public interface PlaceListener {
		public void onEnter();
		public void onExit();
		
		public void onStopPathHandler();
		public void onStartMovementDetector();
	}
 	
	public LociPlaceHandler(Context context, PlaceListener listener, long minTime) {
		mCxt = context;
		mListener = listener;
		mDbUtils = new LociDbUtils(context);
		// check minTime
		mMinTime = checkMinTime(minTime);
	}

	private long checkMinTime(long time) {
		if (time < pMinTime)
			return pMinTime;
		else if (time > pMaxTime)
			return pMaxTime;
		else
			return time;
	}

	public synchronized void start(boolean gpsOn, boolean wifiOn) {
		
		mIsOn = true;
		
		if (wifiOn) {
			mPlaceDetectorWifi = new PlaceDetectorWifi(mCxt, mWifiPlaceListener);
			mWifiPlaceListener.resetMovementDetectorOnTime();
			mWifiPlaceListener.resetPathTrackerOffTime();
			mWifiPlaceListener.resetPlaceCheckTime();
			mPlaceDetectorWifi.start(mMinTime);
		}
			
		if (gpsOn) {
			mPlaceDetectorGps = new PlaceDetectorGps(mCxt, mGpsPlaceListener);
			mPlaceDetectorGps.start(mMinTime);
		}
	}
	
	public void resume() {
		
		mIsOn = true;
		
		if (mPlaceDetectorWifi != null) {
			mWifiPlaceListener.resetMovementDetectorOnTime();
			mWifiPlaceListener.resetPathTrackerOffTime();
			mWifiPlaceListener.resetPlaceCheckTime();
			mPlaceDetectorWifi.start(mMinTime);
		}
		
		if (mPlaceDetectorGps != null) {
			mPlaceDetectorGps.start(mMinTime);
		}
	}
	
	public void reset() {
		if (mPlaceDetectorWifi != null) {
			if (mPlaceDetectorWifi.isAtPlace())
				mDbUtils.updateWifiVisit(mPlaceDetectorWifi.getVisit());
			mPlaceDetectorWifi.clear();
		}
		
		if (mPlaceDetectorGps != null) {
			mPlaceDetectorGps.clear();
		}
	}
	
	public void pause() {
		
		mIsOn = false;
		
		if (mPlaceDetectorWifi != null)
			mPlaceDetectorWifi.stop();
		
		if (mPlaceDetectorGps != null)
			mPlaceDetectorGps.stop();
	}
	
	public synchronized void stop() {
		
		mIsOn = false;
		
		if (mPlaceDetectorWifi != null) {
			mPlaceDetectorWifi.stop();
			mPlaceDetectorWifi = null;
		}
		if (mPlaceDetectorGps != null) {
			mPlaceDetectorGps.stop();
			mPlaceDetectorGps = null;
		}
	}
	
	public boolean isOn() {
		return mIsOn;
	}
	
	public boolean isWifiStable() {
		return mPlaceDetectorWifi.isWifiStable();
	}
	
	public long getCurrentWifiVisitId() {
		if (mPlaceDetectorWifi == null)
			return -1;
		else
			return mPlaceDetectorWifi.getVisit().visitId;
	}
	
	public long getCurrentGpsVisitId() {
		if (mPlaceDetectorGps == null)
			return -1;
		else
			return mPlaceDetectorGps.getVisitId();
	}
	
	public void updateCurrentVisitExitTime(long exit) {
		if (mPlaceDetectorWifi != null) {
			mPlaceDetectorWifi.getVisit().exit = exit;
			mDbUtils.updateWifiVisit(mPlaceDetectorWifi.getVisit());
		}
	}
	
	private PlaceDetectorListener mWifiPlaceListener = new PlaceDetectorListener() {

		private long mPathTrackerOffTime = 0;
		private long mMovementDetectorOnTime = 0;
		private long mPlaceCheckTime = 0;

		
		public void onEnter(long time) {
			
			// set PathTracker off-time
			mPathTrackerOffTime = time + LociConfig.pPTDelay;
			// set next place check-time
			mPlaceCheckTime = time + LociConfig.pRecogDelay;
			// set next movement detector on-time
			mMovementDetectorOnTime = time + LociConfig.pMDDelay;
			
			MyLog.e(LociConfig.D.PD.EVENT, TAG, String.format("[PD] [entering] PTStop=%s recog=%s MDStart=%s", 
					MyDateUtils.getTimeFormatMedium(mPathTrackerOffTime), 
					MyDateUtils.getTimeFormatMedium(mPlaceCheckTime), 
					MyDateUtils.getTimeFormatMedium(mMovementDetectorOnTime)));
			
			// save visit
			mPlaceDetectorWifi.getVisit().visitId = (int) mDbUtils.insertWifiVisit(mPlaceDetectorWifi.getVisit());
			
			mListener.onEnter();
		}

		public void onExit(long time) {

			MyLog.e(LociConfig.D.PD.EVENT, TAG, "[PD] [exiting] " + MyDateUtils.getTimeFormatMedium(time));
			
			// check place suggestion (suggest if the stay was longer than 5 minutes)
			LociVisitWifi visit = mPlaceDetectorWifi.getVisit();
			if (visit.placeId < 0 && visit.getDuration() >= LociConfig.pMinVisitDurationForSuggestion) {
				long placeid = mDbUtils.insertSuggestedWifiPlace(visit);
				visit.placeId = placeid;
				
				if (placeid < 0)
					MyLog.e(LociConfig.D.PD.EVENT, TAG, "=> [new place] failed to create a new suggested place.");
				else
					MyLog.e(LociConfig.D.PD.EVENT, TAG, "=> [new place] created a new suggested place (pid=" + placeid + ")");
			} else {
				MyLog.e(LociConfig.D.PD.EVENT, TAG, String.format("=> [not saving a new place] pid=%d duration=%d", visit.placeId, visit.getDuration()));
			}
			
			// save visit
			mDbUtils.updateWifiVisit(mPlaceDetectorWifi.getVisit());

			// finish current visit
			mPlaceDetectorWifi.clear();
			
			mListener.onExit();
		}

		public void onStay(long time) {
			
			MyLog.e(LociConfig.D.PD.EVENT, TAG, String.format("[PD] [staying] PTStop=%s recog=%s MDStart=%s", 
					MyDateUtils.getTimeFormatMedium(mPathTrackerOffTime), 
					MyDateUtils.getTimeFormatMedium(mPlaceCheckTime), 
					MyDateUtils.getTimeFormatMedium(mMovementDetectorOnTime)));
			
			// save visit 
			mDbUtils.updateWifiVisit(mPlaceDetectorWifi.getVisit());
			
			// turn off PathTracker
			if (LociManagerService.getInstance().isPTOn()) {
				if (time > mPathTrackerOffTime) {
					if (mPlaceDetectorWifi.getConfLevel() >= LociConfig.pConfMax) {
						MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check PT off) turn off path handler.");
						mListener.onStopPathHandler();
					} else {
						MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check PT off) c-level is low. " + mPlaceDetectorWifi.getConfLevel());
					}
				} else {
					MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check PT off) time not reached yet.");
				}
			} else {
				MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check PT off) PT is already off.");
			}
			
			// turn on MovementDetector
			if (!LociManagerService.getInstance().isMDOn()) {
				if (time > mMovementDetectorOnTime) {
					if (mPlaceDetectorWifi.getConfLevel() >= LociConfig.pConfMax) {
						MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check MD on) turn on movement detector.");
						mListener.onStartMovementDetector();
					} else {
						MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check MD on) c-level is low. " + mPlaceDetectorWifi.getConfLevel());
					}
				} else {
					MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check MD on) time not reached yet. " + MyDateUtils.getTimeFormatMedium(mMovementDetectorOnTime));
				}
			} else {
				MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check MD on) MD is already on.");
			}

			// check place
			if (time > mPlaceCheckTime) {
				mPlaceDetectorWifi.checkPlaceId();
				mPlaceCheckTime = time + LociConfig.pRecogDelay;
				MyLog.d(LociConfig.D.PD.CHECK, TAG, "[PD] (check PlaceID) placeId=" + mPlaceDetectorWifi.getVisit().placeId);
			} else {
				MyLog.e(LociConfig.D.PD.CHECK, TAG, "[PD] (check PlaceID) time not reached yet. " + MyDateUtils.getTimeFormatMedium(mPlaceCheckTime));
			}

		}
		
		public void resetMovementDetectorOnTime() {
			mMovementDetectorOnTime = Calendar.getInstance().getTime().getTime() + LociConfig.pMDSmallDelay;
		}
		
		public void resetPathTrackerOffTime() {
			mPathTrackerOffTime = Calendar.getInstance().getTime().getTime() + LociConfig.pPTDelay;
		}

		public void resetPlaceCheckTime() {
			mPlaceCheckTime = Calendar.getInstance().getTime().getTime() + LociConfig.pRecogDelay;
		}
		
	};
	
	private PlaceDetectorListener mGpsPlaceListener = new PlaceDetectorListener() {
		
		public void onEnter(long time) {
			// if WifiPlaceLearner is not used, set next movement detector on-time
			// save visit
		}

		public void onExit(long time) {
			// save visit
		}

		public void onStay(long time) {
			// save visit
			// if WifiPlaceLearner is not used, turn on MovmentDetector
		}

		public void resetMovementDetectorOnTime() {
			// TODO Auto-generated method stub
			
		}

		public void resetPathTrackerOffTime() {
			// TODO Auto-generated method stub
			
		}

		public void resetPlaceCheckTime() {
			// TODO Auto-generated method stub
			
		}
		
	};
	
}
