package edu.cens.loci.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.classes.LociPlace;
import edu.cens.loci.classes.LociVisit;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.ui.maps.overlays.PlacesItemizedOverlay;
import edu.cens.loci.ui.maps.overlays.PlacesOverlayItem;
import edu.cens.loci.ui.maps.overlays.TracksOverlay;
import edu.cens.loci.utils.LocationUtils;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class TabMapActivity extends MapActivity {

	public static final String TAG = "MapTabActivity";
	
	private MapView mMapView;
	private MapController mMapController;

	private PlacesItemizedOverlay 	mPlacesItemizedOverlay;
	private TracksOverlay 				mTracksOverlay;
	
	private Drawable mRedCircle;
	private Date mMapDate = null;
	
	private LociDbUtils mDbUtils = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.map_tab);
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapController = mMapView.getController();
		
		mMapView.setBuiltInZoomControls(true);
		mMapView.setSatellite(false);
		
		mRedCircle = this.getResources().getDrawable(R.drawable.marker_circle_red);
		
		mDbUtils = new LociDbUtils(this);
	}
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	public void onRestart() {
		super.onRestart();
	}
	
	public void onStart() {
		super.onStart();
	}
	
	public void onResume() {
		super.onResume();

		MyLog.d(LociConfig.D.UI.MAP, TAG, "onResume");
		
		// display today's data
		if (mMapDate == null) {
			mMapDate = MyDateUtils.getToday();
		} 
		updateOverlays(0);
	}
	
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		//MyLog.i(LociConfig.Debug.UI.LOG_CALL, TAG, "onSaveInstanceState()");
	}
	
	public void onPause() {
		
		if (mPlacesItemizedOverlay != null)
			mPlacesItemizedOverlay.hideBalloon();
		
		if (mMapView != null) {
			mMapView.getOverlays().clear();
			mMapView.invalidate();
			mMapView.postInvalidate();
		}
		
		super.onPause();
	}
	
	public void onStop() {
		super.onStop();
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * For accounting purposes, the server needs to know whether or not you are currently displaying any kind of route information, 
	 * such as a set of driving directions. 
	 * Subclasses must implement this method to truthfully report this information, or be in violation of our terms of use. 
	 * TODO: provide link 
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public List<Overlay> getOverlays() {
		return mMapView.getOverlays();
	}
	
	public void updateOverlays(int track_filter) {
		List<Overlay> overlays = mMapView.getOverlays();
		overlays.clear();
				
		if (updatePlacesOverlay(mMapDate.getTime()))
			overlays.add(mPlacesItemizedOverlay);
		if (updateTracksOverlay(mMapDate, track_filter))
			overlays.add(mTracksOverlay);
		
		mMapView.postInvalidate();
	}
	
	public boolean updatePlacesOverlay(long date) {
		
		if (mPlacesItemizedOverlay == null)
			mPlacesItemizedOverlay = new PlacesItemizedOverlay(this, mRedCircle, mMapView);
		else
			mPlacesItemizedOverlay.clear();
		
		ArrayList<LociVisit> visits = mDbUtils.getBaseVisits(date);
		HashMap<Long, PlacesOverlayItem> placeOverlay = new HashMap<Long, PlacesOverlayItem>();
		
		MyLog.e(LociConfig.D.UI.MAP, TAG, "updatePlacesOverlay() : #visits="+visits.size());
		
		
		for (LociVisit visit : visits) {
			long pid = visit.placeId;
			
			if (pid >= 0) {
				LociPlace place = mDbUtils.getPlace(pid);
				if (place == null) {
					MyLog.e(LociConfig.D.UI.MAP, TAG, "updatePlacesOverlay() : pid=" + pid + " returned null PlaceInfo object. skip.");
					continue;
				}
				
				if (place.state == Places.STATE_REGISTERED || place.state == Places.STATE_SUGGESTED) {
					
					if (place.areas == null || place.areas.size() <= 0) {
						MyLog.e(LociConfig.D.UI.MAP, TAG, "updatePlacesOverlay() : no area saved for this place " + pid);
						continue;
					}
					
					LociCircleArea circle = place.areas.get(0);
					
					if (place.name == null) {
						MyLog.d(LociConfig.D.UI.MAP, TAG, "updatePlacesOverlay(): name is null");
						continue;
					}
					
					if (circle == null) {
						MyLog.e(LociConfig.D.UI.MAP, TAG, "updatePlacesOverlay() : cirlce is null.");
						MyLog.e(LociConfig.D.UI.MAP, TAG, place.areas.toString());
						continue;
					}
					
					double lat = circle.getLatitude();
					double lon = circle.getLongitude();
					
					if (lat != 0 && lon != 0) {
						MyLog.d(LociConfig.D.UI.MAP, TAG, "updateVisitOverlay(): valid circle");
						GeoPoint gp = LocationUtils.getGeoPoint(lat, lon);

						if (placeOverlay.containsKey(pid)) {
							MyLog.d(LociConfig.D.UI.MAP, TAG, "updateVisitOverlay(): update existing overlay");
							PlacesOverlayItem placeItem = placeOverlay.get(pid);
							placeItem.addVisit(visit.enter, visit.exit);
							placeOverlay.put(pid, placeItem);
						} else {
							MyLog.d(LociConfig.D.UI.MAP, TAG, "updateVisitOverlay(): add new overlay");
							PlacesOverlayItem placeItem = new PlacesOverlayItem(pid, place.type, place.state, place.name, gp, 10);
							placeItem.addVisit(visit.enter, visit.exit);
							placeOverlay.put(pid, placeItem);
						}
					} else {
						MyLog.e(LociConfig.D.ERROR, TAG, "updateVisitOverlay(): invalid circle. skip. " + String.format("(lat=%f lon=%f)", lat, lon));
					}
				}
			}
		}

		Set<Long> keys = placeOverlay.keySet();
		Iterator<Long> iter = keys.iterator();
		
		while (iter.hasNext()) {
			Long key = iter.next();
			mPlacesItemizedOverlay.addOverlay(placeOverlay.get(key));
		}
		
		return (mPlacesItemizedOverlay.size() > 0);
	}
	
	public boolean updateTracksOverlay(Date date, int filter) {
		
		if (mTracksOverlay == null)
			mTracksOverlay = new TracksOverlay(this);
		else
			mTracksOverlay.clearPoints();
		
		ArrayList<LociLocation> track = getTracks(date, filter);
		
		Toast.makeText(TabMapActivity.this, String.format("Displaying %d GPS points.", track.size()), Toast.LENGTH_LONG).show();
		
		// set the zoom-level & center of the map
		if (track.size() > 0) {
			
			LociLocation firstPos = track.get(0);
			
			double minLat = firstPos.getLatitude();
			double maxLat = minLat;
			double minLon = firstPos.getLongitude();
			double maxLon = minLon;
			
			for (LociLocation loc : track) {
				if (minLat > loc.getLatitude()) {
					minLat = loc.getLatitude();
				} else if (maxLat < loc.getLatitude()) {
					maxLat = loc.getLatitude();
				} else if (minLon > loc.getLongitude()) {
					minLon = loc.getLongitude();
				} else if (maxLon < loc.getLongitude()) {
					maxLon = loc.getLongitude();
				}
				mTracksOverlay.addLocation(loc);
			}

			double centerLat = (minLat + maxLat) / 2;
			double centerLon = (minLon + maxLon) / 2;
			
			mMapController.zoomToSpan((int) Math.abs(maxLat*1E6 - minLat*1E6), (int) Math.abs(maxLon*1E6 - minLon*1E6));
			moveMapCenter(centerLat, centerLon);
			
			return true;
		}
		return false;
	}
	
	public ArrayList<LociLocation> getTracks(Date date, int filter) {

		Date start = MyDateUtils.getMidnight(date);
		Date end = MyDateUtils.getNextDay(start);
		
		ArrayList<LociLocation> track = mDbUtils.getTrack(start.getTime(), end.getTime(), filter);
	
		//MyLog.d(LociConfig.Debug.UI.OVERLAYS.LOG_TRACKS, TAG, "getTracks(): number of positions=" + track.size());
		
		return track;
	}
	
	private void moveMapCenter(double latitude, double longitude) {

		Double lat = latitude * 1E6;
		Double lon = longitude * 1E6;

		GeoPoint pt = new GeoPoint(lat.intValue(), lon.intValue());
		
		mMapController.setCenter(pt);
		
		return;
	}

	// View Date
	private static final int MENU_ID_CHANGEDATE1 = 0;
	private static final int MENU_ID_CHANGEDATE2 = 1;
	private static final int MENU_ID_CHANGE_SATLAYER = 2;
	
	 /****
   * MENU BUTTON
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  		//boolean result = super.onCreateOptionsMenu(menu);
  			//menu.add(0, MENU_ID_CHANGEDATE1, 0, "Pick date (show all gps)").setIcon(android.R.drawable.ic_search_category_default);
  			//menu.add(0, MENU_ID_CHANGEDATE2, 1, "Pick date (show selected gps)").setIcon(android.R.drawable.ic_search_category_default);
  			//if (mMapView.isSatellite())
  			//	menu.add(0, MENU_ID_CHANGE_SATLAYER, 2, "Map View").setIcon(android.R.drawable.ic_menu_mapmode);
  			//else 
  			//	menu.add(0, MENU_ID_CHANGE_SATLAYER, 2, "Satellite View").setIcon(android.R.drawable.ic_menu_mapmode);
  			//return result;
  		MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.tab_map_menu, menu);
    	return true;
  }
  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
  
  	//boolean result = super.onPrepareOptionsMenu(menu);
  	//if (mMapView.isSatellite())
  	//	menu.findItem(MENU_ID_CHANGE_SATLAYER).setTitle("Map View");
  	//else
  	//	menu.findItem(MENU_ID_CHANGE_SATLAYER).setTitle("Satellite View");
  	//return result;
    MenuItem mapitem = menu.findItem(R.id.menu_map_mode_map);
    MenuItem satitem = menu.findItem(R.id.menu_map_mode_sat);

    if (mMapView.isSatellite()) {
    	mapitem.setVisible(true);
    	satitem.setVisible(false);
    } else {
    	mapitem.setVisible(false);
    	satitem.setVisible(true);
    }
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	
  	switch (item.getItemId()) {
  	case R.id.menu_map_pick_date:
  		showDialog(0);
  		break;
  	case R.id.menu_map_mode_map:
  		mMapView.setSatellite(false);
  		break;
  	case R.id.menu_map_mode_sat:
  		mMapView.setSatellite(true);
  		break;
  	/*
  	case MENU_ID_CHANGEDATE1:
  			showDialog(0);
  			break;
  	case MENU_ID_CHANGEDATE2:
  			showDialog(1);
  			break;
  	case MENU_ID_CHANGE_SATLAYER:
  			mMapView.setSatellite(!mMapView.isSatellite());
  			break;
  	*/		
  	}
  	return true;
		//return super.onOptionsItemSelected(item);			
  }
  
  protected Dialog onCreateDialog(int id) {
  	switch(id) {
  	case 0:
  		Calendar cal = Calendar.getInstance();
  		cal.setTime(mMapDate);
  		return new DatePickerDialog(this,
  				mDateSetListener,
  				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
  	case 1:
  		Calendar cal1 = Calendar.getInstance();
  		cal1.setTime(mMapDate);
  		return new DatePickerDialog(this,
  				mDateSetListener1,
  				cal1.get(Calendar.YEAR), cal1.get(Calendar.MONTH), cal1.get(Calendar.DAY_OF_MONTH));
  	}
		return null;
  }
  
  private DatePickerDialog.OnDateSetListener mDateSetListener = 
  	new DatePickerDialog.OnDateSetListener() {
  	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
  		Calendar viewDate = Calendar.getInstance();
  		viewDate.set(Calendar.YEAR, year);
  		viewDate.set(Calendar.MONTH, monthOfYear);
  		viewDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
  		viewDate.set(Calendar.HOUR_OF_DAY, 0);
  		viewDate.set(Calendar.MINUTE, 0);
  		viewDate.set(Calendar.SECOND, 0);
  		viewDate.set(Calendar.MILLISECOND, 0);
  		viewDate.add(Calendar.DAY_OF_MONTH, 0);
  		mMapDate = viewDate.getTime();
  		updateOverlays(0);
  	}
  };
  
  private DatePickerDialog.OnDateSetListener mDateSetListener1 = 
  	new DatePickerDialog.OnDateSetListener() {
  	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
  		Calendar viewDate = Calendar.getInstance();
  		viewDate.set(Calendar.YEAR, year);
  		viewDate.set(Calendar.MONTH, monthOfYear);
  		viewDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
  		viewDate.set(Calendar.HOUR_OF_DAY, 0);
  		viewDate.set(Calendar.MINUTE, 0);
  		viewDate.set(Calendar.SECOND, 0);
  		viewDate.set(Calendar.MILLISECOND, 0);
  		viewDate.add(Calendar.DAY_OF_MONTH, 0);
  		mMapDate = viewDate.getTime();
  		updateOverlays(1);
  	}
  };
	
}
