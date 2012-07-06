package edu.cens.loci.classes;

import java.io.Serializable;

import android.location.Location;

import com.google.android.maps.GeoPoint;

import edu.cens.loci.utils.LocationUtils;

public class LociCircleArea implements Serializable {
	
	public static final boolean LOG_DEBUG = true;
	public static final String TAG = "LociCircleArea";
	
	// circle info
	private static final long	serialVersionUID	= 1L;
	private double mLatitude;
	private double mLongitude;
	private float  mRadius;
	private float  mAccuracy;
	
	public String extra;
	
	// info about the data being used to find the center 
	private int mPntCnt;

	public LociCircleArea() {
		clear();
	}
	
	public LociCircleArea(LociCircleArea circle) {
		mLatitude = circle.getLatitude();
		mLongitude = circle.getLongitude();
		mRadius = circle.getRadius();
		mAccuracy = circle.getAccuracy();
		mPntCnt = circle.getCount();
	}
	
	public LociCircleArea(double latitude, double longitude, float radius) {
		clear();
		mLatitude = latitude;
		mLongitude = longitude;
		mRadius = radius;
	}

	
	public void clear() {
		mLatitude 	= 0;
		mLongitude 	= 0;
		mRadius 		= 0;
		mAccuracy   = -1;
		mPntCnt 		= 0;
	}

	public boolean equals(LociCircleArea circle) {
		return (mLatitude == circle.getLatitude() && mLongitude == circle.getLongitude() && mRadius == circle.getRadius());
	}
	
	public String toString() {
		return String.format("[LociCircleArea] lat=%15.10f, lon=%15.10f, radius=%7.2f, accuracy=%7.2f, (#pnts=%d)", mLatitude, mLongitude, mRadius, mAccuracy, mPntCnt);
	}
	
	public void setCenter(LociLocation loc) {
		mLatitude = loc.getLatitude();
		mLongitude = loc.getLongitude();
		mPntCnt = 1;
	}
	
	public void setCenter(double lat, double lon) {
		mLatitude = lat;
		mLongitude = lon;
		mPntCnt = 1;
		
		//MyLog.d(LOG_DEBUG, TAG, String.format("setCenter: lat:%15.10f, lon:%15.10f", lat, lon));
	}
	
	public void addPosition(double lat, double lon) {

		mPntCnt++;
		
		if (mPntCnt == 1) {
			mLatitude = lat;
			mLongitude = lon;
		} else {
			//MyLog.d(false, TAG, "" + ((mPntCnt/mPntCnt+1)));
			//MyLog.d(false, TAG, "" + (mLatitude));
			
			//MyLog.d(false, TAG, "" + (mLatitude*(mPntCnt/mPntCnt+1)));
			//MyLog.d(false, TAG, "" + (mLatitude/mPntCnt));
			//MyLog.d(false, TAG, "" + mLatitude*(mPntCnt/mPntCnt+1) + mLatitude/(mPntCnt+1));
			
			mLatitude = mLatitude*(mPntCnt/(mPntCnt+1)) + mLatitude/(mPntCnt+1);
			mLongitude = mLongitude*(mPntCnt/(mPntCnt+1)) + mLongitude/(mPntCnt+1);
		}
		
		//MyLog.i(LociConfig.D.Classes.LOG_CIRCLE, TAG, "addPosition: mPosCnt=" + mPntCnt);
		//MyLog.i(LociConfig.D.Classes.LOG_CIRCLE, TAG, String.format("addPosition: [add] lat=%15.10f, lon=%15.10f, radius=%7.2f", mLatitude, mLongitude, mRadius));
		//MyLog.i(LociConfig.D.Classes.LOG_CIRCLE, TAG, String.format("addPosition: [new] lat=%15.10f, lon=%15.10f, radius=%7.2f", lat, lon, mRadius));

	}
	
	public void setRadius(float rad) {
		mRadius = rad;
		//MyLog.d(LociConfig.D.Classes.LOG_CIRCLE, TAG, String.format("setRadius: radius=%f", rad));
	}
	
	public void setAccuracy(float acc) {
		mAccuracy = acc;
		//MyLog.d(LociConfig.D.Classes.LOG_CIRCLE, TAG, String.format("setAccuracy: accuracy=%f", acc));
	}
	
	public GeoPoint getGeoPoint() {
		GeoPoint gp = LocationUtils.getGeoPoint(mLatitude, mLongitude);
		return gp;
	}
	
	public Location getLocation() {
    Location result = new Location("");
    result.setLatitude(mLatitude);
    result.setLongitude(mLongitude);
    return result;
	}
	
	public double getLatitude() {
		return mLatitude;
	}
	
	public double getLongitude() {
		return mLongitude;
	}
	
	public float getRadius() {
		return mRadius;
	}
	
	public float getAccuracy() {
		return mAccuracy;
	}
	
	public int getCount() {
		return mPntCnt;
	}
}
