package edu.cens.loci.components;

import android.content.Context;
import android.location.Location;
import edu.cens.loci.classes.LociVisitGps;
import edu.cens.loci.sensors.GpsHandler;
import edu.cens.loci.sensors.GpsHandler.GpsListener;

public class PlaceDetectorGps implements PlaceDetector, GpsListener {

	private static final String TAG = "PlaceDetectorGps";

	private LociVisitGps mVisit;
	private Context mCxt;
	private PlaceDetectorListener mListener;
	
	private GpsHandler mGpsHandler = null;
	
	public PlaceDetectorGps(Context context, PlaceDetectorListener listener) {
		mVisit = new LociVisitGps();
		mCxt = context;
		mListener = listener;

		// get last visit from database
	
		mGpsHandler = GpsHandler.getInstance(mCxt);
	}
	
	public void clear() {
		
	}

	public void onGpsLocationChanged(long time, Location location) {
		// TODO Auto-generated method stub
		
	}
	
	public void start(long minTime) {
		if (mGpsHandler != null)
			mGpsHandler.requestUpdates(minTime, this);
	}

	public void stop() {
		if (mGpsHandler != null)
			mGpsHandler.removeUpdates(this);
	}


	public void pause() {
		// TODO Auto-generated method stub
		
	}

	public void resume() {
		// TODO Auto-generated method stub
		
	}

	public long getVisitId() {
		return mVisit.visitId;
	}
}
