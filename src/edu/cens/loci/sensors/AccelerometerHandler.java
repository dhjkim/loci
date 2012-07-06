package edu.cens.loci.sensors;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.math.stat.descriptive.moment.Variance;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;

public class AccelerometerHandler implements SensorEventListener {

	private static final String TAG = "AccelerometerHandler";
	
	private PowerManager.WakeLock mCpuLock;

	public interface AccelerometerListener {
		public void onAccelerometerChanged(long time, float magnitude);
	}
	
	private Context mCxt;
	private static AccelerometerHandler sInstance;
	public AccelerometerHandler getInstance() {
		return sInstance;
	}
	
	private SensorManager mSensorService;
	
	private AccelerometerListener mListener;
	private int mDuration;
	private int mPeriod;
	private int mRate;
	
	private SensorFloatArray mMagArr;
	//private SensorFloatArray mXArr;
	//private SensorFloatArray mYArr;
	//private SensorFloatArray mZArr;
	
	private Timer mOnTimer = null;
	private Timer mOffTimer = null;
	private Timer mReportTimer = null;
	private boolean mSensorOn = false;
	private boolean mIsOn = false;
	
	private PowerManager cpu() {
		return (PowerManager) mCxt.getSystemService(Context.POWER_SERVICE);
	}
	
	public AccelerometerHandler(Context context, AccelerometerListener listener, int duration, int period, int rate) {
		
		if (duration > period) 
			period = duration;
		
		mCxt = context;
		mListener = listener;
		mDuration = duration;
		mPeriod = period;
		mRate = rate;
		
		mMagArr = new SensorFloatArray(100);
		//mXArr = new SensorFloatArray(100);
		//mYArr = new SensorFloatArray(100);
		//mZArr = new SensorFloatArray(100);
		
		sInstance = this;
	}
	
	public void start() {
		
		if (mIsOn)
			return;
		
		mCpuLock = cpu().newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AccelerometerHandler.CpuLock");
		mCpuLock.setReferenceCounted(false);
		
		mIsOn = true;
		
		if (mDuration > 0) {
			//mCpuLock.acquire();
			mOnTimer = new Timer("sensorTurnOnTimer");
			mOffTimer = new Timer("sensorTurnOffTimer");
			
			mOnTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					mSensorService = (SensorManager) mCxt.getSystemService(Context.SENSOR_SERVICE);
					
					synchronized(this) {
						MyLog.i(LociConfig.D.ACC, TAG, "[ACC] ON ");
						mCpuLock.acquire();
						mSensorOn = true;
						mSensorService.registerListener(sInstance, mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mRate);
					}
				}
			}, 0, mPeriod);
		
			mOffTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					
					synchronized(this) {
						MyLog.i(LociConfig.D.ACC, TAG, "[ACC] OFF ");
						mSensorOn = false;
						if(mCpuLock.isHeld()) mCpuLock.release();
						mSensorService.unregisterListener(sInstance, mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
					}
					// time to turn off sensor -- somehow send data

					//mXArr.reset();
					//mYArr.reset();
					//mZArr.reset();
					long time = Calendar.getInstance().getTime().getTime();
					float magVar = getMagnitudeVariance();
					//LociMobileManagerService.getInstance().eventAccelerometerResult(Calendar.getInstance().getTime().getTime(), magVar);
					
					mListener.onAccelerometerChanged(time, magVar);
					
					//if (LociMobileManagerService.getInstance().isMDOn()) 
					//	LociMobileManagerService.getInstance().getSystemDb().updatePowerRow(SystemLogDbAdapter.SENSOR_ACC, System.currentTimeMillis());
				}
			}, mDuration, mPeriod);
		
		} else {
			
			mCpuLock.acquire();
			mSensorService = (SensorManager) mCxt.getSystemService(Context.SENSOR_SERVICE);
			mSensorService.registerListener(this, mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mRate);
			
			mReportTimer = new Timer("SensorReportTimer");
			mReportTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
						// send data
					long time = Calendar.getInstance().getTime().getTime();
					float magVar = getMagnitudeVariance();
					mListener.onAccelerometerChanged(time, magVar);
					//LociMobileManagerService.getInstance().eventAccelerometerResult(Calendar.getInstance().getTime().getTime(), magVar);
					//if (LociMobileManagerService.getInstance().isMDOn()) 
					//	LociMobileManagerService.getInstance().getSystemDb().updatePowerRow(SystemLogDbAdapter.SENSOR_ACC, System.currentTimeMillis());
				}
			}, 0, mDuration);
		}
		
		//LociMobileManagerService.getInstance().getSystemDb().addPowerRow(SystemLogDbAdapter.SENSOR_ACC, System.currentTimeMillis());
		startChkTimer();
	}
	
	public boolean isOn() {
		return mIsOn;
	}
	
	public void stop() {
		
		if(!mIsOn)
			return;
		
		mIsOn = false;
		
		if (mDuration > 0) {
			mOnTimer.cancel();
			mOffTimer.cancel();
			
			if (mSensorOn) {
				mSensorOn = false;
				mSensorService.unregisterListener(this, mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			}
			if (mCpuLock.isHeld())
				mCpuLock.release();
		}
		else {
			if (mSensorOn) {
				mSensorOn = false;
				mSensorService.unregisterListener(this, mSensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			}
			mReportTimer.cancel();
			
			if (mCpuLock.isHeld())
				mCpuLock.release();
		}
		
		stopChkTimer();
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	public void onSensorChanged(SensorEvent event) {
		if (mSensorOn && mIsOn)
			onSensorChangedLocked(event);
		else
			MyLog.d(LociConfig.D.ACC, TAG, "[ACC] onSensorChagned() : We turned off the sensor, ignore incoming data.");
	}
	
	private synchronized void onSensorChangedLocked(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
			mFlag = true;

			if (mIsOn)
				mMagArr.addMagnitude(event.values[SensorManager.DATA_X], event.values[SensorManager.DATA_Y], event.values[SensorManager.DATA_Z], event.accuracy);
			
			//mXArr.add(event.values[SensorManager.DATA_X]);
			//mYArr.add(event.values[SensorManager.DATA_Y]);
			//mZArr.add(event.values[SensorManager.DATA_Z]);
		}
	}

	private synchronized float getMagnitudeVariance() {
		float magVar = mMagArr.var();
		mMagArr.reset();
		return magVar;
	}
	
	public class SensorFloatArray {
		private double[] mArr = null;
		private int mCnt;
		private int mMaxCnt;
		
		public SensorFloatArray(int maxCnt) {
			mMaxCnt = maxCnt;
			mArr = new double[mMaxCnt];
			mCnt = 0;
		}
		
		public void reset() {
			mCnt = 0;
		}
		
		public void add(float val) {
			if (mMaxCnt <= mCnt) {
				//System.out.println("add [reject] mCnt=" + mCnt);
				return;
			}
			mArr[mCnt++] = val;
		}

		public void addMagnitude(float x, float y, float z, int accuracy) {
			if (mMaxCnt <= mCnt) {
				MyLog.w(LociConfig.D.ACC, TAG, "[ACC] Accelerometer data buffer is full! ignore data. mCnt=" + mCnt);
				return;
			}
			
			if (x == 0 || y == 0 || z == 0) {
				MyLog.i(LociConfig.D.ACC, TAG, String.format("[ACC] Handling outlier... x=%15.12f y=%15.12f z=%15.12f", x, y, z));
				return;
			}
			
			float mag = (float) Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2));
			mArr[mCnt++] = mag;
			
			if (LociConfig.D.ACC) {
				String debugMsg = String.format("[ACC] x=%17.13f  y=%17.13f  z=%17.13f  mag=%15.14f acc=%d", x, y, z, mag, accuracy);
				MyLog.d(LociConfig.D.ACC, TAG, debugMsg);
			}
		}
		
		public float var() {
		
			double [] valArr = new double[mCnt];

			
			if (mCnt == 0)
				return 0;
			
			for (int i=0; i<mCnt; i++) {
				valArr[i] = mArr[i];
			}
			
			Variance var = new Variance();
				
			double varDouble = var.evaluate(valArr);
			float varFloat = (float) varDouble;
			
			//Log.d(TAG, "var (double) = " + varDouble + " var (float) = " + varFloat);
			
			return varFloat;
		}
	}
	
	private static String TIMER_CHECK_ACC = "timer_check_acc";
	private static String TIMER_RESTART_ACC = "timer_restart_acc";
	
	private Timer mChkTimer;
	private boolean mFlag = false;
	
	private void startChkTimer() {
		mFlag = false;
		mChkTimer = new Timer(TIMER_CHECK_ACC);
		mChkTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (isOn() && !mFlag) {
					MyLog.e(LociConfig.D.SYS, TAG, "[ACC] (check timer) Movement Detector is not receiving Accel data. restart handler.");
					
					if (isOn())
						stop();
					
					Timer restartTimer = new Timer(TIMER_RESTART_ACC);
					restartTimer.schedule(new TimerTask() {
						public void run() {
							MyLog.e(LociConfig.D.SYS, TAG, "[ACC] (check timer) Restart Accelerometer.");
							start();
						}
					}, 500);
					
				} else {
					MyLog.e(LociConfig.D.SYS, TAG, "[ACC] (check timer) Movement Detector OK.");
				}
				mFlag = false;
			}
		}, LociConfig.pAccWatch, LociConfig.pAccWatch);
	}
	
	private void stopChkTimer() {
		mChkTimer.cancel();
		mFlag = false;
	}
}
