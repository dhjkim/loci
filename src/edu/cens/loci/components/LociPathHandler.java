package edu.cens.loci.components;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import edu.cens.loci.LociConfig;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.sensors.GpsHandler;
import edu.cens.loci.sensors.GpsHandler.GpsListener;
import edu.cens.loci.utils.MyLog;

public class LociPathHandler implements GpsListener {
	
	public static final String TAG = "LociPathHandler";
	
	private int mState;

	public static final int SPEED_STATE_SLOW = 0;
	public static final int SPEED_STATE_FAST = 1;
	
	private Context mCxt = null;
	private GpsHandler mGpsHandler = null;
	private LociDbUtils mDbUtils = null;
	private PathListener mListener = null;
	private long mMinTime = 0;
	//private LociPathHandler mInstance;
	
	private boolean mIsOn = false;
	
	/**
	 * Event handlers 
	 */
	public interface PathListener {
		public void onFast();
		public void onSlow();
		public void onLocationChanged(Date time, Location loc);
	}
	
	public LociPathHandler(Context context, PathListener listener, long gpsMinTime) {
		mCxt = context;
		mListener = listener;
		mGpsHandler = GpsHandler.getInstance(mCxt);
		mMinTime = gpsMinTime;
		mDbUtils = new LociDbUtils(context);
	
		//mInstance = this;
	}

	public boolean isOn() {
		return mIsOn;
	}
	
	public void start() {
		
		if (mIsOn) {
			MyLog.e(LociConfig.D.PT.CALL, TAG, "[PT] [+] PathHandler (on/off) is already on, ignore.");
			return;
		}
		
		mIsOn = true;
		
		MyLog.e(LociConfig.D.PT.CALL, TAG, "[PT] [+] PathHandler (on/off)");
		mGpsHandler.requestUpdates(mMinTime, this);
		setCheckSpeedTimer();
		mState = SPEED_STATE_SLOW;
		
	}
	
	public void stop() {
		
		if (!mGpsHandler.isOn()) {
			MyLog.e(LociConfig.D.PT.CALL, TAG, "[PT] [-] PathHandler (on/off) is already off, ignore.");
			return;
		}
		
		mIsOn = false;
		
		releaseCheckSpeedTimer();
		
		//mGpsHandler.stop();
		mGpsHandler.removeUpdates(this);
		MyLog.e(LociConfig.D.PT.CALL, TAG, "[PT] [-] PathHandler (on/off)");
	}

	public void setToCheckFast() {
		mState = SPEED_STATE_SLOW;
	}
	
	public void setToCheckSlow() {
		mState = SPEED_STATE_FAST;
	}
	
	public void onSlow() {
		mState = SPEED_STATE_SLOW;
		mListener.onSlow();
	}
	
	public void onFast() {
		mState = SPEED_STATE_FAST;
		mListener.onFast();
	}
	
	// timer to regularly check speed
	private Timer mCheckSpeedTimer;
	
	private void setCheckSpeedTimer() {
		
		mCheckSpeedTimer = new Timer("checkSpeedTimer");
		
		mCheckSpeedTimer.scheduleAtFixedRate(new TimerTask() {
			public synchronized void run() {
				
				float avgSpeed = getAvgSpeed();
				float scanCnt = mSpeedCnt;

				MyLog.d(LociConfig.D.PT.SPEED, TAG, String.format("[PT] (check speed) : avg=%5.2f (%5.2f)", avgSpeed, mSpeedCnt));
				
				if (mState == SPEED_STATE_SLOW) {
					if (scanCnt != 0 && avgSpeed >= LociConfig.pSpeedTh) {
						onFast();
					}
				} else {
					if (scanCnt == 0 || avgSpeed <= LociConfig.pSpeedTh) {
						onSlow();
					}
				}
				clearAvgSpeed();
			}
		}, 0, LociConfig.pSpeedCheckRate);
	}
	
	private void releaseCheckSpeedTimer() {
		mCheckSpeedTimer.cancel();
	}
	
	// used to calculate average speed over a time-window to signal PlaceHandler sleep opportunities
	float mSpeedSum = 0;
	float mSpeedCnt = 0;

	public float getAvgSpeed(){
		return mSpeedSum/mSpeedCnt;
	}
	
	private void updateAvgSpeed(float newVal) {
		mSpeedSum += newVal;
		mSpeedCnt++;
	}
	
	private void clearAvgSpeed() {
		mSpeedSum = 0;
		mSpeedCnt = 0;
	}

	long mLastLocationChangedTime = -1;
	public synchronized void checkLastChangedTime(long time) {
		if (mLastLocationChangedTime == -1) mLastLocationChangedTime = time;
		long timespan = time - mLastLocationChangedTime;
		if (timespan > mMinTime*2) {
			MyLog.d(LociConfig.D.PT.EVENT, TAG, String.format("[PT] haven't received gps for a while. (%d)", timespan));
			if (mState == SPEED_STATE_FAST) {
				MyLog.d(LociConfig.D.PT.EVENT, TAG, "[PT] set state to slow.");
				onSlow();
			}
		}
		mLastLocationChangedTime = time;
	}
	
	public void onGpsLocationChanged(long time, Location loc) {

		if (!mGpsHandler.isOn()) {
			MyLog.e(LociConfig.D.PT.EVENT, TAG, "[PT] eventGpsResult() : received a gps fix, but the path manager is off.\n");
		} else {
			checkLastChangedTime(loc.getTime());
			mDbUtils.insertPosition(time, loc);

			if (loc.hasSpeed()) {
				MyLog.d(LociConfig.D.PT.EVENT, TAG, "[PT] got gps : (has speed) speed = " + loc.getSpeed() + " avg speed=" + getAvgSpeed() + " cnt=" + mSpeedCnt);
				updateAvgSpeed(loc.getSpeed());
			} else {
				MyLog.d(LociConfig.D.PT.EVENT, TAG, "[PT] got gps : (no  speed) speed = " + loc.getSpeed() + " avg speed=" + getAvgSpeed() + " cnt=" + mSpeedCnt);				
				 
			}
		}
	}
	
}
