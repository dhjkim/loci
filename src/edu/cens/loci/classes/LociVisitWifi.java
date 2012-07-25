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
import java.util.HashMap;

import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;
import edu.cens.loci.provider.LociContract.Places;

public class LociVisitWifi extends LociVisit {

	public LociWifiFingerprint wifi;
	
	public LociVisitWifi() {
		super(Places.TYPE_WIFI);
		wifi = new LociWifiFingerprint();
	}
	
	public LociVisitWifi(long visitId, long placeId, long enter, long exit, 
			ArrayList<RecognitionResult> recognitions, LociWifiFingerprint wifi) {
		super(visitId, placeId, Places.TYPE_WIFI, enter, exit, recognitions);
		this.wifi = wifi;
	}
	
	public void clear() {
		super.clear();
		wifi.initialize();
	}
	
	public void update(long time, HashMap<String, APInfoMapItem> scan) {

		// Updates visit enter and exit times. 
		// They should be consistent with the Wi-Fi fingerprint.
		if (enter == -1)
			enter = time;
		exit = time;
		
		if (wifi == null)
			wifi = new LociWifiFingerprint();
		
		wifi.update(time, scan);
	}
}
