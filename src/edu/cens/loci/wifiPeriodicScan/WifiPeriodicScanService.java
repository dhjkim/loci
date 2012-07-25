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
package edu.cens.loci.wifiPeriodicScan;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

public class WifiPeriodicScanService extends Service {

	private boolean mWifiAlwaysOn = true;
	private static final boolean DEBUG = false;
	
	// Sample rate -> List of Listeners (can have multiple listeners with same sample rate) 
	private Hashtable<Long,ArrayList<IWifiScanListener>> mListeners = new Hashtable<Long,ArrayList<IWifiScanListener>>();
	// Sample rate -> Next wakeup time for samples
	private Hashtable<Long,Long> mListenersNextWakeup = new Hashtable<Long,Long>();
	
	private final IWifiPeriodicScanManager.Stub mBinder = new IWifiPeriodicScanManager.Stub() {
		
		// get's listeners (for the broadcast) from here
		public void requestWifiUpdates(long minTime, IWifiScanListener listener) throws RemoteException {
			if (DEBUG) Log.i(TAG, "requestWifiUpdates(): received a new listener.");
			// save the listeners and what they want.
			// using this listener we can return them results synchronously.
			// Activity may start the service after successfully 
			//listener.onWifiUpdated("Hey, It's me service, I got something.");
				
			_requestWifiUpdates(minTime, listener);
		}
		
		private synchronized void _requestWifiUpdates(long minTime, IWifiScanListener listener) {
			// Save the new listener	
			Long key = new Long(minTime); // listener's sample rate
			ArrayList<IWifiScanListener> listeners = mListeners.get(key); // one sample rate can have multiple listeners
			// add the new listener to "mListeners"
			if (listeners == null) {
				listeners = new ArrayList<IWifiScanListener>();
			}
			listeners.add(listener);
			mListeners.put(key, listeners);
			
			long currTime = SystemClock.elapsedRealtime();

			// we only provide seconds-level granularity (milliseconds -> cut off)
			minTime = (minTime/1000) * 1000;
			currTime = (currTime/1000) * 1000;
			
			// Calculate the best (soonest) "next wake up" time for this new listener
			// op1) the new listener is the only one, so use it's own sample rate.
			if (mListenersNextWakeup.isEmpty()) {
				
				long nextTime = currTime + minTime;
				mListenersNextWakeup.put(key, new Long(nextTime));
				mNextWakeupTime = nextTime;
			
				if (DEBUG) Log.d(TAG, "_requestWifiUpdates(): op1. very first listener. delaying scan in.." + (mNextWakeupTime - currTime) + "milliseconds.");
				
				// if timer is on, turn it off and reset it.
				if (mScanTimerOutstanding.compareAndSet(false, true)) {
					if (DEBUG) Log.d(TAG, "_requestWifiUpdates(): no job was posted. set a new job.");
					mPeriodicScanHandler.postDelayed(mRunWifiScan, (mNextWakeupTime - SystemClock.elapsedRealtime()));
				} else {
					if (DEBUG) Log.d(TAG, "_requestWifiUpdates(): cancel previous job and set a new job.");
					mPeriodicScanHandler.removeCallbacks(mRunWifiScan);
					mScanTimerOutstanding.set(true);
					//mNextWakeupTime = currTime + minTime;
					mPeriodicScanHandler.postDelayed(mRunWifiScan, (mNextWakeupTime - SystemClock.elapsedRealtime()));
				}
			// op2) the new listener's sample rate is already in use. just free ride.
			} else if (mListenersNextWakeup.containsKey(key)) {
				if (DEBUG) Log.d(TAG, "requestWifiUpdates: requested for sample rate that is already registered. no work.");
				
			// op3) synchronize with others
			} else {
				// find the next scheduled wake up time for existing listeners
				Long minKey = findOneMinFromHashtable(mListenersNextWakeup);
				if (minKey == null) {
					if (DEBUG) Log.d(TAG, "_requestWifiUpdates: error, hashtable is empty but called findMin,");
					return;
				} else {
					Long minValue = (Long) mListenersNextWakeup.get(minKey);
					
					if (((minValue.longValue() - currTime) % minTime) == 0 ) {
						// wake up at my sample rate
						if (DEBUG) Log.d(TAG, "_requestWifiUpdates(): there is a previous job but use my sample rate.");
						long nextTime = currTime + minTime;
						mListenersNextWakeup.put(key, new Long(nextTime));
						mNextWakeupTime = nextTime;
						
						if (mScanTimerOutstanding.compareAndSet(false, true)) {
							mPeriodicScanHandler.postDelayed(mRunWifiScan, (mNextWakeupTime - SystemClock.elapsedRealtime()));
						} else {
							mPeriodicScanHandler.removeCallbacks(mRunWifiScan);
							mScanTimerOutstanding.set(true);
							mPeriodicScanHandler.postDelayed(mRunWifiScan, (mNextWakeupTime - SystemClock.elapsedRealtime()));
						}
						
					} else {
						if (DEBUG) Log.d(TAG, "_requestWifiUpdates(): use the current wait time.");
						// just wake up when it was planned to.
						if (minValue != mNextWakeupTime) {
							if (DEBUG) Log.e(TAG, "minValue != mNextWakeupTime... something might not going right.");
						}
						// wake up at the min wake up time
						mListenersNextWakeup.put(key, mNextWakeupTime);
					}
			
				}
			}
			if (DEBUG) Log.d(TAG, "mListeners:" + mListeners.toString());
			if (DEBUG) Log.d(TAG, "mListenersNextWakeup:" + mListenersNextWakeup.toString());
		}
		
		/*
		 * Service can be unregistered. Remove registered listener from the table. 
		 * @see edu.ucla.cens.android.iwas.IWifiPeriodicScanManager#removeWifiUpdates(long, edu.ucla.cens.android.iwas.IWifiScanListener)
		 * @minTime sample rate
		 * @param listener 
		 */
		public void removeWifiUpdates(long minTime, IWifiScanListener listener) {
			_removeWifiUpdates(minTime, listener);
		}
		
		public synchronized void _removeWifiUpdates(long minTime, IWifiScanListener listener) {
			// ask service users to provide a hint to find the listener
			if (DEBUG) Log.i(TAG, "REMOVEWIFIUPDATES : sampleRate=" + minTime);
			Long key = new Long(minTime); // use sample rate as the key
			ArrayList<IWifiScanListener> listeners = mListeners.get(key);
			
			if (listeners == null) {
				if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES : already no more listeners for sampleRate=" + minTime);
				// hmm... its not there, oh well just clean up "mListenersNextWakeup"
				if (mListenersNextWakeup.containsKey(key)) {
					mListenersNextWakeup.remove(key);
				}
				if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES: mListenersNextWakeup=" + mListenersNextWakeup.toString());
				return;
			}
			if (DEBUG) Log.e(TAG, "REMOVEWIFIUPDATES: " + listeners.toString());
			if (DEBUG) Log.e(TAG, "REMOVEWIFIUPDATES: " + listener.toString());
			// each sample rate has a list of listeners, remove from the list.
			listeners.remove(listener);
			if (DEBUG) Log.e(TAG, listeners.toString());
			
			// if no listener is on the given sample rate, remove that sample rate from wake up schedules
			if (listeners.isEmpty()) {
				if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES : [rate=" + key + "] no more listeners.");
				if (mListenersNextWakeup.containsKey(key)) {
					if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES : no more listeners for this rate, remove this rate from mListenersNextWakeup.");
					mListenersNextWakeup.remove(key);
				} else {
					if (DEBUG) Log.d(TAG, "   mListenersNW does not have rate=" + key + " contains=" + mListenersNextWakeup.toString());
				}
				mListeners.remove(key);
			}	else {
				if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES : [rate=" + key + "] removed one listener but there are more for this rate.");
				mListeners.put(key, listeners);
			}
			if (DEBUG) Log.d(TAG, "    mListeners:" + mListeners.toString());
			if (DEBUG) Log.d(TAG, "    mListenersNextWakeup:" + mListenersNextWakeup.toString());
			
			if (mListeners.isEmpty()) {
				if (DEBUG) Log.d(TAG, "REMOVEWIFIUPDATES: removecallbacks");
				mPeriodicScanHandler.removeCallbacks(mRunWifiScan);		
				mScanTimerOutstanding.set(false);
			}
		}
	};
	
	//public static final int WIFI_PERIODICSCAN_DISABLED = 0;
	//public static final int WIFI_PERIODICSCAN_ENABLED = 1;
	
	public static final String TAG = "WifiPeriodicScanService";
	
	private WifiManager.WifiLock mWifiLock;
	private PowerManager.WakeLock mCpuLock;
	
	private AtomicBoolean mIsScanning = new AtomicBoolean(false);
	private AtomicBoolean mScanTimerOutstanding = new AtomicBoolean(false);
	private IntentFilter mScanResultIntentFilter = new IntentFilter();
	
  private final Handler mPeriodicScanHandler = new Handler();
	private long mNextWakeupTime = 0;
	
	private WifiManager wifi() {
		return (WifiManager) getSystemService(Context.WIFI_SERVICE);
	}
	private PowerManager cpu() {
		return (PowerManager) getSystemService(Context.POWER_SERVICE);
	}
	
	private void _turnOnWifi() {
		
		if (DEBUG) Log.e(TAG, "turnOnWifi()");
		// turn on the Wifi module
			//if (!wifi().isWifiEnabled()) {			
			if (wifi().setWifiEnabled(true)) {
				if (DEBUG) Log.d(TAG, "_turnOnWifi() : setWifiEnable() success.");
				if (DEBUG) Log.d(TAG, "_turnOnWifi() : starting a while loop to wait until wifi is on...");
				while (!wifi().isWifiEnabled()) {;}

				if (DEBUG) Log.d(TAG, "_turnOnWifi: wifi is enabled.");
				mWifiLock.acquire();
			}
			else {
				if (DEBUG) Log.d(TAG, "_turnOnWifi() : setWifiEnable() failed.");
			}
			//}	
	}
	
	private void _turnOffWifi() {
		
		if (DEBUG) Log.e(TAG, "turnOffWifi()");
		mWifiLock.release();	
		//wifi().setWifiEnabled(false);
	}
	
	public IBinder onBind(Intent intent) {
		mWifiAlwaysOn = intent.getBooleanExtra("isAlwaysOn", true);
		if (DEBUG) Log.i(TAG, "onBind() Wifi:" + mWifiAlwaysOn);
		return mBinder;
	}
	
	public boolean onUnbind(Intent intent) {
		boolean result = super.onUnbind(intent);
		if (DEBUG) Log.i(TAG, "onUnbind() Wifi");
		
		if (mListeners.isEmpty()) {
			if (DEBUG) Log.i(TAG, "UNBIND: no outstanding requests -> stop service");
			this.stopSelf();
		}	
		return result;
	}
	
	public void onCreate() {
		if (DEBUG) Log.i(TAG, "onCreate() : WifiPeriodicService created.");
		super.onCreate();
		
		// create Locks for Wifi and CPU
		mWifiLock = wifi().createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "WifiTraceService.WifiLock");
		mWifiLock.setReferenceCounted(false);
		mCpuLock = cpu().newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WifiTraceService.CpuLock");

		// this service listens to Wifi scan result action
		mScanResultIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		// Register a receiver that receives wifi scan results
		registerReceiver(onScanResult, mScanResultIntentFilter);
			
		if (mWifiAlwaysOn)
			_turnOnWifi();
		// hold on to the cpu and wifi
		mCpuLock.acquire();
	}
	
	public void onStart(Intent intent, int startId) {
		if (DEBUG) Log.i(TAG,"onStart()");
		super.onStart(intent, startId);
		//startScanning(); // start scanning wifi periodically
	}
	
	public void onDestroy() {
		if (DEBUG) Log.i(TAG, "onDestroy()");
		stopScanning();
		super.onDestroy();
	}
	
	private void stopScanning() {
		if (DEBUG) Log.d(TAG, "stopScanning");
		/*
		if (!mIsScanning.compareAndSet(true, false)) {
			if (DEBUG) Log.d(TAG, "Scanning is already stopped.");
			return;
		}
		*/
		unregisterReceiver(onScanResult);
		mCpuLock.release();
		//wifi().setWifiEnabled(false);
		// release all the locks
		//mWifiLock.release();
		this._turnOffWifi();
	}
	
	/**
	 * Periodically Waken and Scans Wifi
	 */
	private Runnable mRunWifiScan = new Runnable() {
		public void run() {
			
			//long currTime = SystemClock.elapsedRealtime();		
			//currTime = (currTime/1000) * 1000;
			//if (DEBUG) Log.d(TAG, "mPeriodicHandler: currTime=" + currTime + ", mNextWakeupTime=" + mNextWakeupTime);
			if (mScanTimerOutstanding.get() == false) {
				if (DEBUG) Log.d(TAG, "SCHEDSCAN: [Cancel] timer fired, but no outstanding request.");
				return;
			}
			
			// turn on wifi
			if (!mWifiAlwaysOn)
				_turnOnWifi();
			
			mScanTimerOutstanding.set(false); // timer fired.
			//if (DEBUG) Log.d(TAG, "   Starting wifi scan.");
			if (wifi().startScan()) {
				if (DEBUG) Log.d(TAG, "SCHEDSCAN: [Ok] ");
				mIsScanning.set(true);
			}
			else {
				if (DEBUG) Log.d(TAG, "SCHEDSCAN: [Fail] Retry in 0.5 seconds..");
				if (DEBUG) Log.d(TAG, "           is wifi on? " + wifi().isWifiEnabled());
				
				if (wifi().isWifiEnabled() == false)
					_turnOnWifi();
				
				mIsScanning.set(false);
				if (mScanTimerOutstanding.compareAndSet(false, true)) {
					// retry in 0.5 seconds later
					mPeriodicScanHandler.postDelayed(mRunWifiScan, 500);
				}
			}
		}
	};
	
	/*
	 * Receives and handles when Wifi Scan results are available.
	 * 
	 */
	private final BroadcastReceiver onScanResult = new BroadcastReceiver() {
		
		public void onReceive(Context context, Intent intent) {
			
			if (DEBUG) Log.i(TAG, "onScanResult: Received a Wifi Braodcast");
			
			if (!intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				if (DEBUG) Log.d(TAG, "onScanResult (Wifi): Received results, but not the right action. Do nothing and return.");
				return;
			}
			
			// When I was expecting wifi scan results
			if (mIsScanning.compareAndSet(true, false)) {
				mIsScanning.set(false);
				//listener.onWifiUpdated("Hey, It's me service, I got something.");
				//if (DEBUG) Log.d(TAG, "onScanResult(): currTime=" + SystemClock.elapsedRealtime() + ", mNextWakeupTime=" + mNextWakeupTime);
				// pass the results to listeners
				List<ScanResult> scanResults = wifi().getScanResults();
				
				// TURN OFF
				if (!mWifiAlwaysOn)
				_turnOffWifi();
				
				// SampleRateSets that were waiting on this result
				ArrayList<Long> sampleRateSets = findMinFromHashtable(mListenersNextWakeup, mNextWakeupTime);
				
				//if (DEBUG) Log.d(TAG, scanResults.toString());
				//if (DEBUG) Log.d(TAG, "Number of beacons found: " + scanResults.size());
				
				if (sampleRateSets != null) {
					// Every Listener within that SampleRateSet
					for (Long key : sampleRateSets) {
						// Store the new listener
						ArrayList<IWifiScanListener> listeners = mListeners.get(key);
						if (listeners == null) {
							if (DEBUG) Log.d(TAG, "onScanResult (Wifi): No listener exists for key=" + key.toString());
							continue;
							//listeners = new ArrayList<IWifiScanListener>();
						}
						for (IWifiScanListener listener : listeners) {
							try {
								//if (DEBUG) Log.e(TAG, "scanResult_list_length=" + scanResults.size());
								//if (DEBUG) Log.d(TAG, "   ->" + scanResults.toString());
								//if (DEBUG) Log.d(TAG, "SENT TO:"+listener.toString());
								listener.onWifiUpdated(scanResults);
							} catch (RemoteException re) {
								// TODO Auto-generated catch block
								if (DEBUG) Log.e(TAG, "onScanResult (Wifi): Failed replying to listener " + listener.toString(), re);
							}
						}
					}
				}
				// calculate who's next!
				long nextWakeupTime = updateNextWakeupTimes(mListenersNextWakeup);
				//if (DEBUG) Log.d(TAG, "nextWakeupTime=" + nextWakeupTime);
				if (nextWakeupTime != 0) {
					mNextWakeupTime = nextWakeupTime;
				} else {
					if (DEBUG) Log.d(TAG, "[!] nextWakeupTime is somehow 0.");
					return;
				}
				//if (DEBUG) Log.d(TAG, "onScanResult: currTime=" + SystemClock.elapsedRealtime() + ", mNextWakeupTime=" + mNextWakeupTime);
				if (mScanTimerOutstanding.compareAndSet(false, true)) {
					// No outstanding scan timer is scheduled
					//if (DEBUG) Log.d(TAG, "onScanResult: scheduling next scan to " + (mNextWakeupTime - SystemClock.elapsedRealtime()));
					//if (DEBUG) Log.d(TAG, "onScanResult: " + mListenersNextWakeup.toString());
					mPeriodicScanHandler.postDelayed(mRunWifiScan, mNextWakeupTime - SystemClock.elapsedRealtime());
				} else {
					// There is an outstanding scan timer
					mPeriodicScanHandler.removeCallbacks(mRunWifiScan);
					mScanTimerOutstanding.set(true);
					//mNextWakeupTime = mNextWakeupTime;
					mPeriodicScanHandler.postDelayed(mRunWifiScan, mNextWakeupTime - SystemClock.elapsedRealtime());
				}

			} else {
				if (DEBUG) Log.d(TAG, "onScanResult: [Ignore] I wasn't scanning, just return.");
				return;
			}
			
		}
	};
	
	/*
	 * Helper function for calculating the next waking time
	 */
	public synchronized long updateNextWakeupTimes(Hashtable<Long,Long> ht) {
		Enumeration<Long> e = ht.keys();
		
		//if (DEBUG) Log.d(TAG, "updateNextWakeupTimes(): mListeners:" + mListeners.toString());
		//if (DEBUG) Log.d(TAG, "updateNextWakeupTimes(): mListenersNextWakeup:" + mListenersNextWakeup.toString());
		
		long minValue = 0;
		long nextTime = 0;
		
		if (!ht.isEmpty()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			
			if (v.longValue() == mNextWakeupTime) {
				nextTime = v.longValue() + k.longValue();
				ht.put(k, new Long(nextTime));
			} else {
				nextTime = v.longValue();
			}
			
			minValue = nextTime;
			//if (DEBUG) Log.d(TAG, "1)minValue="+minValue);
		} else {
			if (DEBUG) Log.d(TAG, "updateNextWakeupTimes(): hashtable is empty...");
			return 0;
		}
		
		while (e.hasMoreElements()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			
			if (v.longValue() == mNextWakeupTime) {
				nextTime = v.longValue() + k.longValue();
				ht.put(k, new Long(nextTime));
			} else {
				nextTime = v.longValue();
			}
			if (minValue > nextTime) {
				minValue = nextTime;
				//Log.d(TAG, "2)minValue="+minValue);
			}
		}
		return minValue;
	}
	
	/*
	 * Finds the key that has the minimum value from a hash table
	 * @param ht hash table you wish to search
	 * @return one key that has the minimum value. 
	 * if multiple keys have minimum value, only the first one will be returned. 
	 * 
	 */
	public Long findOneMinFromHashtable(Hashtable<Long,Long> ht) {
		Enumeration<Long> e = ht.keys();
		
		Long minKey;
		long minValue;
		
		// handle when hashtable is empty 
		if (!ht.isEmpty()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			minKey = k;
			minValue = v.longValue();
		} else {
			return null;
		}
			
		while(e.hasMoreElements()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			if (minValue > v.longValue()) {
				minKey = k;
				minValue = v.longValue();
			}
		}
		return minKey;
	}
	
	/*
	 * Finds the list of keys that has the minimum value from a hash table
	 * @param ht Hash table you wish to search
	 * @return a list of keys
	 * 
	 */
	public ArrayList<Long> findMinFromHashtable(Hashtable<Long,Long> ht, long expectedNextWaitTime) {
		
		Enumeration<Long> e = ht.keys();
		ArrayList<Long> alist = null;
		Long minKey;
		long minValue;
		
		// boostrap the search 
		if (!ht.isEmpty()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			minKey = k;
			minValue = v.longValue();
		} else {
			// or return null when ht is empty
			if (DEBUG) Log.d(TAG, "findMinFromHashTable(): hashtable is empty!");
			return null;
		}
		
		// search for min!	
		while(e.hasMoreElements()) {
			Long k = (Long) e.nextElement();
			Long v = (Long) ht.get(k);
			if (minValue > v.longValue()) {
				minKey = k;
				minValue = v.longValue();
				if (alist != null) {
					alist = null;
				}
			} else if (minValue == v.longValue()) {
				
				if (alist == null) {
					alist = new ArrayList<Long>();
					alist.add(minKey);
				}
				alist.add(k);
			}
		
		}
		
		if (minValue != expectedNextWaitTime) {
			return null;
		}
		
		if (alist == null) {
			alist = new ArrayList<Long>();
			alist.add(minKey);
		}
		return alist;	
	}
}
