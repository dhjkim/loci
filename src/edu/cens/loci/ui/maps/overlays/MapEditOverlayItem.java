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
package edu.cens.loci.ui.maps.overlays;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MapEditOverlayItem extends OverlayItem {

	public int locationId = 0;
	public GeoPoint point;
	public float radius = 0;
	
	public MapEditOverlayItem(GeoPoint point, int locId, float radius) {
		super(point, "", "");
		
		this.point = point;
		this.locationId = locId;
		this.radius = radius;
	}
}
