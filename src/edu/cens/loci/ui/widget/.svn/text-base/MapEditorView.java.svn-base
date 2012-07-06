package edu.cens.loci.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.classes.EntityDelta;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.EntityDelta.ValuesDelta;
import edu.cens.loci.classes.PlacesSource.DataKind;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.ui.MapEditViewActivity;
import edu.cens.loci.ui.ViewIdGenerator;
import edu.cens.loci.ui.maps.overlays.DotOverlay;
import edu.cens.loci.ui.widget.Editor.EditorListener;
import edu.cens.loci.utils.LocationUtils;
import edu.cens.loci.utils.MyLog;

public class MapEditorView extends MapView implements OnClickListener {

	private static final String TAG = "MapEditorView";
	
	private ValuesDelta mEntry;
	private EditorListener mListener;
	
	private boolean mHasSetCircle = false;
	//private boolean mReadOnly;
	
	public MapEditorView(Context context, String apiKey) {
		super(context, apiKey);
		setSatellite(false);
		// TODO Auto-generated constructor stub
	}
	
	public MapEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSatellite(false);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		this.setOnClickListener(this);
	}

	public void onClick(View v) {
		
		//Log.d(TAG, "onClick.");
		
		if (mListener != null) {
			mListener.onRequest(EditorListener.REQUEST_EDIT_MAP);
		}
	}
	
	public void onFieldChanged(String column, String value) {
		throw new UnsupportedOperationException("Maps don't support direct field changes");
	}
	
	private Intent getMapEditViewIntent(LociCircleArea circle, boolean movable, ValuesDelta entry) {
		Intent intent = new Intent(this.getContext(), MapEditViewActivity.class);
		intent.putExtra(Constants.Intents.UI.MAP_EDIT_CIRCLE_EXTRA_KEY, circle);
		intent.putExtra(Constants.Intents.UI.MAP_EDIT_MODE_EXTRA_KEY, movable);
		intent.putExtra(Constants.Intents.UI.MAP_EDIT_VALUES_EXTRA_KEY, entry);
		return intent;
	}
	
	public void setValues(DataKind kind, ValuesDelta values, EntityDelta state, boolean readOnly, 
					ViewIdGenerator vig) {

		//Log.d(TAG, "setValues");
		
		mEntry = values;
		//mReadOnly = readOnly;
		
		setId(vig.getId(state, kind, values, 0));
		
		// modify map here

		if (values != null) {
			//Log.d(TAG, "value!=null");
			
			double lat = values.getAsDouble(GpsCircleArea.LATITUDE);
			double lon = values.getAsDouble(GpsCircleArea.LONGITUDE);
			float radius = values.getAsFloat(GpsCircleArea.RADIUS);
			String extra = values.getAsString(GpsCircleArea.EXTRA1);
			
			MyLog.i(LociConfig.D.UI.MAP, TAG, String.format("[setValues] lat=%f, lon=%f, radius=%f, extra=%s", lat, lon, radius, extra));
			LociCircleArea circle = new LociCircleArea(lat, lon, radius); 
			circle.extra = extra;
			
			// check if the coordinate is valid?
			if (LocationUtils.isValidGeoPoint(circle.getGeoPoint())) {
				// draw overlay

				MapController mc = getController();
				mc.setZoom(16);
				mc.setCenter(circle.getGeoPoint());
				
	   		DotOverlay dotOverlay = new DotOverlay(this, circle.getLocation());
    		dotOverlay.setTapAction(getContext(), getMapEditViewIntent(circle, true, values));
    		getOverlays().clear();
    		getOverlays().add(dotOverlay);
				
    		//addTextViewOverlay();
    		
				mHasSetCircle = true;
				mEntry.setFromTemplate(false);
			} else {
				MyLog.e(LociConfig.D.ERROR, TAG, "[setValues] circle is not valid");
			}
			
		} else {
			resetDefault();
		}
	} 
	
	public boolean hasSetCircle() {
		return mHasSetCircle;
	}
	
	public void setCircle(LociCircleArea circle) {
		
		if (circle == null) {
			mEntry.put(GpsCircleArea.LATITUDE, Double.MAX_VALUE);
			mEntry.put(GpsCircleArea.LONGITUDE, Double.MAX_VALUE);
			mEntry.put(GpsCircleArea.RADIUS, Float.MAX_VALUE);
			resetDefault();
			return;
		}
		
		
		// check if the coordinate is valid?
		if (LocationUtils.isValidGeoPoint(circle.getGeoPoint())) {
			// draw overlay
			mEntry.put(GpsCircleArea.LATITUDE, circle.getLatitude());
			mEntry.put(GpsCircleArea.LONGITUDE, circle.getLongitude());
			mEntry.put(GpsCircleArea.RADIUS, circle.getRadius());
			
			MapController mc = getController();
			mc.setZoom(16);
			mc.setCenter(circle.getGeoPoint());
			
   		DotOverlay dotOverlay = new DotOverlay(this, circle.getLocation());
  		dotOverlay.setTapAction(getContext(), getMapEditViewIntent(circle, true, mEntry));
  		getOverlays().clear();
  		getOverlays().add(dotOverlay);
			
  		addTextViewOverlay();
  		
			mHasSetCircle = true;
			mEntry.setFromTemplate(false);
		}
		
		mHasSetCircle = true;
		mEntry.setFromTemplate(false);
	}
	
	private void addTextViewOverlay() {
		// add text on the map
		TextView textOverlay = new TextView(this.getContext());
		textOverlay.setText("Edit");
		textOverlay.setTextSize(16);
		textOverlay.setTextColor(Color.WHITE);
		textOverlay.setBackgroundColor(Color.argb(128, 139, 137, 137));
		textOverlay.setGravity(Gravity.CENTER);
		textOverlay.setHeight(25);
		
		MapView.LayoutParams textLayoutParams;
		//Log.i(TAG, "height:" + getLayoutParams().height + " " + textOverlay.getMeasuredHeight());
		textLayoutParams = new MapView.LayoutParams(MapView.LayoutParams.FILL_PARENT,
				MapView.LayoutParams.WRAP_CONTENT,
				0,getLayoutParams().height-25, MapView.LayoutParams.TOP_LEFT);
		
		addView(textOverlay, textLayoutParams);
	}
	
	protected void resetDefault() {
		
		// show default map?'				
		MapController mc = getController();
		mc.setZoom(5);
		
 		DotOverlay dotOverlay = new DotOverlay(this);
		dotOverlay.setTapAction(getContext(), getMapEditViewIntent(null, true, null));
		getOverlays().clear();
		getOverlays().add(dotOverlay);

		
		mHasSetCircle = false;
		mEntry.setFromTemplate(true);
	}
	
	public void setEditorListener(EditorListener listener) {
		mListener = listener;
	}
	
}
