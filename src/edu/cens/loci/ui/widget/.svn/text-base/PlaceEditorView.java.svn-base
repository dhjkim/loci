package edu.cens.loci.ui.widget;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.classes.EntityDelta;
import edu.cens.loci.classes.EntityModifier;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.PlacesSource;
import edu.cens.loci.classes.EntityDelta.ValuesDelta;
import edu.cens.loci.classes.PlacesSource.DataKind;
import edu.cens.loci.components.GoogleLocalSearchHandler;
import edu.cens.loci.components.GoogleLocalSearchHandler.GoogleLocalSearchListener;
import edu.cens.loci.components.GoogleLocalSearchHandler.GoogleLocalSearchResult;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Photo;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.StructuredPostal;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Website;
import edu.cens.loci.ui.ViewIdGenerator;
import edu.cens.loci.utils.MyLog;

public class PlaceEditorView extends BasePlaceEditorView implements GoogleLocalSearchListener {
	
	public static final String TAG = "PlaceEditorView";
	
  private View mPhotoStub;
  private EditText mName;
  private ImageButton 	mSearchBtn;
  private View mMapStub;
  private TextView mDetectionType;
  private ImageView mDetectionTypeButton;
  
  
  private ViewGroup mGeneral;
  
  private long mPlaceId = -1;
  
  private Location mCenter;
  
  private GoogleLocalSearchHandler mSearchHandler;
  private boolean mIsSearching;
  
  ArrayList<GoogleLocalSearchHandler> mSearchHandlers = new ArrayList<GoogleLocalSearchHandler>();
  
  private ArrayList<Dialog> mManagedDialogs = new ArrayList<Dialog>();
  
  private ArrayList<SearchResultItem> mResultItems = new ArrayList<SearchResultItem>();
  
	public PlaceEditorView(Context context) {
		super(context);
	}

	public PlaceEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private void startSearch() {
		
		hideSoftKeyboard(mName);
		
		String keyword = mName.getText().toString();
		
		if (TextUtils.isEmpty(keyword)) {
			Toast.makeText(getContext(), "Type a word to search nearby places", Toast.LENGTH_SHORT).show();
			return;
		}

		mSearchHandler = new GoogleLocalSearchHandler(getContext(), this, mCenter);
		
		synchronized(mSearchHandler) {
			mSearchHandler.execute(keyword);
			mIsSearching = true;
			createProgressDialog();
		}
	}
	
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		mInflater = (LayoutInflater)getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);

		mPhoto = (PhotoEditorView)findViewById(R.id.edit_photo);
		mPhotoStub = findViewById(R.id.stub_photo);

		//final int photoSize = getResources().getDimensionPixelSize(R.dimen.edit_photo_size);
		 mName = (EditText) findViewById(R.id.edit_name);
     //mName.setMinimumHeight(photoSize);
     //mName.setDeletable(false);
		 
		 mSearchBtn = (ImageButton) findViewById(R.id.search_btn);
		 mSearchBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//showAndManageDialog(createSearchDialog());
				startSearch();
			}
		 });
		 
		 mMap = (MapEditorView)findViewById(R.id.edit_map);
		 mMapStub = findViewById(R.id.stub_map);
		 
		 mDetectionType = (TextView)findViewById(R.id.detection_type_edit_summary);
		 mDetectionTypeButton = (ImageView)findViewById(R.id.detection_type_edit_btn);
		 mDetectionTypeButton.setOnClickListener(new OnClickListener() {
			 public void onClick(View v) {
				 showAndManageDialog(createDetectionTypeSelectDialog());
			 }
		 });
		 
     mGeneral = (ViewGroup)findViewById(R.id.sect_general);
	}


	@Override
	protected void onDetachedFromWindow () {

		//Log.i(TAG, "onDetachedFromWindow");
		if (mIsSearching) {
			mSearchHandler.cancel(true);
			//Log.i(TAG, "cancled.");
			mIsSearching = false;
		}
		
    for (Dialog dialog : mManagedDialogs) {
      dismissDialog(dialog);
    }
		

		super.onDetachedFromWindow();

	}
	
	
	@Override
	public long getPlaceId() {
		return mPlaceId;
	}

	EntityDelta mState;
	
	@Override
	public void setState(EntityDelta state, PlacesSource source, ViewIdGenerator vig) {
		
		mState = state;
		
		// Remove any existing sections
		mGeneral.removeAllViews();
		
	  // Bail if invalid state or source 
    if (state == null || source == null) return;
    
    setId(vig.getId(state, null, null, ViewIdGenerator.NO_VIEW_INDEX));

    ValuesDelta values = state.getValues();
    
    mPlaceId = values.getAsLong(Places._ID);

    mName.setVisibility(View.VISIBLE);
   	mName.setText(values.getAsString(Places.PLACE_NAME));

    // Prepare listener for writing changes
    mName.addTextChangedListener(new TextWatcher() {
        public void afterTextChanged(Editable s) {
          // Trigger event for newly changed value
        	mState.getValues().put(Places.PLACE_NAME, s.toString());
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    });
    
    // Show and hide the appropriate views
    mGeneral.setVisibility(View.VISIBLE);
    
    EntityModifier.ensureKindExists(state, source, Photo.CONTENT_ITEM_TYPE);
    mHasPhotoEditor = (source.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null);
    
    //Log.e(TAG, "hasPhotoEditor=" + mHasPhotoEditor);
    
    // Set detection type selector
    setDetectionTypeLabel(mState.getValues().getAsInteger(Places.PLACE_TYPE));
    
    for (DataKind kind : source.getSortedDataKinds()) {
    	final String mimeType = kind.mimeType;
    	//Log.d(TAG, "setState: mimeType=" + mimeType);
    	
    	if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
    		
    		//final Value primary = state.getPrimaryEntry(mimeType);
    		ArrayList<ValuesDelta> entries = state.getMimeEntries(Photo.CONTENT_ITEM_TYPE);
    		if (entries != null && entries.size() > 0) {
	    		ValuesDelta entry = entries.get(0);

	    		//Log.e(TAG, "photo entry : " + entry.toString());
	    		
	    		mPhoto.setValues(kind, entry, state, false, vig);
	    		mPhotoStub.setVisibility(View.VISIBLE);
    		} 
    		
    	} else if (GpsCircleArea.CONTENT_ITEM_TYPE.equals(mimeType)) {
    		ArrayList<ValuesDelta> entries = state.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE);
    		if (entries != null && entries.size() > 0) {
	    		ValuesDelta entry = entries.get(0);
	    		
	    		//Log.d(TAG, "set map values : ");
	    		
	    		mMap.setValues(kind, entry, state, false, vig);
	    		mMapStub.setVisibility(View.VISIBLE);
	    		
	    		mCenter = new Location(LocationManager.GPS_PROVIDER);
	    		mCenter.setLatitude(entry.getAsDouble(GpsCircleArea.LATITUDE));
	    		mCenter.setLongitude(entry.getAsDouble(GpsCircleArea.LONGITUDE));
    		}
    	}	else {
    		if (kind.fieldList == null) continue;
    		final KindSectionView section = (KindSectionView)mInflater.inflate(R.layout.item_kind_section, mGeneral, false);
    		section.setState(kind, state, false, vig);
    		mGeneral.addView(section);
    	}
    }
    
    //Log.d(TAG, "state:" + state.toString());
    //ArrayList<ValuesDelta> entries = state.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE);
    //mCenter = null;
		//if (entries != null && entries.size() > 0) {
  	//	ValuesDelta entry = entries.get(0);
  	//	mCenter = new Location(LocationManager.GPS_PROVIDER);
  	//	mCenter.setLatitude(entry.getAsDouble(GpsCircleArea.LATITUDE));
  	//	mCenter.setLongitude(entry.getAsDouble(GpsCircleArea.LONGITUDE));
		//}
    //GoogleLocalSearchHandler googleLocal = new GoogleLocalSearchHandler(this.getContext(), center, 20, "Coffee");
    //googleLocal.execute();
		//GooglePlaceAPIHandler placeAPI = new GooglePlaceAPIHandler(getContext(), center, 20, "Coffee");
		//placeAPI.execute();

	}
	
	InputMethodManager mInputMgr;
	
	private void hideSoftKeyboard(View v) {
		if (mInputMgr == null)
			mInputMgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

		mInputMgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
		
	}
	
	private void setDetectionTypeLabel(int type) {
		String[] items = getResources().getStringArray(R.array.place_detection_types);
		mDetectionType.setText(items[type-1]);
	}
	
	private Dialog createDetectionTypeSelectDialog() {
		
		Context context = getContext();
		//LayoutInflater factory = LayoutInflater.from(context);
		
		int type = mState.getValues().getAsInteger(Places.PLACE_TYPE);
		int checkedItem = 0;
		switch(type) {
		case Places.TYPE_GPS:
			checkedItem = 0;
			break;
		case Places.TYPE_WIFI:
			checkedItem = 1;
			break;
		}
		
		return new AlertDialog.Builder(context)
    //.setIcon(R.drawable.alert_dialog_icon)
    .setTitle("Detection Sensor")
    .setSingleChoiceItems(R.array.place_detection_types, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {

        	
        	int oldType = mState.getValues().getAsInteger(Places.PLACE_TYPE);
        	int newType = whichButton+1;
        		
        	if (oldType != newType) {
        		mState.getValues().put(Places.PLACE_TYPE, newType);
          	setDetectionTypeLabel(whichButton+1);
        	}
        	
        	//Log.d(TAG, "onClick: state=" + mState.toString());
        	
        	dialog.dismiss();
        }
    })
    .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
      		if (mIsSearching) {
      			mSearchHandler.cancel(true);
      			//Log.i(TAG, "cancled.");
      			mIsSearching = false;
      		}
        }
    })
   .create();
	}
	
	/*
	private Dialog createSearchDialog() {
		
		Context context = getContext();
		LayoutInflater factory = LayoutInflater.from(context);
		
		
		final View searchView = factory.inflate(R.layout.dialog_one_edit_text, null);
		final EditText editText = (EditText) searchView.findViewById(R.id.textedit1);
		
		mSearchHandler = new GoogleLocalSearchHandler(context, this, mCenter);
		
		return new AlertDialog.Builder(context)
							.setIcon(R.drawable.ic_btn_search)
							.setTitle("Search nearby places")
							.setView(searchView)
							.setPositiveButton("Search", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									
									hideSoftKeyboard(editText);
									//Log.i(TAG, "keyword:" + editText.getText());
									String keyword = editText.getText().toString();
									
									synchronized(mSearchHandler) {
										mSearchHandler.execute(keyword);
										mIsSearching = true;
										createProgressDialog();
									}
									
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									
								}
							}).create();
		
		
	}
	*/
	
	private ProgressDialog mProgressDialog;
	
	private void createProgressDialog() {
		mProgressDialog = ProgressDialog.show(getContext(), "", "Searching nearby places...", true);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new OnCancelListener(){

			public void onCancel(DialogInterface dialog) {
				MyLog.d(LociConfig.D.UI.PLACE_SEARCH, TAG, "onCancel");
				if (mIsSearching) {
					mSearchHandler.cancel(true);
					//Log.i(TAG, "cancled.");
					mIsSearching = false;
				}
			}});
		this.startManagingDialog(mProgressDialog);
	}
	
	private void dismissProgressDialog() {
		if (mProgressDialog != null)
			mProgressDialog.dismiss();
	}
	
	private Dialog createSearchResultListDialog(ArrayList<GoogleLocalSearchResult> results) {
		
		Context context = getContext();
		SearchResultItemAdapter adapter = new SearchResultItemAdapter(context, R.layout.dialog_search_result_item, mResultItems);
		
    return new AlertDialog.Builder(context)
    .setTitle("Nearby places")
    .setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int which) {
				
				GoogleLocalSearchResult selected = mResults.get(which);
				
      	String name = mResultItems.get(which).text1; //mTitles[which];
      	mState.getValues().put(Places.PLACE_NAME, name);
      	mName.setText(mState.getValues().getAsString(Places.PLACE_NAME));
      	
      	// update GPS
      	if (selected.latitude != Double.MAX_VALUE && selected.longitude != Double.MAX_VALUE) {
	      	for (ValuesDelta entry : mState.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE)) {
	      		//Log.d(TAG, entry.toString());
	      		entry.put(GpsCircleArea.LATITUDE, mResults.get(which).latitude);
	      		entry.put(GpsCircleArea.LONGITUDE, mResults.get(which).longitude);
	      		entry.put(GpsCircleArea.EXTRA1, "from local search");
	  			}
	      	mMap.setCircle(new LociCircleArea(mResults.get(which).latitude, mResults.get(which).longitude, 30));
      	}
      	
      	// update webpage
	      int count = mGeneral.getChildCount();
	      for (int i = 0; i < count; i++) {
          View childView = mGeneral.getChildAt(i);
          if (childView instanceof KindSectionView) {
              KindSectionView section = (KindSectionView) childView;
              String mimeType = section.getMimeType();
              if (mimeType.equals(Website.CONTENT_ITEM_TYPE)) {
              	ValuesDelta newValue = section.addNewEntry();
              	newValue.put(Website.URL, selected.url);
              	section.rebuildFromState();
              } else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
              	ValuesDelta newValue = section.addNewEntry();
              	newValue.put(StructuredPostal.STREET, selected.address);
              	newValue.put(StructuredPostal.CITY, selected.city);
              	newValue.put(StructuredPostal.REGION, selected.region);
              	newValue.put(StructuredPostal.COUNTRY, selected.country);
              	section.rebuildFromState();
              }
          }
	      }
	      
	      // update postal
	      
			}
		})
    .create();
	}

	ArrayList<GoogleLocalSearchResult> mResults = null;
	
	public void onSearchResults(ArrayList<GoogleLocalSearchResult> results) {
		
		dismissProgressDialog();
	
		if (mResults != null) mResults.clear();
		if (mResultItems != null) mResultItems.clear();
		
		for (GoogleLocalSearchResult result : results) {
			mResultItems.add(new SearchResultItem(result.title, result.address + ", " + result.city));
		}
		mResults = results;
		showAndManageDialog(createSearchResultListDialog(results));
	}

	
  /**
   * Start managing this {@link Dialog} along with the {@link Activity}.
   */
  private void startManagingDialog(Dialog dialog) {
      synchronized (mManagedDialogs) {
          mManagedDialogs.add(dialog);
      }
  }

  /**
   * Show this {@link Dialog} and manage with the {@link Activity}.
   */
  void showAndManageDialog(Dialog dialog) {
      startManagingDialog(dialog);
      dialog.show();
  }

  /**
   * Dismiss the given {@link Dialog}.
   */
  static void dismissDialog(Dialog dialog) {
      try {
          // Only dismiss when valid reference and still showing
          if (dialog != null && dialog.isShowing()) {
              dialog.dismiss();
          }
      } catch (Exception e) {
          Log.w(TAG, "Ignoring exception while dismissing dialog: " + e.toString());
      }
  }
  
	public static final class SearchResultItem {
		public String text1;
		public String text2;
		
		public SearchResultItem(String text1, String text2) {
			this.text1 = text1;
			this.text2 = text2;
		}
		
		public String toString() {
			String s = String.format("[SearchResultItem: text1=%s, text2=%s]", this.text1, this.text2); 
			return s;
		}
	}
  
	public class SearchResultItemAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private ArrayList<SearchResultItem> mList;
		private int mLayout;
		
		public SearchResultItemAdapter(Context context, int layout, ArrayList<SearchResultItem> list) {
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mLayout = layout;
			mList = list;
		}
		
		public int getCount() {
			return mList.size();
		}

		public Object getItem(int position) {
			return mList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			
			int pos = position;
			convertView = mInflater.inflate(mLayout, parent, false);
			
			TextView txt1 = (TextView) convertView.findViewById(R.id.text1);
			txt1.setText(mList.get(pos).text1);
			
			TextView txt2 = (TextView) convertView.findViewById(R.id.text2);
			txt2.setText(mList.get(pos).text2);
			
			return convertView;
		}
	}
	
}
