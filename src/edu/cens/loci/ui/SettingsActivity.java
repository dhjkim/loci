package edu.cens.loci.ui;

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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.components.ILociManager;
import edu.cens.loci.components.LociManagerService;
import edu.cens.loci.utils.MyLog;

public class SettingsActivity extends PreferenceActivity 
				implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

	private static final String TAG = "SettingsActivity";
	
	public static final String KEY_USE_LOCI = "use_loci";
	public static final String KEY_USE_PLACE_SENSING = "use_place_sensing";
	public static final String KEY_PLACE_SENSING_RATE = "place_sensing_rate";
	public static final String KEY_USE_PATH_TRACKING = "use_path_tracking";
	public static final String KEY_PATH_TRACKING_RATE = "path_tracking_rate";
	public static final String KEY_VIEW_COMPONENTS_STATUS = "view_components_status";
	
	private CheckBoxPreference mUseLociCheckBox;
	private CheckBoxPreference mUsePlaceCheckBox;
	private ListPreference 		 mPlaceRateList;
	private CheckBoxPreference mUsePathCheckBox;
	private ListPreference 		 mPathRateList;
	private Preference 				 mComponentsStatus;


	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		if (key.equals(KEY_USE_LOCI)) {
			
			boolean value = ((Boolean) newValue).booleanValue();
			
			if (value) {
				mUsePlaceCheckBox.setEnabled(false);
				mPlaceRateList.setEnabled(false);
				mUsePathCheckBox.setEnabled(false);
				mPathRateList.setEnabled(false);
	      startLociService();
	      bindToService();
			} else {
				mUsePlaceCheckBox.setEnabled(true);
				mPlaceRateList.setEnabled(true);
				mUsePathCheckBox.setEnabled(true);
				mPathRateList.setEnabled(true);
				unbindToService();
	  		stopLociService();
			}
		}

		return true;
	}
	

	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		if (key.equals(KEY_VIEW_COMPONENTS_STATUS)) {
			Intent intent = new Intent(this, StatusViewActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
		}
		return true;
	}

	
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		addPreferencesFromResource(R.layout.loci_settings);
	
		mUseLociCheckBox = (CheckBoxPreference) findPreference(KEY_USE_LOCI);
		mUseLociCheckBox.setOnPreferenceChangeListener(this);
		mUseLociCheckBox.setSummaryOn("Loci is running. Turn off to change sensing settings.");
		
		mUsePlaceCheckBox = (CheckBoxPreference) findPreference(KEY_USE_PLACE_SENSING);
		mPlaceRateList = (ListPreference) findPreference(KEY_PLACE_SENSING_RATE);
		mUsePathCheckBox = (CheckBoxPreference) findPreference(KEY_USE_PATH_TRACKING);
		mPathRateList = (ListPreference) findPreference(KEY_PATH_TRACKING_RATE);		
		
		mComponentsStatus = (Preference) findPreference(KEY_VIEW_COMPONENTS_STATUS);
		mComponentsStatus.setOnPreferenceClickListener(this);
		
		if (mUseLociCheckBox.isChecked()) {
			mUsePlaceCheckBox.setEnabled(false);
			mPlaceRateList.setEnabled(false);
			mUsePathCheckBox.setEnabled(false);
			mPathRateList.setEnabled(false);
			if (!isServRunning()) {
	      startLociService();
	      bindToService();
			}
		} else {
			mUsePlaceCheckBox.setEnabled(true);
			mPlaceRateList.setEnabled(true);
			mUsePathCheckBox.setEnabled(true);
			mPathRateList.setEnabled(true);
			if (isServRunning()) {
				unbindToService();
	  		stopLociService();
			}
		}
		
	}
	
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
		MyLog.d(true, TAG, "bindToService() : action = " + action);
		
		if (isServiceOn) {
      bindService(new Intent(ILociManager.class.getName()),
          mConnection, Context.BIND_AUTO_CREATE);
		} else {
			MyLog.i(LociConfig.D.Service.DEBUG, TAG, "bindToService() : service is not running. not binding.");
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
