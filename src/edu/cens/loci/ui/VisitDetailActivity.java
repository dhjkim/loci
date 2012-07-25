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

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.Constants.Intents;
import edu.cens.loci.Constants.PlacesList;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.LociVisit.RecognitionResult;
import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.Visits;
import edu.cens.loci.ui.PlaceViewActivity.ViewEntry;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class VisitDetailActivity extends ListActivity implements 
				AdapterView.OnItemClickListener {

	private static final String TAG = "VisitDetail";
	
	private TextView mPlaceName;
	private ImageView mPlaceTypeIcon;
	private TextView mVisitTime;
	private TextView mVisitDuration;

	LayoutInflater mInflater;
	Resources mResources;
	
	static final String[] VISIT_LOG_PROJECTION = new String[] {
		Visits.PLACE_ID,
		Visits.ENTER,
		Visits.EXIT,
		Visits.TYPE,
		Visits.EXTRA1,
		Visits.EXTRA2
	};
	
	static final int PLACE_ID_INDEX = 0;
	static final int ENTER_INDEX = 1;
	static final int EXIT_INDEX = 2;
	static final int TYPE = 3;
	static final int EXTRA1_INDEX = 4;
	static final int EXTRA2_INDEX = 5;
	
	static final String[] PLACE_PROJECTION = new String[] {
		Places.PLACE_NAME,
		Places.PLACE_STATE
	};
	
	static final int PLACE_NAME_INDEX = 0;
	static final int PLACE_STATE_INDEX = 1;
	
	private LociDbUtils mDbUtils;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.visit_detail);
		
		mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mResources = getResources();
		
		mPlaceName = (TextView) findViewById(R.id.name);
		mPlaceTypeIcon = (ImageView) findViewById(R.id.icon);
		mVisitTime = (TextView) findViewById(R.id.time);
		mVisitDuration = (TextView) findViewById(R.id.duration);

		getListView().setOnItemClickListener(this);
		
		mDbUtils = new LociDbUtils(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateData(getIntent().getData());
	}
	
	private void updateData(Uri visitUri) {
		ContentResolver resolver = getContentResolver();
		Cursor visitCursor = resolver.query(visitUri, VISIT_LOG_PROJECTION, null, null, null);
		try {
			if (visitCursor != null && visitCursor.moveToFirst()) {
				long placeId = visitCursor.getLong(PLACE_ID_INDEX);
				long enter = visitCursor.getLong(ENTER_INDEX);
				long exit  = visitCursor.getLong(EXIT_INDEX);
				int type = visitCursor.getInt(TYPE);
				String extra1 = visitCursor.getString(EXTRA1_INDEX);
				String extra2 = visitCursor.getString(EXTRA2_INDEX);
				
				MyLog.d(LociConfig.D.UI.DEBUG, TAG, String.format("[updateData] placeId=%d enter=%s exit=%s type=%d extra1=%s extra2=%s", placeId, MyDateUtils.getTimeFormatLong(enter), MyDateUtils.getTimeFormatLong(exit), type, extra1, extra2));
				
				// Place name
				String place_name = "";
				int place_state = 0;
				
				if (placeId > 0) {
					String placeSelection = Places._ID + "=" + placeId;
					Uri placeUri = ContentUris.withAppendedId(Places.CONTENT_URI, placeId);
					Cursor placeCursor = resolver.query(placeUri, PLACE_PROJECTION, placeSelection, null, null);
					
					if (placeCursor != null && placeCursor.moveToFirst()) {
						place_name = placeCursor.getString(PLACE_NAME_INDEX);
						place_state = placeCursor.getInt(PLACE_STATE_INDEX);
						placeCursor.close();
					} else {
						place_name = "Unknown";
					}
				} else {
					place_name = "Unknown";
				}
				mPlaceName.setText(place_name);
				
				// Pull out string in format [relative], [date]
				CharSequence dateClause = DateUtils.formatDateRange(this, enter, enter, 
							DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
							DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
				
				mVisitTime.setText(dateClause);
				
				// Set the duration
				mVisitDuration.setText(formatDuration((exit-enter)/1000));
				
				switch(type) {
				case Visits.TYPE_GPS:
					mPlaceTypeIcon.setImageResource(R.drawable.icon_satellite);
					break;
				case Visits.TYPE_WIFI:
					mPlaceTypeIcon.setImageResource(R.drawable.icon_wifi);
					break;
				}
				
				List<ViewEntry> actions = new ArrayList<ViewEntry>();
				
				// View place
				Intent viewPlaceIntent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(Places.CONTENT_URI, placeId));
				String placeViewLabel = "";
				
				MyLog.d(LociConfig.D.UI.DEBUG, TAG, String.format("[updateData] placename=%s placestate=%d", place_name, place_state));
				
				switch(place_state) {
				case Places.STATE_SUGGESTED:
					placeViewLabel = "Handle Suggested Place";
					break;
				case Places.STATE_REGISTERED:
					placeViewLabel = "View " + place_name;
					break;
				default:
					placeViewLabel = null;//place_name + " state " + place_state;
				}
				
				if (placeViewLabel != null) {
					ViewEntry entry = new ViewEntry(LIST_ACTION_VIEW_PLACE, R.drawable.sym_action_map, placeViewLabel, null, null);
					entry.intent = viewPlaceIntent;
					actions.add(entry);
				} 
				
				// View Wifi APs
				if (type == Visits.TYPE_WIFI && extra1 != null) {
					
					LociWifiFingerprint wifi;
					String apsAbstract = "Not available";
					
					try {
						wifi = new LociWifiFingerprint(extra1);
						apsAbstract = wifi.getWifiInfoSubstring(5);
						ViewEntry wifiEntry = new ViewEntry(LIST_ACTION_VIEW_WIFIS, R.drawable.ic_settings_wireless, "View Wi-Fi APs", apsAbstract, null);
						wifiEntry.extra_string = extra1;
						
						actions.add(wifiEntry);
					} catch (JSONException e) {
						MyLog.e(LociConfig.D.JSON, TAG, "wifi json failed : " + extra1);
						e.printStackTrace();
					}
				}
				
				// Additional Actions
				if (placeId > 0) {
					if (place_state == Places.STATE_REGISTERED || place_state == Places.STATE_BLOCKED) {
						if (type == Visits.TYPE_WIFI && extra1 != null) {
							ViewEntry entry = new ViewEntry(LIST_ACTION_CHANGE_PLACE, android.R.drawable.ic_menu_edit, "Change Place", null, null);
							entry.intent = new Intent(Intents.UI.ACTION_INSERT);
							entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_KEY, PlacesList.LIST_ORDER_TYPE_WIFI_SIMILARITY);
							entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_WIFI_KEY, extra1);
							entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY, String.valueOf(enter));
							entry.intent.putExtra(Intents.UI.PLACE_ENTRY_TYPE_EXTRA_KEY, Places.ENTRY_WIFI_OVERWRITE_WRONG_RECOGNITION);
							actions.add(entry);
						}
					}
				} else {
					ViewEntry entry = new ViewEntry(LIST_ACTION_ADD_PLACE, R.drawable.sym_action_add, "Add Place", null, null);
					entry.intent = new Intent(Intents.UI.ACTION_INSERT);
					entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_KEY, PlacesList.LIST_ORDER_TYPE_WIFI_SIMILARITY);
					entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_WIFI_KEY, extra1);
					entry.intent.putExtra(Intents.UI.LIST_ORDER_EXTRA_WIFI_TIME_KEY, String.valueOf(enter));
					entry.intent.putExtra(Intents.UI.PLACE_ENTRY_TYPE_EXTRA_KEY, Places.ENTRY_WIFI_USE_SHORT_VISIT);
					actions.add(entry);
				}
				
					
				// View Recognition Results
				//Log.d(TAG, "recog: " + extra2);
				if (extra2 != null && !TextUtils.isEmpty(extra2)) {
					ViewEntry recogEntry = new ViewEntry(LIST_ACTION_VIEW_RECOGNITION, R.drawable.ic_clock_strip_desk_clock, "View Recogntion Results", "", null);
					recogEntry.extra_string = extra2;
					actions.add(recogEntry);
				}
				
				ViewAdapter adapter = new ViewAdapter(this, actions);
				setListAdapter(adapter);
				
				//Log.d(TAG, String.format("placeId=%d enter=%s exit=%s", placeId, MyDateUtils.getDateFormatLong(enter), MyDateUtils.getDateFormatLong(exit)));
				//Log.d(TAG, String.format("extra1=%s", extra1));
				//Log.d(TAG, String.format("extra2=%s", extra2));
			}
			
		} finally {
			if (visitCursor != null) {
				visitCursor.close();
			}
		}
	}
	
	private static final int LIST_ACTION_NO_ACTION = 1;
	private static final int LIST_ACTION_VIEW_PLACE = 2;
	private static final int LIST_ACTION_VIEW_WIFIS = 3;
	private static final int LIST_ACTION_VIEW_RECOGNITION = 4;
	private static final int LIST_ACTION_ADD_PLACE = 5;
	private static final int LIST_ACTION_CHANGE_PLACE = 6;
	
	private String formatDuration(long elapsedSeconds) {
		long hours = 0;
		long minutes = 0;
		//long seconds = 0;
		
		if (elapsedSeconds >= 3600) {
			hours = elapsedSeconds / 3600;
			//minutes = elapsedSeconds / 60;
			elapsedSeconds -= hours * 3600;
			//elapsedSeconds -= minutes * 60;
		}
		//seconds = elapsedSeconds;
		minutes = elapsedSeconds/60;
		
		return getString(R.string.visitDetailsDurationFormat, hours, minutes);
		
	}
	
	static final class ViewAdapter extends BaseAdapter {
		private final List<ViewEntry> mActions;
		private final LayoutInflater mInflater;
		
    public ViewAdapter(Context context, List<ViewEntry> actions) {
      mActions = actions;
      mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  public int getCount() {
      return mActions.size();
  }

  public Object getItem(int position) {
      return mActions.get(position);
  }

  public long getItemId(int position) {
      return position;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
      // Make sure we have a valid convertView to start with
      if (convertView == null) {
          convertView = mInflater.inflate(R.layout.visit_detail_list_item, parent, false);
      }

      // Fill action with icon and text.
      ViewEntry entry = mActions.get(position);
      convertView.setTag(entry);

      ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
      TextView text = (TextView) convertView.findViewById(android.R.id.text1);

      icon.setImageResource(entry.icon);
      text.setText(entry.text);

      View line2 = convertView.findViewById(R.id.line2);
      boolean labelEmpty = TextUtils.isEmpty(entry.label);
      if (labelEmpty) {
          line2.setVisibility(View.GONE);
      } else {
          line2.setVisibility(View.VISIBLE);

          TextView label = (TextView) convertView.findViewById(R.id.text2);
          if (labelEmpty) {
              label.setVisibility(View.GONE);
          } else {
              label.setText(entry.label);
              label.setVisibility(View.VISIBLE);
          }
      }

      return convertView;
  	}
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Handle passing action off to correct handler.
		if (view.getTag() instanceof ViewEntry) {
			ViewEntry entry = (ViewEntry) view.getTag();
			
			switch (entry.action) {
			case LIST_ACTION_VIEW_WIFIS:
				Bundle args = new Bundle();
				args.putString("wifi", entry.extra_string);
				//Log.d(TAG, "save:" + entry.extra_string);
				showDialog(DIALOG_WIFI, args);
				break;
			case LIST_ACTION_VIEW_PLACE:
				if (entry.intent != null) {
					startActivity(entry.intent);
				}
				break;
			case LIST_ACTION_VIEW_RECOGNITION:
				args = new Bundle();
				args.putString("recognition", entry.extra_string);
				showDialog(DIALOG_RECOGNITION, args);
			case LIST_ACTION_NO_ACTION:
				break;
			case LIST_ACTION_ADD_PLACE:
				if (entry.intent != null) {
					startActivityForResult(entry.intent, SUBACTIVITY_ADD_PLACE);
				}
				break;
			case LIST_ACTION_CHANGE_PLACE:
				if (entry.intent != null) {
					args = new Bundle();
					args.putParcelable("intent", entry.intent);
					showDialog(DIALOG_CHANGE_PLACE, args);
				}
				break;
			}
		}
	}
	
	private static final int SUBACTIVITY_ADD_PLACE = 1;
	private static final int SUBACTIVITY_CHANGE_PLACE = 2;
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		MyLog.d(LociConfig.D.UI.DEBUG, TAG, "onActivityResult:" + String.format(" requestCode=%d resultCode=%d ", requestCode, resultCode));
		if (data != null)
			MyLog.d(LociConfig.D.UI.DEBUG, TAG, "onActivityResult:" + data.toString());
		
		switch(requestCode) {
		case SUBACTIVITY_ADD_PLACE:
			if (resultCode == RESULT_OK) {
				// created or add to existing place successfully. update visits' placeid.
				final long placeId = ContentUris.parseId(data.getData());
				final long visitId = ContentUris.parseId(getIntent().getData());
				MyLog.d(LociConfig.D.UI.DEBUG, TAG, "[Update] placeId=" + placeId + ", visitId=" + visitId);
				mDbUtils.updateVisitPlaceId(visitId, placeId);
			}
			break;
		case SUBACTIVITY_CHANGE_PLACE:
			if (resultCode == RESULT_OK) {
				// created or add to existing place successfully. update visits' placeid
				//MyLog.d(LociConfig.D.UI.DEBUG, TAG, "[VisitDetail] data:" + data.getData().toString());
				final long placeId = ContentUris.parseId(data.getData());
				final long visitId = ContentUris.parseId(getIntent().getData());
				MyLog.d(LociConfig.D.UI.DEBUG, TAG, "[Update] placeId=" + placeId + ", visitId=" + visitId);
				mDbUtils.updateVisitPlaceId(visitId, placeId);
			}
		}
	}
	
	private static final int DIALOG_WIFI = 1;
	private static final int DIALOG_RECOGNITION = 2;
	private static final int DIALOG_CHANGE_PLACE = 3;
	
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
									
								}
							}).create();
		case DIALOG_RECOGNITION:
			final View recognitionView = factory.inflate(R.layout.dialog_recogresult_view, null);
			final ListView recognitionList = (ListView) recognitionView.findViewById(android.R.id.list);
			updateRecognitionList(recognitionList, args.getString("recognition"));
			return new AlertDialog.Builder(this)
							.setIcon(R.drawable.ic_clock_strip_desk_clock)
							.setTitle("Recognition Results")
							.setView(recognitionView)
							.setPositiveButton("Close", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									
								}
							}).create();
		case DIALOG_CHANGE_PLACE:
			
			CharSequence[] items = {getResources().getString(R.string.visitDetail_addWiFiPlace)};
			final Intent intent = args.getParcelable("intent");
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_menu_edit)
			.setTitle("Change place")
			.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    /* User clicked so do some stuff */
                    switch(which) {
                    case 0:
                    	startActivityForResult(intent, SUBACTIVITY_CHANGE_PLACE);
                    default:
                    	break;
                    }

                }
            }).create();
		}
		return null;
	}

	private void updateRecognitionList(ListView list, String jsonString) {
			try {
				JSONArray jArr = new JSONArray(jsonString);
				
				ArrayList<RecognitionResult> recogs = new ArrayList<RecognitionResult>();
		
				LociDbUtils dbUtils = new LociDbUtils(this);
				
				long pid = -1;
				String pname = "";
				
				for (int i=0; i<jArr.length(); i++) {
					
					RecognitionResult recog = new RecognitionResult(jArr.getJSONObject(i));
					if (pid != recog.placeId) {
						pid = recog.placeId;
						if (pid > 0)
							pname = dbUtils.getPlace(pid).name;
						else
							pname = "Unknown";
					}
					recog.placeName = pname;
					
					recogs.add(recog);
					
					//recogs.add(new RecognitionResult(jArr.getJSONObject(i)).setPlaceName(dbUtils));
					
				}
				
				//MyLog.e(true, TAG, "# of recog results returned : " + recogs.size());
				RecogResultAdapter adapter = new RecogResultAdapter(this, R.layout.dialog_recogresult_item, recogs);
				list.setAdapter(adapter); 
			
			}	catch (JSONException e) {
				e.printStackTrace();
			}
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
	
	public class RecogResultAdapter extends BaseAdapter {
		
		private LayoutInflater mInflater;
		private ArrayList<RecognitionResult> mList;
		private int mLayout;

		public RecogResultAdapter(Context context, int layout, ArrayList<RecognitionResult> list) {
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

			final int pos = position;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mLayout, parent, false);
			}

			TextView txt1 = (TextView) convertView.findViewById(R.id.recogTime);
			txt1.setText(MyDateUtils.getTimeFormatMedium(mList.get(pos).time));

			String name = mList.get(pos).placeName;
			if (name != null) {
				TextView txt2 = (TextView) convertView.findViewById(R.id.recogPlaceName);
				txt2.setText(name);
			}
				
			TextView txt3 = (TextView) convertView.findViewById(R.id.recogSigTag);
			
			long fingerprintId = mList.get(pos).fingerprintId;
			if (fingerprintId > 0) {
				txt3.setText(MyDateUtils.getTimeFormatShort(mList.get(pos).fingerprintId));
			} else {
				txt3.setText("-");
			}
			
			TextView txt4 = (TextView) convertView.findViewById(R.id.recogScore);
			txt4.setText(String.format("%5.2f", mList.get(pos).score));
			
			return convertView;
		}
		
	}
	
	
	public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
		if (globalSearch) {
			super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
		} else {
			// ContactsSearchManager.startSearch(this, initialQuery);
		}
	}


}
