package edu.cens.loci.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.Constants.Intents;
import edu.cens.loci.Constants.PlacesList;
import edu.cens.loci.Constants.Intents.UI;
import edu.cens.loci.components.ILociManager;
import edu.cens.loci.components.LociManagerService;
import edu.cens.loci.provider.LociContract.Places;

public class TabListActivity extends Activity {
	private static final String TAG = "ListTabActivity";
	
	private ArrayList<String> mPlaceStates = new ArrayList<String>();
	private ArrayList<Integer> mFilterState = new ArrayList<Integer>();
	
	private String [] mLabels = {"My Places", "Suggested Places", "Blocked Places"};
	
	/** Activity life-cycles */
	
	@Override
  public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.places_list_by_state);
		
		createPlaceViewList();
	}

	private void createPlaceViewList() {
		mPlaceStates.add("My Places"); mFilterState.add(Constants.Intents.UI.FILTER_STATE_REGISTERED);
		mPlaceStates.add("Suggested Places"); mFilterState.add(Constants.Intents.UI.FILTER_STATE_SUGGESTED);
		mPlaceStates.add("Blocked Places"); mFilterState.add(Constants.Intents.UI.FILTER_STATE_BLOCKED);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPlaceStates);
		ListView list = (ListView) this.findViewById(R.id.placelist);
		list.setAdapter(adapter);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		list.setOnItemClickListener(mPlaceListClickListener);
	}
	
	AdapterView.OnItemClickListener mPlaceListClickListener = new AdapterView.OnItemClickListener() {
		@SuppressWarnings("unchecked")
		public void onItemClick(AdapterView parent, View view, int position, long id) {
			
			Intent intent = new Intent(Intent.ACTION_VIEW);
			//Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
			//intent.setClass(TabListActivity.this, PlaceListActivity.class);
			intent.putExtra(UI.TITLE_EXTRA_KEY, mLabels[position]);
			intent.setType(Places.CONTENT_TYPE);
			intent.putExtra(UI.FILTER_STATE_EXTRA_KEY, mFilterState.get(position));
			intent.putExtra(UI.FILTER_TYPE_EXTRA_KEY, Constants.Intents.UI.FILTER_TYPE_GPS | Constants.Intents.UI.FILTER_TYPE_WIFI);
			intent.putExtra(UI.LIST_ORDER_EXTRA_KEY, PlacesList.LIST_ORDER_TYPE_NAME);
			startActivity(intent);
		}
};
	
	@Override
	public void onRestart() {
		super.onRestart();
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		bindToService();
	}
	
	@Override
	public void onPause() {
		unbindToService();
		super.onPause();
	}
	
	@Override
	public void onStop() {
		unbindToService(); // safe-net
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		}
	}

	 /** Menu button */
	/*
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.tab_list_menu, menu);
  	return true;
  }
  
  public boolean onPrepareOptionsMenu(Menu menu) {
  	
	    boolean isServiceOn = isServRunning();
	    
	    MenuItem item = menu.findItem(R.id.service_start);
	    if (item != null) {
	    	item.setEnabled(!isServiceOn);
	    	item.setVisible(!isServiceOn);
	    }
	
	    item = menu.findItem(R.id.service_stop);
	    if (item != null) {
	        item.setEnabled(isServiceOn);
	        item.setVisible(isServiceOn);
	    }
	    
	    //item = menu.findItem(R.id.service_settings);
	    
	    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
  	
  	switch (item.getItemId()) {
    
        case R.id.service_start:
            startLociService();
            bindToService();
            return true;
        case R.id.service_stop: 
      			unbindToService();
        		stopLociService();
      			return true;
        case R.id.service_settings:
        		Intent intent = new Intent(Intents.UI.ACTION_VIEW_SETTINGS);
        		startActivity(intent);
        		return true;
    }
    return false;
  }
	*/
  
  /** Connecting to the service */
  
	private static Intent mServiceIntent = null;

	public void startLociService() {
		saveServiceTurnedOnState(true);
		if (mServiceIntent == null)
			mServiceIntent = new Intent(LociManagerService.SERV_NAME);
		startService(mServiceIntent);
	}
	
	public void stopLociService() {
		saveServiceTurnedOnState(false);
		if (mServiceIntent == null) 
			mServiceIntent = new Intent(LociManagerService.SERV_NAME);
		stopService(mServiceIntent);
	}

	private void saveServiceTurnedOnState(boolean isOn) {
		SharedPreferences activityPreferences = getSharedPreferences(LociConfig.Preferences.PREFS_NAME, Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = activityPreferences.edit();
		editor.putBoolean(LociConfig.Preferences.PREFS_KEY_SERVICE_POWER_BUTTON_ON, isOn);
		editor.commit();
	}
	
	public boolean loadServiceTurnedOnState() {
		SharedPreferences activityPreferences = getSharedPreferences(LociConfig.Preferences.PREFS_NAME, Activity.MODE_PRIVATE);
		return activityPreferences.getBoolean(LociConfig.Preferences.PREFS_KEY_SERVICE_POWER_BUTTON_ON, false);
	}	

	
	/**
	 * Check if LociManagerService is running
	 * @return true if running
	 */
	public boolean isServRunning() {
		boolean isRunning = false;
		
		ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> runningServ = am.getRunningServices(100);
		
		for (ActivityManager.RunningServiceInfo serv : runningServ) {
			if (serv.service.getClassName().equals(LociManagerService.SERV_NAME)) {
				isRunning = true;
			}
		}
		return isRunning;
	}
	
	
	private boolean mIsBound = false;
	@SuppressWarnings("unused")
	private static ILociManager mService = null;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			//MyLog.d(LociConfig.Debug.UI.LOG_CALL, TAG, "onServiceConnected() <= ILociManager connected");
			mService = ILociManager.Stub.asInterface(service);
			mIsBound = true;
		}
		public void onServiceDisconnected(ComponentName className) {
			//MyLog.d(LociConfig.Debug.UI.LOG_CALL, TAG, "onServiceDisconnected()");
			mService = null;
			mIsBound = false;
		}
	};
	
	private void bindToService() {
		
		boolean isServiceOn = isServRunning();
		
		String action = ILociManager.class.getName();
		//MyLog.d(true, TAG, "bindToService() : action = " + action);
		
		if (isServiceOn) {
      bindService(new Intent(ILociManager.class.getName()),
          mConnection, Context.BIND_AUTO_CREATE);
		} else {
			//MyLog.i(LociConfig.Debug.UI.LOG_CALL, TAG, "bindToService() : service is not running. not binding.");
		}

	}
	
	private void unbindToService() {
		if (mIsBound) {
      // Detach our existing connection.
      unbindService(mConnection);
      mIsBound = false;
		}		
	}
}
