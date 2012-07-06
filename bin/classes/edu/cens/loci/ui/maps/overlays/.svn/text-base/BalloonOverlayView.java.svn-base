/***
 * Copyright (c) 2010 readyState Software Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package edu.cens.loci.ui.maps.overlays;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import edu.cens.loci.R;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.ui.maps.overlays.PlacesOverlayItem.VisitTimeItem;

/**
 * A view representing a MapView marker information balloon.
 * <p>
 * This class has a number of Android resource dependencies:
 * <ul>
 * <li>drawable/balloon_overlay_bg_selector.xml</li>
 * <li>drawable/balloon_overlay_close.png</li>
 * <li>drawable/balloon_overlay_focused.9.png</li>
 * <li>drawable/balloon_overlay_unfocused.9.png</li>
 * <li>layout/balloon_overlay.xml</li>
 * </ul>
 * </p>
 * 
 * @author Jeff Gilfelt modified by Donnie H. Kim
 *
 */
public class BalloonOverlayView extends FrameLayout {

	//private static final String TAG = "BalloonOverlayView";
	
	private LinearLayout mLayout;
	private TextView mTitle;				// place name
	private ListView mList; 				// visits (times)
	private ImageView mIcon;				// place type icon
	
	private Context mCxt;
	
	/**
	 * Create a new BalloonOverlayView.
	 * 
	 * @param context - The activity context.
	 * @param balloonBottomOffset - The bottom padding (in pixels) to be applied
	 * when rendering this view.
	 */
	public BalloonOverlayView(Context context, int balloonBottomOffset) {
		super(context);

		mCxt = context;
		setPadding(10, 0, 10, balloonBottomOffset);
		mLayout = new LinearLayout(context);
		mLayout.setVisibility(VISIBLE);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.balloon_overlay, mLayout);
		mTitle = (TextView) v.findViewById(R.id.balloon_item_title);
		mList = (ListView) v.findViewById(R.id.visitList);
		mIcon  = (ImageView) v.findViewById(R.id.type_icon);
		
		ImageView close = (ImageView) v.findViewById(R.id.close_img_button);
		close.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mLayout.setVisibility(GONE);
			}
		});

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.NO_GRAVITY;

		addView(mLayout, params);
	}
	
	/**
	 * Sets the view data from a given overlay item.
	 * 
	 * @param item - The overlay item containing the relevant view data 
	 * (title and line1). 
	 */
	public void setData(PlacesOverlayItem item) {
		
		mLayout.setVisibility(VISIBLE);
		if (item.getName() != null) {
			mTitle.setVisibility(VISIBLE);
			mTitle.setText(item.getName());
		} else {
			mTitle.setVisibility(GONE);
		}
		
		if (item.getVisitTimes() != null) {
			mList.setVisibility(VISIBLE);
			mList.setFocusable(false);
			mList.setClickable(false);
			mList.setSelected(false);
			mList.setFocusableInTouchMode(false);
			mList.setDividerHeight(0);
			
			VisitTimeAdapter adapter = new VisitTimeAdapter(mCxt, R.layout.balloon_overlay_item, item.getVisitTimes());
			mList.setAdapter(adapter);
			
		} else {
			mList.setVisibility(GONE);
		}
		
		Drawable drawable = null;
		
		int type = item.getType();
		
		if (type == Places.TYPE_GPS) {
			drawable = mCxt.getResources().getDrawable(R.drawable.icon_satellite);
			mIcon.setVisibility(VISIBLE);
			mIcon.setImageDrawable(drawable);
		} else if (type == Places.TYPE_WIFI) { 
			drawable = mCxt.getResources().getDrawable(R.drawable.icon_wifi);
				mIcon.setVisibility(VISIBLE);
			mIcon.setImageDrawable(drawable);
		} else {
			mIcon.setVisibility(GONE);
		}
	}
	
	private class VisitTimeAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private ArrayList<VisitTimeItem> mList;
		private int mLayout;
		
		public VisitTimeAdapter(Context context, int layout, ArrayList<VisitTimeItem> list) {
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
			
			TextView txt1 = (TextView) convertView.findViewById(R.id.balloon_item_enter);
			TextView txt2 = (TextView) convertView.findViewById(R.id.balloon_item_exit);
			
			if (mList != null) {
				txt1.setText(mList.get(pos).enter + " ");
				txt2.setText(mList.get(pos).exit);
			}
			return convertView;
		}
		
	}
}
