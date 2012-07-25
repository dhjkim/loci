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
