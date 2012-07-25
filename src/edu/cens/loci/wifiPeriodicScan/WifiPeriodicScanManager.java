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

import java.util.HashMap;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class WifiPeriodicScanManager {
	private static final boolean DEBUG = false;
	private static final String TAG = "WifiPeriodicScanManager";
	private IWifiPeriodicScanManager mService;
	private HashMap<WifiScanListener,ListenerTransport> mListeners = 
		new HashMap<WifiScanListener, ListenerTransport>();
	
	private class ListenerTransport extends IWifiScanListener.Stub {

		private static final int TYPE_WIFI_UPDATE = 1;
		
		private WifiScanListener mListener;
		private final Handler mListenerHandler;
		
		ListenerTransport(WifiScanListener listener, Looper looper) {
			
			if (DEBUG) Log.e(TAG, "ListenerTransport()");
			
			mListener = listener;
			
			if (listener != null)
				if (DEBUG) Log.d(TAG, "listener:" + listener.toString());
			else
				if (DEBUG) Log.d(TAG, "listener: null");
			
			if (looper == null) {
				mListenerHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						_handleMessage(msg);
					}
				};
			} else {
				
				if (DEBUG) Log.d(TAG, "looper:  " + looper.toString());
				
				mListenerHandler = new Handler(looper) {
					@Override
					public void handleMessage(Message msg) {
						_handleMessage(msg);
					}
				};
			}
		}
		
		@SuppressWarnings("unchecked")
		public void onWifiUpdated(List test) throws RemoteException {
			if (DEBUG) Log.d(TAG, "onWifiUpdated():" + test.toString());
			Message msg = Message.obtain();
			msg.what = TYPE_WIFI_UPDATE;
			msg.obj = test;
			mListenerHandler.sendMessage(msg);
		}

		@SuppressWarnings("unchecked")
		private void _handleMessage(Message msg) {
			switch(msg.what) {
				case TYPE_WIFI_UPDATE:
					//if (DEBUG) Log.d(TAG, "_handleMessage(): msg=" + msg.toString());
					List test = (List) msg.obj;
					//if (DEBUG) Log.d(TAG, "_handleMessage():" + test.toString());
					mListener.onWifiUpdated(test);
					break;
			}
		}
		
	}
	
	public WifiPeriodicScanManager(IWifiPeriodicScanManager service) {	
		mService = service;
	}
	

	public void requestWifiUpdates(long minTime, WifiScanListener listener) {
		_requestWifiUpdates(minTime, listener, null);
	}
	
	private void _requestWifiUpdates(long minTime, WifiScanListener listener, Looper looper) {
		
		Log.v(TAG, "_requestWifiUpdates received listener");
		
		if (minTime < 0L) {
			minTime = 0L;
		}
		
		try {
			synchronized (mListeners) {
				ListenerTransport transport = mListeners.get(listener);
				if (transport == null) {
					transport = new ListenerTransport(listener, looper);
				}
				mListeners.put(listener, transport);
				mService.requestWifiUpdates(minTime, transport);
			}
		} catch (RemoteException ex) {
			if (DEBUG) Log.e(TAG, "requestWifiUpdates: DeadObjectException", ex);
		}
	}
	
	public void removeWifiUpdates(long minTime, WifiScanListener listener) {
		_removeWifiUpdates(minTime, listener);
	}
	
	private void _removeWifiUpdates(long minTime, WifiScanListener listener) {
		if (listener == null) {
			if (DEBUG) Log.d(TAG, "removeWifiUpdates(): listener == null");
			throw new IllegalArgumentException("listener==null");
		}
		try {
			synchronized (mListeners) {
				ListenerTransport transport = mListeners.remove(listener);
				if (transport != null) {
					mService.removeWifiUpdates(minTime, transport);
				}
			}
		} catch (RemoteException ex) {
			if (DEBUG) Log.e(TAG, "removeUpdates: DeadObjectException", ex);
		}
	}
}
