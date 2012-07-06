package edu.cens.loci;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import edu.cens.loci.ui.SettingsActivity;
import edu.cens.loci.ui.TabListActivity;
import edu.cens.loci.ui.TabMapActivity;
import edu.cens.loci.ui.calendar.MonthlyCalendar;
import edu.cens.loci.utils.MyLog;

public class LociTabActivity extends TabActivity {
	
	public static final String TAG = "LociTabActivity";
	
	//private static final int TAB_INDEX_CALENDAR = 0;
	//private static final int TAB_INDEX_MAP = 1;
	//private static final int TAB_INDEX_LIST = 2;
	
	private TabHost mTabHost;
	
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      MyLog.i(LociConfig.D.UI.DEBUG, TAG, "onCreate");
      
      mTabHost = getTabHost();
      
      setupCalendarTab();
  	  setupMapTab();
  	  setupListTab();
  	  setupSettingsTab();
  	  
  	  // set default to "Profiles"
      mTabHost.setCurrentTab(2);
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
  	super.onNewIntent(intent);
    MyLog.i(LociConfig.D.UI.DEBUG, TAG, "onNewIntent");
  }
  
  @Override
  protected void onResume() {
  	super.onResume();
    MyLog.i(LociConfig.D.UI.DEBUG, TAG, "onResume");
  }
  
  @Override
  protected void onRestart() {
  	super.onRestart();
  	MyLog.i(LociConfig.D.UI.DEBUG, TAG, "onRestart");
  }

  private void setupCalendarTab() {
  	Intent intent = new Intent("edu.cens.loci.action.VIEW_CALENDAR");
  	intent.setClass(this, MonthlyCalendar.class);
    mTabHost.addTab(mTabHost.newTabSpec("calendar")
    	  .setIndicator("Calendar", getResources().getDrawable(android.R.drawable.ic_menu_my_calendar))
    	  .setContent(intent));
  }
  
  private void setupMapTab() {
  	Intent intent = new Intent("edu.cens.loci.action.VIEW_MAP");
  	intent.setClass(this, TabMapActivity.class);
    mTabHost.addTab(mTabHost.newTabSpec("map")
    	  .setIndicator("Map", getResources().getDrawable(android.R.drawable.ic_menu_mapmode))
    	  .setContent(intent));
  }
  
  private void setupListTab() {
  	Intent intent = new Intent("edu.cens.loci.action.VIEW_LIST");
  	intent.setClass(this, TabListActivity.class);
    mTabHost.addTab(mTabHost.newTabSpec("list")
    	  //.setIndicator("Places", getResources().getDrawable(android.R.drawable.ic_menu_preferences))
    		.setIndicator("Places", getResources().getDrawable(android.R.drawable.ic_menu_myplaces))
    		.setContent(intent));
  }
	
  private void setupSettingsTab() {
  	Intent intent = new Intent("edu.cens.loci.action.VIEW_SETTINGS");
  	intent.setClass(this, SettingsActivity.class);
    mTabHost.addTab(mTabHost.newTabSpec("settings")
    	  //.setIndicator("Places", getResources().getDrawable(android.R.drawable.ic_menu_preferences))
    		.setIndicator("Settings", getResources().getDrawable(android.R.drawable.ic_menu_preferences))
    		.setContent(intent));
  }
  
   
}

