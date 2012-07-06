/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cens.loci.ui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.classes.EntityDelta;
import edu.cens.loci.classes.EntityModifier;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.EntityDelta.ValuesDelta;
import edu.cens.loci.classes.LociWifiFingerprint.APInfoMapItem;
import edu.cens.loci.classes.PlacesSource.DataKind;
import edu.cens.loci.classes.PlacesSource.EditField;
import edu.cens.loci.classes.PlacesSource.EditType;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Keyword;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;
import edu.cens.loci.ui.ViewIdGenerator;
import edu.cens.loci.ui.PlaceViewActivity.WifiViewListItem;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

/**
 * Simple editor that handles labels and any {@link EditField} defined for
 * the entry. Uses {@link ValuesDelta} to read any existing
 * {@link Entity} values, and to correctly write any changes values.
 */
public class GenericEditorView extends RelativeLayout implements Editor, View.OnClickListener {
    protected static final int RES_FIELD = R.layout.item_editor_field;
    protected static final int RES_LABEL_ITEM = android.R.layout.simple_list_item_1;
    protected static final int RES_WIFI_FIELD = R.layout.item_editor_wifi_field;
    protected static final int RES_AUTOCOMPLETE_FIELD = R.layout.item_editor_autocomplete_field;
    
    public static final String TAG = "GenericEditorView";
    
    protected LayoutInflater mInflater;

    protected static final int INPUT_TYPE_CUSTOM = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;

    protected TextView mLabel;
    protected ViewGroup mFields;
    protected View mDelete;
    protected View mMore;
    protected View mLess;
    
    protected int mWifiFieldButtonId;
    private LociWifiFingerprint mWifiFingerprint;
    private String mWifiFingerprintTimeStamp;
    
    protected DataKind mKind;
    protected ValuesDelta mEntry;
    protected EntityDelta mState;
    protected boolean mReadOnly;

    protected boolean mHideOptional = true;

    protected EditType mType;
    
    private ViewIdGenerator mViewIdGenerator;

    public GenericEditorView(Context context) {
       super(context);
    }

    public GenericEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        mInflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mLabel = (TextView)findViewById(R.id.edit_label);
        mLabel.setOnClickListener(this);

        mFields = (ViewGroup)findViewById(R.id.edit_fields);

        mDelete = findViewById(R.id.edit_delete);
        mDelete.setOnClickListener(this);

        mMore = findViewById(R.id.edit_more);
        mMore.setOnClickListener(this);

        mLess = findViewById(R.id.edit_less);
        mLess.setOnClickListener(this);
    }

    protected EditorListener mListener;

    public void setEditorListener(EditorListener listener) {
        mListener = listener;
    }

    public void setDeletable(boolean deletable) {
        mDelete.setVisibility(deletable ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mLabel.setEnabled(enabled);
        final int count = mFields.getChildCount();
        for (int pos = 0; pos < count; pos++) {
            final View v = mFields.getChildAt(pos);
            v.setEnabled(enabled);
        }
        mMore.setEnabled(enabled);
        mLess.setEnabled(enabled);
    }

    /**
     * Build the current label state based on selected {@link EditType} and
     * possible custom label string.
     */
    private void rebuildLabel() {
        // Handle undetected types
        if (mType == null) {
            mLabel.setText(R.string.unknown);
            return;
        }

        if (mType.customColumn != null) {
            // Use custom label string when present
            final String customText = mEntry.getAsString(mType.customColumn);
            if (customText != null) {
                mLabel.setText(customText);
                return;
            }
        }

        // Otherwise fall back to using default label
        mLabel.setText(mType.labelRes);
    }
    
    /** {@inheritDoc} */
    public void onFieldChanged(String column, String value) {
        // Field changes are saved directly
        mEntry.put(column, value);
        if (mListener != null) {
            mListener.onRequest(EditorListener.FIELD_CHANGED);
        }
        
        //Log.i("GenericEditorView", "onFieldChanged:" + mState.toString());
    }

    public boolean isAnyFieldFilledOut() {
        int childCount = mFields.getChildCount();
        for (int i = 0; i < childCount; i++) {
            EditText editorView = (EditText) mFields.getChildAt(i);
            if (!TextUtils.isEmpty(editorView.getText())) {
                return true;
            }
        }
        return false;
    }

    private void rebuildValues() {
        setValues(mKind, mEntry, mState, mReadOnly, mViewIdGenerator);
    }

    /**
     * Prepare this editor using the given {@link DataKind} for defining
     * structure and {@link ValuesDelta} describing the content to edit.
     */
    public void setValues(DataKind kind, ValuesDelta entry, EntityDelta state, boolean readOnly,
            ViewIdGenerator vig) {
        mKind = kind;
        mEntry = entry;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;
        
        setId(vig.getId(state, kind, entry, ViewIdGenerator.NO_VIEW_INDEX));

        final boolean enabled = !readOnly;

        //Log.d(TAG, "setValues: kind=" + mKind.mimeType);
        
        if (!entry.isVisible()) {
            // Hide ourselves entirely if deleted
            setVisibility(View.GONE);
            return;
        } else {
            setVisibility(View.VISIBLE);
        }

        // Display label selector if multiple types available
        final boolean hasTypes = EntityModifier.hasEditTypes(kind);
        mLabel.setVisibility(hasTypes ? View.VISIBLE : View.GONE);
        mLabel.setEnabled(enabled);
        if (hasTypes) {
        	mType = EntityModifier.getCurrentType(entry, kind);
        	rebuildLabel();
        }

        // Build out set of fields
        mFields.removeAllViews();
        boolean hidePossible = false;
        int n = 0;
        
        if (mKind.mimeType.equals(WifiFingerprint.CONTENT_ITEM_TYPE)) {
        	
        	//Log.d(TAG, "setValues: Wifi");
        	
         	for (EditField field : kind.fieldList) {
        		Button fieldView = (Button)mInflater.inflate(RES_WIFI_FIELD, mFields, false);
        		
        		mWifiFieldButtonId = vig.getId(state, kind, entry, n++);
        		
        		fieldView.setId(mWifiFieldButtonId);
        		
        		final String column = field.column;
        		final String value = entry.getAsString(column);
        		fieldView.setText("Fingerprint on " + MyDateUtils.getAbrv_MMM_d_h_m(new Long(value)));
        		
        		final String extra1column = field.extra1;
        		final String extra1value = entry.getAsString(extra1column);
        		try {
							mWifiFingerprint = new LociWifiFingerprint(extra1value);
							mWifiFingerprintTimeStamp = MyDateUtils.getDateFormatLong(new Long(value));
						} catch (JSONException e) {
							MyLog.e(LociConfig.D.JSON, TAG, "LociWifiFingerprint parsing failed");
							e.printStackTrace();
						}
        		
            // Hide field when empty and optional value
            final boolean couldHide = (field.optional);
            final boolean willHide = (mHideOptional && couldHide);
            fieldView.setVisibility(willHide ? View.GONE : View.VISIBLE);
            fieldView.setEnabled(enabled);
            hidePossible = hidePossible || couldHide;

            fieldView.setOnClickListener(this);
            
            mFields.addView(fieldView);
        	}
        } else if (mKind.mimeType.equals(Keyword.CONTENT_ITEM_TYPE)) {
        	
        	//Log.d(TAG, "setValues: Keywords");
        	
        	for (EditField field : kind.fieldList) {
        	
        		AutoCompleteTextView fieldView = (AutoCompleteTextView) mInflater.inflate(RES_AUTOCOMPLETE_FIELD, mFields, false);
        		fieldView.setId(vig.getId(state, kind, entry, n++));
        		if (field.titleRes > 0) {
        			fieldView.setHint(field.titleRes);
        		}
        		int inputType = field.inputType;
        		fieldView.setInputType(inputType);
        		fieldView.setMinLines(field.minLines);
        		
            // Read current value from state
            final String column = field.column;
            final String value = entry.getAsString(column);
            fieldView.setText(value);

            // Prepare listener for writing changes
            fieldView.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    // Trigger event for newly changed value
                    onFieldChanged(column, s.toString());
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            // Hide field when empty and optional value
            final boolean couldHide = (field.optional);
            final boolean willHide = (mHideOptional && couldHide);
            fieldView.setVisibility(willHide ? View.GONE : View.VISIBLE);
            fieldView.setEnabled(enabled);
            hidePossible = hidePossible || couldHide;
            
            String[] usedKeywords = getResources().getStringArray(R.array.keyword_default);
            
            LociDbUtils myDb = new LociDbUtils(getContext());
            ArrayList<String> suggestedKeywords = myDb.getSavedKeywords();
            HashSet<String> suggestedKeywordsSet = new HashSet<String>();
            
            for (String keyword : suggestedKeywords) {
            	suggestedKeywordsSet.add(keyword);
            }
            
            //Log.d(TAG, "size of usedKeywords : " + usedKeywords.length);
            //Log.d(TAG, "size of suggestedKeywords : " + suggestedKeywords.size());
             
            
            for (String usedKeyword : usedKeywords) {
            	if (!suggestedKeywordsSet.contains(usedKeyword))
            		suggestedKeywords.add(usedKeyword);
            }
            
            //Log.d(TAG, "size of suggestedKeywords : " + suggestedKeywords.size());
            
            Collections.sort(suggestedKeywords);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), R.layout.item_suggestion_list, suggestedKeywords);
            fieldView.setAdapter(adapter);
            fieldView.setThreshold(0);
            
            mFields.addView(fieldView);
        	}
        	
        	
        	
        } else {
        	
        	//Log.d(TAG, "General Types...");
        	
	        for (EditField field : kind.fieldList) {
            // Inflate field from definition
            EditText fieldView = (EditText)mInflater.inflate(RES_FIELD, mFields, false);
            fieldView.setId(vig.getId(state, kind, entry, n++));
            if (field.titleRes > 0) {
                fieldView.setHint(field.titleRes);
            }
            int inputType = field.inputType;
            fieldView.setInputType(inputType);
            if (inputType == InputType.TYPE_CLASS_PHONE) {
                fieldView.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
            }
            fieldView.setMinLines(field.minLines);

            // Read current value from state
            final String column = field.column;
            final String value = entry.getAsString(column);
            fieldView.setText(value);

            //Log.d(TAG, "setValues: column=" + column);
            //Log.d(TAG, "setValues: value=" + value);
            
            // Prepare listener for writing changes
            fieldView.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    // Trigger event for newly changed value
                    onFieldChanged(column, s.toString());
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            // Hide field when empty and optional value
            final boolean couldHide = (field.optional);
            final boolean willHide = (mHideOptional && couldHide);
            fieldView.setVisibility(willHide ? View.GONE : View.VISIBLE);
            fieldView.setEnabled(enabled);
            hidePossible = hidePossible || couldHide;

            mFields.addView(fieldView);
	        }
        }
       
        // When hiding fields, place expandable
        if (hidePossible) {
            mMore.setVisibility(mHideOptional ? View.VISIBLE : View.GONE);
            mLess.setVisibility(mHideOptional ? View.GONE : View.VISIBLE);
        } else {
            mMore.setVisibility(View.GONE);
            mLess.setVisibility(View.GONE);
        }
        mMore.setEnabled(enabled);
        mLess.setEnabled(enabled);
    }


    /** {@inheritDoc} */
    public void onClick(View v) {
    	
    	//Log.i(TAG, "onClick");
    	
        switch (v.getId()) {
            case R.id.edit_delete: {
            	
            		//Log.i(TAG, "edit_delete");
            	
                // Keep around in model, but mark as deleted
                mEntry.markDeleted();

                // Remove editor from parent view
                final ViewGroup parent = (ViewGroup)getParent();
                parent.removeView(this);

                if (mListener != null) {
                    // Notify listener when present
                    mListener.onDeleted(this);
                }
                break;
            }
            case R.id.edit_more:
            case R.id.edit_less: {
          		//Log.i(TAG, "edit_more_less");           
          		mHideOptional = !mHideOptional;
          		rebuildValues();
              break;
            }
            default:
          		//Log.i(TAG, "default");           

            	if (mWifiFieldButtonId == v.getId()) {
            		//Log.i("GenericEditorView", "onClick(): wifi button. " + v.getId());
            		createWifiDialog();
            	}
        }
    }

    private static class SavedState extends BaseSavedState {
        public boolean mHideOptional;
        public int[] mVisibilities;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mVisibilities = new int[in.readInt()];
            in.readIntArray(mVisibilities);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mVisibilities.length);
            out.writeIntArray(mVisibilities);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Saves the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.mHideOptional = mHideOptional;

        final int numChildren = mFields.getChildCount();
        ss.mVisibilities = new int[numChildren];
        for (int i = 0; i < numChildren; i++) {
            ss.mVisibilities[i] = mFields.getChildAt(i).getVisibility();
        }

        return ss;
    }

    /**
     * Restores the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mHideOptional = ss.mHideOptional;

        int numChildren = Math.min(mFields.getChildCount(), ss.mVisibilities.length);
        for (int i = 0; i < numChildren; i++) {
            mFields.getChildAt(i).setVisibility(ss.mVisibilities[i]);
        }
    }
    
  	private void createWifiDialog() {
  		
  		//Log.i("GenericEditor", "createWifiDialog");
  		
  		Context context = getContext();
  		LayoutInflater factory = LayoutInflater.from(context);
  		
  		final View wifiView = factory.inflate(R.layout.dialog_wifi_view, null);
  		
  		final TableLayout wifiTable = (TableLayout) wifiView.findViewById(R.id.wifi_table);
  		
  		updateWifiList(wifiTable, mWifiFingerprint);
  		
  		new AlertDialog.Builder(context)
  							.setIcon(R.drawable.ic_settings_wireless)
  							.setTitle(mWifiFingerprintTimeStamp)
  							.setView(wifiView)
  							.setPositiveButton("Close", new DialogInterface.OnClickListener() {
  								
  								public void onClick(DialogInterface dialog, int which) {
  									// TODO Auto-generated method stub
  									
  								}
  							}).create().show();
  		
  		
  	}

  	private ArrayList<View> mAddedRows = new ArrayList<View>();
  	
  	private void updateWifiList(TableLayout table, LociWifiFingerprint wifi) {
  		
  		ArrayList<WifiViewListItem> items = new ArrayList<WifiViewListItem>();
  		
  		HashMap<String, APInfoMapItem> apMap = wifi.getAps(); 
  		Set<String> keys = apMap.keySet();
  		Iterator<String> iter = keys.iterator();
  		while(iter.hasNext()) {
  			String bssid = iter.next();
  			APInfoMapItem ap = apMap.get(bssid);
  			items.add(new WifiViewListItem(bssid, ap.ssid, ap.rss, ap.count, ap.rssBuckets));
  		}

  		Collections.sort(items);
  		
  		table.setColumnCollapsed(0, false);
  		table.setColumnCollapsed(1, true);
  		table.setColumnShrinkable(0, true);
  		
  		for (int i=0; i<mAddedRows.size(); i++) {
  			table.removeView(mAddedRows.get(i));
  		}
  		mAddedRows.clear();
  		
  		int totalCount = wifi.getScanCount();
  		
  		Context context = getContext();
  		
  		for (WifiViewListItem item : items) {
  			TableRow row = new TableRow(context);
  			
  			TextView ssidView = new TextView(context);
  			ssidView.setText(item.ssid);
  			//ssidView.setText("very very very veryvery very very very very very");
  			ssidView.setPadding(2, 2, 2, 2);
  			ssidView.setTextColor(0xffffffff);
  			
  			TextView bssidView = new TextView(context);
  			bssidView.setText(item.bssid);
  			bssidView.setPadding(2, 2, 2, 2);
  			bssidView.setTextColor(0xffffffff);

  			TextView cntView = new TextView(context);
  			cntView.setText("" + (item.count*100)/totalCount);
  			cntView.setPadding(2, 2, 2, 2);
  			cntView.setGravity(Gravity.CENTER);
  			cntView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
  			
  			TextView rssView = new TextView(context);
  			rssView.setText("" + item.rss);
  			rssView.setPadding(2, 2, 6, 2);
  			rssView.setGravity(Gravity.CENTER);
  			rssView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);

  			row.addView(ssidView, new TableRow.LayoutParams(0));
  			row.addView(bssidView, new TableRow.LayoutParams(1));
  			row.addView(cntView, new TableRow.LayoutParams(2));
  			row.addView(rssView, new TableRow.LayoutParams(3));

  			//Log.d(TAG, item.ssid);
  			for (int i=0; i<item.rssBuckets.length; i++) {
  				TextView box = new TextView(context);
  				box.setText("  ");
  				box.setGravity(Gravity.RIGHT);
  				box.setPadding(2, 2, 2, 2);
  				box.setHeight(15);
  				box.setGravity(Gravity.CENTER_VERTICAL);
  				
  				float colorVal = 256 * ((float) item.rssBuckets[i] / (float) wifi.getScanCount());
  				//Log.d(TAG, "colorVal=" + (int) colorVal + ", " + item.histogram[i]);
  				int colorValInt = ((int) colorVal) - 1;
  				if (colorValInt < 0)
  					colorValInt = 0;
  				
  				box.setBackgroundColor(0xff000000 + colorValInt);//+ 0x000000ff * (item.histogram[i]/totScan));
  				box.setTextColor(0xffffffff);
  			
  				row.addView(box, new TableRow.LayoutParams(4+i));
  			}

  			row.setGravity(Gravity.CENTER);
  			
  			table.addView(row, new TableLayout.LayoutParams());
  	  	table.setColumnStretchable(3, true);
  	  	mAddedRows.add(row);
  		}
  		
  	}
    
}
