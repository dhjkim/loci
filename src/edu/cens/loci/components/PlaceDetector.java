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


public interface PlaceDetector {
	
	public interface PlaceDetectorListener {
		public void onEnter(long time);
		public void onStay(long time);
		public void onExit(long time);
		public void resetMovementDetectorOnTime();
		public void resetPathTrackerOffTime();
		public void resetPlaceCheckTime();
	}
	
	public void clear();
	
	public void start(long minTime);
	public void stop();
}
