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
package edu.cens.loci.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.Entity.NamedContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.Constants.Intents;
import edu.cens.loci.Constants.PlacesList;
import edu.cens.loci.Constants.Intents.UI;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.classes.LociPlace;
import edu.cens.loci.classes.LociVisit;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.PlacesSource;
import edu.cens.loci.classes.Sources;
import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;
import edu.cens.loci.classes.PlacesSource.DataKind;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociProvider;
import edu.cens.loci.provider.LociContract.Data;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.PlacesEntity;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Keyword;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Note;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.StructuredPostal;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Website;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;
import edu.cens.loci.ui.maps.overlays.DotOverlay;
import edu.cens.loci.utils.LocationUtils;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;
import edu.cens.loci.utils.NotifyingAsyncQueryHandler;

public class PlaceViewActivity extends MapActivity
					implements AdapterView.OnItemClickListener, NotifyingAsyncQueryHandler.AsyncQueryListener {

	private static final String TAG = "PlaceViewActivity";

	private static final int LIST_ACTION_NO_ACTION = 1;
	private static final int LIST_ACTION_VIEW_VISITS = 2;
	private static final int LIST_ACTION_VIEW_WIFIS = 3;
	private static final int LIST_ACTION_ADD = 4;
	private static final int LIST_ACTION_DELETE = 5;
	private static final int LIST_ACTION_POSTAL = 6;
	//private static final int LIST_ACTION_KEYWORD = 7;
	private static final int LIST_ACTION_WEBSITE = 8;
	
	private Uri mUri;
	
	private TextView mNameView;
	private MapView  mMapView;
	private ListView mListView;
	
	private long 			mPlaceId;
	private LociPlace mPlace;
	private LociDbUtils mDbUtils;

	ArrayList<ViewEntry> mWifiEntries = new ArrayList<ViewEntry>();
	ArrayList<ViewEntry> mGpsEntries = new ArrayList<ViewEntry>();
	ArrayList<ViewEntry> mPostalEntries = new ArrayList<ViewEntry>();
	ArrayList<ViewEntry> mTagEntries = new ArrayList<ViewEntry>();
	ArrayList<ViewEntry> mWebsiteEntries = new ArrayList<ViewEntry>();
	ArrayList<ViewEntry> mOtherEntries = new ArrayList<ViewEntry>();
	
	ArrayList<ArrayList<ViewEntry>> mSections = new ArrayList<ArrayList<ViewEntry>>(); 
	
	protected Uri mLookupUri;
	private NotifyingAsyncQueryHandler mHandler;
	
	protected LayoutInflater mInflater;
	
	private static final int TOKEN_ENTITIES = 1;
	
  private boolean mHasEntities = false;
	
	private ArrayList<Entity> mEntities = new ArrayList<Entity>();
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// parse incoming data
		final Intent intent = getIntent();
		final String action = intent.getAction(); 
		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
			if (mUri == null) {
				MyLog.e(LociConfig.D.ERROR, TAG, "intent.getData() is empty, exiting.");
				finish();
			}
			MyLog.d(LociConfig.D.UI.DEBUG, TAG, "mUri=" + mUri.toString());
		} else {
			MyLog.e(LociConfig.D.ERROR, TAG, "Unknown action, exiting.");
			finish();
			return;
		}
		mPlaceId = LociProvider.checkPlaceIdUri(mUri);
		
		if (mPlaceId < 0) {
			MyLog.e(LociConfig.D.ERROR, TAG, "Invalid uri, exiting.");
			finish();
		}

		mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.place_view);
		
		mNameView = (TextView) findViewById(R.id.name);
		mMapView = (MapView) findViewById(R.id.map);
		
		mListView =  (ListView) findViewById(R.id.place_data);
		mListView.setOnItemClickListener(this);
		mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
		mListView.setEmptyView(findViewById(android.R.id.empty));

		mDbUtils = new LociDbUtils(this);
		
		mPlace = mDbUtils.getPlace(mPlaceId);

		//DEBUG
		MyLog.d(LociConfig.D.UI.DEBUG, TAG, "viewing..." + mPlace.toString());
		
		mHandler = new NotifyingAsyncQueryHandler(this, this);
		
		// The order they're added to mSections dictates the order they are diplayed in the list.
		mSections.add(mGpsEntries);
		mSections.add(mWifiEntries);
		mSections.add(mPostalEntries);
		mSections.add(mTagEntries);
		mSections.add(mWebsiteEntries);
		mSections.add(mOtherEntries);
	}
	
	public void onResume() {
		
		mPlace = mDbUtils.getPlace(mPlaceId);
		
		super.onResume();
		updateList();
	}
	
	private synchronized void startEntityQuery() {
		 
		 final long placeId = LociProvider.checkPlaceIdUri(mUri);
		 
		 mHandler.startQuery(TOKEN_ENTITIES, null, PlacesEntity.CONTENT_URI, null, Places._ID + "=?", new String[] {
				 String.valueOf(placeId),
		 }, null);
	}
	
	private void updateList() {

		if (mPlace == null) {
			MyLog.e(LociConfig.D.ERROR, TAG, "No Place Info");
			if (mPlaceId > 0) {
				MyLog.e(LociConfig.D.ERROR, TAG, "Reload place..." + mPlaceId);
				mPlace = mDbUtils.getPlace(mPlaceId);
			}
			if (mPlace == null)
				finish();
		}
		
		if (mPlace.state == Places.STATE_SUGGESTED)
			updateListForSuggestedPlace();
		else
			updateListDefault();
	}
	
	private void updatePlaceName() {
		String name = mPlace.name;
		mNameView.setText(name);
	}
	
	private void updateMapView(LociCircleArea circle) {
		
		mMapView.setClickable(true);
		mMapView.setSatellite(false);
		mMapView.getOverlays().clear();
		
		if (circle != null) {
						
			// check if the coordinate is valid?
			if (LocationUtils.isValidGeoPoint(circle.getGeoPoint())) {
				// draw overlay
				MapController mc = mMapView.getController();
				mc.setZoom(16);
				mc.setCenter(circle.getGeoPoint());
	   		DotOverlay dotOverlay = new DotOverlay(circle.getLocation());
    		dotOverlay.setTapAction(this, getMapEditViewIntent(circle, false));
    		mMapView.getOverlays().clear();
    		mMapView.getOverlays().add(dotOverlay);
			}
		}
	}
		
	private Intent getMapEditViewIntent(LociCircleArea circle, boolean movable) {
		Intent intent = new Intent(this, MapEditViewActivity.class);
		intent.putExtra(Constants.Intents.UI.MAP_EDIT_CIRCLE_EXTRA_KEY, circle);
		intent.putExtra(Constants.Intents.UI.MAP_EDIT_MODE_EXTRA_KEY, movable);
		return intent;
	}
	
	
	private void updateListDefault() {
		updatePlaceName();
		
		if (mPlace.areas != null)
			updateMapView(mPlace.areas.get(0));
		
		startEntityQuery();
	}
	
	private void updateListForSuggestedPlace() {
		
		updatePlaceName();
		updateMapView(mDbUtils.getPlacePositionEstimate(mPlace.placeId));
		
		ArrayList<ViewEntry> items = new ArrayList<ViewEntry>();
		
		// recent visit time
		String recentVisitTime = getRecentVisitSubstring(); //"May 4, 3:00pm, 1hr";
		items.add(new ViewEntry(LIST_ACTION_VIEW_VISITS, R.drawable.ic_clock_strip_desk_clock, "View recent visits", recentVisitTime, null));
	
		String apsAbstract = "Not available";
		// wifis
		if (mPlace.wifis.size() > 0)
			//apsAbstract = getWifiInfoSubstring(5, mPlace.wifis.get(0).toJsonString());
			apsAbstract = mPlace.wifis.get(0).getWifiInfoSubstring(5);
			
		//String apsAbstract = getWifiInfoSubstring(5); //"CENSTemp, UCLAWAN, Go away this Wifi is mine and not yours";
		ViewEntry entry = new ViewEntry(LIST_ACTION_VIEW_WIFIS, R.drawable.ic_settings_wireless, "View Wi-Fi APs", apsAbstract, null);
		try {
			if (mPlace.wifis != null && mPlace.wifis.size() > 0)
				entry.extra_string = mPlace.wifis.get(0).toJsonObject().toString();
			else
				entry.extra_string = "";
		} catch (JSONException e) {
			MyLog.e(LociConfig.D.JSON, TAG, "updateListForSuggestedPlace: json failed.");
			entry.extra_string = "";
		}
		
		items.add(entry);
		
		// Add
		//Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT, ContentUris.withAppendedId(Places.CONTENT_URI, mPlace.placeId));
		Intent intent = new Intent(Intents.UI.ACTION_CREATE_OR_ADDTO_FROM_SUGGESTED_PLACE, ContentUris.withAppendedId(Places.CONTENT_URI, mPlace.placeId));
		intent.putExtra(UI.TITLE_EXTRA_KEY, "Places");
		//intent.setType(Places.CONTENT_TYPE);
		intent.putExtra(UI.FILTER_STATE_EXTRA_KEY, Constants.Intents.UI.FILTER_STATE_REGISTERED);
		intent.putExtra(UI.FILTER_TYPE_EXTRA_KEY, Constants.Intents.UI.FILTER_TYPE_GPS | Constants.Intents.UI.FILTER_TYPE_WIFI);
		if (mPlace.wifis == null || mPlace.wifis.size() == 0) {
			intent.putExtra(UI.LIST_ORDER_EXTRA_KEY, PlacesList.LIST_ORDER_TYPE_NAME);
		} else {
			intent.putExtra(UI.LIST_ORDER_EXTRA_KEY, PlacesList.LIST_ORDER_TYPE_WIFI_SIMILARITY);
			intent.putExtra(UI.LIST_ORDER_EXTRA_WIFI_KEY, mPlace.wifis.get(0).toJsonString());
		}
		items.add(new ViewEntry(LIST_ACTION_ADD, R.drawable.sym_action_add, "Add place", null, intent));
		
		// delete
		items.add(new ViewEntry(LIST_ACTION_DELETE, R.drawable.ic_menu_delete, "Delete suggestion", null, new Intent()));
	
		ViewEntryAdapter adapter = new ViewEntryAdapter(this, R.layout.place_view_list_item, items);
		mListView.setAdapter(adapter);
	}
	
	private String getRecentVisitSubstring() {
		// get recent visit time
		ArrayList<LociVisit> visits = mDbUtils.getBaseVisits(mPlace.placeId, Visits.ENTER + " DESC", String.valueOf(1));
		
		String recentVisitTime = "Not available";
		
		if (visits.size() > 0) {
			long enter = visits.get(0).enter;
			long exit = visits.get(0).exit;
			
      // Pull out string in format [relative], [date]
      //CharSequence dateClause = DateUtils.formatDateRange(this, enter, enter,
      //        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
      //        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
      //recentVisitTime = dateClause + ", ";
			
      recentVisitTime = MyDateUtils.getAbrv_h_m_MMM_d(visits.get(0).enter) + ", ";
			recentVisitTime += "stayed ";
			recentVisitTime += MyDateUtils.humanReadableDuration(exit - enter, 2);
		}
	
		return recentVisitTime;
	}
	
	private CharSequence [] getAllVisits() {
		ArrayList<LociVisit> visits = mDbUtils.getBaseVisits(mPlace.placeId, Visits.ENTER + " DESC", "5");
		CharSequence [] visitLabels = new CharSequence[visits.size()];
		int count = 0;
		for (LociVisit visit : visits) {
			visitLabels[count++] = MyDateUtils.getAbrv_h_m_MMM_d(visit.enter) + ", stayed " + MyDateUtils.humanReadableDuration(visit.exit-visit.enter, 2);
		}
		
		MyLog.i(LociConfig.D.UI.LIST, TAG, "getAllVisits: count=" + count + " visits.size=" + visits.size());
		
		return visitLabels;
	}
	
	public void onQueryComplete(int token, Object cookie, final Cursor cursor) {
		final ArrayList<Entity> oldEntities = mEntities;
		(new AsyncTask<Void, Void, ArrayList<Entity>>() {
      @Override
      protected ArrayList<Entity> doInBackground(Void... params) {
          ArrayList<Entity> newEntities = new ArrayList<Entity>(cursor.getCount());
          EntityIterator iterator = Places.newEntityIterator(cursor);
          try {
              while (iterator.hasNext()) {
                  Entity entity = iterator.next();
                  newEntities.add(entity);
              }
          } finally {
              iterator.close();
          }
          return newEntities;
      }

      @Override
      protected void onPostExecute(ArrayList<Entity> newEntities) {
          if (newEntities == null) {
              // There was an error loading.
              return;
          }
          synchronized (PlaceViewActivity.this) {
              if (mEntities != oldEntities) {
                  // Multiple async tasks were in flight and we
                  // lost the race.
                  return;
              }
              mEntities = newEntities;
              mHasEntities = true;
          }
          considerBindData();
      }
		}).execute();
	}

  private void considerBindData() {
    if (mHasEntities) {
        bindData();
    }
  }
  
  private void bindData() {

    // Build up the contact entries
    buildEntries();


  }
  
  static String buildActionString(DataKind kind, ContentValues values, boolean lowerCase,
      Context context) {
	  if (kind.actionHeader == null) {
	      return null;
	  }
	  CharSequence actionHeader = kind.actionHeader.inflateUsing(context, values);
	  if (actionHeader == null) {
	      return null;
	  }
	  return lowerCase ? actionHeader.toString().toLowerCase() : actionHeader.toString();
	}

	static String buildDataString(DataKind kind, ContentValues values, Context context) {
	  if (kind.actionBody == null) {
	      return null;
	  }
	  CharSequence actionBody = kind.actionBody.inflateUsing(context, values);
	  return actionBody == null ? null : actionBody.toString();
	}
  
	public static final class ViewEntry {
		public String label = null;
		public String data = null;
		public Uri uri = null;
		
		public int action = -1;
		public int icon = -1;
		public String text = null;
		public String subtext = null;
		public Intent intent = null;
		public String extra_string = null;
		public double extra_double1 = -1;
		public double extra_double2 = -1;
		public float 	extra_float1 = -1;
		
		private ViewEntry() {
		}
		
		public ViewEntry(int action, int icon, String text, String subtext, Intent intent) {
			this.action = action;
			this.icon = icon;
			this.text = text;
			this.subtext = subtext;
			this.intent = intent;
		}
		
		public static ViewEntry fromValues(Context context, String mimeType, DataKind kind,
				long placeId, long dataId, ContentValues values) {
			final ViewEntry item = new ViewEntry();
			item.label = buildActionString(kind, values, false, context);
			item.data = buildDataString(kind, values, context);
			item.uri = ContentUris.withAppendedId(Data.CONTENT_URI, dataId);
			if (kind.iconRes > 0) {
				item.icon = kind.iconRes;
			}
			return item;
		}
	}
	
	public static final class ViewEntryAdapter extends BaseAdapter {
		
		private final List<ViewEntry> mList;
		private final LayoutInflater mInflater;
		private int mLayout;
		
		public ViewEntryAdapter(Context context, int layout, ArrayList<ViewEntry> list) {
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
			mList = list;
		}
		
		public int getCount() {
			return mList.size();
		}

		public Object getItem(int position) {
			return mList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			if (convertView == null) 
				convertView = mInflater.inflate(mLayout, parent, false);
			
			ViewEntry item = mList.get(position);
			convertView.setTag(item);
			
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			TextView line1 = (TextView) convertView.findViewById(android.R.id.text1);
			View line2 = convertView.findViewById(R.id.subtext);
			
			if (item.icon > 0)
				icon.setImageResource(item.icon);
			
			line1.setText(item.text);
			
			if (TextUtils.isEmpty(item.subtext)) {
				line2.setVisibility(View.GONE);
			} else {
				line2.setVisibility(View.VISIBLE);
				TextView subtext = (TextView) convertView.findViewById(R.id.subtext);
				subtext.setText(item.subtext);
			}
			
			return convertView;
		}

	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		if (view.getTag() instanceof ViewEntry) {
			ViewEntry item = (ViewEntry) view.getTag();
			switch (item.action) {
			case LIST_ACTION_VIEW_VISITS:
				showDialog(DIALOG_VISITS);
				break;
			case LIST_ACTION_VIEW_WIFIS:
				Bundle args = new Bundle();
				args.putString("wifi", item.extra_string);
				showDialog(DIALOG_WIFI, args);
				break;
			case LIST_ACTION_ADD:
					startActivityForResult(item.intent, SUBACTIVITY_ADD_PLACE);
				break;
			case LIST_ACTION_DELETE:
					showDialog(DIALOG_CONFIRM_DELETE, null);
				break;
			case LIST_ACTION_WEBSITE:
			case LIST_ACTION_POSTAL:
				if (item.intent != null) 	
					startActivity(item.intent);
				break;
			}
		} 
	}
	
	private static final int SUBACTIVITY_ADD_PLACE = 1;
	private static final int SUBACTIVITY_VIEW_PLACE = 2;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.d(TAG, "onActivityResult:" + String.format(" requestCode=%d resultCode=%d ", requestCode, resultCode));
		
		switch(requestCode) {
		case SUBACTIVITY_ADD_PLACE:
			if (resultCode == RESULT_OK) {
				
				long placeId = ContentUris.parseId(data.getData());
				
				if (mPlace.state == Places.STATE_SUGGESTED) {
					if (placeId != mPlaceId) {
						// A suggested place has been merged to an existing place
						// update all visit's placeId's here
						LociDbUtils dbUtils = new LociDbUtils(this);
						dbUtils.updateVisitPlaceId(mPlaceId, placeId);
						mPlaceId = placeId;
					}
				}
				
				setResult(RESULT_OK, data);
			}
			break;
		case SUBACTIVITY_VIEW_PLACE:
			if (resultCode == RESULT_OK) {
				
			}
		}
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	
  /**
   * Build up the entries to display on the screen.
   *
   * @param personCursor the URI for the contact being displayed
   */
  private final void buildEntries() {
      
    final Context context = this;
    final Sources sources = Sources.getInstance(context);

    ArrayList<ViewEntry> items = new ArrayList<ViewEntry>();
	
    int typeIcon = R.drawable.icon_question;
    String typeString = "Unknown";
    
    if (mPlace.type == Places.TYPE_GPS) {
    	typeIcon = R.drawable.icon_satellite;
    	typeString = "GPS";
    } else if (mPlace.type == Places.TYPE_WIFI) {
    	typeIcon = R.drawable.icon_wifi;
			typeString = "Wi-Fi";
    } 
		
		// detection type
    items.add(new ViewEntry(LIST_ACTION_NO_ACTION, typeIcon, "Dectection Sensor", typeString, null));

		// recent visit time
		String recentVisitTime = getRecentVisitSubstring(); //"May 4, 3:00pm, 1hr";
		items.add(new ViewEntry(LIST_ACTION_VIEW_VISITS, R.drawable.ic_clock_strip_desk_clock, "View recent visits", recentVisitTime, null));

    for (Entity entity: mEntities) {
    	final ContentValues entValues = entity.getEntityValues();
    	final String accountType = entValues.getAsString(Places.ACCOUNT_TYPE);
    	final long placeId = entValues.getAsLong(Places._ID);
 
    	for (NamedContentValues subValue : entity.getSubValues()) {
	    	final ContentValues entryValues = subValue.values;
	    	entryValues.put(Places.Data.PLACE_ID, placeId);
        final long dataId = entryValues.getAsLong(Data._ID);
        final String mimeType = entryValues.getAsString(Data.MIMETYPE);
        if (mimeType == null) continue;

        final DataKind kind = sources.getKindOrFallback(accountType, mimeType, this,
                PlacesSource.LEVEL_MIMETYPES);
        if (kind == null) continue;

        //Log.e(TAG, "buildEntries: dataId=" + dataId + ", mimeType=" + mimeType);
        
        // public ViewEntry(int action, int icon, String text, String subtext, Intent intent) {
        
        if (WifiFingerprint.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	String fingerprint = entryValues.getAsString(WifiFingerprint.FINGERPRINT);
        	long timestamp = entryValues.getAsLong(WifiFingerprint.TIMESTAMP);
        	String subtext = "Captured at " + MyDateUtils.getAbrv_MMM_d_h_m(timestamp);
        	//Log.d(TAG, fingerprint);
        	//String apsAbstract = getWifiInfoSubstring(5, fingerprint); 
        	ViewEntry item = new ViewEntry(LIST_ACTION_VIEW_WIFIS, R.drawable.ic_settings_wireless, "View Wi-Fi APs", subtext, null);
        	item.extra_string = fingerprint;
        	mWifiEntries.add(item);
        } else if (GpsCircleArea.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	double lat = entryValues.getAsDouble(GpsCircleArea.LATITUDE);
        	double lon = entryValues.getAsDouble(GpsCircleArea.LONGITUDE);
        	float  rad = entryValues.getAsFloat(GpsCircleArea.RADIUS);
        	//Log.d(TAG, "lat=" + lat + ",lon=" + lon + ",rad=" + rad);
        	ViewEntry item = new ViewEntry(-1, -1, null, null, null);
        	item.extra_double1 	= lat;
        	item.extra_double2 	= lon;
        	item.extra_float1 	= rad;
        	mGpsEntries.add(item);
        } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	ViewEntry item = ViewEntry.fromValues(context, mimeType, kind, placeId, dataId, entryValues);
        	String uri = "geo:0,0?q=" + TextUtils.htmlEncode(item.data);
        	item.intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        	item.action = LIST_ACTION_POSTAL;
        	item.text = item.label;
        	item.subtext = item.data;
        	mPostalEntries.add(item);
        } else if (Keyword.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	ViewEntry item = ViewEntry.fromValues(context, mimeType, kind, placeId, dataId, entryValues);
        	item.text = item.data;
        	item.subtext = null;
        	mTagEntries.add(item);
        } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	ViewEntry item = ViewEntry.fromValues(context, mimeType, kind, placeId, dataId, entryValues);
        	item.text = item.label;
        	item.subtext = item.data;
        	mOtherEntries.add(item);
        } else if (Website.CONTENT_ITEM_TYPE.equals(mimeType)) {
        	ViewEntry item = ViewEntry.fromValues(context, mimeType, kind, placeId, dataId,  entryValues);
        	item.uri = null;
        	item.action = LIST_ACTION_WEBSITE;
        	item.text = item.label;
        	item.subtext = item.data;
        	item.intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.data));
        	mWebsiteEntries.add(item);
        }
      }
    }
      
    for (ViewEntry item: mWifiEntries) {
    	items.add(item);
    }
    mWifiEntries.clear();
    for (ViewEntry item: mPostalEntries) {
    	items.add(item);
    }
    mPostalEntries.clear();
    for (ViewEntry item: mTagEntries) {
    	items.add(item);
    }
    mTagEntries.clear();
    for (ViewEntry item: mWebsiteEntries) {
    	items.add(item);
    }
    mWebsiteEntries.clear();
    for (ViewEntry item: mOtherEntries) {
    	items.add(item);
    }
    mOtherEntries.clear();
    // Log.d(TAG, "size of items = " + items.size());

		ViewEntryAdapter adapter = new ViewEntryAdapter(this, R.layout.place_view_list_item, items);
		mListView.setAdapter(adapter);
  }
	
  

	
	private static final int DIALOG_WIFI = 1;
	private static final int DIALOG_VISITS = 2;
	private static final int DIALOG_CONFIRM_DELETE = 3;
	private static final int DIALOG_CONFIRM_BLOCK = 4;
	private static final int DIALOG_CONFIRM_UNBLOCK = 5;
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		LayoutInflater factory = LayoutInflater.from(this);
		
		switch(id) {
		case DIALOG_WIFI:
			final View wifiView = factory.inflate(R.layout.dialog_wifi_view, null);
			final TableLayout wifiTable = (TableLayout) wifiView.findViewById(R.id.wifi_table);
			//updateWifiList(wifiTable, mPlace.wifis.get(0));
			String wifiJson = args.getString("wifi");
			//Log.d(TAG, "createDialog: " + wifiJson);
			try {
				updateWifiList(wifiTable, new LociWifiFingerprint(wifiJson));
			} catch (JSONException e) {
				MyLog.e(LociConfig.D.JSON, TAG, "updateWifiList: json failed.");
				e.printStackTrace();
				return null;
			}
			return new AlertDialog.Builder(this)
							.setIcon(R.drawable.ic_settings_wireless)
							.setTitle("Nearby Wi-Fi Access Points")
							.setView(wifiView)
							.setPositiveButton("Close", new DialogInterface.OnClickListener() {
								
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									
								}
							}).create();
		case DIALOG_VISITS:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.ic_clock_strip_desk_clock)
			.setTitle("Recent visits")
      .setItems(getAllVisits(), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {

          }
      })
      .setPositiveButton("Close", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			})
      .create();
		case DIALOG_CONFIRM_DELETE:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.ic_menu_delete)
			.setTitle("Are you sure?")
			.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					//mDbUtils.updatePlaceState(mPlaceId, Places.STATE_DELETED);

					MyLog.d(LociConfig.D.UI.DEBUG, TAG, "[delete] before");
					mDbUtils.checkPlaceTable();
					mDbUtils.checkDataTable();
					mDbUtils.deletePlace(mPlaceId);
					MyLog.d(LociConfig.D.UI.DEBUG, TAG, "[delete] after");
					mDbUtils.checkPlaceTable();
					mDbUtils.checkDataTable();
					finish();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			})
			.create();
		case DIALOG_CONFIRM_BLOCK:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
			.setTitle("Are you sure?")
			.setMessage("Visits to a blocked place will not be recognized.")
			.setPositiveButton("Block", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mDbUtils.updatePlaceState(mPlaceId, Places.STATE_BLOCKED);
					finish();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			})
			.create();
		case DIALOG_CONFIRM_UNBLOCK:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
			.setTitle("Are you sure?")
			.setPositiveButton("Unblock", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mDbUtils.updatePlaceState(mPlaceId, Places.STATE_REGISTERED);
					finish();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			})
			.create();
		}
		return null;
	}

	private ArrayList<View> mAddedRows = new ArrayList<View>();
	
	private void updateWifiList(TableLayout table, LociWifiFingerprint wifi) {
		
		ArrayList<WifiViewListItem> items = new ArrayList<WifiViewListItem>();
		
		HashMap<String, APInfoMapItem> apMap = wifi.getAps(); 
		Set<String> keys = apMap.keySet();
		Iterator<String> iter = keys.iterator();
		while(iter.hasNext()) {
			String bssid = iter.next();
			APInfoMapItem ap = apMap.get(bssid);
			items.add(new WifiViewListItem(bssid, ap.ssid, ap.rss, ap.count, ap.rssBuckets));
		}

		Collections.sort(items);
		
		table.setColumnCollapsed(0, false);
		table.setColumnCollapsed(1, true);
		table.setColumnShrinkable(0, true);
		
		for (int i=0; i<mAddedRows.size(); i++) {
			table.removeView(mAddedRows.get(i));
		}
		mAddedRows.clear();
		
		int totalCount = wifi.getScanCount();
		
		for (WifiViewListItem item : items) {
			TableRow row = new TableRow(this);
			
			TextView ssidView = new TextView(this);
			ssidView.setText(item.ssid);
			//ssidView.setText("very very very veryvery very very very very very");
			ssidView.setPadding(2, 2, 2, 2);
			ssidView.setTextColor(0xffffffff);
			
			TextView bssidView = new TextView(this);
			bssidView.setText(item.bssid);
			bssidView.setPadding(2, 2, 2, 2);
			bssidView.setTextColor(0xffffffff);

			TextView cntView = new TextView(this);
			cntView.setText("" + (item.count*100)/totalCount);
			cntView.setPadding(2, 2, 2, 2);
			cntView.setGravity(Gravity.CENTER);
			cntView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			
			TextView rssView = new TextView(this);
			rssView.setText("" + item.rss);
			rssView.setPadding(2, 2, 6, 2);
			rssView.setGravity(Gravity.CENTER);
			rssView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);

			row.addView(ssidView, new TableRow.LayoutParams(0));
			row.addView(bssidView, new TableRow.LayoutParams(1));
			row.addView(cntView, new TableRow.LayoutParams(2));
			row.addView(rssView, new TableRow.LayoutParams(3));

			//Log.d(TAG, item.ssid);
			for (int i=0; i<item.rssBuckets.length; i++) {
				TextView box = new TextView(this);
				box.setText("  ");
				box.setGravity(Gravity.RIGHT);
				box.setPadding(2, 2, 2, 2);
				box.setHeight(15);
				box.setGravity(Gravity.CENTER_VERTICAL);
				
				float colorVal = 256 * ((float) item.rssBuckets[i] / (float) wifi.getScanCount());
				//Log.d(TAG, "colorVal=" + (int) colorVal + ", " + item.histogram[i]);
				int colorValInt = ((int) colorVal) - 1;
				if (colorValInt < 0)
					colorValInt = 0;
				
				box.setBackgroundColor(0xff000000 + colorValInt);//+ 0x000000ff * (item.histogram[i]/totScan));
				box.setTextColor(0xffffffff);
			
				row.addView(box, new TableRow.LayoutParams(4+i));
			}

			row.setGravity(Gravity.CENTER);
			
			table.addView(row, new TableLayout.LayoutParams());
	  	table.setColumnStretchable(3, true);
	  	mAddedRows.add(row);
		}
		
	}
	
	public static final class WifiViewListItem implements Comparable<WifiViewListItem> {
		public String bssid = null;
		public String ssid = null;
		public int rss = 0;
		public int count = 0;
		public int [] rssBuckets = null;
		
		public WifiViewListItem(String bssid, String ssid, int rss, int count, int [] buckets) {
			this.bssid = bssid;
			this.ssid = ssid;
			this.rss = rss;
			this.count = count;
			this.rssBuckets = buckets;
		}
		
		public int compareTo(WifiViewListItem item) {
			return item.count - this.count;
		}
	}
	


	
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);

      final MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.view, menu);
      return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
      super.onPrepareOptionsMenu(menu);
      
      if (mPlace.state == Places.STATE_SUGGESTED) {
      	menu.findItem(R.id.menu_delete).setVisible(false);
      	menu.findItem(R.id.menu_edit).setVisible(false);
      	menu.findItem(R.id.menu_block).setVisible(false);
      	menu.findItem(R.id.menu_unblock).setVisible(false);
      } else if (mPlace.state == Places.STATE_BLOCKED) {
      	menu.findItem(R.id.menu_block).setVisible(false);
      	menu.findItem(R.id.menu_unblock).setVisible(true);
      } else if (mPlace.state == Places.STATE_REGISTERED) {
      	menu.findItem(R.id.menu_block).setVisible(true);
      	menu.findItem(R.id.menu_unblock).setVisible(false);
      }
      
      return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	
  	switch (item.getItemId()) {
    	case R.id.menu_edit:
    		Intent intent = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(Places.CONTENT_URI, mPlace.placeId));
        startActivity(intent);
        return true;
    	case R.id.menu_delete: 
    		showDialog(DIALOG_CONFIRM_DELETE, null);
      	return true;
      case R.id.menu_block:
      	showDialog(DIALOG_CONFIRM_BLOCK, null);
      	return true;
      case R.id.menu_unblock:
      	showDialog(DIALOG_CONFIRM_UNBLOCK, null);
      	return true;
    }	
    return false;
  }
}

/*
 * 
 * 	

	
	private static final String[] PROJECTION_PLACE = new String[] {
		Places._ID,
		Places.NAME,
		Places.TYPE,
		Places.STATE,
	};
	
	private static final String[] PROJECTION_WIFIFINGERPRINT = new String[] {
		Data._ID,
		Data.MIMETYPE,
		WifiFingerprint.FINGERPRINT,
		WifiFingerprint.TIMESTAMP,
	};
	
	private static final String[] PROJECTION_VISIT = new String[] {
		Visits._ID,
		Visits.ENTER,
		Visits.EXIT,
	};
	
	private Cursor mCursor;

mCursor = managedQuery(mUri, PROJECTION_PLACE, null, null, null);

if (mCursor.moveToFirst()) {
	do {
		long placeId = mCursor.getLong(mCursor.getColumnIndex(Places._ID));
		String name = mCursor.getString(mCursor.getColumnIndex(Places.NAME));
		int state = mCursor.getInt(mCursor.getColumnIndex(Places.STATE));
		Log.d(TAG, String.format("placeId=%d, name=%s, state=%d", placeId, name, state));

		Cursor dataC = managedQuery(Uri.parse(Places.CONTENT_URI + "/" + placeId + "/wifi"), PROJECTION_WIFIFINGERPRINT, null, null, null);
	
		if (dataC.moveToFirst()) {
			String wifi = dataC.getString(dataC.getColumnIndex(WifiFingerprint.FINGERPRINT));
			Log.d(TAG, wifi);
		}
		
		Uri visitUri = Uri.parse(Visits.CONTENT_URI + "/place/" + placeId);
		visitUri = visitUri.buildUpon().appendQueryParameter("limit", "1").build();
		Cursor visitsC = managedQuery(visitUri, PROJECTION_VISIT, null, null, null);
		
		if (visitsC.moveToFirst()) {
			long enter = visitsC.getLong(visitsC.getColumnIndex(Visits.ENTER));
			long exit = visitsC.getLong(visitsC.getColumnIndex(Visits.EXIT));
			Log.d(TAG, String.format("enter=%s exit=%s", MyDateUtils.getTimeFormatMedium(enter), MyDateUtils.getTimeFormatMedium(exit)));
		}
		
	} while (mCursor.moveToNext());
} else {
	Log.d(TAG, "mCursor is empty.");
}

*/
