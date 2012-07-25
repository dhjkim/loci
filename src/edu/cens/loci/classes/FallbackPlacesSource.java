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
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.view.inputmethod.EditorInfo;
import edu.cens.loci.R;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Keyword;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Note;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Photo;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.StructuredPostal;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Website;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;

public class FallbackPlacesSource extends PlacesSource {

  protected static final int FLAGS_KEYWORD = EditorInfo.TYPE_CLASS_TEXT
  | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;
  protected static final int FLAGS_NOTE = EditorInfo.TYPE_CLASS_TEXT
  | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
  protected static final int FLAGS_WEBSITE = EditorInfo.TYPE_CLASS_TEXT
  | EditorInfo.TYPE_TEXT_VARIATION_URI;
  protected static final int FLAGS_POSTAL = EditorInfo.TYPE_CLASS_TEXT
  | EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
  | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
	
	public FallbackPlacesSource() {
		this.accountType = null;
	}
	
	@Override
	protected void inflate(Context context, int inflateLevel) {
		
		//Log.i("FallbackPlaceSource", "inflate()");
		
		inflatePhoto(context, inflateLevel);
		
		inflateGpsCircleArea(context, inflateLevel);
		
		inflateKeyword(context, inflateLevel);
		inflateWifiFingerprint(context, inflateLevel);
		inflateStructuredPostal(context, inflateLevel);
		inflateWebsite(context, inflateLevel);
		inflateNote(context, inflateLevel);
		
    setInflatedLevel(inflateLevel);

	}
	
  protected DataKind inflatePhoto(Context context, int inflateLevel) {
    DataKind kind = getKindForMimetype(Photo.CONTENT_ITEM_TYPE);
    if (kind == null) {
        kind = addKind(new DataKind(Photo.CONTENT_ITEM_TYPE, -1, -1, -1, true));
    }

    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
        kind.fieldList = new ArrayList<EditField>();
        kind.fieldList.add(new EditField(Photo.PHOTO, -1, -1));
    }

    return kind;
  }


	protected DataKind inflateKeyword(Context context, int inflateLevel) {
		DataKind kind = getKindForMimetype(Keyword.CONTENT_ITEM_TYPE);
		if (kind == null) {
			kind = addKind(new DataKind(Keyword.CONTENT_ITEM_TYPE, R.string.keywordLabelsGroup, -1, 100, true));
			kind.secondary = false;
			kind.actionHeader = new SimpleInflater(R.string.keywordLabelsGroup);
			kind.actionBody = new SimpleInflater(Keyword.TAG);
		}
		
    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
      kind.fieldList = new ArrayList<EditField>();
      kind.fieldList.add(new EditField(Keyword.TAG, R.string.keywordLabelsGroup, FLAGS_KEYWORD));
    }

    return kind;
	}
	
	protected DataKind inflateWifiFingerprint(Context context, int inflateLevel) {
		DataKind kind = getKindForMimetype(WifiFingerprint.CONTENT_ITEM_TYPE);
		if (kind == null) {
			kind = addKind(new DataKind(WifiFingerprint.CONTENT_ITEM_TYPE, R.string.wifiFingerprintLabelsGroup, -1, 10, true));
			kind.secondary = false;
			kind.actionHeader = new SimpleInflater(R.string.wifiFingerprintLabelsGroup);
			kind.actionBody = new SimpleInflater(WifiFingerprint.FINGERPRINT);
		}
		
    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
      kind.fieldList = new ArrayList<EditField>();
      kind.fieldList.add(new EditField(WifiFingerprint.TIMESTAMP, R.string.wifiFingerprintLabelsGroup, FLAGS_KEYWORD).setExtra1(WifiFingerprint.FINGERPRINT));
    }
		
		return kind;
	}
	
	protected DataKind inflateGpsCircleArea(Context context, int inflateLevel) {
		DataKind kind = getKindForMimetype(GpsCircleArea.CONTENT_ITEM_TYPE);
		
		if (kind == null) {
			kind = addKind(new DataKind(GpsCircleArea.CONTENT_ITEM_TYPE, 0, -1,2, true));
		}
		if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
			kind.fieldList = new ArrayList<EditField>();
			kind.fieldList.add(new EditField(GpsCircleArea.LATITUDE,-1, 0));
			kind.fieldList.add(new EditField(GpsCircleArea.LONGITUDE,-1,0));
			kind.fieldList.add(new EditField(GpsCircleArea.RADIUS,-1,0));
		}
		return kind;
	}
	
  protected DataKind inflateStructuredPostal(Context context, int inflateLevel) {
    DataKind kind = getKindForMimetype(StructuredPostal.CONTENT_ITEM_TYPE);
    if (kind == null) {
        kind = addKind(new DataKind(StructuredPostal.CONTENT_ITEM_TYPE,
                R.string.postalLabelsGroup, R.drawable.sym_action_map, 25, true));
        kind.actionHeader = new SimpleInflater(R.string.map_address);
        kind.actionBody = new SimpleInflater(StructuredPostal.FORMATTED_ADDRESS);
    }

    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
        final boolean useJapaneseOrder =
            Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
        //kind.typeColumn = StructuredPostal.TYPE;
        //kind.typeList = Lists.newArrayList();
        //kind.typeList.add(buildPostalType(StructuredPostal.TYPE_HOME));
        //kind.typeList.add(buildPostalType(StructuredPostal.TYPE_WORK));
        //kind.typeList.add(buildPostalType(StructuredPostal.TYPE_OTHER));
        //kind.typeList.add(buildPostalType(StructuredPostal.TYPE_CUSTOM).setSecondary(true)
        //        .setCustomColumn(StructuredPostal.LABEL));

        kind.fieldList = new ArrayList<EditField>();

        if (useJapaneseOrder) {
            kind.fieldList.add(new EditField(StructuredPostal.COUNTRY,
                    R.string.postal_country, FLAGS_POSTAL).setOptional(true));
            kind.fieldList.add(new EditField(StructuredPostal.POSTCODE,
                    R.string.postal_postcode, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.REGION,
                    R.string.postal_region, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.CITY,
                    R.string.postal_city, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD,
                    R.string.postal_neighborhood, FLAGS_POSTAL).setOptional(true));
            kind.fieldList.add(new EditField(StructuredPostal.STREET,
                    R.string.postal_street, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.POBOX,
                    R.string.postal_pobox, FLAGS_POSTAL).setOptional(true));
        } else {
            kind.fieldList.add(new EditField(StructuredPostal.STREET,
                    R.string.postal_street, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.POBOX,
                    R.string.postal_pobox, FLAGS_POSTAL).setOptional(true));
            kind.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD,
                    R.string.postal_neighborhood, FLAGS_POSTAL).setOptional(true));
            kind.fieldList.add(new EditField(StructuredPostal.CITY,
                    R.string.postal_city, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.REGION,
                    R.string.postal_region, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.POSTCODE,
                    R.string.postal_postcode, FLAGS_POSTAL));
            kind.fieldList.add(new EditField(StructuredPostal.COUNTRY,
                    R.string.postal_country, FLAGS_POSTAL).setOptional(true));
        }
    }

    return kind;
  }

	protected DataKind inflateWebsite(Context context, int inflateLevel) {
	    DataKind kind = getKindForMimetype(Website.CONTENT_ITEM_TYPE);
	    if (kind == null) {
	        kind = addKind(new DataKind(Website.CONTENT_ITEM_TYPE,
	                R.string.websiteLabelsGroup, -1, 120, true));
	        kind.secondary = true;
	        kind.actionHeader = new SimpleInflater(R.string.websiteLabelsGroup);
	        kind.actionBody = new SimpleInflater(Website.URL);
	    }
	
	    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
	        //kind.defaultValues = new ContentValues();
	        //kind.defaultValues.put(Website.TYPE, Website.TYPE_OTHER);
	
	        kind.fieldList = new ArrayList<EditField>();
	        kind.fieldList.add(new EditField(Website.URL, R.string.websiteLabelsGroup, FLAGS_WEBSITE));
	    }
	
	    return kind;
	}
	
  protected DataKind inflateNote(Context context, int inflateLevel) {
    DataKind kind = getKindForMimetype(Note.CONTENT_ITEM_TYPE);
    if (kind == null) {
        kind = addKind(new DataKind(Note.CONTENT_ITEM_TYPE,
                R.string.label_notes, R.drawable.sym_note, 110, true));
        kind.isList = false;
        kind.secondary = true;
        kind.actionHeader = new SimpleInflater(R.string.label_notes);
        kind.actionBody = new SimpleInflater(Note.NOTE);
    }

    if (inflateLevel >= PlacesSource.LEVEL_CONSTRAINTS) {
        kind.fieldList = new ArrayList<EditField>();
        kind.fieldList.add(new EditField(Note.NOTE, R.string.label_notes, FLAGS_NOTE));
    }

    return kind;
  }
	
	/**
	 * Simple inflater that assumes a string resource has a "%s" that will be
	 * filled from the given column.
	 */
	public static class SimpleInflater implements StringInflater {
    private final int mStringRes;
    private final String mColumnName;

    public SimpleInflater(int stringRes) {
        this(stringRes, null);
    }

    public SimpleInflater(String columnName) {
        this(-1, columnName);
    }

    public SimpleInflater(int stringRes, String columnName) {
        mStringRes = stringRes;
        mColumnName = columnName;
    }

    public CharSequence inflateUsing(Context context, Cursor cursor) {
        final int index = mColumnName != null ? cursor.getColumnIndex(mColumnName) : -1;
        final boolean validString = mStringRes > 0;
        final boolean validColumn = index != -1;

        final CharSequence stringValue = validString ? context.getText(mStringRes) : null;
        final CharSequence columnValue = validColumn ? cursor.getString(index) : null;

        if (validString && validColumn) {
            return String.format(stringValue.toString(), columnValue);
        } else if (validString) {
            return stringValue;
        } else if (validColumn) {
            return columnValue;
        } else {
            return null;
        }
    }

    public CharSequence inflateUsing(Context context, ContentValues values) {
        final boolean validColumn = values.containsKey(mColumnName);
        final boolean validString = mStringRes > 0;

        final CharSequence stringValue = validString ? context.getText(mStringRes) : null;
        final CharSequence columnValue = validColumn ? values.getAsString(mColumnName) : null;

        if (validString && validColumn) {
            return String.format(stringValue.toString(), columnValue);
        } else if (validString) {
            return stringValue;
        } else if (validColumn) {
            return columnValue;
        } else {
            return null;
        }
    }
	}

  
  @Override
  public int getHeaderColor(Context context) {
      return 0xff7f93bc;
  }

  @Override
  public int getSideBarColor(Context context) {
      return 0xffbdc7b8;
  }
}
