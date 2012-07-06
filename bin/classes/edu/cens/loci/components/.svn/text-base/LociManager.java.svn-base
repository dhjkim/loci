package edu.cens.loci.components;

import java.util.HashMap;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class LociManager {

	private static final String TAG = "LociManager";
	private ILociManager mService;
	
	public static final String KEY_EVENT_ENTERING = "entering";
	
	private HashMap<LociListener, ListenerTransport> mListeners =
		new HashMap<LociListener, ListenerTransport>();
	
	private class ListenerTransport extends ILociListener.Stub {

		private static final int TYPE_LOCATION_CHANGED = 1;
		
		private LociListener mListener;
		private final Handler mListenerHandler;
		
		ListenerTransport(LociListener listener, Looper looper) {
			mListener = listener;
			
			if (looper == null) {
				mListenerHandler = new Handler() {
						public void handleMessage(Message msg) {
							_handleMessage(msg);
						}
				};
      } else {
        mListenerHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                _handleMessage(msg);
            }
        };
      }
		}
		
		public void onLocationChanged(Location location) throws RemoteException {
			// TODO Auto-generated method stub
      Message msg = Message.obtain();
      msg.what = TYPE_LOCATION_CHANGED;
      msg.obj = location;
      mListenerHandler.sendMessage(msg);
		}
		
    private void _handleMessage(Message msg) {
      switch (msg.what) {
          case TYPE_LOCATION_CHANGED:
              Location location = new Location((Location) msg.obj);
              mListener.onLocationChanged(location);
              break;
      }
      try {
          mService.locationCallbackFinished(this);
      } catch (RemoteException e) {
          Log.e(TAG, "locationCallbackFinished: RemoteException", e);
      }
    }
	}
	
	public LociManager(ILociManager service) {
		mService = service;
	}
	
	public void requestLocationUpdates(long minTime, LociListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener==null");
		}
		_requestLocationUpdates(minTime, listener, null);
	}
	
	private void _requestLocationUpdates(long minTime, LociListener listener, Looper looper) {
		if (minTime < 0L)
			minTime = 0L;
		
		try {
			synchronized (mListeners) {
				ListenerTransport transport = mListeners.get(listener);
				if (transport == null) {
					transport = new ListenerTransport(listener, looper);
				}
				mListeners.put(listener, transport);
				mService.requestLocationUpdates(minTime, transport);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "requestLocationUpdates: DeadObjectException", ex);
		}
	}
	
	public void removeUpdates(LociListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener==null");
		}
		try {
			ListenerTransport transport = mListeners.remove(listener);
			if (transport != null) {
				mService.removeUpdates(transport);
			}
		} catch (RemoteException ex) {
			Log.e(TAG, "removeUpdates: DeadObjectException", ex);
		}
	}
	
	
}
