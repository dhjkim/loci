package edu.cens.loci.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.ContactsContract.RawContacts;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import edu.cens.loci.classes.EntityDelta;
import edu.cens.loci.classes.LociCircleArea;
import edu.cens.loci.classes.PlacesSource;
import edu.cens.loci.provider.LociContract.CommonPlaceDataKinds.Photo;
import edu.cens.loci.ui.ViewIdGenerator;

public abstract class BasePlaceEditorView extends LinearLayout {
  protected LayoutInflater mInflater;

  protected PhotoEditorView mPhoto;
  protected boolean mHasPhotoEditor = false;
  
  protected MapEditorView mMap;
  protected boolean mHasMapEditor = false;

  public BasePlaceEditorView(Context context) {
      super(context);
  }

  public BasePlaceEditorView(Context context, AttributeSet attrs) {
      super(context, attrs);
  }

  /**
   * Assign the given {@link Bitmap} to the internal {@link PhotoEditorView}
   * for the {@link EntityDelta} currently being edited.
   */
  public void setPhotoBitmap(Bitmap bitmap) {
      mPhoto.setPhotoBitmap(bitmap);
  }
  
  public void setMapCircle(LociCircleArea circle) {
  	mMap.setCircle(circle);
  }

  /**
   * Return true if the current {@link RawContacts} supports {@link Photo},
   * which means that {@link PhotoEditorView} is enabled.
   */
  public boolean hasPhotoEditor() {
      return mHasPhotoEditor;
  }
  
  public boolean hasMapEditor() {
  	return mHasMapEditor;
  }

  /**
   * Return true if internal {@link PhotoEditorView} has a {@link Photo} set.
   */
  public boolean hasSetPhoto() {
      return mPhoto.hasSetPhoto();
  }

  public boolean hasSetMap() {
  	return mMap.hasSetCircle();
  }
  
  public PhotoEditorView getPhotoEditor() {
      return mPhoto;
  }
  
  public MapEditorView getMapEditor() {
  	return mMap;
  }

  /**
   * @return the Place ID that this editor is editing.
   */
  public abstract long getPlaceId();

  /**
   * Set the internal state for this view, given a current
   * {@link EntityDelta} state and the {@link ContactsSource} that
   * apply to that state.
   */
	public void setState(EntityDelta state, PlacesSource source, ViewIdGenerator vig) {
		// TODO Auto-generated method stub
		
	}

  /**
   * Sets the {@link EditorListener} on the name field
   */
  //public abstract void setNameEditorListener(EditorListener listener);

}
