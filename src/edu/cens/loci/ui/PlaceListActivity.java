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

import org.json.JSONException;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.Constants.Intents;
import edu.cens.loci.Constants.PlacesList;
import edu.cens.loci.Constants.Intents.Insert;
import edu.cens.loci.Constants.Intents.UI;
import edu.cens.loci.classes.LociPlace;
import edu.cens.loci.classes.LociVisit;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class PlaceListActivity extends ListActivity {

	private static final String TAG = "PlaceListActivity";
	
	private static final int SUBACTIVITY_NEW_PLACE = 1;
	private static final int SUBACTIVITY_VIEW_PLACE = 2;
	private static final int SUBACTIVITY_FILTER = 3;
	
	static final int MODE_VIEW = 1;
	static final int MODE_INSERT = 2;
	static final int MODE_REGISTER_SUGGESTED = 3;
	static final int MODE_PICK = 4;
	
	static final int FILTER_STATE_SUGGESTED 	= Constants.Intents.UI.FILTER_STATE_SUGGESTED;
	static final int FILTER_STATE_REGISTERED 	= Constants.Intents.UI.FILTER_STATE_REGISTERED;
	static final int FILTER_STATE_BLOCKED 		= Constants.Intents.UI.FILTER_STATE_BLOCKED;
	
	static final int FILTER_TYPE_GPS 	= Constants.Intents.UI.FILTER_TYPE_GPS;
	static final int FILTER_TYPE_WIFI = Constants.Intents.UI.FILTER_TYPE_WIFI;
	
	int mMode = MODE_VIEW; // default
	int mFilterState 	= FILTER_STATE_REGISTERED;		// default
	int mFilterType 	= FILTER_TYPE_WIFI;	// default
	String mFilterTag = "";

	int mListOrder = PlacesList.LIST_ORDER_TYPE_NAME;
	
	private ArrayList<PlaceListItem> mList;	
	private LociDbUtils mDbUtils = null;
	
	private void printMode() {
		if (LociConfig.D.UI.DEBUG) {
			switch(mMode) {
			case MODE_VIEW:
				Log.i(TAG, "mode: MODE_VIEW");
				break;
			case MODE_INSERT:
				Log.i(TAG, "mode: MODE_INSERT");
				break;
			case MODE_REGISTER_SUGGESTED:
				Log.i(TAG, "mode: MODE_REGISTER_SUGGESTED");
				break;
			case MODE_PICK:
				Log.i(TAG, "mode: MODE_PICK");
				break;
			default:
				Log.i(TAG, "mode: unknown");
			}
		}
	}
	
	private void printState() {
		if (LociConfig.D.UI.DEBUG) {
			switch(mFilterState) {
			case FILTER_STATE_SUGGESTED:
				Log.i(TAG, "state: suggested");
				break;
			case FILTER_STATE_REGISTERED:
				Log.i(TAG, "state: registered");
				break;
			case FILTER_STATE_BLOCKED:
				Log.i(TAG, "state: blocked");
				break;
			default:
				Log.i(TAG, "state: unknown");
				break;
			}
		}
	}
	
	private void printType() {
		if (LociConfig.D.UI.DEBUG) {
			switch(mFilterType) {
			case FILTER_TYPE_GPS:
				Log.i(TAG, "type: gps");
				break;
			case FILTER_TYPE_WIFI:
				Log.i(TAG, "type: wifi");
				break;
			default:
				Log.i(TAG, "type: unknown");
				break;
			}
		}
	}
	/**
	 * USE CASES:
	 * 
	 * 1. DEFAULT :
	 * 		list places and when selected, ACTION_VIEW
	 * 
	 * 		ACTION ... send ACTION_VIEW content://places/pid 
	 * 		SHOW ...
	 *    - Suggested places ... add "View GPS Places"
	 *    - Registered places ...
	 *    - Blocked places ...
	 *    - Filter by tag ...
	 * 
	 * 2. INSERT_OR_EDIT :
	 *    list "create a new place" and "registered" places, when selected, ACTION_EDIT
	 *    
	 *    ACTION ... send ACTION_EDIT content://places/pid or ACTION_INSERT content://places
	 *    - show all suggested/registered/blocked places
	 *    SHOW...
	 *    - "create a new place" and "registered places"
	 *    
	 * 3. PICK :
	 * 		list places and when selected, return with a uri.
	 * 
	 * 		ACTION ... return a data URL back to the caller
	 * 		SHOW... 
	 * 
	 * 4. SEARCH :
	 *  	TBD
	 * 
	 */
	
	@Override
	protected void onCreate(Bundle icicle) {
		
		super.onCreate(icicle);
		
		setContentView(R.layout.place_list);
		
		// Resolve the intent
		final Intent intent = getIntent();
		
		String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
		if (title != null) {
			setTitle(title);
		}
		
		String action = intent.getAction();
		
		if (Intent.ACTION_VIEW.equals(action)) {
			mMode = MODE_VIEW;
		} else if (Intents.UI.ACTION_INSERT.equals(action)) {
			mMode = MODE_INSERT;
		} else if (Intents.UI.ACTION_CREATE_OR_ADDTO_FROM_SUGGESTED_PLACE.equals(action)) {
			mMode = MODE_REGISTER_SUGGESTED;
		} else if (Intent.ACTION_PICK.equals(action)) {
			mMode= MODE_PICK;
		}
		
		MyLog.i(LociConfig.D.UI.DEBUG, TAG, String.format("List: mode=%d (%s)", mMode, action));
		
		Bundle extras = intent.getExtras();
		
		if (extras != null) {
			if (extras.containsKey(Intents.UI.FILTER_STATE_EXTRA_KEY))
				mFilterState = extras.getInt(Intents.UI.FILTER_STATE_EXTRA_KEY);
			if (extras.containsKey(Intents.UI.FILTER_TYPE_EXTRA_KEY))
				mFilterType = extras.getInt(Intents.UI.FILTER_TYPE_EXTRA_KEY);
			if (extras.containsKey(Intents.UI.FILTER_TAG_EXTRA_KEY))
				mFilterTag = extras.getString(Intents.UI.FILTER_TAG_EXTRA_KEY);
			if (extras.containsKey(Intents.UI.LIST_ORDER_EXTRA_KEY))
				mListOrder = extras.getInt(Intents.UI.LIST_ORDER_EXTRA_KEY);
		}
		
		mDbUtils = new LociDbUtils(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateList();
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	public void updateList() {
		
		String selection = getSelectionStatement();
		ArrayList<LociPlace> places = mDbUtils.getPlaces(selection);

		// if displaying places ordered by WiFi similarity
		if (mListOrder == PlacesList.LIST_ORDER_TYPE_WIFI_SIMILARITY) {
			String wifiJson = getIntent().getExtras().getString(UI.LIST_ORDER_EXTRA_WIFI_KEY);
			if (wifiJson != null) {
				try {
					places = mDbUtils.sortPlacesByWifiSimilarity(places, new LociWifiFingerprint(wifiJson));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		initIcons();
		
		if (mList == null)
			mList = new ArrayList<PlaceListItem> ();
		else
			mList.clear();
		
		printMode();
		printState();
		printType();
		
		for (LociPlace place : places) {
			
			String subtitle = "Not available";
			
			if (mListOrder == PlacesList.LIST_ORDER_TYPE_WIFI_SIMILARITY) {
				subtitle = String.format("%s%3.1f %%", "Similarity : ", Double.valueOf(place.extra_double) * 100);
			}
			else {
				// get recent visit time
				ArrayList<LociVisit> visits = mDbUtils.getBaseVisits(place.placeId, Visits.ENTER + " DESC", "1");
				if (visits.size() > 0)
					subtitle = MyDateUtils.getRelativeDate(this, visits.get(0).enter);
			}
			
			mList.add(new PlaceListItem(place.placeId, place.type, place.state, place.name, subtitle));
		}
		
		PlaceListItemAdapter adapter = new PlaceListItemAdapter(this, R.layout.place_list_item, mList);
		setListAdapter(adapter);
	}
	
	private Uri getSelectedUri(int position) {
		if (position == ListView.INVALID_POSITION) {
			throw new IllegalArgumentException("Position not in list bound");
		}
		final long placeId = mList.get(position).placeId;
		return ContentUris.withAppendedId(Places.CONTENT_URI, placeId);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		Intent intent;
		Bundle extras;
		
		switch(mMode) {
		case MODE_VIEW:
			//Log.i(TAG, "onListItemClick : position=" + position + ", id=" + id);
			if ((mFilterState & FILTER_STATE_SUGGESTED) != 0) {
				if (position == 0) {
					intent = new Intent(Constants.Intents.UI.ACTION_VIEW_SUGGESTED_GPS_PLACES);
					Toast.makeText(this, "Not supported yet", Toast.LENGTH_SHORT).show();
					return;
				} else {
					intent = new Intent(Intent.ACTION_VIEW, getSelectedUri(position-1));
				}
			} else {
				intent = new Intent(Intent.ACTION_VIEW, getSelectedUri(position));
			}
			startActivity(intent);
			break;
		case MODE_INSERT:
			extras = getIntent().getExtras();
			if (position == 0) {
				intent = new Intent(Intent.ACTION_INSERT, Places.CONTENT_URI);
				if (extras.containsKey(UI.LIST_ORDER_EXTRA_WIFI_KEY) && extras.containsKey(UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY)) {
					String wifiJson = getIntent().getExtras().getString(UI.LIST_ORDER_EXTRA_WIFI_KEY);
					String timestamp = getIntent().getExtras().getString(UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY);
					intent.putExtra(Insert.WIFI_FINTERPRINT, wifiJson);
					intent.putExtra(Insert.TIME, timestamp);
				}
				//intent = new Intent(Intent.ACTION_EDIT, getSelectedUri(position));
			} else {
				intent = new Intent(Intent.ACTION_EDIT, getSelectedUri(position-1));
				if (extras.containsKey(UI.LIST_ORDER_EXTRA_WIFI_KEY) && extras.containsKey(UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY)) {
					String wifiJson = getIntent().getExtras().getString(UI.LIST_ORDER_EXTRA_WIFI_KEY);
					String timestamp = getIntent().getExtras().getString(UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY);
					intent.putExtra(Insert.WIFI_FINTERPRINT, wifiJson);
					intent.putExtra(Insert.TIME, timestamp);
				}
			}
			//extras = getIntent().getExtras();
			//if (extras != null) 
			//	intent.putExtras(extras);
		
			startActivityForResult(intent, SUBACTIVITY_NEW_PLACE);
			break;
		case MODE_REGISTER_SUGGESTED:
			extras = getIntent().getExtras();
			if (position == 0) {
				intent = new Intent(Intents.UI.ACTION_CREATE_FROM_SUGGESTED_PLACE, getIntent().getData());
			} else {
				//intent = new Intent(Intents.UI.ACTION_ADDTO_FROM_SUGESTED_PLACE, getIntent().getData());
				//extras.putLong(Intents.UI.SELECTED_PLACE_ID_EXTRA_KEY, getSelectedPlaceId(position-1));
				intent = new Intent(Intents.UI.ACTION_ADDTO_FROM_SUGESTED_PLACE, getSelectedUri(position-1));
				extras.putLong(Intents.UI.SELECTED_PLACE_ID_EXTRA_KEY, ContentUris.parseId(getIntent().getData()));
			}
			if (extras != null) 
				intent.putExtras(extras);

			startActivityForResult(intent, SUBACTIVITY_NEW_PLACE);
			break;
		case MODE_PICK:
			final Uri uri = getSelectedUri(position);
			intent = new Intent();
			setResult(RESULT_OK, intent.setData(uri));
			finish();
			break;
		}

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		MyLog.i(LociConfig.D.UI.DEBUG, TAG, "onActivityResult:" + String.format(" requestCode=%d resultCode=%d ", requestCode, resultCode));
		if (data != null)
			MyLog.d(LociConfig.D.UI.DEBUG, TAG, "onActivityResult:" + data.toString());
		
		switch(requestCode) {
		case SUBACTIVITY_NEW_PLACE:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
				finish();
			}
			break;
		case SUBACTIVITY_VIEW_PLACE:
			if (resultCode == RESULT_OK) {
				
			}
		case SUBACTIVITY_FILTER:
			if (resultCode == RESULT_OK) {
				setResult(RESULT_OK, data);
				finish();
			}
		}
	}

	
	public static final class PlaceListItem {
		public long placeId;
		public int type;
		public int state;
		public String text1;
		public String text2;
		
		public PlaceListItem(long placeId, int type, int state, String text1, String text2) {
			this.placeId = placeId;
			this.type = type;
			this.state = state;
			this.text1 = text1;
			this.text2 = text2;
		}
		
		public String toString() {
			String s = String.format("PlaceListItem [placeId=%d, type=%d, state=%d, text1=%s, text2=%s]", this.placeId, this.type, this.state, this.text1, this.text2); 
			return s;
		}
		
	}
	
	private Bitmap mSatIcon;
	private Bitmap mWifiIcon;
	private Bitmap mUnknownIcon;
	
	private Bitmap mRegisteredIcon;
	private Bitmap mSuggestedIcon;
	private Bitmap mBlockedIcon;
	
	public void initIcons() {
		
		int resId;
		
		if (mSatIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_satellite", null, null);
			mSatIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
		if (mWifiIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_wifi", null, null);
			mWifiIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
		if (mUnknownIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_question", null, null);
			mUnknownIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
		if (mRegisteredIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_check", null, null);
			mRegisteredIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
		if (mSuggestedIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_suggest", null, null);
			mSuggestedIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
		if (mBlockedIcon == null) {
			resId = getResources().getIdentifier("edu.cens.loci:drawable/icon_block", null, null);
			mBlockedIcon = overlay(BitmapFactory.decodeResource(getResources(), resId));
		}
	}
	
	public class PlaceListItemAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private ArrayList<PlaceListItem> mList;
		private int mLayout;
		
		public PlaceListItemAdapter(Context context, int layout, ArrayList<PlaceListItem> list) {
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
			mList = list;
		}
		
		public int getCount() {
			if (mMode == MODE_INSERT || mMode == MODE_REGISTER_SUGGESTED) 
				return mList.size() + 1;
			else if (mMode == MODE_VIEW && (mFilterState & FILTER_STATE_SUGGESTED) != 0 && (mFilterType & FILTER_TYPE_GPS) != 0)
				return mList.size() + 1;
			else
				return mList.size();
		}

		public Object getItem(int position) {
			if (mMode == MODE_INSERT || mMode == MODE_REGISTER_SUGGESTED)
				if (position == 0)
					return null;
				else
					return mList.get(position-1);
			else if (mMode == MODE_VIEW && (mFilterState & FILTER_STATE_SUGGESTED) != 0 && (mFilterType & FILTER_TYPE_GPS) != 0)
				if (position == 0)
					return null;
				else
					return mList.get(position-1);
			else
				return mList.get(position);
		}

		public long getItemId(int position) {
			if (mMode == MODE_INSERT || mMode == MODE_REGISTER_SUGGESTED)
				if (position == 0)
					return -1;
				else
					return position-1;
			else if (mMode == MODE_VIEW && (mFilterState & FILTER_STATE_SUGGESTED) != 0 && (mFilterType & FILTER_TYPE_GPS) != 0)
				if (position == 0)
					return -1;
				else
					return position;
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			int pos = position;
			
			Bitmap icon = null;
			
			// when "insert" mode, adding create new place
			if (position == 0 && (mMode == MODE_INSERT || mMode == MODE_REGISTER_SUGGESTED)) {
			 return mInflater.inflate(R.layout.create_new_place, parent, false);	
			}
			
			// when viewing suggested places, adding suggested GPS places
			if (mMode == MODE_VIEW && (mFilterState & FILTER_STATE_SUGGESTED) != 0 && (mFilterType & FILTER_TYPE_GPS) != 0) {

				if (position == 0) {
					convertView = mInflater.inflate(mLayout, parent, false);
					icon = mSatIcon;
					ImageView imageView = (ImageView) convertView.findViewById(R.id.type_icon);
					imageView.setImageBitmap(icon);
					
					TextView txt1 = (TextView) convertView.findViewById(R.id.text1);
					txt1.setText("View GPS places");
					
					TextView txt2 = (TextView) convertView.findViewById(R.id.text2);
					txt2.setText("View suggested GPS places");
					
					return convertView;
				} else {
					pos = position -1;
				}
			} else if (mMode == MODE_INSERT || mMode == MODE_REGISTER_SUGGESTED) {
				pos = position - 1;
			} 
			
			convertView = mInflater.inflate(mLayout, parent, false);

			//ImageView typeIcon = (ImageView) convertView.findViewById(R.id.type_icon);
			//ImageView stateIcon = (ImageView) convertView.findViewById(R.id.state_icon);
			
			switch (mList.get(pos).type) {
			case Places.TYPE_GPS:
				//typeIcon.setImageBitmap(mSatIcon);
				icon = mSatIcon;
				break;
			case Places.TYPE_WIFI:
				//typeIcon.setImageBitmap(mWifiIcon);
				icon = mWifiIcon;
				break;
			default:
				//typeIcon.setImageBitmap(mUnknownIcon);
				icon = mUnknownIcon;
				break;
			}
			
			switch (mList.get(pos).state) {
			case Places.STATE_SUGGESTED:
				//stateIcon.setImageBitmap(mSuggestedIcon);
				icon = overlay(icon, mSuggestedIcon);
				break;
			case Places.STATE_REGISTERED:
				//stateIcon.setImageBitmap(mRegisteredIcon);
				icon = overlay(icon, mRegisteredIcon);
				break;
			case Places.STATE_BLOCKED:
				//stateIcon.setImageBitmap(mBlockedIcon);
				icon = overlay(icon, mBlockedIcon);
				break;
			default:
				//stateIcon.setImageBitmap(mUnknownIcon);
				icon = overlay(icon, mUnknownIcon);
			}
			
			ImageView imageView = (ImageView) convertView.findViewById(R.id.type_icon);
			imageView.setImageBitmap(icon);
			
			TextView txt1 = (TextView) convertView.findViewById(R.id.text1);
			txt1.setText(mList.get(pos).text1);
			
			TextView txt2 = (TextView) convertView.findViewById(R.id.text2);
			txt2.setText(mList.get(pos).text2);
			
			return convertView;
		}
	}
	
	private Bitmap overlay(Bitmap... bitmaps) {
		
		if (bitmaps[0].equals(null))
			return null;

		Bitmap bmOverlay = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_4444);

		Canvas canvas = new Canvas(bmOverlay);
		for (int i = 0; i < bitmaps.length; i++)
			canvas.drawBitmap(bitmaps[i], new Matrix(), null);

		return bmOverlay;
	}
	
	
	public String getSelectionStatement() {
		String select = "";
		ArrayList<String> selects = new ArrayList<String>();
		
		String stateSelect = getStateSelectionStatement(mFilterState);
		String typeSelect = getTypeSelectionStatement(mFilterType);
	
		if (!TextUtils.isEmpty(stateSelect))
			selects.add(stateSelect);
		if (!TextUtils.isEmpty(typeSelect))
			selects.add(typeSelect);
		
		int len = selects.size();
		
		for (int i=0; i<len; i++) {
			select += ("(" + selects.get(i) + ")");
			if (i < len-1)
				select += " AND ";
		}
		
		return select;
	}

	private String getStateSelectionStatement(int state) {
		
		String select = "";
		ArrayList<String> states = new ArrayList<String>();
		
		if ((state & FILTER_STATE_SUGGESTED) != 0) {
			states.add(Places.PLACE_STATE + "=" + Places.STATE_SUGGESTED);
		}
		if ((state & FILTER_STATE_REGISTERED) != 0) {
			states.add(Places.PLACE_STATE + "=" + Places.STATE_REGISTERED);
		}
		if ((state & FILTER_STATE_BLOCKED) != 0) {
			states.add(Places.PLACE_STATE + "=" + Places.STATE_BLOCKED);
		}
		
		int len = states.size();
		
		for (int i=0; i<len; i++) {
			select += states.get(i);
			if (i < len-1)
				select += " OR ";
		}
		
		return select;
	}

	private String getTypeSelectionStatement(int type) {
		String select = "";
		ArrayList<String> types = new ArrayList<String>();
		
		if ((type & FILTER_TYPE_GPS) != 0) {
			types.add(Places.PLACE_TYPE + "=" + Places.TYPE_GPS);
		}
		if ((type & FILTER_TYPE_WIFI) != 0) {
			types.add(Places.PLACE_TYPE + "=" + Places.TYPE_WIFI);
		}
		
		int len = types.size();
		
		for (int i=0; i<len; i++) {
			select += types.get(i);
			if (i < len-1)
				select += " OR ";
		}
		
		return select;
	}
	
}
