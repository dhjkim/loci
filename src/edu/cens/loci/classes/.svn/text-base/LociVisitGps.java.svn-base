package edu.cens.loci.classes;

import java.util.ArrayList;

import android.location.Location;
import edu.cens.loci.provider.LociContract.Places;

public class LociVisitGps extends LociVisit {

	public Location center;
	public int radius = -1;
	
	public LociVisitGps() {
		super(Places.TYPE_WIFI);
		center = new Location("loci");
	}
	
	public LociVisitGps(long visitId, long placeId, long enter, long exit, 
			ArrayList<RecognitionResult> recognitions, Location center, int radius) {
		super(visitId, placeId, Places.TYPE_WIFI, enter, exit, recognitions);
		this.center = center;
		this.radius = radius;
	}
	
	public void clear() {
		super.clear();
		center.reset();
		radius = -1;
	}
	
	
}
