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
package edu.cens.loci.ui;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.google.android.maps.MapActivity;

import edu.cens.loci.Constants;
import edu.cens.loci.LociConfig;
import edu.cens.loci.R;
import edu.cens.loci.Constants.Intents;
import edu.cens.loci.Constants.Intents.Insert;
import edu.cens.loci.classes.EntityDelta;
import edu.cens.loci.classes.EntityModifier;
import edu.cens.loci.classes.EntitySet;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.LociLocation;
import edu.cens.loci.classes.LociWifiFingerprint;
import edu.cens.loci.classes.PlacesSource;
import edu.cens.loci.classes.Sources;
import edu.cens.loci.classes.WeakAsyncTask;
import edu.cens.loci.classes.EntityDelta.ValuesDelta;
import edu.cens.loci.classes.PlacesSource.EditType;
import edu.cens.loci.provider.LociContract;
import edu.cens.loci.provider.LociDbUtils;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.GpsCircleArea;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.WifiFingerprint;
import edu.cens.loci.ui.widget.BasePlaceEditorView;
import edu.cens.loci.ui.widget.Editor;
import edu.cens.loci.ui.widget.MapEditorView;
import edu.cens.loci.ui.widget.PhotoEditorView;
import edu.cens.loci.ui.widget.Editor.EditorListener;
import edu.cens.loci.utils.EmptyService;
import edu.cens.loci.utils.MyDateUtils;
import edu.cens.loci.utils.MyLog;

public class PlaceEditActivity extends MapActivity 
				implements View.OnClickListener, Comparator<EntityDelta> {

	private static final String TAG = "PlaceEditActivity";
	

	//private static final int DIALOG_CONFIRM_DELETE = 1;
	
	private static final File PHOTO_DIR = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/Camera");
	
	private File mCurrentPhotoFile;
	
  private static final String KEY_EDIT_STATE = "state";
  private static final String KEY_PLACE_ID_REQUESTING_PHOTO = "photorequester";
  private static final String KEY_PLACE_ID_REQUESTING_MAP = "maprequester";
  private static final String KEY_VIEW_ID_GENERATOR = "viewidgenerator";
  private static final String KEY_QUERY_SELECTION = "queryselection";
  private static final String KEY_CURRENT_PHOTO_FILE = "currentphotofile";

  public static final int SAVE_MODE_DEFAULT = 0;
  public static final int SAVE_MODE_SPLIT = 1;
  public static final int SAVE_MODE_JOIN = 2;

  String mQuerySelection;

  private static final int STATUS_LOADING = 0;
  private static final int STATUS_EDITING = 1;
  private static final int STATUS_SAVING = 2;
  
  public static final int REQUEST_CODE_MAP_EDIT = 3020;
  /** The launch code when picking a photo and the raw data is returned */
  private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 3021;

  /** The launch code when taking a picture */
  private static final int REQUEST_CODE_CAMERA_WITH_DATA = 3023;


  private static final int ICON_SIZE = 96;

  private long mPlaceIdRequestingPhoto = -1;
  private long mPlaceIdRequestingMap = -1;
  
	private int mStatus;
  private boolean mActivityActive;  // true after onCreate/onResume, false at onPause
  private long mSuggestedPlaceId = -1;
  
  EntitySet mState;

  // The linear layout holding the PlaceEditorViews 
	private LinearLayout mContent;

  private ArrayList<Dialog> mManagedDialogs = new ArrayList<Dialog>();
	
	private ViewIdGenerator mViewIdGenerator;
	
	private LociDbUtils mDbUtils = null;

	@Override
	protected void onCreate(Bundle icicle) {
		//Log.i(TAG, "onCreate");
		super.onCreate(icicle);
		
		mDbUtils = new LociDbUtils(this);
		
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		setContentView(R.layout.place_edit);
		
		// Build editor and listen for photo requests
		mContent = (LinearLayout) findViewById(R.id.editors);
		
		findViewById(R.id.btn_done).setOnClickListener(this);
		findViewById(R.id.btn_discard).setOnClickListener(this);
		
		// Handle initial actions only when existing state missing
		final boolean hasIncomingState = icicle != null && icicle.containsKey(KEY_EDIT_STATE);
		
		mActivityActive = true;
		
		if (icicle == null) {
			mViewIdGenerator = new ViewIdGenerator();
		}
		
		if (Intent.ACTION_EDIT.equals(action) && !hasIncomingState) {
			setTitle(R.string.editPlace_title_edit);
			mStatus = STATUS_LOADING;
			new QueryEntitiesTask(this).execute(intent);
		} else if (Intent.ACTION_INSERT.equals(action) && !hasIncomingState) {
			setTitle(R.string.editPlace_title_insert);
			mStatus = STATUS_EDITING;
			doAddAction();
		} else if (Intents.UI.ACTION_CREATE_FROM_SUGGESTED_PLACE.equals(action) && !hasIncomingState) {
			setTitle(R.string.editPlace_title_insert);
			new QueryEntitiesTask(this).execute(intent);
		} else if (Intents.UI.ACTION_ADDTO_FROM_SUGESTED_PLACE.equals(action) && !hasIncomingState) {
			Bundle extras = intent.getExtras();
			mSuggestedPlaceId = extras.getLong(Intents.UI.SELECTED_PLACE_ID_EXTRA_KEY);
			ArrayList<LociWifiFingerprint> fingerprints = mDbUtils.getWifiFingerprint(mSuggestedPlaceId);

			if (fingerprints.size() != 1) {
				MyLog.d(LociConfig.D.ERROR, TAG, "suggested place should have 1, but this has " + fingerprints.size());
				Toast.makeText(this, "Selected place has " + fingerprints.size() + " Wi-Fi fingerprint.", Toast.LENGTH_LONG).show();
				setResult(RESULT_CANCELED, null);
				finish();
				return;
			}
			
			intent.putExtra(Insert.WIFI_FINTERPRINT, fingerprints.get(0).toJsonString());
			String time = String.valueOf(fingerprints.get(0).getEnter());
			intent.putExtra(Insert.TIME, time);
			
			this.setIntent(intent);
			
			setTitle("Add Wi-Fi fingerprint to an existing place");
			new QueryEntitiesTask(this).execute(intent);
		}
		


	}
	
	@Override
	protected void onResume() {
		//Log.i(TAG, "onResume");
		super.onResume();
		mActivityActive = true;
	}
	
	@Override
	protected void onPause() {
		//Log.i(TAG, "onPause");
		super.onPause();
		mActivityActive = false;
	}
	
	private static class QueryEntitiesTask extends
					WeakAsyncTask<Intent, Void, EntitySet, PlaceEditActivity> {

		private String mSelection;
		
		public QueryEntitiesTask(PlaceEditActivity target) {
			super(target);
		}
		
		@Override
		protected EntitySet doInBackground(PlaceEditActivity target, Intent... params) {
			final Intent intent = params[0];
			
			final ContentResolver resolver = target.getContentResolver();
			
			final Uri data = intent.getData();
			final String authority = data.getAuthority();
			final String mimeType = intent.resolveType(resolver);
			
			//MyLog.d(true, TAG, "doInBackground:");
			//MyLog.d(true, TAG, "data:" + data.toString());
			//MyLog.d(true, TAG, "autority:" + authority);
			//MyLog.d(true, TAG, "mimeType:" + mimeType);
			
			mSelection = "0";
			if (LociContract.AUTHORITY.equals(authority)) {
				if (Places.CONTENT_ITEM_TYPE.equals(mimeType)) {
					final long placeId = ContentUris.parseId(data);
					mSelection = Places._ID + "=" + placeId;
				}
			}
			
			//Log.d(TAG, "mSelection = " + mSelection);
			
			return EntitySet.fromQuery(target.getContentResolver(), mSelection, null, null);
		}
		
		@Override
	  protected void onPostExecute(PlaceEditActivity target, EntitySet entitySet) {
      target.mQuerySelection = mSelection;

      // Load edit details in background
      final Context context = target;
      final Sources sources = Sources.getInstance(context);

      // Handle any incoming values that should be inserted
      final Bundle extras = target.getIntent().getExtras();
      final boolean hasExtras = extras != null && extras.size() > 0;
      final boolean hasState = entitySet.size() > 0;
      
      //if (extras != null)
      //	Log.d(TAG, "onPostExecute: " + extras.toString());
      
      if (hasExtras && hasState) {
      	
      		//Log.d(TAG, "onPostExecute: hasExtras and hasState");
      	
          // Find source defining the first RawContact found
          final EntityDelta state = entitySet.get(0);
          final String accountType = state.getValues().getAsString(Constants.ACCOUNT_TYPE);
          final PlacesSource source = sources.getInflatedSource(accountType,
                  PlacesSource.LEVEL_CONSTRAINTS);
          EntityModifier.parseExtras(context, source, state, extras);
      }
      target.mState = entitySet;

      // Bind UI to new background state
      target.bindEditors();
		}
		
	}
	
  @Override
  protected void onSaveInstanceState(Bundle outState) {
  	
  	//Log.i(TAG, "onSaveInstanceState()");
  	
  	//Log.i(TAG, mState.toString());
  	
      if (hasValidState()) {
          // Store entities with modifications
      	//Log.i(TAG, "save mState.");
          outState.putParcelable(KEY_EDIT_STATE, mState);
      } else {
      	//Log.i(TAG, "not saving mState.");
      }

      outState.putLong(KEY_PLACE_ID_REQUESTING_PHOTO, mPlaceIdRequestingPhoto);
      outState.putLong(KEY_PLACE_ID_REQUESTING_MAP, mPlaceIdRequestingMap);
      
      outState.putParcelable(KEY_VIEW_ID_GENERATOR, mViewIdGenerator);
      if (mCurrentPhotoFile != null) {
          outState.putString(KEY_CURRENT_PHOTO_FILE, mCurrentPhotoFile.toString());
      }
      outState.putString(KEY_QUERY_SELECTION, mQuerySelection);
      //outState.putLong(KEY_CONTACT_ID_FOR_JOIN, mContactIdForJoin);
      super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
  	
  	//Log.i(TAG, "onRestoreInstanceState()");
  	  // Read modifications from instance
      mState = savedInstanceState.<EntitySet> getParcelable(KEY_EDIT_STATE);
      mPlaceIdRequestingPhoto = savedInstanceState.getLong(
              KEY_PLACE_ID_REQUESTING_PHOTO);
      mPlaceIdRequestingMap = savedInstanceState.getLong(
      		    KEY_PLACE_ID_REQUESTING_MAP);
      mViewIdGenerator = savedInstanceState.getParcelable(KEY_VIEW_ID_GENERATOR);
      String fileName = savedInstanceState.getString(KEY_CURRENT_PHOTO_FILE);
      if (fileName != null) {
          mCurrentPhotoFile = new File(fileName);
      }
      mQuerySelection = savedInstanceState.getString(KEY_QUERY_SELECTION);
      //mContactIdForJoin = savedInstanceState.getLong(KEY_CONTACT_ID_FOR_JOIN);

      //if (mState != null)
      //	Log.i(TAG, mState.toString());
      //else
      //	Log.i(TAG, "mState is null.");
      
      bindEditors();

      super.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  protected void onDestroy() {
  	
  	//Log.i(TAG, "onDestroy()");
    super.onDestroy();

    for (Dialog dialog : mManagedDialogs) {
        dismissDialog(dialog);
    }
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

  /**
   * Check if our internal {@link #mState} is valid, usually checked before
   * performing user actions.
   */
  protected boolean hasValidState() {
      return mStatus == STATUS_EDITING && mState != null && mState.size() > 0;
  }

	protected void bindEditors() {
		if (mState == null) {
			//Log.i(TAG, "bindEditors(): mState is null");
			return;
		}
		
		final LayoutInflater inflater = (LayoutInflater) getSystemService(
							Context.LAYOUT_INFLATER_SERVICE);
		final Sources sources = Sources.getInstance(this);
		
		// Sort the editors
		Collections.sort(mState, this);
		
		mContent.removeAllViews();
		
		// get related data rows
		int size = mState.size();
		for (int i=0; i<size; i++) {
			
			EntityDelta entity = mState.get(i);
			final ValuesDelta values = entity.getValues();
			if (!values.isVisible()) continue;

			final String accountType = Constants.ACCOUNT_TYPE;
			final PlacesSource source = sources.getInflatedSource(accountType, 
							PlacesSource.LEVEL_CONSTRAINTS);
			final long placeId = values.getAsLong(Places._ID);
			
			//MyLog.d(true, TAG, "bindEditors: placeId=" + placeId);
			
			BasePlaceEditorView editor = (BasePlaceEditorView) inflater.inflate(R.layout.item_place_editor, mContent, false);
			
			PhotoEditorView photoEditor = editor.getPhotoEditor();
			photoEditor.setEditorListener(new PhotoListener(placeId, source.readOnly, photoEditor));
			
			editor.getPlaceId();
			
			//Put empty name when status is suggested
			if (values.getAsInteger(Places.PLACE_STATE) == Places.STATE_SUGGESTED)
				values.put(Places.PLACE_NAME, "");
			
			//Check Place Position
			boolean hasEntries = entity.hasMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE);
  		if (!hasEntries) {
  			
  			//Log.d(TAG, "has wifi timestamp : " + getIntent().getExtras().containsKey(Insert.TIME));
  			
  			if (values.getAsInteger(Places.PLACE_STATE) == Places.STATE_SUGGESTED) {
  				addPlacePosition(entity, source, placeId);
  			} else if (getIntent().getExtras().containsKey(Insert.TIME)) {
  				addPlacePositionByTime(entity, source, Long.valueOf(getIntent().getExtras().getString(Insert.TIME)));
  			}
  		}
  		MapEditorView mapEditor = editor.getMapEditor();
  		mapEditor.setEditorListener(new MapListener(placeId, source.readOnly, mapEditor));
			
			mContent.addView(editor);
			
			//Log.d(TAG, "buildEditors: " + entity.toString());
			
			editor.setState(entity, source, mViewIdGenerator);
		}
		
		mContent.setVisibility(View.VISIBLE);
		mStatus = STATUS_EDITING;
	}
	
	private void addPlacePositionByTime(EntityDelta entity, PlacesSource source, long time) {
		LociLocation after = mDbUtils.getFirstLocationBeforeOrAfterTime(time, false);
		LociLocation before = mDbUtils.getFirstLocationBeforeOrAfterTime(time, true);
		
		LociLocation placeLoc = null;
		String extra = "";
		long offTime = 0;
		
		
		if (before == null && after != null) {
			placeLoc = after;
			offTime = after.getTime() - time;
			
			//Log.d(TAG, "before==null : " + offTime);
			
			extra = MyDateUtils.humanReadableDuration(offTime, 2) + " after"; 
		} else if (after == null && before != null) {
			placeLoc = before;
			offTime = time - before.getTime();
			extra = MyDateUtils.humanReadableDuration(offTime, 2) + " before";

			//Log.d(TAG, "after==null : " + offTime);

		
		} else if (before != null && after != null) {
			if (Math.abs(after.getTime()-time) > Math.abs(before.getTime()-time)) {
				placeLoc = before;
				offTime = time-before.getTime();
				if (offTime == 0) 
					extra = " while staying";
				else 
					extra = MyDateUtils.humanReadableDuration(offTime, 2) + " before";			
				//Log.d(TAG, "befor : " + offTime);

			} else {
				placeLoc = after;
				offTime = after.getTime()-time;
				if (offTime == 0)
					extra = " while staying";
				else
					extra = MyDateUtils.humanReadableDuration(offTime, 2) + " after";
				//Log.d(TAG, "after : " + offTime);
			}
		} else {
			return;
		}
		
		EntityModifier.insertChild(entity, source.getKindForMimetype(GpsCircleArea.CONTENT_ITEM_TYPE));
		for (ValuesDelta entry : entity.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE)) {
			entry.put(GpsCircleArea.LATITUDE, placeLoc.getLatitude());
			entry.put(GpsCircleArea.LONGITUDE, placeLoc.getLongitude());
			entry.put(GpsCircleArea.RADIUS, placeLoc.getAccuracy());
			entry.put(GpsCircleArea.EXTRA1, extra);
		}
	}
	
	private void addPlacePosition(EntityDelta entity, PlacesSource source, long placeId) {
		
		LociCircleArea circle = mDbUtils.getPlacePositionEstimate(placeId);
		
		if (circle != null) {
			EntityModifier.insertChild(entity, source.getKindForMimetype(GpsCircleArea.CONTENT_ITEM_TYPE));
			for (ValuesDelta entry : entity.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE)) {
				entry.put(GpsCircleArea.LATITUDE, circle.getLatitude());
				entry.put(GpsCircleArea.LONGITUDE, circle.getLongitude());
				entry.put(GpsCircleArea.RADIUS, circle.getAccuracy());
				entry.put(GpsCircleArea.EXTRA1, "while staying");
			}
		} else {
			MyLog.e(LociConfig.D.UI.DEBUG, TAG, "addPlacePosition: circle is null. failed adding. placeId=" + placeId);
		}
	}
	
	private class MapListener implements EditorListener {
		private long mPlaceId;
		private boolean mReadOnly;
		private MapEditorView mEditor;
		
		public MapListener(long placeId, boolean readOnly, MapEditorView editor) {
			mPlaceId = placeId;
			mReadOnly = readOnly;
			mEditor = editor;
		}
		
		public void onRequest(int request) {
			if (!hasValidState()) return;

			if (request == EditorListener.REQUEST_EDIT_MAP) {
      	doEditMapAction(mPlaceId);
      } else {
      	//Log.i(TAG, "request != REQUEST_EDIT_MAP");
      }
		}

		public void onDeleted(Editor editor) {
			// TODO Auto-generated method stub
			
		}
	}
	
  /**
   * Class that listens to requests coming from photo editors
   */
  private class PhotoListener implements EditorListener, DialogInterface.OnClickListener {
      private long mPlaceId;
      private boolean mReadOnly;
      private PhotoEditorView mEditor;

      public PhotoListener(long rawContactId, boolean readOnly, PhotoEditorView editor) {
          mPlaceId = rawContactId;
          mReadOnly = readOnly;
          mEditor = editor;
      }

      public void onDeleted(Editor editor) {
          // Do nothing
      }

      public void onRequest(int request) {
          if (!hasValidState()) return;

          if (request == EditorListener.REQUEST_PICK_PHOTO) {
              if (mEditor.hasSetPhoto()) {
                  // There is an existing photo, offer to remove, replace, or promoto to primary
              	//Log.i(TAG, "hasSetPhoto");
              	createPhotoDialog().show();
              } else if (!mReadOnly) {
                  // No photo set and not read-only, try to set the photo
                //Log.i(TAG, "doPickPhotoAction");
              	doPickPhotoAction(mPlaceId);
              } else {
              	//Log.i(TAG, "else");
              }
          } else {
          	//Log.i(TAG, "request != REQUEST_PICK_PHOTO");
          }
      }

      /**
       * Prepare dialog for picking a new {@link EditType} or entering a
       * custom label. This dialog is limited to the valid types as determined
       * by {@link EntityModifier}.
       */
      public Dialog createPhotoDialog() {
          Context context = PlaceEditActivity.this;

          // Wrap our context to inflate list items using correct theme
          final Context dialogContext = new ContextThemeWrapper(context,
                  android.R.style.Theme_Light);

          String[] choices;
          if (mReadOnly) {
              choices = new String[1];
              choices[0] = getString(R.string.use_photo_as_primary);
          } else {
              choices = new String[3];
              choices[0] = getString(R.string.use_photo_as_primary);
              choices[1] = getString(R.string.removePicture);
              choices[2] = getString(R.string.changePicture);
          }
          final ListAdapter adapter = new ArrayAdapter<String>(dialogContext,
                  android.R.layout.simple_list_item_1, choices);

          final AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
          builder.setTitle(R.string.attachToPlace);
          builder.setSingleChoiceItems(adapter, -1, this);
          return builder.create();
      }

      /**
       * Called when something in the dialog is clicked
       */
      public void onClick(DialogInterface dialog, int which) {

      	//Log.d(TAG, "onClick: which=" + which);
      	
      	dialog.dismiss();

          switch (which) {
              case 0:
                  // Set the photo as super primary
                  //mEditor.setSuperPrimary(true);

                  // And set all other photos as not super primary
                  int count = mContent.getChildCount();
                  
                  for (int i = 0; i < count; i++) {
                      View childView = mContent.getChildAt(i);
                      if (childView instanceof BasePlaceEditorView) {
                          BasePlaceEditorView editor = (BasePlaceEditorView) childView;
                          PhotoEditorView photoEditor = editor.getPhotoEditor();
                          if (!photoEditor.equals(mEditor)) {
                              photoEditor.setSuperPrimary(false);
                          }
                      }
                  }
                  break;

              case 1:
                  // Remove the photo
                  mEditor.setPhotoBitmap(null);
                  break;

              case 2:
                  // Pick a new photo for the place
                  doPickPhotoAction(mPlaceId);
                  break;
          }
      }
  }
	
  /**
   * Pick a specific photo to be added under the currently selected tab.
   */
  boolean doPickPhotoAction(long placeId) {
      if (!hasValidState()) return false;

      mPlaceIdRequestingPhoto = placeId;

      showAndManageDialog(createPickPhotoDialog());

      return true;
  }
  
  boolean doEditMapAction(long placeId) {
  		if (!hasValidState()) return false;
  		
  		mPlaceIdRequestingMap = placeId;
  		
  		return true;
  }

  /**
   * Creates a dialog offering two options: take a photo or pick a photo from the gallery.
   */
  private Dialog createPickPhotoDialog() {
      Context context = PlaceEditActivity.this;

      // Wrap our context to inflate list items using correct theme
      final Context dialogContext = new ContextThemeWrapper(context,
              android.R.style.Theme_Light);

      String[] choices;
      choices = new String[2];
      choices[0] = getString(R.string.take_photo);
      choices[1] = getString(R.string.pick_photo);
      final ListAdapter adapter = new ArrayAdapter<String>(dialogContext,
              android.R.layout.simple_list_item_1, choices);

      final AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
      builder.setTitle(R.string.attachToPlace);
      builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
              switch(which) {
                  case 0:
                      doTakePhoto();
                      break;
                  case 1:
                      doPickPhotoFromGallery();
                      break;
              }
          }
      });
      return builder.create();
  }

  /**
   * Create a file name for the icon photo using current time.
   */
  private String getPhotoFileName() {
      Date date = new Date(System.currentTimeMillis());
      SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
      return dateFormat.format(date) + ".jpg";
  }

  /**
   * Launches Camera to take a picture and store it in a file.
   */
  protected void doTakePhoto() {
      try {
          // Launch camera to take photo for selected contact
          PHOTO_DIR.mkdirs();
          mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());
          final Intent intent = getTakePickIntent(mCurrentPhotoFile);
          startActivityForResult(intent, REQUEST_CODE_CAMERA_WITH_DATA);
      } catch (ActivityNotFoundException e) {
          Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
      }
  }

  /**
   * Constructs an intent for capturing a photo and storing it in a temporary file.
   */
  public static Intent getTakePickIntent(File f) {
      Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
      intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
      return intent;
  }

  /**
   * Sends a newly acquired photo to Gallery for cropping
   */
  protected void doCropPhoto(File f) {
      try {

          // Add the image to the media store
          MediaScannerConnection.scanFile(
                  this,
                  new String[] { f.getAbsolutePath() },
                  new String[] { null },
                  null);

          // Launch gallery to crop the photo
          final Intent intent = getCropImageIntent(Uri.fromFile(f));
          startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
      } catch (Exception e) {
          Log.e(TAG, "Cannot crop image", e);
          Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
      }
  }

  /**
   * Constructs an intent for image cropping.
   */
  public static Intent getCropImageIntent(Uri photoUri) {
      Intent intent = new Intent("com.android.camera.action.CROP");
      intent.setDataAndType(photoUri, "image/*");
      intent.putExtra("crop", "true");
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("outputX", ICON_SIZE);
      intent.putExtra("outputY", ICON_SIZE);
      intent.putExtra("return-data", true);
      return intent;
  }

  /**
   * Launches Gallery to pick a photo.
   */
  protected void doPickPhotoFromGallery() {
      try {
          // Launch picker to choose photo for selected contact
          final Intent intent = getPhotoPickIntent();
          startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
      } catch (ActivityNotFoundException e) {
          Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
      }
  }

  /**
   * Constructs an intent for picking a photo from Gallery, cropping it and returning the bitmap.
   */
  public static Intent getPhotoPickIntent() {
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
      intent.setType("image/*");
      intent.putExtra("crop", "true");
      intent.putExtra("aspectX", 1);
      intent.putExtra("aspectY", 1);
      intent.putExtra("outputX", ICON_SIZE);
      intent.putExtra("outputY", ICON_SIZE);
      intent.putExtra("return-data", true);
      return intent;
  }
  
	public void onClick(View view) {
    switch (view.getId()) {
    case R.id.btn_done:
        doSaveAction(SAVE_MODE_DEFAULT);
        break;
    case R.id.btn_discard:
        doRevertAction();
        break;
    }
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public int compare(EntityDelta arg0, EntityDelta arg1) {
		// TODO Auto-generated method stub
		return 0;
	}
	
  ///** {@inheritDoc} */
  //@Override
  public void onBackPressed() {
  	Toast.makeText(this, "Please press done or cancel.", Toast.LENGTH_SHORT).show();
  }
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		MyLog.i(LociConfig.D.UI.LIST, TAG, "onActivityResult()" + requestCode + ", " + resultCode);

		if (resultCode != RESULT_OK) { 
			MyLog.e(LociConfig.D.ERROR, TAG, "onActivityResult: NOT RESULT_OK --> return..");
			return;
		}
		
		switch(requestCode) {
		case REQUEST_CODE_PHOTO_PICKED_WITH_DATA: {
			BasePlaceEditorView requestingEditor = null;
			
			//Log.d(TAG, "REQUEST_CODE_PHOTO_PICKED_WITH_DATA getChildCount=" + mContent.getChildCount());
			
			for (int i=0; i<mContent.getChildCount(); i++) {
				View childView = mContent.getChildAt(i);
				if (childView instanceof BasePlaceEditorView) {
					
					
					BasePlaceEditorView editor = (BasePlaceEditorView) childView;

					//Log.d(TAG, "instanceof BasePlaceEditorView : placeId=" + editor.getPlaceId());
					//Log.d(TAG, " placeIdRequestingPhoto : " + mPlaceIdRequestingPhoto);
					
					if (editor.getPlaceId() == mPlaceIdRequestingPhoto) {
						requestingEditor = editor;
						//Log.i(TAG, "found requestingPlace : " + mPlaceIdRequestingPhoto);
						break;
					}
				}
			}
			
			if (requestingEditor != null) {
				final Bitmap photo = data.getParcelableExtra("data");
				requestingEditor.setPhotoBitmap(photo);
				mPlaceIdRequestingPhoto = -1;
			} else {
				
			}
			break;
		}
		case REQUEST_CODE_CAMERA_WITH_DATA: {
			doCropPhoto(mCurrentPhotoFile);
			break;
		}
		case REQUEST_CODE_MAP_EDIT: {
			
			BasePlaceEditorView requestingEditor = null;
			
			for (int i=0; i<mContent.getChildCount(); i++) {
				View childView = mContent.getChildAt(i);
				if (childView instanceof BasePlaceEditorView) {
					BasePlaceEditorView editor = (BasePlaceEditorView) childView;
					//Log.d(TAG, "instanceof BasePlaceEditorView : placeId=" + editor.getPlaceId());
					//Log.d(TAG, " placeIdRequestingMap : " + mPlaceIdRequestingMap);
					if (editor.getPlaceId() == mPlaceIdRequestingMap) {
						requestingEditor = editor;
						//Log.i(TAG, "found requestingPlace : " + mPlaceIdRequestingMap);
						break;
					}
				}
			}
			
			if (requestingEditor != null) {
				final LociCircleArea circle = (LociCircleArea) data.getSerializableExtra("newLoc");
				//Log.d(TAG, "setMapCircle:" + circle.toString());
				requestingEditor.setMapCircle(circle);
				mPlaceIdRequestingMap = -1;
			}
			
			//updatePlacePosition(circle);
			//bindEditors();
			break;
		}
		default:
			MyLog.e(LociConfig.D.UI.LIST, TAG, "onActivityResult(): got nothing. ");
		}
	}
	
	void updatePlacePosition(LociCircleArea circle) {
		
		if (circle == null) return;
		//Log.i(TAG, "updatePlacePosition:" + circle.toString());
		Collections.sort(mState, this);
		// get related data rows
		int size = mState.size();
		
		for (int i=0; i<size; i++) {
			EntityDelta entity = mState.get(i);
			//Check Place Position
			for (ValuesDelta entry : entity.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE)) {
				entry.put(GpsCircleArea.LATITUDE, circle.getLatitude());
				entry.put(GpsCircleArea.LONGITUDE, circle.getLongitude());
				entry.put(GpsCircleArea.RADIUS, circle.getRadius());
				entry.put(GpsCircleArea.EXTRA1, "modified");
			}
			
			//Log.d(TAG, "updatePlacePosition (" + i + ")");
			//Log.d(TAG, entity.toString());
		}
		
	}
	
  /**
   * Background task for persisting edited contact data, using the changes
   * defined by a set of {@link EntityDelta}. This task starts
   * {@link EmptyService} to make sure the background thread can finish
   * persisting in cases where the system wants to reclaim our process.
   */
  public static class PersistTask extends
          WeakAsyncTask<EntitySet, Void, Integer, PlaceEditActivity> {
      private static final int PERSIST_TRIES = 3;

      private static final int RESULT_UNCHANGED = 0;
      private static final int RESULT_SUCCESS = 1;
      private static final int RESULT_FAILURE = 2;
      private static final int RESULT_NO_NAME = 3;
      private static final int RESULT_NO_WIFI = 4;
      private static final int RESULT_NO_GPS  = 5;

      private WeakReference<ProgressDialog> mProgress;

      private int mSaveMode;
      private Uri mPlaceLookupUri = null;

      public PersistTask(PlaceEditActivity target, int saveMode) {
          super(target);
          mSaveMode = saveMode;
      }

      /** {@inheritDoc} */
      @Override
      protected void onPreExecute(PlaceEditActivity target) {
          mProgress = new WeakReference<ProgressDialog>(ProgressDialog.show(target, null,
                  target.getText(R.string.savingPlace)));

          // Before starting this task, start an empty service to protect our
          // process from being reclaimed by the system.
          final Context context = target;
          context.startService(new Intent(context, EmptyService.class));
      }

      /** {@inheritDoc} */
      @Override
      protected Integer doInBackground(PlaceEditActivity target, EntitySet... params) {
          final Context context = target;
          final ContentResolver resolver = context.getContentResolver();

          EntitySet state = params[0];

          // Trim any empty fields, and RawContacts, before persisting
          final Sources sources = Sources.getInstance(context);
          EntityModifier.trimEmpty(state, sources);

          // check essentials
          EntityDelta entity = state.get(0);
    			final ValuesDelta values = entity.getValues();
          
    			
    			Log.e(TAG, "[CHECK] type=" + values.getAsInteger(Places.PLACE_TYPE));
    			Log.e(TAG, " wifi? " + entity.getValues().getAfter().toString());
    			Log.e(TAG, " gps? " + entity.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE).size());
    			
     			Log.d(TAG, entity.toString());
     			Log.d(TAG, entity.getMimeEntries(WifiFingerprint.CONTENT_ITEM_TYPE).toString());
     			Log.d(TAG, entity.getMimeEntries(GpsCircleArea.CONTENT_ITEM_TYPE).toString());
    			
     			Log.d(TAG, "wifi: " + entity.getMimeEntriesValidCount(WifiFingerprint.CONTENT_ITEM_TYPE));
     			Log.d(TAG, "gps=" + entity.getMimeEntriesValidCount(GpsCircleArea.CONTENT_ITEM_TYPE));
     			//Log.d(TAG, "tag=" + entity.getMimeEntriesValidCount(Keyword.CONTENT_ITEM_TYPE));
     			
    			//return RESULT_FAILURE;
    			
    			
    			String placeName = values.getAsString(Places.PLACE_NAME);
    			if (placeName == null || TextUtils.getTrimmedLength(placeName) == 0)
    				return RESULT_NO_NAME;
    			
    			int placeType = values.getAsInteger(Places.PLACE_TYPE);
    			switch(placeType) {
    			case Places.TYPE_GPS:
    				if (entity.getMimeEntriesValidCount(GpsCircleArea.CONTENT_ITEM_TYPE) == 0)
    					return RESULT_NO_GPS;
    				break;
    			case Places.TYPE_WIFI:
    				if (entity.getMimeEntriesValidCount(WifiFingerprint.CONTENT_ITEM_TYPE) == 0)
    					return RESULT_NO_WIFI;
    				break;
    			}
    			
    			values.put(Places.PLACE_STATE, Places.STATE_REGISTERED);
    			
          // Attempt to persist changes
          int tries = 0;
          Integer result = RESULT_FAILURE;
          while (tries++ < PERSIST_TRIES) {
              try {
                  // Build operations and try applying
                  final ArrayList<ContentProviderOperation> diff = state.buildDiff();
                  
                  ContentProviderResult[] results = null;
                  if (!diff.isEmpty()) {
                  	//for (ContentProviderOperation d : diff) {
                  	//	Log.e(TAG, "diff:" + d.toString());
                  	//}
                    results = resolver.applyBatch(LociContract.AUTHORITY, diff);
                    //Log.e(TAG, "applied batch.");
                  }

                  result = (diff.size() > 0) ? RESULT_SUCCESS : RESULT_UNCHANGED;
                  
                  final long placeId = getPlaceId(state, diff, results);
                  
                	//Log.d(TAG, "doInBackground: placeId" + placeId);
                  
                  
                  if (placeId != -1) {
                      
                  	mPlaceLookupUri = ContentUris.withAppendedId(
                        Places.CONTENT_URI, placeId);

                  	//Log.d(TAG, "doInBackground: " + mPlaceLookupUri.toString());
                  	
                  	//final Uri placeUri = ContentUris.withAppendedId(
                      //        Places.CONTENT_URI, placeId);

                      // convert the raw contact URI to a contact URI
                      //mPlaceLookupUri = RawContacts.getContactLookupUri(resolver,
                      //        placeUri);
                  }
                  break;

                  
                  
              } catch (RemoteException e) {
                  // Something went wrong, bail without success
                  Log.e(TAG, "Problem persisting user edits", e);
                  break;

              } catch (OperationApplicationException e) {
                  // Version consistency failed, re-parent change and try again
                  Log.w(TAG, "Version consistency failed, re-parenting: " + e.toString());
                  final EntitySet newState = EntitySet.fromQuery(resolver,
                          target.mQuerySelection, null, null);
                  state = EntitySet.mergeAfter(newState, state);
              }
          }

          return result;
          
      }

      private long getPlaceId(EntitySet state,
              final ArrayList<ContentProviderOperation> diff,
              final ContentProviderResult[] results) {
      	
          long placeId = state.findPlaceId();
          if (placeId != -1) {
              return placeId;
          }
          
          // we gotta do some searching for the id
          for (ContentProviderResult result : results) {
          	if (result.uri.getEncodedPath().contains(Places.CONTENT_URI.getEncodedPath())) {
          		return ContentUris.parseId(result.uri);
          	}
          }
          
          return -1;
      }

      /** {@inheritDoc} */
      @Override
      protected void onPostExecute(PlaceEditActivity target, Integer result) {
      	
        //Log.i(TAG, "PersitsTask.onPostExecute =" + result);
      	
          final Context context = target;
          final ProgressDialog progress = mProgress.get();

          if (result == RESULT_NO_NAME) {
          	Toast.makeText(context, "Place name cannot be empty", Toast.LENGTH_SHORT).show();
          	result = RESULT_FAILURE;
          } else if (result == RESULT_NO_WIFI) {
          	Toast.makeText(context, "Need at least one wifi fingerprint", Toast.LENGTH_SHORT).show();
          	result = RESULT_FAILURE;
          } else if (result == RESULT_NO_GPS) {
          	Toast.makeText(context, "Need a gps area", Toast.LENGTH_SHORT).show();
          	result = RESULT_FAILURE;
          }
          
          
          if (result == RESULT_SUCCESS && mSaveMode != SAVE_MODE_JOIN) {
              Toast.makeText(context, R.string.placeSavedToast, Toast.LENGTH_SHORT).show();
          } else if (result == RESULT_FAILURE) {
              Toast.makeText(context, R.string.placeSavedErrorToast, Toast.LENGTH_LONG).show();
          }

          dismissDialog(progress);

          // Stop the service that was protecting us
          context.stopService(new Intent(context, EmptyService.class));

          target.onSaveCompleted(result != RESULT_FAILURE, mSaveMode, mPlaceLookupUri);
      }
  }
  
  private void onSaveCompleted(boolean success, int saveMode, Uri placeLookupUri) {

    //Log.i(TAG, "onSaveCompleted");
  	
  	switch (saveMode) {
        case SAVE_MODE_DEFAULT:
          if (success && placeLookupUri != null) {
        		
        			if (mSuggestedPlaceId != -1) {
        				//mDbUtils.updatePlaceState(mSuggestedPlaceId, Places.STATE_MERGED);
        				long placeId = ContentUris.parseId(placeLookupUri);
        				mDbUtils.changeVisitPlaceIdAll(mSuggestedPlaceId, placeId);
        				mDbUtils.deletePlace(mSuggestedPlaceId);
        			}
        		
              final Intent resultIntent = new Intent();
              //Log.d(TAG, "onSaveComplete: " + placeLookupUri);
              resultIntent.setData(placeLookupUri);
              	/*

                final Uri requestData = getIntent().getData();
                final String requestAuthority = requestData == null ? null : requestData
                        .getAuthority();
                if (android.provider.Contacts.AUTHORITY.equals(requestAuthority)) {
                    // Build legacy Uri when requested by caller
                    final long contactId = ContentUris.parseId(Contacts.lookupContact(
                            getContentResolver(), contactLookupUri));
                    final Uri legacyUri = ContentUris.withAppendedId(
                            android.provider.Contacts.People.CONTENT_URI, contactId);
                    resultIntent.setData(legacyUri);
                } else {
                    // Otherwise pass back a lookup-style Uri
                    resultIntent.setData(contactLookupUri);
                }
								*/
                
                //Log.i(TAG, "SAVE_MODE_DEFAULT, success!");
                //mDbUtils.checkDataTable();
                
              setResult(RESULT_OK, resultIntent);
            } else {
            	//Log.i(TAG, "SAVE_MODE_DEFAULT, canceled.");
            	setResult(RESULT_CANCELED, null);
            }
            finish();
            break;
    }
  }
  
  /**
   * Revert any changes the user has made, and finish the activity.
   */
  private boolean doRevertAction() {
  		setResult(RESULT_CANCELED, null);
      finish();
      return true;
  }
  
  /**
   * Saves or creates the contact based on the mode, and if successful
   * finishes the activity.
   */
  boolean doSaveAction(int saveMode) {
      if (!hasValidState()) {
          return false;
      }

      //Log.i(TAG, "doSaveAction");
      mStatus = STATUS_SAVING;
      final PersistTask task = new PersistTask(this, saveMode);
      task.execute(mState);

      return true;
  }
  

  /**
   * Create a new {@link RawContacts} which will exist as another
   * {@link EntityDelta} under the currently edited {@link Contacts}.
   */
  private boolean doAddAction() {
      if (mStatus != STATUS_EDITING) {
          return false;
      }

      // Adding is okay when missing state
      createPlace();
      return true;
  }
  
  /**
   * @param account may be null to signal a device-local contact should
   *     be created.
   */
  private void createPlace() {
      final Sources sources = Sources.getInstance(this);
      final ContentValues values = new ContentValues();
      //if (account != null) {
      //    values.put(RawContacts.ACCOUNT_NAME, account.name);
      //    values.put(RawContacts.ACCOUNT_TYPE, account.type);
      //} else {
      values.putNull(Places.ACCOUNT_NAME);
      values.putNull(Places.ACCOUNT_TYPE);
      //}

      long time = Calendar.getInstance().getTimeInMillis();
      
      values.put(Places.PLACE_TYPE, Places.TYPE_WIFI);
      values.put(Places.PLACE_STATE, Places.STATE_REGISTERED);
      values.put(Places.ENTRY, getIntent().getExtras().getInt(Intents.UI.PLACE_ENTRY_TYPE_EXTRA_KEY));
      values.put(Places.ENTRY_TIME, time);
      values.put(Places.REGISTER_TIME, time);
      
      // Parse any values from incoming intent
      EntityDelta insert = new EntityDelta(ValuesDelta.fromAfter(values));
      final PlacesSource source = sources.getInflatedSource(
          null,
          PlacesSource.LEVEL_CONSTRAINTS);
      final Bundle extras = getIntent().getExtras();
      EntityModifier.parseExtras(this, source, insert, extras);

      // Ensure we have some default fields
      //EntityModifier.ensureKindExists(insert, source, Phone.CONTENT_ITEM_TYPE);
      //EntityModifier.ensureKindExists(insert, source, Email.CONTENT_ITEM_TYPE);

      // Create "My Contacts" membership for Google contacts
      // TODO: move this off into "templates" for each given source
      //if (GoogleSource.ACCOUNT_TYPE.equals(source.accountType)) {
      //    GoogleSource.attemptMyContactsMembership(insert, this);
      //}

      if (mState == null) {
          // Create state if none exists yet
          mState = EntitySet.fromSingle(insert);
      } else {
          // Add contact onto end of existing state
          mState.add(insert);
      }

      bindEditors();
  }

	
}
