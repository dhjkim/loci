package edu.cens.loci.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;

import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.ui.maps.overlays.MapEditOverlay;
import edu.cens.loci.ui.maps.overlays.MapEditOverlayItem;
import edu.cens.loci.utils.LocationUtils;

public class MapEditViewActivity extends MapActivity implements OnFocusChangeListener{

	//private static final String TAG = "MapEditView";

	private MapView 				mMapView;
	private MapController 	mMapCont;
	private Drawable 				mRedCircle;
	private Button 					mDoneButton;
	private LociCircleArea 	mCircle;
	private MapEditOverlay 	mOverlay;
	
	private ArrayList<Toast> mToastList = new ArrayList<Toast>();
	
	private boolean mMovable = false;
	
	@SuppressWarnings("unchecked")
	public void onFocusChanged(ItemizedOverlay overlay, OverlayItem focused) {
		//MyLog.d(LociConfig.D.UI.MAP, TAG, "onFocusChanged():");
	}

	
	/**
	 * Two different mode depending on the extra passed via intent
	 *  1. not-movable mode
	 *  2. edit mode
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.map_edit);

		// Get Circle (to be displayed) and Mode
		Intent intent = this.getIntent();
		mCircle = (LociCircleArea) intent.getSerializableExtra(Constants.Intents.UI.MAP_EDIT_CIRCLE_EXTRA_KEY);
		mMovable = intent.getBooleanExtra(Constants.Intents.UI.MAP_EDIT_MODE_EXTRA_KEY, false);
		
		// Done Button
		mDoneButton = (Button) findViewById(R.id.maps_header_btn);
		mDoneButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exit();
			}
		});
		// show different text on Done Button
		if (mMovable)
			mDoneButton.setText("Done");
		else
			mDoneButton.setText("Back");
		
		prepareToasts();
		
		// Map properties
		mMapView = (MapView) findViewById(R.id.map_view);
		mMapCont = mMapView.getController();
		mMapView.setBuiltInZoomControls(true);
		mMapView.setSatellite(false);

		// Drawable
		mRedCircle = this.getResources().getDrawable(R.drawable.circle_marker_red);
	
		mOverlay = new MapEditOverlay(mRedCircle, mMapView, mMovable);
		
		mMapView.getOverlays().clear();
		mMapView.getOverlays().add(mOverlay);
		
		// Set the center of the map
		if (mCircle != null) {
			GeoPoint gp = mCircle.getGeoPoint();
			
			if (mCircle.getRadius() < LociConfig.pMinSuggestedRadius)
				mCircle.setRadius(LociConfig.pMinSuggestedRadius);
			
			MapEditOverlayItem item = new MapEditOverlayItem(gp, 1, (float) mCircle.getRadius());
			
			mOverlay.addOverlay(item);
			mOverlay.setOnFocusChangeListener(this);
			
			mMapCont.setCenter(gp);
			mMapCont.setZoom(18);
		} 
	}
	
	public void onRestart() {
		super.onRestart();
	}
	
	public void onStart() {
		super.onStart();
	}
	
	private void prepareToasts() {
		mToastList.clear();
		
		//Log.d(TAG, "prepareToast: " + mCircle.extra);
		
		if (!mMovable) {
			//mToastList.add(Toast.makeText(this, "Approximate position of the place", Toast.LENGTH_LONG));
			if (mCircle.extra != null && !mCircle.extra.equals(""))
				mToastList.add(Toast.makeText(this, "Captured " + mCircle.extra, Toast.LENGTH_LONG));
			mToastList.add(Toast.makeText(this, "Cannot move the position in view mode. Click the marker to see its radius.", Toast.LENGTH_LONG));
		} else {
			
			//Log.e(TAG, mCircle.toString());
			//Log.e(TAG, mCircle.extra);
			
			if (mCircle.extra != null && !TextUtils.isEmpty(mCircle.extra))
				mToastList.add(Toast.makeText(this, "Captured " + mCircle.extra, Toast.LENGTH_LONG));
			mToastList.add(Toast.makeText(this, "To move the center, first click the marker.", Toast.LENGTH_LONG));
			mToastList.add(Toast.makeText(this, "Then tap on to the desired new location.", Toast.LENGTH_LONG));
			mToastList.add(Toast.makeText(this, "To change radius, first click the marker.", Toast.LENGTH_LONG));
			mToastList.add(Toast.makeText(this, "Then long press the surrounding circle. When the circle gets darker, drag to adjust.", Toast.LENGTH_LONG));
		}
	}
	
	private void showToasts() {
		//Log.d(TAG, "showToasts : " + mToastList.size());
		for (Toast toast : mToastList) {
			toast.show();
		}
	}
	
	private void cancelToasts() {
		//Log.d(TAG, "cancelToasts : " + mToastList.size());
		for (Toast toast : mToastList) {
			toast.setDuration(Toast.LENGTH_SHORT);
			toast.cancel();
		}
	}
	
	public void onResume() {
		super.onResume();
		showToasts();
	}
	
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}
	
	public void onPause() {
		super.onPause();
		cancelToasts();
	}

	public void onStop() {
		super.onStop();
	}
	
	public void onDestroy() {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
			mSearchTask = null;
		}
		this.mOverlay.setFocus(null);
		this.mOverlay = null;
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void exit() {
		
		if (!mMovable) {
			//MyLog.d(true, TAG, "exit(): not movable. just finish with out intent.");
			finish();
		} else {
			if (mOverlay.size() > 0) {
				//MyLog.d(true, TAG, "exit(): movable. just finish with out intent.");
				MapEditOverlayItem item = mOverlay.getItem(0);
				GeoPoint gp = item.getPoint();
				Location loc = LocationUtils.getLocation(gp);
				// set accuracy as 0 when the user defines the circle
				LociCircleArea circle = new LociCircleArea();
				circle.setCenter(loc.getLatitude(), loc.getLongitude());
				circle.setRadius(item.radius);
				circle.setAccuracy(0);
	
				//Log.i(TAG, "exit: new circle = " + circle.toString());
				
				Intent intent = new Intent();
				intent.putExtra("newLoc", circle);
				setResult(RESULT_OK, intent);
			}
			finish();
		}
	}
	
	private static final int GEOCODING_RETRIES = 5;
	private static final long GEOCODING_RETRY_INTERVAL = 500;
	
	/* The search address task class. Performs address search in the bg thread */
	@SuppressWarnings("unused")
	private class SearchAddressTask extends AsyncTask<String, Void, Address> {
		
		private ProgressDialog mBusyPrg = null;
		
		@Override
		protected void onPreExecute() {
			//Start progress bar
			mBusyPrg = ProgressDialog.show(MapEditViewActivity.this, "", "searching...", true);
			mBusyPrg.setOwnerActivity(MapEditViewActivity.this);
		}
		
		@Override
		protected Address doInBackground(String... params) {
			//Search the address
			Geocoder geoCoder = new Geocoder(MapEditViewActivity.this, Locale.getDefault());
			
			for(int i = 0; i < GEOCODING_RETRIES; i++) {
			    try {
			    	List<Address> addrs = geoCoder.getFromLocationName(params[0], 5);
		
			        if(addrs.size() > 0) {
			        	return addrs.get(0);
			        }
			    }
			    catch (Exception e) {
			    }
			    
			    try {
					Thread.sleep(GEOCODING_RETRY_INTERVAL);
				} catch (InterruptedException e) {
				}
			}
		  return null;
		}
		
		@Override
		protected void onPostExecute(Address adrr) {
			//Address search done, kill progressbar and notify
			
			if(mBusyPrg != null) {
				mBusyPrg.cancel();
				mBusyPrg = null;
			}

			if(adrr != null && adrr.hasLongitude() && adrr.hasLatitude()) {
				handleSearchResult(adrr);
			}
			else {
				Toast.makeText(getApplicationContext(), "Search failed.", Toast.LENGTH_SHORT).show();
			}
	  }
	}
	
  private void handleSearchResult(Address adr) {
  	mSearchTask = null;
  	
  	@SuppressWarnings("unused")
		GeoPoint gp = new GeoPoint((int) (adr.getLatitude() * 1E6), 
  							   (int) (adr.getLongitude() * 1E6));
  	
  	String addrText = "";
  	
      int addrLines = adr.getMaxAddressLineIndex();
  	for (int i=0; i<Math.min(2, addrLines); i++) {
  		addrText += adr.getAddressLine(i) + "\n";
  	}

  	mMapView.getController().setZoom(mMapView.getMaxZoomLevel());
  	
  }
  
	private AsyncTask<String, Void, Address> mSearchTask = null;

	/* Menu ids */
	//private static final int MENU_MY_LOC_ID = Menu.FIRST;
	//private static final int MENU_SEARCH_ID = Menu.FIRST + 1;
	private static final int MENU_ID_CHANGE_SATLAYER = Menu.FIRST;
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      boolean ret = super.onCreateOptionsMenu(menu);
      //menu.add(0, MENU_MY_LOC_ID, 0, "My Location")
      //	.setIcon(android.R.drawable.ic_menu_mylocation);
      //menu.add(0, MENU_SEARCH_ID, 0, "Search")
      //	.setIcon(android.R.drawable.ic_menu_search);

      if (mMapView.isSatellite())
				menu.add(0, MENU_ID_CHANGE_SATLAYER, 2, "Map View").setIcon(android.R.drawable.ic_menu_mapmode);
			else 
				menu.add(0, MENU_ID_CHANGE_SATLAYER, 2, "Satellite View").setIcon(android.R.drawable.ic_menu_mapmode);
      
      return ret;
  }
  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
  	boolean result = super.onPrepareOptionsMenu(menu);
  	if (mMapView.isSatellite())
  		menu.findItem(MENU_ID_CHANGE_SATLAYER).setTitle("Map View");
  	else
  		menu.findItem(MENU_ID_CHANGE_SATLAYER).setTitle("Satellite View");
  	return result;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	switch(item.getItemId()) {
  	//case MENU_MY_LOC_ID: //Show my location
  	//	return true;
  	//case MENU_SEARCH_ID: //Search an address
  	//	return true;
  	case MENU_ID_CHANGE_SATLAYER:
			mMapView.setSatellite(!mMapView.isSatellite());
			break;
  	}
    return super.onOptionsItemSelected(item);
  }
  
}
