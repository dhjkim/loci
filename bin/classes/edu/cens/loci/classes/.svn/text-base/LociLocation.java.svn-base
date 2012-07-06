package edu.cens.loci.classes;

import java.util.ArrayList;

import edu.cens.loci.LociConfig;
import edu.cens.loci.utils.LocationUtils;
import edu.cens.loci.utils.MyLog;

import android.location.Location;

/**
 * @author dhjkim
 * A class that attaches time information to a location fix.
 */
public class LociLocation extends Location {

	public static final String TAG = "LociLocation";
	
	private long mTime = -1;
	
	/**
	 * Constructs a new LociLocation.
	 * By default, time is -1;
	 * @param provider the name of the location provider that generated this location fix.
	 */
	public LociLocation(String provider) {
		super(provider);
	}
	
	/**
	 * Constructs a new LociLocation object that copies Location and assigns the given time.
	 * @param time
	 * @param l
	 */
	public LociLocation(long time, Location l) {
		super(l);
		mTime = time;
	}

	/**
	 * Sets the contents of the LociLocation to values from the given arguments.
	 */
	public void set(long time, double lat, double lon, double alt, float speed, float bearing, float accuracy) {
		mTime = time;
		super.setLatitude(lat);
		super.setLongitude(lon);
		super.setAltitude(alt);
		super.setSpeed(speed);
		super.setBearing(bearing);
		super.setAccuracy(accuracy);
	}
	/**
	 * Returns the time of the fix, -1 if not assigned a time.
	 */
	public long getTime() {
		return mTime;
	}
	
	public void setTime(long time) {
		mTime = time;
	}
	
	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		double pi80 = (double) Math.PI / 180;
		lat1 = lat1 * pi80;
		lon1 = lon1 * pi80;
		lat2 = lat2 * pi80;
		lon2 = lon2 * pi80;
		
		double r = 6372.797f;
		double dlat = lat2 - lat1;
		double dlon = lon2 - lon1;
		
		double a = (double) (Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2));
    double c = 2 * (double) (Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    double km = r * c;

    double dist = km*1000;
		return dist;   
	}
	
	public static LociLocation averageLocation(ArrayList<LociLocation> locs, float accuracy) {
		int size = 0;
		double sumLat = 0;
		double sumLon = 0;
		
		for (LociLocation loc : locs) {
			//MyLog.d(LociConfig.DEBUG, TAG, "averageLocation:  " + loc.toString());
			if (loc.getAccuracy() <= accuracy) {
				size++;
				sumLat += loc.getLatitude();
				sumLon += loc.getLongitude();
			} 
		}
		LociLocation avg = new LociLocation("loci");
		avg.setLatitude(sumLat/size);
		avg.setLongitude(sumLon/size);
		return avg;
	}
	
	public boolean isValid() {
		boolean isValid = LocationUtils.isValidLocation(this);

		MyLog.d(LociConfig.D.Classes.LOG_LOCATION, TAG, "isValid=" + isValid + " " + toString());
		
		return isValid;
	}
}
