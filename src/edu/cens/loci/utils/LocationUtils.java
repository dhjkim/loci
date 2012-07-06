package edu.cens.loci.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class LocationUtils {
	
  public static final double KM_TO_MI = 0.621371192;
  public static final double M_TO_FT = 3.2808399;
  public static final double MI_TO_M = 1609.344;
  public static final double MI_TO_FEET = 5280.0;
  public static final double KMH_TO_MPH = 1000 * M_TO_FT / MI_TO_FEET;
  public static final double TO_RADIANS = Math.PI / 180.0;
	
  public static double distance(final Location c0, final Location c1, final Location c2) {

  	if (c1.equals(c2)) {
      return c2.distanceTo(c0);
    }

    final double s0lat = c0.getLatitude() 	* TO_RADIANS;
    final double s0lng = c0.getLongitude() 	* TO_RADIANS;
    final double s1lat = c1.getLatitude() 	* TO_RADIANS;
    final double s1lng = c1.getLongitude() 	* TO_RADIANS;
    final double s2lat = c2.getLatitude() 	* TO_RADIANS;
    final double s2lng = c2.getLongitude() 	* TO_RADIANS;

    double s2s1lat = s2lat - s1lat;
    double s2s1lng = s2lng - s1lng;
    final double u =
        ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
            / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
    if (u <= 0) {
      return c0.distanceTo(c1);
    }
    if (u >= 1) {
      return c0.distanceTo(c2);
    }
    Location sa = new Location("");
    sa.setLatitude(c0.getLatitude() - c1.getLatitude());
    sa.setLongitude(c0.getLongitude() - c1.getLongitude());
    Location sb = new Location("");
    sb.setLatitude(u * (c2.getLatitude() - c1.getLatitude()));
    sb.setLongitude(u * (c2.getLongitude() - c1.getLongitude()));
    return sa.distanceTo(sb);
  }

  /**
   * Decimates the given locations for a given zoom level. This uses a
   * Douglas-Peucker decimation algorithm.
   * 
   * @param tolerance in meters
   * @param locations input
   * @param decimated output
   */
  public static void decimate(double tolerance, ArrayList<Location> locations,
      ArrayList<Location> decimated) {
    final int n = locations.size();
    if (n < 1) {
      return;
    }
    int idx;
    int maxIdx = 0;
    Stack<int[]> stack = new Stack<int[]>();
    double[] dists = new double[n];
    dists[0] = 1;
    dists[n - 1] = 1;
    double maxDist;
    double dist = 0.0;
    int[] current;

    if (n > 2) {
      int[] stackVal = new int[] {0, (n - 1)};
      stack.push(stackVal);
      while (stack.size() > 0) {
        current = stack.pop();
        maxDist = 0;
        for (idx = current[0] + 1; idx < current[1]; ++idx) {
          dist = LocationUtils.distance(
              locations.get(idx),
              locations.get(current[0]),
              locations.get(current[1]));
          if (dist > maxDist) {
            maxDist = dist;
            maxIdx = idx;
          }
        }
        if (maxDist > tolerance) {
          dists[maxIdx] = maxDist;
          int[] stackValCurMax = {current[0], maxIdx};
          stack.push(stackValCurMax);
          int[] stackValMaxCur = {maxIdx, current[1]};
          stack.push(stackValMaxCur);
        }
      }
    }

    int i = 0;
    idx = 0;
    decimated.clear();
    for (Location l : locations) {
      if (dists[idx] != 0) {
        decimated.add(l);
        i++;
      }
      idx++;
    }
    Log.d("LocationUtils:decimate", "Decimating " + n + " points to " + i
        + " w/ tolerance = " + tolerance);
  }

  /**
   * Decimates the given track for the given precision.
   * 
   * @param track a track
   * @param precision desired precision in meters
   */
  public static void decimate(Track track, double precision) {
    ArrayList<Location> decimated = new ArrayList<Location>();
    decimate(precision, track.getLocations(), decimated);
    track.setLocations(decimated);
  }

  /**
   * Limits number of points by dropping any points beyond the given number of
   * points. Note: That'll actually discard points.
   * 
   * @param track a track
   * @param numberOfPoints maximum number of points
   */
  public static void cut(Track track, int numberOfPoints) {
    ArrayList<Location> locations = track.getLocations();
    while (locations.size() > numberOfPoints) {
      locations.remove(locations.size() - 1);
    }
  }


  /**
   * Test if a given GeoPoint is valid, i.e. within physical bounds.
   * 
   * @param geoPoint the point to be tested
   * @return true, if it is a physical location on earth.
   */
  public static boolean isValidGeoPoint(GeoPoint geoPoint) {
    return Math.abs(geoPoint.getLatitudeE6()) < 90E6
        && Math.abs(geoPoint.getLongitudeE6()) <= 180E6;
  }

  /**
   * Checks if a given location is a valid (i.e. physically possible) location
   * on Earth. Note: The special separator locations (which have latitude =
   * 100) will not qualify as valid. Neither will locations with lat=0 and lng=0
   * as these are most likely "bad" measurements which often cause trouble.
   * 
   * @param location the location to test
   * @return true if the location is a valid location.
   */
  public static boolean isValidLocation(Location location) {
    return location != null && Math.abs(location.getLatitude()) <= 90
        && Math.abs(location.getLongitude()) <= 180;
  }

  /**
   * Gets a location from a GeoPoint.
   * 
   * @param p a GeoPoint
   * @return the corresponding location
   */
  public static Location getLocation(GeoPoint p) {
    Location result = new Location("");
    result.setLatitude(p.getLatitudeE6() / 1.0E6);
    result.setLongitude(p.getLongitudeE6() / 1.0E6);
    return result;
  }

  public static GeoPoint getGeoPoint(Location location) {
    return new GeoPoint((int) (location.getLatitude() * 1E6),
                        (int) (location.getLongitude() * 1E6));
  }
  
	public static GeoPoint getGeoPoint(double lat, double lon) {
		Double dlat = lat * 1E6;
		Double dlon = lon * 1E6;
		GeoPoint pt = new GeoPoint(dlat.intValue(), dlon.intValue());
		return pt;
	}

	public static String getAddress(Context context, Location loc) {
		
		String addrString = "No address found";
		
		Geocoder gc = new Geocoder(context, Locale.getDefault());
		try {
			List<Address> addresses = gc.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
			StringBuilder sb = new StringBuilder();
			
			if (addresses.size() > 0) {
				Address address = addresses.get(0);
				
				for (int i=0; i<address.getMaxAddressLineIndex(); i++) 
					sb.append(address.getAddressLine(i)).append("\n");
				
				//sb.append(address.getLocality()).append("\n");
				//sb.append(address.getPostalCode()).append("\n");
				//sb.append(address.getCountryName());
				addrString = sb.toString();
			} 
			addrString = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return addrString;
	}
  
  public class Track {
  	
  	private ArrayList<Location> mLocations;
  	
  	public Track() {
  		mLocations = new ArrayList<Location>();
  	}
  	
  	public ArrayList<Location> getLocations() {
  		return mLocations;
  	}
  	
  	public void setLocations(ArrayList<Location> locs) {
  		mLocations = locs;
  	}
  	
    public void addLocation(Location l) {
      mLocations.add(l);
    }

  	
  }

}
