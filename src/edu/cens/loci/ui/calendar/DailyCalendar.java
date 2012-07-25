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
package edu.cens.loci.ui.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.classes.LociPlace;
import edu.cens.loci.classes.LociVisit;
import edu.cens.loci.classes.LociVisitWifi;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class DailyCalendar extends ListActivity {

	private static final String TAG = "DailyCalendar";
	
	
	private ArrayList<ListItem> mItems;
	private ListView mListView;
	private DailyCalendarAdapter mAdapter; 
	private ListItem mListItem;
	private Date mDate;
	
	private LociDbUtils mDbUtils;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.calendar_daily);
	    
	    Intent getIntent = getIntent();
	    
	    mDate = (Date) getIntent.getExtras().getSerializable("date");
	    setTitle(" " + MyDateUtils.getFullDate(mDate));
	    
	    //MyLog.d(false, TAG, "Date=" + date.toLocaleString());
	    mDbUtils = new LociDbUtils(this);
	}
	
	private ArrayList<LociVisit> mVisits;
	
	@Override
	public void onResume() {
		super.onResume();
		updateList();
	}

	private void updateList() {
		
		if (mItems == null)
			mItems = new ArrayList<ListItem>();
		else 
			mItems.clear();
		
    // add time frames
    final Date date = mDate;
    Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(date);
		endCalendar.add(Calendar.DAY_OF_MONTH, 1);
		
		//mVisits = Loci.getInstance().getVisitDb().fetchVisitsByTime(date, endCalendar.getTime());
		mDbUtils.getWifiVisits(date.getTime(), endCalendar.getTime().getTime());
		mVisits = mDbUtils.getBaseVisits(date.getTime());
		LociVisitWifi lastVisit = mDbUtils.getLastWifiVisit();
		
		for (LociVisit visit : mVisits) {
			
			MyLog.d(true, TAG, visit.toString());
			
			long pid = visit.placeId;
			if (mFilter) {
				if (pid == -1 && visit.visitId != lastVisit.visitId) 
					continue;
			}
			
			//int state = Loci.getInstance().getPlaceDb().getState(pid);
			LociPlace place = mDbUtils.getPlace(pid);
			
			int state = 0;
			String name = null;
			
			if (place != null) {
				state = place.state;
				name = place.name;
			}
			
			
			//if (state == PlaceDatabaseAdapter.STATE_BLOCKED || state == PlaceDatabaseAdapter.STATE_DELETED || state == PlaceDatabaseAdapter.STATE_MERGEDTO) {
			if (state == Places.STATE_BLOCKED || state == Places.STATE_DELETED) {
				MyLog.d(LociConfig.D.UI.CALENDAR, TAG, "updateList() : pid=" + pid + " place is deleted or blocked. skip.");
				continue;
			}
			
			//if (state == PlaceDatabaseAdapter.STATE_REGISTERED || state == PlaceDatabaseAdapter.STATE_SUGGESTED || (visit.getVisitID() == lastVisit.getVisitID() && state == -1)) {
			long entTime = visit.enter;
			long extTime = visit.exit;
			
			//String name = place.getName();
			//String name = Loci.getInstance().getPlaceDb().getName(pid);
			//MyLog.d(false, TAG, "place:" + name + ", extTime:" + entTime.toLocaleString());
			
			mListItem = new ListItem(visit.visitId, name, MyDateUtils.getTimeFormatMedium(entTime), MyDateUtils.getTimeFormatMedium(extTime), visit.getDuration());
			mItems.add(mListItem);
			//} 		
		}
		
    // connect to the adapter
    mAdapter = new DailyCalendarAdapter(DailyCalendar.this, R.layout.calendar_daily_items, mItems);
    mListView = getListView();
    mListView.setFastScrollEnabled(true);
    mListView.setAdapter(mAdapter);
	}
	
	public class ListItem {
		public ListItem(long aVid, String aName, String aEntTime, String aExtTime, long aStayTime) {
			vid = aVid;
			name = aName;
			entTime = aEntTime;
			extTime = aExtTime;
			stayTime = aStayTime;
		}
		public long vid;
		public String entTime;
		public String extTime;
		public String name;
		public long stayTime;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		//long vid = mItems.get(position).vid;
		//Intent intent = new Intent(DailyCalendar.this, VisitInfoViewerActivity.class);
		//intent.putExtra("visitId", vid);
		//Toast.makeText(this, ""+position, Toast.LENGTH_SHORT).show();
		//startActivity(intent);
		
		Intent intent = new Intent(Intent.ACTION_VIEW, getSelectedUri(position));
		startActivity(intent);
	}
	
	private Uri getSelectedUri(int position) {
		final long visitId = mItems.get(position).vid;
		return ContentUris.withAppendedId(Visits.CONTENT_URI, visitId);
	}
	
	
	 /****
	   * MENU BUTTON
	   */
		
		private static final int MENU_ID_FILTER = 0;
		private static final int MENU_ID_SURVEY = 1;
		private boolean mFilter = true;
		
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	  		boolean result = super.onCreateOptionsMenu(menu);
	  			if (mFilter)
	  				menu.add(0, MENU_ID_FILTER, 0, "Show all visits").setIcon(android.R.drawable.ic_menu_set_as);
	  			else 
	  				menu.add(0, MENU_ID_FILTER, 0, "Hide short & unknown visits").setIcon(android.R.drawable.ic_menu_set_as);
	  			
	  			menu.add(0, MENU_ID_SURVEY, 0, "Daily Survey").setIcon(android.R.drawable.ic_menu_more);
	  			
	  			return result;
	  }
	  
	  @Override
	  public boolean onPrepareOptionsMenu(Menu menu) {
	  	boolean result = super.onPrepareOptionsMenu(menu);
	  	if (mFilter)
	  		menu.findItem(MENU_ID_FILTER).setTitle("Show all visits");
	  	else
	  		menu.findItem(MENU_ID_FILTER).setTitle("Hide short & unknown visits");
	  	return result;
	  }
	  
	  @Override
	  public boolean onOptionsItemSelected(MenuItem item) {
	  	
	  	switch (item.getItemId()) {
	  		case MENU_ID_FILTER:
	  			mFilter = !mFilter;
	  			updateList();
	  			break;
	  		case MENU_ID_SURVEY:
	  			//Intent intent = new Intent(DailyCalendar.this, DailySurveyActivity.class);
	  			//intent.putExtra("date", mDate.getTime());
	  			//startActivity(intent);
	  			break;
	  	}
			return super.onOptionsItemSelected(item);			
	  }
}


