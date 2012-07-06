package edu.cens.loci.classes;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Entity;
import android.content.EntityIterator;
import android.os.Parcel;
import android.os.Parcelable;
import edu.cens.loci.classes.EntityDelta.ValuesDelta;
import edu.cens.loci.provider.LociContract.Places;
import edu.cens.loci.provider.LociContract.PlacesEntity;

/**
 * Container for multiple {@link EntityDelta} objects, usually when editing
 * together as an entire aggregate. Provides convenience methods for parceling
 * and applying another {@link EntitySet} over it.
 */
public class EntitySet extends ArrayList<EntityDelta> implements Parcelable {

		public static final String TAG = "EntitySet";
	
		private static final long	serialVersionUID	= 2677148609416143354L;
		//private boolean mSplitRawContacts;

    private EntitySet() {
    }

    /**
     * Create an {@link EntitySet} that contains the given {@link EntityDelta},
     * usually when inserting a new {@link Contacts} entry.
     */
    public static EntitySet fromSingle(EntityDelta delta) {
        final EntitySet state = new EntitySet();
        state.add(delta);
        return state;
    }

    /**
     * Create an {@link EntitySet} based on {@link Contacts} specified by the
     * given query parameters. This closes the {@link EntityIterator} when
     * finished, so it doesn't subscribe to updates.
     */
    public static EntitySet fromQuery(ContentResolver resolver, String selection,
            String[] selectionArgs, String sortOrder) {
    	
    	  //Log.i("EntitySet", "fromQuery: selection=" + selection);
    	
        EntityIterator iterator = Places.newEntityIterator(resolver.query(
                PlacesEntity.CONTENT_URI, null, selection, selectionArgs,
                sortOrder));
        
        try {
            final EntitySet state = new EntitySet();
            // Perform background query to pull contact details
            
            //Log.d(TAG, "fromQuery: state=" + state.toString());
            
            while (iterator.hasNext()) {
                // Read all contacts into local deltas to prepare for edits
                final Entity before = iterator.next();
                
                //Log.d(TAG, "fromQuery: before=" + before.toString());
                
                final EntityDelta entity = EntityDelta.fromBefore(before);
                state.add(entity);
                //Log.d(TAG, "add: entityDelta=" + entity.toString());
                //Log.d(TAG, "fromQuery: state=" + state.toString());
            }
            
            //Log.d(TAG, "fromQuery: state=" + state.toString());
            return state;
        } finally {
            iterator.close();
        }
    }
    

    /**
     * Merge the "after" values from the given {@link EntitySet}, discarding any
     * previous "after" states. This is typically used when re-parenting user
     * edits onto an updated {@link EntitySet}.
     */
    public static EntitySet mergeAfter(EntitySet local, EntitySet remote) {
        if (local == null) local = new EntitySet();

        // For each entity in the remote set, try matching over existing
        for (EntityDelta remoteEntity : remote) {
            final Long placeId = remoteEntity.getValues().getId();

            // Find or create local match and merge
            final EntityDelta localEntity = local.getByPlaceId(placeId);
            final EntityDelta merged = EntityDelta.mergeAfter(localEntity, remoteEntity);

            if (localEntity == null && merged != null) {
                // No local entry before, so insert
                local.add(merged);
            }
        }

        return local;
    }

    /**
     * Build a list of {@link ContentProviderOperation} that will transform all
     * the "before" {@link Entity} states into the modified state which all
     * {@link EntityDelta} objects represent. This method specifically creates
     * any {@link AggregationExceptions} rules needed to groups edits together.
     */
    public ArrayList<ContentProviderOperation> buildDiff() {
        final ArrayList<ContentProviderOperation> diff = new ArrayList<ContentProviderOperation>();

        //final long placeId = this.findPlaceId();
        //int firstInsertRow = -1;

        // First pass enforces versions remain consistent
        for (EntityDelta delta : this) {
            delta.buildAssert(diff);
        }

        final int assertMark = diff.size();
        int backRefs[] = new int[size()];

        int placeIndex = 0;

        // Second pass builds actual operations
        for (EntityDelta delta : this) {
            final int firstBatch = diff.size();
            final boolean isInsert = delta.isPlaceInsert();
            backRefs[placeIndex++] = isInsert ? firstBatch : -1;

            delta.buildDiff(diff);

            // Only create rules for inserts
            if (!isInsert) continue;

            // If we are going to split all contacts, there is no point in first combining them
            //if (mSplitRawContacts) continue;
        }


        // No real changes if only left with asserts
        if (diff.size() == assertMark) {
            diff.clear();
        }
        
        return diff;
    }

    /**
     * Search all contained {@link EntityDelta} for the first one with an
     * existing {@link Place#_ID} value. Usually used when creating
     * {@link AggregationExceptions} during an update.
     */
    public long findPlaceId() {
        for (EntityDelta delta : this) {
            final Long placeId = delta.getValues().getAsLong(Places._ID);
            if (placeId != null && placeId >= 0) {
                return placeId;
            }
        }
        return -1;
    }

    /**
     * Find {@link RawContacts#_ID} of the requested {@link EntityDelta}.
     */
    public Long getPlaceId(int index) {
        if (index >= 0 && index < this.size()) {
            final EntityDelta delta = this.get(index);
            final ValuesDelta values = delta.getValues();
            if (values.isVisible()) {
                return values.getAsLong(Places._ID);
            }
        }
        return null;
    }

    public EntityDelta getByPlaceId(Long placeId) {
        final int index = this.indexOfPlaceId(placeId);
        return (index == -1) ? null : this.get(index);
    }

    /**
     * Find index of given {@link RawContacts#_ID} when present.
     */
    public int indexOfPlaceId(Long placeId) {
        if (placeId == null) return -1;
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            final Long currentId = getPlaceId(i);
            if (placeId.equals(currentId)) {
                return i;
            }
        }
        return -1;
    }

    //public void splitRawContacts() {
    //    mSplitRawContacts = true;
    //}

    /** {@inheritDoc} */
    public int describeContents() {
        // Nothing special about this parcel
        return 0;
    }

    /** {@inheritDoc} */
    public void writeToParcel(Parcel dest, int flags) {
        final int size = this.size();
        dest.writeInt(size);
        for (EntityDelta delta : this) {
            dest.writeParcelable(delta, flags);
        }
    }

    public void readFromParcel(Parcel source) {
        final ClassLoader loader = getClass().getClassLoader();
        final int size = source.readInt();
        for (int i = 0; i < size; i++) {
            this.add(source.<EntityDelta> readParcelable(loader));
        }
    }

    public static final Parcelable.Creator<EntitySet> CREATOR = new Parcelable.Creator<EntitySet>() {
        public EntitySet createFromParcel(Parcel in) {
            final EntitySet state = new EntitySet();
            state.readFromParcel(in);
            return state;
        }

        public EntitySet[] newArray(int size) {
            return new EntitySet[size];
        }
    };
}
