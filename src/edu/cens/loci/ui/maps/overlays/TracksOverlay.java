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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.cens.loci.LociConfig;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.utils.LocationUtils;
import edu.cens.loci.utils.MyLog;

public class TracksOverlay extends Overlay {
	
	private static final String TAG = "TrackOverlay";
	
	private static class CachedLocation {
		public final boolean valid;
		public final GeoPoint geoPnt;
		public LociLocation loc;
		
		public CachedLocation(LociLocation location) {
			this.valid = LocationUtils.isValidLocation(location);
			this.geoPnt = valid ? LocationUtils.getGeoPoint(location) : null;
			loc = location;
		}
	}
	
	private Paint mBluePaint;
	private Paint mBackPaint;
	private Paint mRedPaint;
	
	private ArrayList<CachedLocation> mPoints;
	private final BlockingQueue<CachedLocation> mPendingPoints;
	
	private ArrayList<CachedLocation> mLastPath = null;
	
	private Rect mLastViewRect;
	private boolean mTrackDrawingEnabled;
	private GeoPoint mLastReferencePoint;
	
	
	public TracksOverlay(Context context) {

		mBackPaint = new Paint();
	  mBackPaint.setARGB(200, 200, 200, 200);
	  mBackPaint.setAntiAlias(true);
	
	  
	  mBluePaint = new Paint();
	  mBluePaint.setARGB(255, 10, 10, 255);
	  mBluePaint.setAntiAlias(true);
	
	  mBluePaint.setFakeBoldText(true);
	  mBluePaint.setStyle(Paint.Style.FILL_AND_STROKE);
	  mBluePaint.setStrokeJoin(Paint.Join.ROUND);
	  mBluePaint.setStrokeCap(Paint.Cap.ROUND);
	  mBluePaint.setStrokeWidth(3);
	  
	  
	  mRedPaint = new Paint();
	  mRedPaint.setARGB(255, 255, 10, 10);
	  mRedPaint.setAntiAlias(true);

	  mRedPaint.setFakeBoldText(true);
	  mRedPaint.setStrokeWidth(3);
	  
	  mPoints = new ArrayList<CachedLocation>();
	  this.mPendingPoints = new ArrayBlockingQueue<CachedLocation>(10000, true);
	  
	  mTrackDrawingEnabled = true;
	}

	public void addLocation(LociLocation loc) {
		mPendingPoints.offer(new CachedLocation(loc));
	}

	public void addLocation(ArrayList<LociLocation> locs) {
		for (LociLocation loc : locs) {
			addLocation(loc);
		}
	}
	
	public int getNumLocations() {
		synchronized (mPoints) {
			return mPoints.size() + mPendingPoints.size();
		}
	}
	
	public void clearPoints() {
		synchronized (mPoints) {
			mPoints.clear();
			mPendingPoints.clear();
			mLastViewRect = null;
			mLastPath = null;
		}
	}
	
	public void setTrackDrawingEnabled(boolean enabled) {
		this.mTrackDrawingEnabled = enabled;
	}
	
  // Visible for testing.
  Rect getMapViewRect(MapView mapView) {
    int w = mapView.getLongitudeSpan();
    int h = mapView.getLatitudeSpan();
    int cx = mapView.getMapCenter().getLongitudeE6();
    int cy = mapView.getMapCenter().getLatitudeE6();
    return new Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
  }
	
  // Visible for testing.
  Projection getMapProjection(MapView mapView) {
    return mapView.getProjection();
  }
  
  
  public void draw(Canvas canvas, MapView mapView, boolean shadow) {
  	
    if (shadow) {
      return;
    }

    // It's safe to keep projection within a single draw operation.
    final Projection projection = getMapProjection(mapView);
    // Get the current viewing window.
    if (mTrackDrawingEnabled) {
      Rect viewRect = getMapViewRect(mapView);
      
      // Draw the selected track:
      drawTrack(canvas, projection, viewRect);
    } else {
    	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "mTrackDrawingEnabled is set to false.");
    }
  }	
  
	public void drawTrack(Canvas canvas, Projection projection, Rect viewRect) {
		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "drawTrack()");
  	
    ArrayList<CachedLocation> path;
    synchronized (mPoints) {
      // Merge the pending points with the list of cached locations.
      final GeoPoint referencePoint = projection.fromPixels(0, 0);
      int newPoints = mPendingPoints.drainTo(mPoints); 
      boolean newProjection = !viewRect.equals(mLastViewRect) ||
          !referencePoint.equals(mLastReferencePoint); 
      if (newPoints == 0 && mLastPath != null && !newProjection) {
        // No need to recreate path (same points and viewing area).
      	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "No need to recreate path (same points and viewing area)");
      	path = mLastPath;
      } else {
        int numPoints = mPoints.size();
        if (numPoints < 2) {
          // Not enough points to draw a path.
        	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "Not enough points to draw a path.");
        	path = null;
        } else if (mLastPath != null && !newProjection) {
          // Incremental update of the path, without repositioning the view.
        	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "Incremental update of the path, without repositioning the view.");
          path = mLastPath;
          updatePath(projection, viewRect, path, numPoints - newPoints); 
        } else {
          // The view has changed so we have to start from scratch.
        	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "The view has changed so we have to start from scratch.");
        	path = new ArrayList<CachedLocation>();
          updatePath(projection, viewRect, path, 0); 
        }
        mLastPath = path;
      }
      mLastReferencePoint = referencePoint;
      mLastViewRect = viewRect;
    }
    if (path != null) {
    	MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "drawPath.");
    	//canvas.drawPath(path, mTrackPaint);
    	drawPath(canvas, path, projection);
    }
	}
	 
  private void updatePath(Projection projection, Rect viewRect, ArrayList<CachedLocation> path, int startLocationIdx) {
    // Whether to start a new segment on new valid and visible point.
  	
  	for (int i = startLocationIdx; i < mPoints.size(); ++i) {
      CachedLocation loc = mPoints.get(i);
      // Check if valid, if not then indicate a new segment.
      if (!loc.valid) 
        continue;
      
      if (loc.loc.getAccuracy() > 100) 
      	continue;
      
      path.add(loc);
    }
  }

  private void drawPath(Canvas canvas, ArrayList<CachedLocation> path, Projection projection) {
  	
  	Point pPoint = new Point();
  	Point cPoint = new Point();
  	
  	if (path.size() > 0) {
  		CachedLocation pLoc = path.get(0);
  		
  		for (int i=1; i < path.size(); i++) {
  			CachedLocation cLoc = path.get(i);
  			projection.toPixels(pLoc.geoPnt, pPoint);
  			projection.toPixels(cLoc.geoPnt, cPoint);
  			
  			float dist = pLoc.loc.distanceTo(cLoc.loc);
  			
  			//Log.d(TAG, "center=" + mLastViewRect.centerX() + ", " + mLastViewRect.centerY());
  			//Log.d(TAG, "weight=" + mLastViewRect.width() + ", height=" + mLastViewRect.height());
  			
  			if (dist >= LociConfig.pMapPathMinDistance) {
  				
  				float accuracy = cLoc.loc.getAccuracy();
  				float speed = cLoc.loc.getSpeed();
  				
  			  mBluePaint.setARGB(accuracy2alpha(accuracy), speed2red(speed), speed2green(speed), speed2blue(speed));
  			  canvas.drawLine(pPoint.x, pPoint.y, cPoint.x, cPoint.y, mBluePaint);
  			  pLoc = cLoc;
  			}
  		}
  	}
  }

  private int accuracy2alpha(float accuracy) {
  	int alpha = (int) (1.3 * accuracy + 255);
  	if (alpha > 255) alpha = 255;
  	if (alpha < 10) alpha = 10;
  	return alpha;
  }
  
  private int speed2red(float speed) {
  	
  	int red = 0;
  	if (speed < 1.5)
  		red = 139;
  	else if (speed <= 5)
  		red = 255;
  	else if (speed <= 10)
  		red = 255;
  	else
  		red = 0;
  	
  	return red;
  }

  private int speed2green(float speed) {
  	
  	int green = 0;
  	if (speed <= 1.5)
  		green = 137;
  	else if (speed <= 5)
  		green = 0;
  	else if (speed <= 10)
  		green = 255;
  	else
  		green = 192;
  		
  	return green;
  }
  
  private int speed2blue(float speed) {
  	
  	int blue = 0;
  	if (speed < 1.5)
  		blue = 137;
  	else if (speed <= 5)
  		blue = 0;
  	else if (speed <= 10)
  		blue = 0;
  	else
  		blue = 0;
  	
  	return blue;
  }
}
