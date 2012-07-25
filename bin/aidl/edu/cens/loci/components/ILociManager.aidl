package edu.cens.loci.components;

//import android.app.PendingIntent;
import edu.cens.loci.components.ILociListener;

interface ILociManager
{
	void requestLocationUpdates(long minTime, in ILociListener listener);
	void removeUpdates(in ILociListener listener);
	
	void locationCallbackFinished(ILociListener listener);
	
	boolean isPlaceDetectorOn();
	boolean isPathTrackerOn();
	boolean isMovementDetectorOn();
	
	void addPlaceAlert(long placeid, long expiration, in PendingIntent intent);
	void removePlaceAlert(in PendingIntent intent);
}