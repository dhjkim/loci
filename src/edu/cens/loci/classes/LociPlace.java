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


public class LociPlace {
	public long placeId;
	public String name;
	public int state;
	public int type;
	public int entry;
	public long entryTime;
	public long registerTime;
	public int timesVisited;
	public double extra_double;

	public ArrayList<LociWifiFingerprint> wifis;
	public ArrayList<LociCircleArea> areas;
 	public ArrayList<String> keywords;

 	public LociPlace() {
 	}
 	
 	public String toString() {
 		return String.format("LociPlace [placeId=%d, name=%s, state=%d, type=%d, entry=%d]", placeId, name, state, type, entry);
 	}
 	
}
