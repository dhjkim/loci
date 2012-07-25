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
package edu.cens.loci.ui.maps.overlays;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.util.FloatMath;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.MyLog;

public class MapEditOverlay extends ItemizedOverlay<MapEditOverlayItem> {

	private static final String TAG = "MapEditOverlay";
	
	//Location id for the 'my location' overlay
	private static final int CURR_LOC_ID = -1;
	//Drag gesture threshold
	private static final int DRAG_THREHOLD = 16; //pixels
	//Long press action delay
	private static final int LONG_PRESS_DELAY = 500; //ms
	//Alpha value for light circles
	private static final int ALPHA_LIGHT = 20;
	//Alpha value for dark circles
	private static final int ALPHA_DARK = 50;
	
	private MapView mMapView;
	
	//Overlay list
	private ArrayList<MapEditOverlayItem> mOverlays = 
		new ArrayList<MapEditOverlayItem>();
	
	//Flag to check if the user if touching the screen
	private boolean mTouching = false;
	//Flag to check if a drag action is being performed
	private boolean mDragging = false;
	//The number of runnables posted while a touch down even is 
	//received. Only the last runnable needs to handled.
	private int mPendingRunnables = 0;
	//The point which was touched
	private Point mTouchPoint = new Point();
	//The point where a drag operation is being performed
	private Point mDragPoint = new Point();
	//Flag to check if the circle around a marker is being resized
	private boolean mCircleResizing = false;
	//The radius if a circle being resized 
	@SuppressWarnings("unused")
	private float mDragItemRadius = 50;
	
	
	//Index of 'my location' (blue marker) overlay in the list
	private int mMyLocOverlayIndex = -1;
	//The marker drawables
	private Drawable mBluMarker;
	private Drawable mRedMarker;
	
	//Thread handler (for posting runnables)
	private Handler mHandler = new Handler();
	
	
 // for marker dragging
	@SuppressWarnings("unused")
	private MapEditOverlayItem mInDrag = null;
	
	private boolean mMovable = false;
	
	public MapEditOverlay(Drawable defaultMarker, MapView mapView, boolean movable) {
		super(boundCenter(defaultMarker));
		
		mMapView 		= mapView;
		mRedMarker 	= defaultMarker;
		mMovable 		= movable;
		
		populate();
	}
	
	@Override
	protected MapEditOverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	
	/* Handle long press on a marker. Display the 'delete' message */
	private void handleMarkerLongPress(int locId) {
		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "handleMarkerLongPress");
	}
	
  /* Handle long press on maps. Display the 'add location' balloon */
	private void handleLongPress(Point p) {

		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "handleLongPress()");
		
		if (!mMovable)
			return;
		
		GeoPoint gp = mMapView.getProjection().fromPixels(p.x, p.y);
		
		if (mOverlays.size() == 0) {
			
			MapEditOverlayItem newItem = new MapEditOverlayItem(gp, 1, LociConfig.pMinRadius);
			this.addOverlay(newItem);
		}
	}
	
	private void handleShortPress(Point p) {
		
		if (!mMovable)
			return;
			
		MapEditOverlayItem item = getFocus();
		
		if (item != null) {
			
			MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "handleShortPress: item selected");
			
			GeoPoint gp = mMapView.getProjection().fromPixels(p.x, p.y);

			MyLog.i(true, TAG, "old pos = " + item.point.toString());
			
			MapEditOverlayItem newItem = new MapEditOverlayItem(gp, item.locationId, item.radius);

			MyLog.i(true, TAG, "new pos = " + newItem.point.toString());

			this.removeOverlay(item.locationId);
			this.addOverlay(newItem);
			
		} else {
			MyLog.i(true, TAG, "handleShortPress: item not selected");
		}
	}
	
	/* Handle tap on maps. If tapped on 'my location' overlay, 
	 * display 'add my location' balloon
	 */
	private void handleMarkerTap(int locId) {
		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "handleMarkerTap");
		
	}
	
	/* Handle touch event to detect long press, tap and drag */
	public boolean onTouchEvent(MotionEvent ev, MapView view) {

		//MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "Maps: onTouchEvent: " + ev.getAction());
		
		Point currP = new Point((int) ev.getX(), (int) ev.getY());
		
		/* State machine for handling touch events. 
		   Long press is handled by posting a delayed
		   runnable to this thread. If the action is still
		   'down' when the runnable is run, it is a long press.
		   The variable 'pendingRunnables' is used to ignore 
		   unwanted runs due to previous 'down' events */
		
			//Runnable for detecting long press
	    Runnable runLongpress = new Runnable() {
	    	
	    	public void run() {
	    		//If the user hasn't lifted the finger, invoke the
	    		//long press handler
	    		if(mTouching && mPendingRunnables == 1) {


	    			//set 'touching' to false to avoid a tap event
	    			mTouching = false;
						int hitIndex = hitTestMarker(mTouchPoint);
					
	    			if(hitIndex != -1) {
	    				//Long pressed a red marker
	    				handleMarkerLongPress(mOverlays.get(hitIndex).locationId);
	    			} else if(hitTestOverlayCircle(mTouchPoint) != null) {
	    				//Long pressed a red circle
	    				if(!mDragging && hitTestOverlayCircle(mTouchPoint) != null) {
	    					mDragging = true;
	    					handleCircleDragStart(mTouchPoint);
	    				}
	    			}	else {
	    				//Long pressed elsewhere
	    				handleLongPress(mTouchPoint);
	    			}
	    		} 
	    		mPendingRunnables--; //Handle only the last runnable
	    	} 
	    };
		
		//Check if the drag ended
		if(ev.getAction() != MotionEvent.ACTION_MOVE && mDragging) {
			mDragging = false;
			handleCircleDragEnd(currP);
			return true;
		}
		
		//Check for other events
		switch(ev.getAction()) {
		
		case MotionEvent.ACTION_DOWN: //Press down

			//Set the flag and post the runnable for long press
			mTouching = true;
			mTouchPoint.set((int) ev.getX(), (int) ev.getY());
			
			//wait for long press 
			mPendingRunnables++;
			mHandler.postDelayed(runLongpress, LONG_PRESS_DELAY);
			
			break;
			
		case MotionEvent.ACTION_UP: //Lift finger
				
			if(mTouching) {
				mTouching = false;
				
				
				//remove the balloon
				//if(mAddLocBalloon != null) {    
			//		mAddLocBalloon.hide();
			//	}
			
				//Check if it is a 'tap' action on the overlays
				int hitIndex = hitTestMarker(mTouchPoint);
				if(hitIndex != -1) {
					handleMarkerTap(mOverlays.get(hitIndex).locationId);	
				} else {
					handleShortPress(currP);
				}
				//Prevent marker from going out of focus when tapping
				//on the circle
				//else if(hitTestOverlayCircle(mTouchPoint) != null) {
				//	return true;
				//}
			}
			break;
			
		case MotionEvent.ACTION_MOVE: //Drag
			
			if(mDragging) {
				//The circle is being dragged
				handleCircleDrag(currP);
				
				
				return true;
			}
			
			//Clear the 'touching' flag only after dragging beyond
			//a threshold
			if(mTouching) {	
				if(euclidDist(currP, mTouchPoint) > DRAG_THREHOLD) {
					mTouching = false;
				}
			}
			break;
			
		default:
			mTouching = false;
			break;
		}
		
		return false;
	}
	
	/* Check if a point hits a red circle.
	 * Returns the overlay item in that case. 
	 * Returns null otherwise.
	 */
	private MapEditOverlayItem hitTestOverlayCircle(Point p) {

		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "hitTestOverlayCircle()");
		MapEditOverlayItem item = getFocus();
		
		if (mMovable) {
			
		} else {
			if(item == null || item.locationId == CURR_LOC_ID) {
				//No red circle is visible
				return null;
			}
			
			Point markerP = mMapView.getProjection().toPixels(
									item.getPoint(), null);
			
			float rInPixels = mMapView.getProjection().metersToEquatorPixels(item.radius);
			if(euclidDist(p, markerP) > rInPixels) {
				return null;
			}
		}
		return item;
	}
	
	/* Test if a point belongs to any of the markers.
	 * If the test if positive, returns the index of
	 * the marker in the list. Otherwise, returns -1.
	 */
	private int hitTestMarker(Point p) {
		
		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "hitTestMarker");
		
		//Check all the markers for a hit
		for(int i=0; i<mOverlays.size(); i++) {
			MapEditOverlayItem item = mOverlays.get(i);
			GeoPoint ovGp = item.getPoint();
			Point ovP = mMapView.getProjection().toPixels(ovGp, null);
			
			RectF markerRect = new RectF();
			
			int w; 
			int h;
			
			if(i == mMyLocOverlayIndex) {
				w = mBluMarker.getIntrinsicWidth();
				h = mBluMarker.getIntrinsicHeight();
				markerRect.set(-w/2 - 5, -h/2 - 5, w/2 + 5, h/2 + 5);
			}
			else {
				w = mRedMarker.getIntrinsicWidth();
				h = mRedMarker.getIntrinsicHeight();
				markerRect.set(0, -h, w, 0);
			}

			markerRect.offset(ovP.x, ovP.y);
			
			if(markerRect.contains(p.x, p.y)) {
				return i;
			}
		}
		
		return -1;
	}

	/* Calculate the euclidian distance between two points */
	private float euclidDist(Point a, Point b) {
		int dx = a.x - b.x;
		int dy = a.y - b.y;
		
		return FloatMath.sqrt(dx * dx + dy * dy);
	}
	
	
	/* Draw a circle */
	private void drawCircle(Canvas c, int color, GeoPoint center, 
							float radius, int alpha, boolean border) {
		
		Point scCoord = mMapView.getProjection().toPixels(center, null);
		float r = mMapView.getProjection().metersToEquatorPixels(radius);
		 
		Paint p = new Paint();
        p.setStyle(Style.FILL);
		p.setColor(color);
		p.setAlpha(alpha);
		p.setAntiAlias(true);
		c.drawCircle(scCoord.x, scCoord.y, r, p);
		
		if(border) { //Draw border
			p.setStyle(Style.STROKE);
			p.setColor(color);
			p.setAlpha(100);
			p.setStrokeWidth(1.2F);
			p.setAntiAlias(true);
			c.drawCircle(scCoord.x, scCoord.y,r, p);
		}
	}
	
	/* The overridden draw method of the overlay.
	 * This method is overridden to draw the circle when the
	 * overlay is in focus
	 */
	public void draw(Canvas canvas, MapView mMapView, boolean shadow) {
		
		MapEditOverlayItem item = getFocus();
		//Draw circle if a red marker is focused
		if(item != null && item.locationId != CURR_LOC_ID) {
			
			//Draw dark circle while resizing
			int alpha = mCircleResizing ? ALPHA_DARK : ALPHA_LIGHT;
			boolean border = mCircleResizing ? true : false;
			drawCircle(canvas, Color.RED, item.getPoint(), 
							item.radius, alpha, border);
		} 
		
		//Draw blue circle (accuracy) if my location dot is present
		if(mMyLocOverlayIndex != -1) {
			MapEditOverlayItem currItem = getItem(mMyLocOverlayIndex);
			if(currItem.radius > 0) {
				drawCircle(canvas, Color.BLUE, currItem.getPoint(), 
							currItem.radius, ALPHA_LIGHT, true);
			}
		}
		
		super.draw(canvas, mMapView, shadow);
	}
	
	/* The focused red circle is about to get resized */
	private void handleCircleDragStart(Point p) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "Maps: Drag Start");
		
		if (!mMovable)
			return;
		
		MapEditOverlayItem item = getFocus();
		if(item == null) {
			return;
		}
		
		//Backup the old radius (needed if the resize fails)
		mDragItemRadius = item.radius;
		
		mCircleResizing = true;
		mDragPoint.set(p.x, p.y);
		mMapView.invalidate();
	}
	
	/* The focused red circle is being resized */ 
	private void handleCircleDrag(Point p) {
		//Log.i(LociConfig.D.UI.OVERLAYS, "Maps: Dragging");
		
		if (!mMovable)
			return;
		
		if(!mCircleResizing) {
			return;
		}
		
		MapEditOverlayItem item = getFocus();
		if(item == null) {
			return;
		}
		
		if(euclidDist(mDragPoint, p) < DRAG_THREHOLD) {
			return;
		}
		
		//If the resize radius falls within allowed range, update
		//the radius of the overlay
		GeoPoint gp = mMapView.getProjection().fromPixels(p.x, p.y);
		float dist[] = new float[1];
		Location.distanceBetween(item.point.getLatitudeE6() / 1E6, 
				item.point.getLongitudeE6() / 1E6, 
				gp.getLatitudeE6() / 1E6, 
				gp.getLongitudeE6() / 1E6, dist);
		
		if(dist[0] <= LociConfig.pMaxRadius && 
		   dist[0] >= LociConfig.pMinRadius) {
			item.radius = dist[0];
		}

		mMapView.invalidate();
	}
	
	// Handle circle resize end 
	private void handleCircleDragEnd(Point p) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "Maps: Drag End");
		
		if (!mMovable)
			return;
		
		mCircleResizing = false;
		
		MapEditOverlayItem item = getFocus();
		if(item == null) {
			return;
		}
		
		/*
		String cName = chekLocOverlap(mCategId, item.point, item.radius);
		if(cName != null) {
			//Overlapping, restore the radius
			item.radius = mDragItemRadius;
			//displayMessage(cName, R.string.loc_too_big_msg);
		}
		else {
			//Everything ok, update the radius in the db and notify service
			
		}
		*/
		mMapView.invalidate();
	}
	
	/* Add a new overlay item */
	public void addOverlay(MapEditOverlayItem overlay) {
		mOverlays.add(overlay);
	    populate();
	}
	
	/* Remove an overlay item */
	public void removeOverlay(int locId) {
		
		if(locId == CURR_LOC_ID) {
			mOverlays.remove(mMyLocOverlayIndex);
			mMyLocOverlayIndex = -1;
		}
		else {
			for(int i=0; i<mOverlays.size(); i++) {
				if(mOverlays.get(i).locationId == locId) {
					mOverlays.remove(i);
					break;
				}
			}
			
			if(mMyLocOverlayIndex > 0) {
				mMyLocOverlayIndex--;
			}
		}
		//Work around for platform bug
		setLastFocusedIndex(-1);
	    populate();
	}
	
	/* Add the 'my location' (blue marker) overlay.
	 * This will add a new item to the overlay list if there
	 * is not 'my location' overlay item in the list. If there is
	 * one, it will be replaced.
	 */
	public void setCurrentLocOverlay(MapEditOverlayItem overlay) {
		
		overlay.setMarker(boundCenter(mBluMarker));
		
		if(mMyLocOverlayIndex == -1) {
			if(mOverlays.add(overlay)) {
				mMyLocOverlayIndex = mOverlays.size() - 1;
			}
		}
		else {
			mOverlays.set(mMyLocOverlayIndex, overlay);
		}
	  populate();
	}
	
	/*
	 * Return 'my location' overlay item if present
	 */
	public MapEditOverlayItem getCurrentLocOverlay() {
		if(mMyLocOverlayIndex != -1) {
			return mOverlays.get(mMyLocOverlayIndex);
		}
		return null;
	}
	
}
