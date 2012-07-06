package edu.cens.loci.components;

import android.content.Context;
import android.hardware.SensorManager;
import edu.cens.loci.LociConfig;
import edu.cens.loci.sensors.AccelerometerHandler;
import edu.cens.loci.sensors.AccelerometerHandler.AccelerometerListener;
import edu.cens.loci.utils.MyLog;

public class LociMovementHandler implements AccelerometerListener {
	
	public static final String TAG = "LociMovementHandler";
	
	public static final int MOVEMENT_STATE_STILL 	= 1;
	public static final int MOVEMENT_STATE_MOVE 	= 2;
	
	private Context mCxt;
	private AccelerometerHandler mAccHandler;
	private MovementListener mListener;
	
	private boolean mIsOn = false;
	private int 		mState;
	private long 		mTimer;
	private int 		mCnt;

	private int   mInstantState;
	private long  mInstantStateTime;
	
	public interface MovementListener {
		public void onStill();
		public void onMove();
		public void onStillUpdateExitTime(long extTime);
	}
	
	public LociMovementHandler(Context context, MovementListener listener, int duration, int period) {
		// initiate properties
		mCxt 				= context;
		mListener   = listener;
		mAccHandler = new AccelerometerHandler(mCxt, this, duration, period, SensorManager.SENSOR_DELAY_NORMAL);
		
		mState = MOVEMENT_STATE_MOVE;
	}
	
	public synchronized void start() {
		
		if (mIsOn == true) {
			MyLog.e(LociConfig.D.MD.CALL, TAG, "[MD] [+] MovementHandler (on/off) is already on, ignore.");
			return;
		}
		
		MyLog.e(LociConfig.D.MD.CALL, TAG, "[MD] [+] MovementHandler (on/off)");
		mIsOn = true;
		mAccHandler.start();

	}
	
	public synchronized void stop() {
		
		mState = MOVEMENT_STATE_MOVE;
		
		if (mIsOn == false) {
			MyLog.e(LociConfig.D.MD.CALL, TAG, "[MD] [-] MovementHandler (on/off) is already off, ignore.");
			return;
		}
		
		MyLog.e(LociConfig.D.MD.CALL, TAG, "[MD] [-] MovementHandler (on/off)");
		mIsOn = false;

		mAccHandler.stop();
	}

	
	public synchronized boolean isOn() {
		return mIsOn;
	}

	public int getInstantState(long time) {
		
		if (LociConfig.pStaleTime >= Math.abs(time - mInstantStateTime))
			return mInstantState;
		else
			return -1;
	}
	
	private synchronized void setInstantState(long date, int state) {
		mInstantStateTime = date;
		mInstantState = state;
	}
	
	public synchronized void setToCheckStill() {
		mState = MOVEMENT_STATE_MOVE;
		mTimer = 0;
	}
	
	public synchronized void setToCheckMove() {
		mState = MOVEMENT_STATE_STILL;
		mTimer = 0;
	}
	
	private void checkStill(long date, float magnitude) {
		if (mTimer == 0) {
			mTimer = date;
			mCnt = 0;
		}
		
		MyLog.d(LociConfig.D.MD.EVENT, TAG, String.format("[MD] check still ... var=%10.7f", magnitude));
		
		long curTime = date;
		
		if (magnitude <= LociConfig.pVarThreshold) {
			mCnt++;
			setInstantState(date, MOVEMENT_STATE_STILL);
			if ((curTime - mTimer) >= LociConfig.pStillTime) {
				// event
				//LociMobileManagerService.getInstance().eventStill();
				mListener.onStill();
			} else {
				MyLog.d(LociConfig.D.MD.EVENT, TAG, "[MD] not time yet (diff=" + (curTime - mTimer) + ", threshold=" + LociConfig.pStillTime + ")");
			}
		} else {
				mTimer = curTime;
				mCnt = 0;
				setInstantState(date, MOVEMENT_STATE_MOVE);
				//MyLog.e(false, TAG, String.format("checkStill() : moved. reset timer to %s (%d)", DateFormat.getTimeInstance().format(new Date(mTimer)), mTimer));
		}
	}
	
	private void checkMove(long time, float magnitude) {
		
		if (mTimer == 0) {
			mTimer = time;
			mCnt = 0;
		}

		MyLog.d(LociConfig.D.MD.EVENT, TAG, String.format("[MD] check move .... var=%10.7f", magnitude));
		
		long curTime = time;
		
		if (magnitude >= LociConfig.pVarThreshold) {
			mCnt++;
			setInstantState(time, MOVEMENT_STATE_MOVE);
			// confirm moving if the phone consistently moved during the inspection time
			if ((curTime - mTimer) >= LociConfig.pMoveTime) {
				// event
				//LociMobileManagerService.getInstance().eventMove();
				mListener.onMove();
				return;
				
			} else {
				MyLog.d(LociConfig.D.MD.EVENT,TAG, "[MD] not time yet, (diff=" + (curTime - mTimer) + ", threshold=" + LociConfig.pMoveTime + ")");
			}
		} else { // reset the timer if the phone was still 
			mTimer = curTime;
			mCnt = 0;
			setInstantState(time, MOVEMENT_STATE_STILL);
			//MyLog.e(false, TAG, String.format("checkMove() : still. reset timer to %s (%d)", DateFormat.getTimeInstance().format(new Date(mTimer)), mTimer));
		}
		mListener.onStillUpdateExitTime(time);
	}

	public void onAccelerometerChanged(long time, float magnitude) {

		MyLog.d(LociConfig.D.MD.EVENT, TAG, String.format("[MD] onAccelerometerChanged : magnitude=%f", magnitude));
		

		if (mState == MOVEMENT_STATE_MOVE) 
			checkStill(time, magnitude);
		else
			checkMove(time, magnitude);
	}
}
