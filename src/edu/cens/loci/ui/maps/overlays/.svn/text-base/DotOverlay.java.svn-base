package edu.cens.loci.ui.maps.overlays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import edu.cens.loci.ui.PlaceEditActivity;
import edu.cens.loci.ui.widget.MapEditorView;

public class DotOverlay extends Overlay {
	Location mLoc;
	
	private Context mCxt = null;
	private Intent mIntent = null;
	private MapEditorView mParent;
	
	public DotOverlay() {
		mLoc = null;
		mParent = null;
	}
	
	public DotOverlay(Location loc) {
		mLoc = loc;
		mParent = null;
	}
	
	public DotOverlay(MapEditorView parent) {
		mLoc = null;
		mParent = parent;
	}
	
	
	public DotOverlay(MapEditorView parent, Location loc) {
		mLoc = loc;
		mParent = parent;
	}
	
	public Location getLocation() {
		return mLoc;
	}
	
	public void setTapAction(Context context, Intent intent) {
		mCxt = context;
		mIntent = intent;
	}
	
	public void setLocation(Location location) {
		this.mLoc = location;
	}
	
	private final int  mRadius = 5;
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		
		Projection projection = mapView.getProjection();
		
		if (shadow == false) {
			
			if (mLoc == null)
				return;
			
			Double lat = mLoc.getLatitude() * 1E6;
			Double lon = mLoc.getLongitude() * 1E6;
			GeoPoint geoPoint = new GeoPoint(lat.intValue(), lon.intValue());
			
			// Convert the location to screen pixels
			Point point = new Point();
			projection.toPixels(geoPoint, point);
			
			RectF oval = new RectF(point.x-mRadius, point.y-mRadius, point.x+mRadius, point.y+mRadius);
			
			// Setup the paint
			Paint paint = new Paint();
			paint.setARGB(250, 250, 0, 0);
			paint.setAntiAlias(true);
			paint.setFakeBoldText(true);
			
			Paint backPaint = new Paint();
			backPaint.setARGB(50, 50, 50, 50);
			backPaint.setAntiAlias(true);
			
			RectF backRect = new RectF(point.x + 2 + mRadius,
																 point.y - 3*mRadius,
																 point.x + 40,
																 point.y + mRadius);

			// Draw the marker
			canvas.drawOval(oval, paint);
			canvas.drawRoundRect(backRect, 5, 5, backPaint);

			canvas.drawText("Here", point.x+ 2*mRadius, point.y, paint);
		}
		super.draw(canvas, mapView, shadow);
	}
	
	@Override
	public boolean onTap(GeoPoint point, MapView mapView) {
		//MapController mapCont = mapView.getController();
		//mapCont.animateTo(point);
		if (mCxt != null && mIntent != null) {
			((Activity) mCxt).startActivityForResult(mIntent, PlaceEditActivity.REQUEST_CODE_MAP_EDIT);
		}
		if (mParent != null)
			mParent.onClick(mapView);
		return false;
	}
}
