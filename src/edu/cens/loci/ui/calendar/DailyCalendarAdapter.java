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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import edu.cens.loci.R;
import edu.cens.loci.ui.calendar.DailyCalendar.ListItem;
import edu.cens.loci.utils.MyLog;

public class DailyCalendarAdapter extends BaseAdapter{

	private static final boolean LOG_CALL = false;
	private static final String TAG = "DailyCalendarAdapter";
	
	@SuppressWarnings("unused")
	private Context mCxt;
	private LayoutInflater mInflater;
	private ArrayList<ListItem> mItems;
	private int mResourceID;
	
	public DailyCalendarAdapter(Context context, int resource, ArrayList<ListItem> items) {
		this.mCxt = context;
		this.mItems = items;
		this.mResourceID = resource;
		this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int position) {
		return mItems.get(position);
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		
		MyLog.d(LOG_CALL, TAG, "getView () : pos=" + position);
		
		if(convertView == null) {
			convertView = mInflater.inflate(mResourceID, parent, false);
		}
		
		TextView entTimeTxt = (TextView) convertView.findViewById(R.id.entTime);
		entTimeTxt.setText(mItems.get(position).entTime);
		
		TextView extTimeTxt = (TextView) convertView.findViewById(R.id.extTime);
		extTimeTxt.setText(mItems.get(position).extTime);
		
		TextView nameTxt = (TextView) convertView.findViewById(R.id.name);
		
		String name = mItems.get(position).name;
		
		if (name != null) {
			nameTxt.setTextColor(0xff000000);
			nameTxt.setText(mItems.get(position).name);
		} else {
			nameTxt.setTextColor(0x50000000);
			nameTxt.setText("unknown");
		}
		return convertView;
	}
}
