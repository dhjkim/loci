package edu.cens.loci.components;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;

public class WatchDogAlarmReceiver extends BroadcastReceiver {

	public static final String TAG = "WatchDogAlarmReceiver";
	private Intent mServiceIntent = new Intent(LociManagerService.SERV_NAME);
	
	
	public void onReceive(Context context, Intent intent) {
		
		if (loadPowerState(context) && !isServRunning(context)) {
			MyLog.d(LociConfig.D.Watchdog.DEBUG, TAG, "[WatchDog] service is turned on, but not running. turn on.");
			startLociService(context);
		} else if (!loadPowerState(context) && isServRunning(context)) {
			MyLog.d(LociConfig.D.Watchdog.DEBUG, TAG, "[WatchDog] service is turned off, but running. shut off.");
			stopLociService(context);
		} else {
			MyLog.d(LociConfig.D.Watchdog.DEBUG, TAG, "[WatchDog] no problem.");
		}
		
		
	}	
	
	/////////////////////////////////////
	//////// SERVICE
	public void startLociService(Context context) {
		if (mServiceIntent == null)
			mServiceIntent= new Intent(LociManagerService.SERV_NAME);
		context.startService(mServiceIntent);
	}
	
	public void stopLociService(Context context) {
		if (mServiceIntent == null) 
			mServiceIntent = new Intent(LociManagerService.SERV_NAME);
		context.stopService(mServiceIntent);
	}
	
	public boolean isServRunning(Context context) {
		boolean isRunning = false;
		
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> runningServ = am.getRunningServices(100);
		
		for (ActivityManager.RunningServiceInfo serv : runningServ) {
			if (serv.service.getClassName().equals(LociManagerService.SERV_NAME)) {
				isRunning = true;
			}
		}
		return isRunning;
	}
	
	public boolean loadPowerState(Context context) {
		SharedPreferences activityPreferences = context.getSharedPreferences(LociConfig.Preferences.PREFS_NAME, Activity.MODE_PRIVATE);
		return activityPreferences.getBoolean(LociConfig.Preferences.PREFS_KEY_SERVICE_POWER_BUTTON_ON, false);
	}
	
}
