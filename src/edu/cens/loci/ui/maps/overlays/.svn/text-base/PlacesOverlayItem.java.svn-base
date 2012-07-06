package edu.cens.loci.ui.maps.overlays;

import java.util.ArrayList;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.utils.MyDateUtils;

public class PlacesOverlayItem extends OverlayItem {

	private long mPid = -1;
	private int mType = Places.TYPE_WIFI;
	private int mState = -1;
	private String mName = "";
	private GeoPoint mPoint;
	private float mRadius = 0;
	
	ArrayList<VisitTimeItem> mVisitTimes;
	
	public PlacesOverlayItem(long pid, int type, int state, String name, GeoPoint pnt, float rad) {
		super(pnt, "", "");
		mPid 			= pid;
		mType     = type;
		mState    = state;
		mName     = name;
		mPoint 		= pnt;
		mRadius   = rad;
		mVisitTimes = new ArrayList<VisitTimeItem>();
	}

	public long getPid() {
		return mPid;
	}
	public int getType() {
		return mType;
	}
	public int getState() {
		return mState;
	}
	public String getName() {
		return mName;
	}
	public GeoPoint getPoint() {
		return mPoint;
	}
	public float getRadius() {
		return mRadius;
	}
	
	public void addVisit(long entTime, long extTime) {
		String enter = MyDateUtils.getTimeFormatShort(entTime);
		String exit = MyDateUtils.getTimeFormatShort(extTime);
		mVisitTimes.add(new VisitTimeItem(enter, exit));
	}
	
	public ArrayList<VisitTimeItem> getVisitTimes() {
		return mVisitTimes;
	}
	
	public class VisitTimeItem {
		public String enter;
		public String exit;
		
		public VisitTimeItem(String enter, String exit) {
			this.enter = enter;
			this.exit = exit;
		}
	}
	
}
