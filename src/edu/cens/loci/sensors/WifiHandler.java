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
package edu.cens.loci.sensors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.os.IBinder;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;
import edu.cens.loci.wifiPeriodicScan.IWifiPeriodicScanManager;
import edu.cens.loci.wifiPeriodicScan.WifiPeriodicScanManager;
import edu.cens.loci.wifiPeriodicScan.WifiPeriodicScanService;
import edu.cens.loci.wifiPeriodicScan.WifiScanListener;

public class WifiHandler implements WifiScanListener {

	private static final String TAG = "WifiHandler";
	
	private Intent mServiceIntent = null;
	private IWifiPeriodicScanManager mIService = null;
	private WifiPeriodicScanManager mService = null;
	private AtomicBoolean mIsConnectedToWifiService = new AtomicBoolean(false);
  private List<ScanResult> mRecentScanList = null;
  private long mRecentScanTime = -1;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] onServiceConnected(): connected to WifiPeriodicService. ready for registering services.");
			mIService = IWifiPeriodicScanManager.Stub.asInterface(binder);
			mService = new WifiPeriodicScanManager(mIService);
   		mIsConnectedToWifiService.set(true);
   		mService.requestWifiUpdates(mMinTime, mInstance);
		}

		// this is called when the connection to with service unexpectedly disconnected.
		public void onServiceDisconnected(ComponentName className) {
			MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] onServiceDisonnected(): disconnected from WifiPeriodicService.");
			// Try to recover from the failure...
			if (mIsConnectedToWifiService.get()) {
				MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] attempting to reconnect WifiPeriodicService");
				if (mCxt.bindService(mServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE)) {		 
					mService.requestWifiUpdates(mMinTime, mInstance);
				} else {
					mIsConnectedToWifiService.set(false);
					MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] Can't restart WifiPeriodicService.. Killing myself..");
					mCxt.stopService(mServiceIntent);
				}
			} else {
				MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] Wifi Scan is turned off. Do not try to reconnect.");
			}
		}
	};

	////////////////////////////////////////////////////////////////////////////////
	
	private boolean mIsOn;
	private Context mCxt;
	private long mMinTime = -1;
	private WifiHandler mInstance;
	
	private static WifiHandler sSingleton = null;
	public static WifiHandler getInstance(Context context) {
		if (sSingleton == null) {
			sSingleton = new WifiHandler(context);
		}
		return sSingleton;
	}
	
	public interface WifiListener {
		public void onWifiChanged(long time, ArrayList<ScanResult> wifiscan);
	}
	
	private WifiHandler(Context context) {
		mCxt = context;
		mInstance = this;
	}
	
	private HashMap<WifiListener, Long> mListeners = new HashMap<WifiListener, Long>();
	
	public void requestUpdates(long minTime, WifiListener listener) {
		mListeners.put(listener, minTime);
		
		if (!mIsOn || mMinTime > minTime)
			start(minTime);
	}

	public void removeUpdates(WifiListener listener) {
		mListeners.remove(listener);
		
		if (mListeners.size() == 0) {
			MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] removeUpdates: size=0, no more registered listeners, stop.");
			stop();
		} else {
			MyLog.d(LociConfig.D.WIFI, TAG, "[WiFi] removeUpdates: has " + mListeners.size() + " more listeners to serve.");
			long minTime = getMinimumTime();
			MyLog.d(LociConfig.D.WIFI, TAG, "   setting new minTime=" + minTime);
			if (minTime > mMinTime)
				start(minTime);
		}
	}
	
	private long getMinimumTime() {
		
		if (mListeners.size() == 0)
			return -1;
		
		Set<WifiListener> keys = mListeners.keySet();
		Iterator<WifiListener> iter = keys.iterator();
		long minTime = Long.MAX_VALUE;
		while (iter.hasNext()) {
			WifiListener listener = iter.next();
			long time = mListeners.get(listener);
			if (time < minTime)
				minTime = time;
		}
		return minTime;
	}
	
	private synchronized boolean start(long minTime) {

		if (mIsOn)
			stop();
		
		mIsOn = true;
		mServiceIntent = new Intent(mCxt, WifiPeriodicScanService.class);
		mServiceIntent.putExtra("isAlwaysOn", true);
		
		mMinTime = minTime;
		
		if (!mCxt.bindService(mServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE)) {
			return false;
		}
		
		//LociMobileManagerService.getInstance().getSystemDb().addPowerRow(SystemLogDbAdapter.SENSOR_WIFI, System.currentTimeMillis());
		
		MyLog.e(LociConfig.D.WIFI, TAG, "[WiFi] start wifi : " + mMinTime);
		
		return true;
	}
	
	private synchronized boolean stop() {
		
		if (!mIsOn)
			return true;
		
		mIsOn = false;
		if (mService != null) {
			mService.removeWifiUpdates(mMinTime, this);
			mCxt.unbindService(mServiceConn);
			mIsConnectedToWifiService.set(false);
		}
		
		mMinTime = -1;
		
		MyLog.e(LociConfig.D.WIFI, TAG, "[WiFi] stop wifi : ");
		
		return true;
	}

	public boolean isOn() {
		return mIsOn;
	}
	
  public List<ScanResult> getRecentScanList() {
  	return mRecentScanList;
  }
  
  public long getRecentScanTime() {
  	return mRecentScanTime;
  }
	
  public int getListenerSize() {
  	return mListeners.size();
  }
	
	@SuppressWarnings("unchecked")
	public void onWifiUpdated(List scanResultList) {
		final long time = Calendar.getInstance().getTime().getTime();
		final ArrayList<ScanResult> wifiscan = (ArrayList) scanResultList;
		
		//LociMobileManagerService.getInstance().eventWifiResult(date, wifiscan);
		//mListener.onWifiChanged(time, wifiscan);
		
		mRecentScanList = scanResultList;
		mRecentScanTime = time;
		
		Set<WifiListener> keys = mListeners.keySet();
		Iterator<WifiListener> iter = keys.iterator();
		
		while (iter.hasNext()) {
			WifiListener listener = iter.next();
			listener.onWifiChanged(time, wifiscan);
		}
		
		//if (LociMobileManagerService.getInstance().isPDOn()) 
		//	LociMobileManagerService.getInstance().getSystemDb().updatePowerRow(SystemLogDbAdapter.SENSOR_WIFI, date);
	}

}
