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
