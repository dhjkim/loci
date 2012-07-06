package edu.cens.loci.components;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import edu.cens.loci.LociConfig;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.classes.LociVisitWifi;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.WifiScanBuffer;
import edu.cens.loci.classes.LociVisit.RecognitionResult;
import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.sensors.WifiHandler;
import edu.cens.loci.sensors.WifiHandler.WifiListener;
import edu.cens.loci.utils.MyLog;

public class PlaceDetectorWifi implements PlaceDetector, WifiListener {
	
	private static final String TAG = "PlaceDetectorWifi";

	private LociVisitWifi mVisit;
	private Context mCxt;
	private PlaceDetectorListener mListener;
	
	private WifiHandler mWifiHandler = null;
	private WifiScanBuffer mScanBuf = null;
	private LociWifiFingerprint mLastWifiScan = null;
	
	private int mConfLev = 0;
	private boolean mAtPlace = false;
	private LociDbUtils mDbUtils = null;
	private boolean mIsOn = false;
	
	private int mMaxConfLev = LociConfig.pConfMax;
	
	
	public PlaceDetectorWifi(Context context, PlaceDetectorListener listener) {

		mCxt = context;
		mListener = listener;
		
		mDbUtils = new LociDbUtils(context);
		
		// get last visit from database
		mVisit = mDbUtils.getLastWifiVisit();
		
		if (mVisit == null) {
			mVisit = new LociVisitWifi();
			MyLog.d(LociConfig.D.PD.LOAD, TAG, "[PD Wifi] loaded a new (Wifi) Visit.");
			
			mConfLev = 0;

		} else {
			MyLog.d(LociConfig.D.PD.LOAD, TAG, "[PD Wifi] loaded last (Wifi) Visit.");
			mAtPlace = true;
			mListener.resetMovementDetectorOnTime();
			mListener.resetPathTrackerOffTime();
			mListener.resetPlaceCheckTime();

			mConfLev = 1;
		}
		
		MyLog.d(LociConfig.D.PD.LOAD, TAG, mVisit.toString());
		
		mWifiHandler = WifiHandler.getInstance(mCxt);
		mScanBuf = new WifiScanBuffer(LociConfig.pBufSize);
		mLastWifiScan = new LociWifiFingerprint();
	}
	
	public void clear() {
		mVisit.clear();
		mConfLev = 0;
		mScanBuf.clear();
		mLastWifiScan.initialize();
	}
	
	public boolean isWifiStable() {
		return mConfLev >= mMaxConfLev;
	}
	
	public int getConfLevel() {
		return mConfLev;
	}
	
	public boolean isWifiOn() {
		return mWifiHandler.isOn();
	}
	
	/**
	 * 
	 */
	public void onWifiChanged(long time, ArrayList<ScanResult> wifiscan) {
		mScanBuf.updateScanBuffer(time, wifiscan);
		
		if (!mAtPlace) 
			checkEnter(time, mScanBuf.getAPInfoMap());
		else
			checkExit(time, mScanBuf.getAPInfoMap());
	}
	
	public boolean isAtPlace() {
		return mAtPlace;
	}
	
	/**
	 * 
	 * @param time
	 * @param scan
	 */
	public void checkEnter(long time, HashMap<String, APInfoMapItem> scan) {
		
		if (mConfLev == 0) {
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check enter] : c-level is 0.");
			mVisit.update(time, scan);
			increaseConfLev();
		} else {
			mLastWifiScan.initialize();
			mLastWifiScan.update(time, scan);
			
			if (mLastWifiScan.getNumAPs() == 0) {
				MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check enter] : last wifi scan is empty.");
				MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi]  --> " + mLastWifiScan.toString());
				return;
			}
			
			float score = (float) LociWifiFingerprint.tanimotoScore(mVisit.wifi, mLastWifiScan, null, null, 0, true);
			
			//MyLog.d(LociConfig.D.PD.EVENT, TAG, "  sig: " + mVisit.wifi.toString());
			//MyLog.d(LociConfig.D.PD.EVENT, TAG, " scan: " + mLastWifiScan.toString());
			MyLog.d(LociConfig.D.PD.SCORE, TAG, "[PD Wifi] [check enter] : score = " + score);
			
			if (score < LociConfig.pSimTh) {
				MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check enter] : scans are not similar, initialize.");
				mVisit.clear();
				mConfLev = 0;
			}
			
			mVisit.update(time, scan);
			increaseConfLev();
			if (mConfLev >= mMaxConfLev) {
				mAtPlace = true;
				mListener.onEnter(time);
			}
		}
	}
	
	private boolean mCheckingPlacePosition = false;
	
	private void checkPlacePosition() {
		
		if (mCheckingPlacePosition)
			return;
		
		ArrayList<LociLocation> locs = mDbUtils.getTrack(mVisit.enter-30000, mVisit.exit+30000, 0);
		
		if (locs.size() == 0) {
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] has no position information, collect one.");
			
			final LocationManager locManager = (LocationManager) mCxt.getSystemService(Context.LOCATION_SERVICE);
	    
			LocationProvider locProvider = locManager.getProvider(LocationManager.NETWORK_PROVIDER);
			
			final LocationListener locListener = new LocationListener() {
				public void onLocationChanged(Location location) {
					long time = Calendar.getInstance().getTime().getTime();
					MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] place position update, save.");
					mDbUtils.insertPosition(time, location);
				}
				public void onProviderDisabled(String provider) {}
				public void onProviderEnabled(String provider) {}
				public void onStatusChanged(String provider, int status, Bundle extras) {}
			};
			
			mCheckingPlacePosition = true;
			
			locManager.requestLocationUpdates(locProvider.getName(), 0, 0, locListener);
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] start collecting place position.");
			
			Timer stopTimer = new Timer();
			stopTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] stop collecting place position.");
					locManager.removeUpdates(locListener);
					mCheckingPlacePosition = false;
				}}, 60000);
			
		}
	}
	
	/**
	 * 
	 * @param time
	 * @param scan
	 */
	public void checkExit(long time, HashMap<String, APInfoMapItem> scan) {

		mLastWifiScan.initialize();
		mLastWifiScan.update(time, scan);
		
		HashSet<String> repAPs = mVisit.wifi.getRepAPs();
		
		double score = LociWifiFingerprint.tanimotoScore(mVisit.wifi, mLastWifiScan, repAPs, repAPs, 0, true);

		//if (LociConfig.D.PD.EVENT) {
		//	mVisit.wifi.log();
		//	mLastWifiScan.log();
		//}
		MyLog.d(LociConfig.D.PD.SCORE, TAG, "[PD Wifi] [check exit] : score = " + score);
		
		if (mLastWifiScan.getNumAPs() == 0) {
			// no APs around
			//MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : last scan has no APs, do nothing for now.");
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : last scan has no APs, decrease confidence.");
			decreaseConfLev();
		} else if (score < LociConfig.pSimTh) {
			// similarity score is low
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : similarity score is low.");
			if (mVisit.wifi.hasNewBssid(mLastWifiScan)) {
				// if it contains new APs
				MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : and contains new beacons, decrease.");
				decreaseConfLev();
			} else {
				MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : no new beacons, do nothing.");
				mVisit.update(time, scan);
			}
		} else {
			// similarity score is high
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : similarity score is high, increase.");
			increaseConfLev();
			mVisit.update(time, scan);
		}
	
		if (mConfLev <= 0) {
			MyLog.d(LociConfig.D.PD.EVENT, TAG, "[PD Wifi] [check exit] : similarity score below 0. exit.");
			mAtPlace = false;
			mListener.onExit(time);
		} else {
			
			mListener.onStay(time);
			
			long stayTime = time - mVisit.enter;
			
			if (stayTime >= 60000) {
				//Log.d(TAG, "CHECK PLACE POSITION");
				checkPlacePosition();
			} else {
				//Log.d(TAG, "NOT YET : " + stayTime);
			}

		}
	}
	
	public long checkPlaceId() {
		ArrayList<RecognitionResult> results = mDbUtils.getRecogntionScoresWifi(mVisit.wifi);
		
		// DEBUG
		//for (RecognitionResult result : results) {
		//	Log.d(TAG, result.toString());
		//}
		
		if (results.size() > 0) {
			RecognitionResult bestRecognitionResult = results.get(0);
			double score = bestRecognitionResult.score;
			mVisit.updateRecognitionResult(bestRecognitionResult.time, bestRecognitionResult.placeId, bestRecognitionResult.fingerprintId, bestRecognitionResult.score);
			if (score >= LociConfig.pSimRecTh) {
				mVisit.placeId = bestRecognitionResult.placeId;
				return mVisit.placeId;
			}
		} else {
			mVisit.updateRecognitionResult(System.currentTimeMillis(), -1, 0, 1);
		}
		return -1;
	}

	public void start(long minTime) {
		
		if (minTime <= 10000)
			mMaxConfLev = 4;
		else
			mMaxConfLev = 2;
			
		if (mIsOn) {
			MyLog.e(LociConfig.D.PD.CALL, TAG, "[PD Wifi] [+] start (on/off), is already on.");
			return;
		}
		mIsOn = true;
		
		MyLog.e(LociConfig.D.PD.CALL, TAG, "[PD Wifi] [+] start (on/off)");
		MyLog.e(LociConfig.D.PD.CALL, TAG, String.format("  minTime=%d, maxConfLev=%d", minTime, mMaxConfLev));
		mWifiHandler.requestUpdates(minTime, this);
	}

	public void stop() {
		
		if (!mIsOn) {
			MyLog.e(LociConfig.D.PD.CALL, TAG, "[PD Wifi] [-] stop (on/off), is already on.");
		}
		mIsOn = false;
		
		MyLog.e(LociConfig.D.PD.CALL, TAG, "[PD Wifi] [-] stop (on/off)");
		mWifiHandler.removeUpdates(this);
	}
	
	public LociVisitWifi getVisit() {
		return mVisit;
	}
	
	private void increaseConfLev() {
		if (mConfLev < mMaxConfLev) 
			mConfLev = mConfLev + 1;
	}
	
	private void decreaseConfLev() { 
		if (mConfLev > 0) 
			mConfLev = mConfLev - 1;
	}
}
