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
