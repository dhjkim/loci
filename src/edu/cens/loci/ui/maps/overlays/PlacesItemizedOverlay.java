package edu.cens.loci.ui.maps.overlays;

import java.util.ArrayList;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.maps.MapView;

import edu.cens.loci.LociConfig;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.utils.MyLog;

public class PlacesItemizedOverlay extends BalloonItemizedOverlay<PlacesOverlayItem>{
	
	private static final String TAG = "PlaceItemizedOverlay";
	
	private ArrayList<PlacesOverlayItem> mOverlays = new ArrayList<PlacesOverlayItem>();
	private Context mContext;
	
	public static Drawable boundCenter(Drawable marker) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, String.format("height:%d weight:%d", marker.getIntrinsicHeight(), marker.getIntrinsicWidth()));
		marker.setBounds(-marker.getIntrinsicWidth()/2, -marker.getIntrinsicHeight()/2, marker.getIntrinsicWidth()/2, marker.getIntrinsicHeight()/2);
		return marker;
	}
	
	public static Drawable boundLeftBottom(Drawable marker) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, String.format("height:%d weight:%d", marker.getIntrinsicHeight(), marker.getIntrinsicWidth()));
		marker.setBounds(0, -marker.getIntrinsicHeight(), marker.getIntrinsicWidth(), 0);
		return marker;
	}
	
	public PlacesItemizedOverlay(Context context, Drawable defaultMarker, MapView mapView) {
		super(boundCenter(defaultMarker), mapView);
		mContext = context;
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "constructor()");
	}
	
	public void clear() {
		mOverlays.clear();
	}
	
	public void addOverlay(PlacesOverlayItem overlay) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "addOverlay()");
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected PlacesOverlayItem createItem(int i) {
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "createItem()");
		return mOverlays.get(i);
	}
	
	@Override	
	public int size() {
		int size = mOverlays.size();
		MyLog.i(LociConfig.D.UI.OVERLAYS, TAG, "size() : " + size);
		return size;
	}

	@Override
	protected boolean onBalloonTap(int index) {
		PlacesOverlayItem poi = mOverlays.get(index);
		
		MyLog.d(LociConfig.D.UI.OVERLAYS, TAG, "onBalloonTap: index=" + index + ", poi=" + poi.toString());
		
		//Intent intent = new Intent(mCxt, PlaceInfoViewActivity.class);
		//intent.putExtra("viewType", PlaceInfoViewActivity.TYPE_PLACEINFO_VIEW);
		//intent.putExtra("origin", PlaceInfoViewActivity.ORIGIN_PLACE);
		//intent.putExtra("placeId", poi.getPid());
		//intent.putExtra("placeState", poi.getState());
		//intent.putExtra("uiRoute", PlaceDatabaseAdapter.UIROUTE_MAP);
		//mCxt.startActivity(intent);	
		
		Intent intent = new Intent(Intent.ACTION_VIEW, getSelectedUri(poi.getPid()));
		mContext.startActivity(intent);
		
		return true;
	}
	
	private Uri getSelectedUri(long placeId) {
		return ContentUris.withAppendedId(Places.CONTENT_URI, placeId);
	}

}
