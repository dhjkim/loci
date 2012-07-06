package edu.cens.loci.sensors;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;

public class GpsHandler implements LocationListener {
	
	private static final String TAG = "GpsHandler";
		
	private Context mCxt;
	private LocationManager 	mLocService = null;
	private LocationProvider 	mLocProvider = null;
	
	public interface GpsListener {
		public void onGpsLocationChanged(long time, Location location);
	}
	
	private static GpsHandler sSingleton = null;
	public static synchronized GpsHandler getInstance(Context context) {
		if (sSingleton == null) {
			sSingleton = new GpsHandler(context);
		}
		return sSingleton;
	}
	
	private GpsHandler(Context context) {
		mCxt 	= context;
	}
	
	private HashMap<GpsListener, Long> mListeners = new HashMap<GpsListener, Long>();
	private long mMinTime = -1;
	
	public void printListeners() {
		Set<GpsListener> set = mListeners.keySet();
		Iterator<GpsListener> iter = set.iterator();
		MyLog.d(LociConfig.D.GPS, TAG, String.format("== GPS listeners (#listeners=%d) ==", set.size()));
		while (iter.hasNext()) {
			long time = mListeners.get(iter.next());
			MyLog.d(LociConfig.D.GPS, TAG, String.format("  time=%d (min)", time));
		}
	}
	
	public void requestUpdates(long minTime, GpsListener listener) {

		MyLog.e(LociConfig.D.GPS, TAG, String.format("[GPS] request gps (@ %d min)", minTime));		
		mListeners.put(listener, minTime);
		printListeners();
		if (mMinTime == -1 || mMinTime > minTime) {
			start(minTime);
		}
	}
	
	public void removeUpdates(GpsListener listener) {

		MyLog.e(LociConfig.D.GPS, TAG, String.format("[GPS] remove gps (@ %s)", listener.toString()));		
		mListeners.remove(listener);
		printListeners();
		if (mListeners.size() == 0) {
			stop();
		} else {
			long minTime = getMinimumTime();
			if (minTime > mMinTime)
				start(minTime);
		}
	}

	private long getMinimumTime() {
		
		if (mListeners.size() == 0)
			return -1;
		
		Set<GpsListener> keys = mListeners.keySet();
		Iterator<GpsListener> iter = keys.iterator();
		long minTime = Long.MAX_VALUE;
		while (iter.hasNext()) {
			GpsListener listener = iter.next();
			long time = mListeners.get(listener);
			if (time < minTime)
				minTime = time;
		}
		return minTime;
	}
	
	private boolean mIsOn = false;
	
	private synchronized boolean start(long minTime) {

		if (mIsOn) 
			stop();
		
		mIsOn = true;
		mLocService = (LocationManager) mCxt.getSystemService(Context.LOCATION_SERVICE);
		mLocProvider = mLocService.getProvider(LocationManager.GPS_PROVIDER);
	
		if (mLocProvider == null) {
			MyLog.e(LociConfig.D.GPS, TAG, "[GPS] start : getting gps provider failed.");
			return false;
		}
		
		if (Looper.myLooper() == null)
			Looper.prepare();
		
		mMinTime = minTime;
		mLocService.requestLocationUpdates(mLocProvider.getName(), mMinTime, 0, this);
		//LociMobileManagerService.getInstance().getSystemDb().addPowerRow(SystemLogDbAdapter.SENSOR_GPS, System.currentTimeMillis());
		MyLog.e(LociConfig.D.GPS, TAG, "[GPS] start gps : " + mMinTime);
		startWatchdogTimer();
		return true;
	}
	
	private synchronized void stop() {
		
		mIsOn = false;
		mLocService.removeUpdates(this);
		mMinTime = -1;
		MyLog.e(LociConfig.D.GPS, TAG, "[GPS] stop gps : ");
		stopWatchdogTimer();
	}

	public boolean isOn() {
		return mIsOn;
	}
	
	public void onLocationChanged(Location location) {
		long time = Calendar.getInstance().getTime().getTime();
		MyLog.i(LociConfig.D.GPS, TAG, "[GPS] received location : " + location.toString());

		mWatchdogFlag = true;
		
		Set<GpsListener> keys = mListeners.keySet();
		Iterator<GpsListener> iter = keys.iterator();
		
		while (iter.hasNext()) {
			GpsListener listener = iter.next();
			listener.onGpsLocationChanged(time, location);
		}
		
		//if (LociMobileManagerService.getInstance().isPTOn()) 
		//	LociMobileManagerService.getInstance().getSystemDb().updatePowerRow(SystemLogDbAdapter.SENSOR_GPS, date);
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}
	
	// Timer for a Watchdog which checks if the handler receives gps coordinates 
	private Timer mWatchdogTimer;
	private boolean mWatchdogFlag;
	private String TIMER_CHECK_GPS = "GpsCheckTimer";
	private String TIMER_RESTART_GPS = "GpsRestartTimer";
	
	
	private void startWatchdogTimer() {
		mWatchdogFlag = false;
		
		mWatchdogTimer = new Timer(TIMER_CHECK_GPS);
		
		mWatchdogTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (isOn() && !mWatchdogFlag) {
					MyLog.e(LociConfig.D.SYS, TAG, "[GPS] (check) GpsHandler is not receiving GPS fixes. restart handler.");
					
					if (mIsOn)
						stop();
					
					// restart Gps after 500 ms, instead of immediately restarting
					Timer restartTimer = new Timer(TIMER_RESTART_GPS);
					restartTimer.schedule(new TimerTask() {
						public void run() {
							MyLog.e(LociConfig.D.SYS, TAG, "[GPS] (check) Restart Gps.");
							start(mMinTime);
						}
					}, 500);
					
					//if (mState == SPEED_STATE_FAST) 
					//	mListener.onSlow();
				} else {
					MyLog.e(LociConfig.D.SYS, TAG, "[GPS] (check) GpsHandler OK.");
				}
				mWatchdogFlag = false;
				
			}
		}, LociConfig.pGpsWatch, LociConfig.pGpsWatch);
	}
	
	public void stopWatchdogTimer() {
		mWatchdogTimer.cancel();
		mWatchdogFlag = false;
	}

}
