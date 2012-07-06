package edu.cens.loci.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;
import edu.cens.loci.LociConfig;
import edu.cens.loci.LociTabActivity;
import edu.cens.loci.R;
import edu.cens.loci.components.LociMovementHandler.MovementListener;
import edu.cens.loci.components.LociPathHandler.PathListener;
import edu.cens.loci.components.LociPlaceHandler.PlaceListener;
import edu.cens.loci.ui.SettingsActivity;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class LociManagerService extends Service {

	private static final String TAG = "LociManagerService";
	
	public static final String SERV_NAME = "edu.cens.loci.components.LociManagerService";
	
	
	/**
	 * Implements SensLoc algorithm
	 * 
	 */
	private class SensLocService implements PlaceListener, PathListener, MovementListener {

		public void start() {
			// start PlaceHandler
			//mMovementHandler.start();
			if (mIsPlaceOn)
				mPlaceHandler.start(false, true);
			
			// start PathHandler
			if (mIsPathOn)
				mPathHandler.start();
		}
		
		public void stop() {
			// stop all handlers
			if (mIsPathOn)
				mPathHandler.stop();
			
			if (mIsPlaceOn) {
				mPlaceHandler.stop();
				mMovementHandler.stop();
			}
		}
		
		public void onLocationChanged(Date time, Location loc) {
			// TODO Auto-generated method stub
			
		}

		public void onEnter() {
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on enter. PD (off).");
			Toast.makeText(LociManagerService.this, "Entering a place", Toast.LENGTH_SHORT).show();
		}

		public void onExit() {
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on exit. MD (off), PT (on).");
			Toast.makeText(LociManagerService.this, "Leaving a place", Toast.LENGTH_SHORT).show();		
			
			// turn off MD
			mMovementHandler.stop();
			// turn on PT
			
			if (mIsPathOn) {
				if (!isPTOn())
					mPathHandler.start();
			}
		}

		public void onStill() {
			// if wifiStableLevel is good, pause PD, and set to check-move (MD)
			// else, check still (MD)
			if (mPlaceHandler.isWifiStable()) {
				MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on still. wifi is stable, stop wifi and check for movement. PD (off)");
				mPlaceHandler.pause();
				mMovementHandler.setToCheckMove();
			} else {
				MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on still. wifi is not stable, keep checking for still.");
				mMovementHandler.setToCheckStill();
			}
		}

		public void onMove() {
			// resume PD
			// turn off MD
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on move. PD (on) MD (off).");
			mPlaceHandler.resume();
			mMovementHandler.stop();
		}
		
		// by PT
		public void onFast() {
			// if wifiStableLevel is bad, pause PD, reset PD, stop MD, and set to check-slow (PT)
			
			if (!mIsPlaceOn)
				return;
			
			if (!mPlaceHandler.isWifiStable()) {
				mPlaceHandler.pause();
				mPlaceHandler.reset();
				mMovementHandler.stop();
				MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on fast. pause.reset PD (off), MD (off).");
			} else {
				mPathHandler.setToCheckFast();
				MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on fast. wifi is stable do nothing.");
			}
			
		}

		// by PT
		public void onSlow() {
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on slow. PD (on).");
			// resume PD, and set to check-fast
			
			if (!mIsPlaceOn)
				return;
			
			mPlaceHandler.resume();
		}
		
		public void onStillUpdateExitTime(long exitTime) {
			// update latest visit's exit times, while PD was paused.
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on still update exit time " + MyDateUtils.getTimeFormatLong(exitTime));
			mPlaceHandler.updateCurrentVisitExitTime(exitTime);
		}

		public void onStartMovementDetector() {
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on start movement detector. MD (on).");
			// set to check-still, and start MD
			//Log.d(TAG, "onStartMovementDetector()");
			
			if (mIsPathOn) {
				if (mPathHandler.isOn()) {
					onStopPathHandler();
				}
			}
			
			mMovementHandler.setToCheckStill();
			mMovementHandler.start();
		}

		public void onStopPathHandler() {
			MyLog.i(LociConfig.D.MANAGER.EVENT, TAG, " [manager] on stop path handler PT (off).");
			// stop PD
			//Log.d(TAG, "onStopPathHandler()");
			if (mIsPathOn)
				mPathHandler.stop();
		}
		
		
	}
	
  /**
   * Object used internally for synchronization
   */
  private final Object mLock = new Object();

	private final ILociManager.Stub mServiceBinder = new ILociManager.Stub() {
		
		public void requestLocationUpdates(long minTime, ILociListener listener)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
		
		public void removeUpdates(ILociListener listener) throws RemoteException {
			// TODO Auto-generated method stub
			
		}
		
		public void locationCallbackFinished(ILociListener listener)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}

		public void addPlaceAlert(long placeid, long expiration,
				PendingIntent intent) throws RemoteException {
			
			synchronized (mLock) {

				MyLog.d(LociConfig.D.Service.ALERT, TAG, "addPlaceAlert(): placeid=" + placeid + ", expiration=" + expiration + ", intent=" + intent.toString());
				
				if (expiration != -1)
					expiration += System.currentTimeMillis();
				
				PlaceAlert alert = new PlaceAlert(Binder.getCallingUid(), placeid, expiration, intent);
				mPlaceAlerts.put(intent, alert);
				
				if (mPlaceAlertListener == null)
					mPlaceAlertListener = new PlaceAlertListener();
			}
		}

		public void removePlaceAlert(PendingIntent intent) throws RemoteException {
			// TODO Auto-generated method stub
			synchronized (mLock) {
				
				//Log.d(TAG, "removePlaceAlert(): intent=" + intent.toString());
				
				mPlaceAlerts.remove(intent);
				if (mPlaceAlerts.size() == 0) {
					mPlaceAlertListener = null;
				}
			}
		}

		public boolean isPlaceDetectorOn() throws RemoteException {
			return isPDOn();
		}

		public boolean isPathTrackerOn() throws RemoteException {
			// TODO Auto-generated method stub
			return isPTOn();
		}

		public boolean isMovementDetectorOn() throws RemoteException {
			return isMDOn();
		}		
	};
	
	@Override
	public IBinder onBind(Intent intent) {

		if (ILociManager.class.getName().equals(intent.getAction())) {
			MyLog.i(LociConfig.D.Service.EVENT, TAG, "Service binding succeeded.");
			return mServiceBinder;
		} else {
			MyLog.i(LociConfig.D.Service.EVENT, TAG, "Service binding failed, had different code?");
			MyLog.i(LociConfig.D.Service.EVENT, TAG, " --> intent.action: " + intent.getAction());
		}
		return null;
	}

	public boolean onUnbind(Intent intent) {
		return true;
	}
	
	private Context mContext;
	private boolean mIsRunning = false;
	
	/* notification */
	static final int NOTIFICATION_STATUS = 1;
	private NotificationManager mNotiManager;
	
	// test place alerts
	private Timer mPlaceTestTimer = null;
	
	private LociPlaceHandler 		mPlaceHandler;
	private LociPathHandler 		mPathHandler;
	private LociMovementHandler mMovementHandler;
	
	private static LociManagerService sInstance = null;
	
	private SensLocService mSensLoc;
	
	private boolean mIsPlaceOn = true;
	private boolean mIsPathOn = true;
	
	public static LociManagerService getInstance() {
		return sInstance;
	}
	
	public void onCreate() {
		
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, " [manager] onCreate()");
		
		mContext = this;
		sInstance = this;
		
		// open database connection
		
		// instantiate PlaceHandler, PathHandler, MovementHandler
		//mPlaceHandler 		= new LociPlaceHandler(mContext, this);
		//mPathHandler 			= new LociPathHandler(mContext, this, LociConfig.pGpsMinTime);
		//mMovementHandler 	= new LociMovementHandler(mContext, this, LociConfig.pAccDuration, LociConfig.pAccPeriod);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		mIsPlaceOn = sp.getBoolean(SettingsActivity.KEY_USE_PLACE_SENSING, true);
		mIsPathOn = sp.getBoolean(SettingsActivity.KEY_USE_PATH_TRACKING, true);
		
		//long wifiMinTime = sp.getLong(SettingsActivity.KEY_PLACE_SENSING_RATE, LociConfig.pWifiMinTime);
		
		long placeMinTime = Long.valueOf(sp.getString(SettingsActivity.KEY_PLACE_SENSING_RATE, null));
		long pathMinTime = Long.valueOf(sp.getString(SettingsActivity.KEY_PATH_TRACKING_RATE, null));
		
		//long gpsMinTime = sp.getLong(SettingsActivity.KEY_PATH_TRACKING_RATE, LociConfig.pGpsMinTime);
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, "placeOn=" + mIsPlaceOn + ", pathOn=" + mIsPathOn);
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, String.format("wifiMinTime=%d gpsMinTime=%d", placeMinTime, pathMinTime));
		
		
		mSensLoc = new SensLocService();
		
		if (mIsPlaceOn) {
			mPlaceHandler 		= new LociPlaceHandler(mContext, mSensLoc, placeMinTime);
			mMovementHandler 	= new LociMovementHandler(mContext, mSensLoc, LociConfig.pAccDuration, LociConfig.pAccPeriod);
		}
		
		if (mIsPathOn)
			mPathHandler 			= new LociPathHandler(mContext, mSensLoc, pathMinTime);
		
		
		// prepare upload handler

		// Prepare notification for displaying that the service is running
		mNotiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Create a wake lock
    PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
		
    //Log.d(TAG, "onCreate");
    
		// check running state
		//if (loadServiceOnState() && !mIsRunning)
		_startService();
		//else 
		//MyLog.d(LociConfig.D.Service.DEBUG, TAG, "onCreate: can't start loadServiceOnState()" + loadServiceOnState() + " mIsRunning=" + mIsRunning);
		
	}
	
	private boolean mEvent = false;
	
	private void setPlaceTestTimer() {
		
		MyLog.d(LociConfig.D.Service.DEBUG, TAG, "setPlaceTestTimer()");
		
		mPlaceTestTimer = new Timer("PlaceAlertTestTimer");
		mPlaceTestTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				
				MyLog.d(LociConfig.D.Service.ALERT, TAG, "[AlertTest] Timer fired!");
				
				if (mPlaceAlertListener != null) {
					MyLog.d(LociConfig.D.Service.ALERT, TAG, " --> request exists.");
					mPlaceAlertListener.onPlaceChanged(1, mEvent);
					mEvent = !mEvent;
				} else {
					MyLog.d(LociConfig.D.Service.ALERT, TAG, " --> no request exists,");
				}
			}
		}, 0, 10000);
	}
	
	private void cancelPlaceTestTimer() {
		MyLog.d(LociConfig.D.Service.ALERT, TAG, "cancelPlaceTestTimer()");
		mPlaceTestTimer.cancel();
	}
	
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, " [manager] onStart()");
		//if (loadServiceOnState() && !mIsRunning)
		//	_startService();
		//else 
		//	MyLog.d(LociConfig.D.Service.DEBUG, TAG, "onStart: can't start loadServiceOnState=" + loadServiceOnState() + " mIsRunning=" + mIsRunning);
	}
	
	public int onStartCommend(Intent intent, int flags, int startID) {
		return 1;
	}
	
	public void onDestroy() {
		_stopService();
		//closeDb();
		//unregisterReceiver(mBatteryInfoReceiver);
		
		super.onDestroy();
	}
	
	private void saveServiceOnState(boolean isRunning) {
		SharedPreferences activityPreferences = getSharedPreferences(LociConfig.Preferences.PREFS_NAME, Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = activityPreferences.edit();
		
		editor.putBoolean(LociConfig.Preferences.PREFS_KEY_SERVICE_ON, isRunning);
		editor.commit();
	}
	
	//private boolean loadServiceOnState() {
	//	SharedPreferences activityPreferences = getSharedPreferences(LociConfig.Preferences.PREFS_NAME, Activity.MODE_PRIVATE);
	//	return activityPreferences.getBoolean(LociConfig.Preferences.PREFS_KEY_SERVICE_ON, false);
	//}	
	
	public boolean isPDOn() {
		if (mPlaceHandler != null)
			return mPlaceHandler.isOn();
		else
			return false;
	}
	
	public boolean isPTOn() {
		if (mPathHandler != null)
			return mPathHandler.isOn();
		else 
			return false;
	}
	
	public boolean isMDOn() {
		if (mMovementHandler != null)
			return mMovementHandler.isOn();
		else
			return false;
	}
	
	private synchronized void _startService() {
		
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, " [manager] start service.");
		
		if (mIsRunning) {
			MyLog.i(LociConfig.D.MANAGER.CALL, TAG, " [manager] already running, ignore.");			
			return;
		}
		
		mIsRunning = true;
		saveServiceOnState(mIsRunning);
		
		mSensLoc.start();

		
		// Show notification
		showNotification();
		alarmOn();
		//setPlaceTestTimer();
	}
	
	private void showNotification() {
		Notification noti = new Notification(R.drawable.loci_notification_marker, "Loci Service has started.", System.currentTimeMillis());
		noti.flags |= Notification.FLAG_ONGOING_EVENT;
		//Intent intent = new Intent(this, StatusViewActivity.class);
		Intent intent = new Intent(this, LociTabActivity.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent content = PendingIntent.getActivity(this, 0, intent, 0);
		noti.setLatestEventInfo(this, "Loci service is running.", "Press to check the status in details.", content);
		mNotiManager.notify(NOTIFICATION_STATUS, noti);
	}

	private synchronized void _stopService() {
		
		MyLog.i(LociConfig.D.MANAGER.CALL, TAG, " [manager] stop service.");
		
		//if (!mIsRunning)
		//	return;
		
		mIsRunning = false;
		saveServiceOnState(mIsRunning);
		
		mSensLoc.stop();
		
		// Hide notification
		mNotiManager.cancel(NOTIFICATION_STATUS);
		alarmOff();	
		//cancelPlaceTestTimer();
	}
	
	
	
	/**
	 *  Handling place alert requests
	 */
	
  private HashMap<PendingIntent,PlaceAlert> mPlaceAlerts =
    new HashMap<PendingIntent,PlaceAlert>();
  
	class PlaceAlert {
		final int mUid;
		final long mPlaceId;
		final long mExpiration;
		final PendingIntent mIntent;
		
		public PlaceAlert(int uid, long placeId, long expiration, PendingIntent intent) {
			mUid = uid;
			mPlaceId = placeId;
			mExpiration = expiration;
			mIntent = intent;
		}
		
		PendingIntent getIntent() {
			return mIntent;
		}
		
		long getExpiration() {
			return mExpiration;
		}
		
		boolean isAt(long placeId) {
			return mPlaceId == placeId;
		}
	}

	
	private final static String WAKELOCK_KEY = "LociManagerService";
  private PowerManager.WakeLock mWakeLock = null;
  private int mPendingBroadcasts;
  
  private PlaceAlertListener mPlaceAlertListener = null;
  
  // Wake locks
  private void incrementPendingBroadcasts() {
      synchronized (mWakeLock) {
          if (mPendingBroadcasts++ == 0) {
              try {
                  mWakeLock.acquire();
                  MyLog.d(LociConfig.D.Service.ALERT, TAG, "Acquired wakelock");
              } catch (Exception e) {
                  // This is to catch a runtime exception thrown when we try to release an
                  // already released lock.
                  MyLog.e(LociConfig.D.Service.ALERT, TAG, "exception in acquireWakeLock()");
              }
          }
      }
  }

  private void decrementPendingBroadcasts() {
      synchronized (mWakeLock) {
          if (--mPendingBroadcasts == 0) {
              try {
                  // Release wake lock
                  if (mWakeLock.isHeld()) {
                      mWakeLock.release();
                      MyLog.d(LociConfig.D.Service.ALERT, TAG, "Released wakelock");
                  } else {
                     	MyLog.d(LociConfig.D.Service.ALERT, TAG, "Can't release wakelock again!");
                  }
              } catch (Exception e) {
                  // This is to catch a runtime exception thrown when we try to release an
                  // already released lock.
                  MyLog.e(LociConfig.D.Service.ALERT, TAG, "exception in releaseWakeLock()");
              }
          }
      }
  }
	
	class PlaceAlertListener implements PendingIntent.OnFinished {

		public void onSendFinished(PendingIntent pendingIntent, Intent intent,
				int resultCode, String resultData, Bundle resultExtras) {
	      // synchronize to ensure incrementPendingBroadcasts()
	      // is called before decrementPendingBroadcasts()
	      synchronized (this) {
	          decrementPendingBroadcasts();
	      }
		}

		public void onPlaceChanged(long placeId, boolean event) {

			
			MyLog.d(LociConfig.D.Service.ALERT, TAG, "onPlaceChanged() : placeId=" + placeId + ", event=" + event);
			
			long now = System.currentTimeMillis();
			
			ArrayList<PendingIntent> intentsToRemove = null;
			
			for (PlaceAlert alert : mPlaceAlerts.values()) {
				PendingIntent intent = alert.getIntent();
				long expiration = alert.getExpiration();
				
				//Log.d(TAG, "intent=" + intent.toString() + ", expiration=" + expiration);
				
				if ((expiration == -1) || (now <= expiration)) {
					boolean atPlace = alert.isAt(placeId);
					
					if (atPlace && event) {
						// entered place
						Intent enteredIntent = new Intent();
						enteredIntent.putExtra(LociManager.KEY_EVENT_ENTERING, true);
						try {
							synchronized (this) {
								intent.send(mContext, 0, enteredIntent, this, null);
								incrementPendingBroadcasts();
								//Log.d(TAG, "broadcast sent. (enter)");
							}
						} catch (PendingIntent.CanceledException e) {
							//Log.e(TAG, "PendingIntent.CanceledException.");
							if (intentsToRemove == null)
								intentsToRemove = new ArrayList<PendingIntent>();
							intentsToRemove.add(intent);
						}
					} else if (atPlace && !event) {
						// exited place
						Intent exitedIntent = new Intent();
						exitedIntent.putExtra(LociManager.KEY_EVENT_ENTERING, false);
						try {
							synchronized (this) {
								intent.send(mContext, 0, exitedIntent, this, null);
								incrementPendingBroadcasts();
								//Log.d(TAG, "broadcast sent. (exit)");
							}
						} catch (PendingIntent.CanceledException e) {
							//Log.e(TAG, "PendingIntent.CanceledException.");
							if (intentsToRemove == null) {
								intentsToRemove = new ArrayList<PendingIntent>();
							}
							intentsToRemove.add(intent);
						}
					}
				} else {
					// Mark alert for expiration
					if (intentsToRemove == null) {
						intentsToRemove = new ArrayList<PendingIntent>();
					}
					intentsToRemove.add(alert.getIntent());
				}
			}
			
      // Remove expired alerts
      if (intentsToRemove != null) {
          for (PendingIntent i : intentsToRemove) {
              mPlaceAlerts.remove(i);
              if (mPlaceAlerts.size() == 0)
              	mPlaceAlertListener = null;
          }
      }
		}
	}
	
	private AlarmManager mAlarm;
	private PendingIntent mPending;
	
	private synchronized void alarmOn() {
	
		if (mPending == null) {
			Intent alarmIntent = new Intent(LociManagerService.this, WatchDogAlarmReceiver.class);
			mPending = PendingIntent.getBroadcast(LociManagerService.this, 0, alarmIntent, 0);
		}
		if (mAlarm == null) {
			mAlarm = (AlarmManager) getSystemService(ALARM_SERVICE);
		}
		
		long firstTime = SystemClock.elapsedRealtime() + LociConfig.pWatchDog;
		mAlarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, LociConfig.pWatchDog, mPending);
	}
	
	private synchronized void alarmOff() {
		
		if (mAlarm != null && mPending != null)
			mAlarm.cancel(mPending);
		else
			MyLog.e(LociConfig.D.Watchdog.DEBUG, TAG," alarmOff() : mAlarm is null, do nothing.");
	}



}
