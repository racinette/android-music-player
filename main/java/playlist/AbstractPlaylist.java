package playlist;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import audioeffect.EffectBundle;
import helper.IndexTransformer;

public abstract class AbstractPlaylist implements Presetable {

    public static final int UNDEFINED = -1;

    private String title;

    private boolean shuffled;
    private boolean editable;

    private ArrayList<Track> tracks;
    private int[] shuffledIndices;

    private EffectBundle preset;

    private int lastAccessedTrackIndex;

    private OnShuffleStateChangeListener stateChangeListener;

    // size in this case means both actual size of the playlist and its lastAccessedTrackIndex
    // whenever lastAccessedTrackIndex or size change, OnSizeChangeListener is triggered (exception: getTrack(int) method)
    private OnSizeChangeListener sizeChangeListener;

    // this comparator is used to maintain a sorted order of elements inside a playlist
    private Comparator<Track> trackComparator;

    private final static Comparator<Track> titleComparator = new TitleComparator();
    final static Comparator<Track> artistComparator = new ArtistComparator();

    AbstractPlaylist(String title, boolean editable) {
        tracks = new ArrayList<>();
        this.title = title;
        this.editable = editable;
        shuffled = false;
        trackComparator = titleComparator;
        lastAccessedTrackIndex = UNDEFINED;
        preset = EffectBundle.getStandardPreset();
    }

    public void setStateChangeListener(OnShuffleStateChangeListener stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    public void setSizeChangeListener(OnSizeChangeListener sizeChangeListener) {
        this.sizeChangeListener = sizeChangeListener;
    }

    // does all the job but does not notify the listeners about it
    private void addTrackSilently(Track track){
        int position = findPlace(track);
        if (position != UNDEFINED){
            tracks.add(position, track);
            if (lastAccessedTrackIndex >= position){
                lastAccessedTrackIndex++;
            }
            track.addedToPlaylist(this);
        }
    }

    public void addTrack(Track track) {
        addTrackSilently(track);
        if (sizeChangeListener != null) sizeChangeListener.onChange(this);
    }

    private int findPlace(Track track){
        if (isEmpty()){
            return 0;
        }

        int i = UNDEFINED;

        final int APPROXIMATE_POSITION = approximateIndexOf(track);

        // if APPROXIMATE_POSITION is UNDEFINED, it means that track is already in the playlist,
        // so it doesn't need to be added
        if (APPROXIMATE_POSITION != UNDEFINED){
            // the direction in which the search is conducted
            int direction = 1;

            // previously compared value, needed to decide if the place is appropriate.
            // if the method starts from 0, there is no and can be no previous comparison
            // so previousComparison is artificially set to the value which will satisfy
            // insertion condition
            int previousComparison = APPROXIMATE_POSITION == 0 ? -direction : direction * trackComparator.compare(track, tracks.get(APPROXIMATE_POSITION - 1));

            i = APPROXIMATE_POSITION;

            boolean outOfBounds;

            do {
                outOfBounds = i < tracks.size() || i > -1;

                int compared = direction * trackComparator.compare(track, tracks.get(i));

                if (compared <= 0) {
                    if (previousComparison > 0){
                        // if previous comparison was not satisfied and the current is, then this is the right place
                        return i;
                    } else {
                        // else the previous comparison was satisfied and the direction must be changed
                        direction = -direction;
                    }
                }

                previousComparison = compared;
                i = i + direction;
            } while (!outOfBounds);

            // if index is out of bounds it means that the track is either less than every track in the playlist
            // or that it is more than them
            if (i < 0) return 0;
        }
        // returns either UNDEFINED or tracks.size()
        return i;
    }

    /**
     * The main problem with this method is that the tracks in the playlist are in sorted order.
     * So, it is hard to follow indices since when a track is added you never know which place it goes.
     * For example, three tracks return the same value from findPlace() method: 3, 3 and 3.
     * Which of the three is the first one, which is the last?
     * Where does the initial third track in the playlist go?
     *
     * For dealing with this kind of situation, PlaylistAppender was written.
     */
    public void addTracks(final AbstractPlaylist from, final int [] which){
        if (which.length > 0){
            Track lastAccessedTrack = null;
            if (lastAccessedTrackIndex > UNDEFINED) {
                lastAccessedTrack = getTrackSilently(lastAccessedTrackIndex);
            }
            if (isShuffled()) {
                // queue is used because it is not known if all tracks can be added
                // since some of them might already be in the playlist
                // and you cannot have two same tracks in one playlist
                final ArrayList<Track> addedTracks = new ArrayList<>(which.length);

                for (int index : which) {
                    final Track track = from.getTrackSilently(index);

                    if (!this.contains(track)) {
                        addedTracks.add(track);
                    }
                }

                final PlaylistAppender appender = new PlaylistAppender(this, addedTracks);

                tracks = appender.getTracks();
                shuffledIndices = appender.getShuffledIndices();

                if (stateChangeListener != null) stateChangeListener.onChange(this, true);
            } else {
                tracks.ensureCapacity(size() + which.length);
                for (int index : which) {
                    final Track track = from.getTrackSilently(index);
                    if (!tracks.contains(track)){
                        tracks.add(track);
                    }
                }
                Collections.sort(tracks, trackComparator);

                lastAccessedTrackIndex = indexOf(lastAccessedTrack);
            }

            if (sizeChangeListener != null) sizeChangeListener.onChange(this);
        }
    }

    public void unshuffle() {
        boolean wasShuffled = isShuffled();
        setShuffled(false);

        if (lastAccessedTrackIndex > UNDEFINED && wasShuffled){
            lastAccessedTrackIndex = shuffledToNormal(lastAccessedTrackIndex);
            if (stateChangeListener != null) stateChangeListener.onChange(this,false);
            if (sizeChangeListener != null) sizeChangeListener.onChange(this);
        }
    }

    public void shuffle() {
        int indexToProject = isShuffled() ? shuffledIndices[lastAccessedTrackIndex] : lastAccessedTrackIndex;

        // create a new array
        shuffledIndices = new int[size()];

        for (int i = 0; i < size(); i++) {
            shuffledIndices[i] = i;
        }

        final int previouslyAccessedTrackIndex = lastAccessedTrackIndex;
        lastAccessedTrackIndex = shuffleArray(shuffledIndices, indexToProject);

        setShuffled(true);

        if (sizeChangeListener != null && previouslyAccessedTrackIndex != lastAccessedTrackIndex) sizeChangeListener.onChange(this);
        if (stateChangeListener != null) stateChangeListener.onChange(this,true);
    }

    private static int shuffleArray(int[] ar, int indexToProject)
    {
        Random rnd = ThreadLocalRandom.current();

        if (indexToProject <= UNDEFINED){
            for (int i = ar.length - 1; i > 0; i--)
            {
                int index = rnd.nextInt(i + 1);
                // Simple swap
                int a = ar[index];
                ar[index] = ar[i];
                ar[i] = a;
            }
        } else {
            for (int i = ar.length - 1; i > 0; i--)
            {
                int index = rnd.nextInt(i + 1);
                // Simple swap
                int a = ar[index];
                ar[index] = ar[i];
                ar[i] = a;

                // they were swapped, so now one of them might be the projected index
                if (index == indexToProject || i == indexToProject){
                    // if index == indexToProject then the followed element was swapped and now is
                    // under i, and vise versa
                    indexToProject = index == indexToProject ? i : indexToProject;
                }
            }
        }
        return indexToProject;
    }

    // returns the index under which the track by the given index is placed in a shuffled version
    // of this playlist
    private int normalToShuffled(int index) {
        if (index > UNDEFINED && index < size()) {
            for (int i = 0; i < shuffledIndices.length; i++){
                if (i == index) return i;
            }
        }
        return UNDEFINED;
    }

    public int getLastAccessedTrackIndex() {
        return lastAccessedTrackIndex;
    }

    // does vice versa
    private int shuffledToNormal(int index) {
        if (index > UNDEFINED && index < size()) {
            return shuffledIndices[index];
        }
        return UNDEFINED;
    }

    public boolean contains(Track track) {
        if (size() > 0) {
            return contains(track, 0, size() - 1);
        } else {
            return false;
        }
    }

    private boolean contains(Track track, int start_index, int end_index) {
        if (start_index > end_index) {
            return false;
        } else {
            int size = (end_index - start_index) + 1;
            int index = start_index + size / 2;
            int compared = trackComparator.compare(track, tracks.get(index));

            if (compared == 0) {
                // the track was found in the playlist
                return true;
            } else if (compared > 0) {
                // the track might be on the right of the checked track
                return contains(track, index + 1, end_index);
            } else {
                // the track might be on the left of the checked track
                return contains(track, start_index, index - 1);
            }
        }
    }

    /**
     * this method is used to approximate track's position in logn time
     *
     * @param track - track to approximate
     * @return its approximated index
     */
    public int approximateIndexOf(Track track){
        if (size() > 0) {
            return approximateIndexOf(track, 0, size() - 1);
        } else {
            return 0;
        }
    }

    private int approximateIndexOf(Track track, int start_index, int end_index) {
        if (start_index > end_index) {
            return (start_index + end_index) / 2;
        } else {
            int size = (end_index - start_index) + 1;
            int index = start_index + size / 2;
            int compared = trackComparator.compare(track, tracks.get(index));

            if (compared > 0) {
                // the track might be on the right of the checked track
                return approximateIndexOf(track, index + 1, end_index);
            } else if (compared < 0){
                // the track might be on the left of the checked track
                return approximateIndexOf(track, start_index, index - 1);
            } else {
                return UNDEFINED;
            }
        }
    }

    public int indexOf(Track track) {
        if (track == null) return UNDEFINED;

        if (size() > 0) {
            int index = indexOf(track, 0, size() - 1);
            return isShuffled() ? normalToShuffled(index) : index;
        } else {
            return -1;
        }
    }

    public Track removeTrack(int number) {

        // if number == lastAccessedTrackIndex then the last accessed track will now be deleted
        // and its index is undefined
        if (lastAccessedTrackIndex == number) {
            lastAccessedTrackIndex = UNDEFINED;
        } else if (lastAccessedTrackIndex > number){
            lastAccessedTrackIndex--;
        }

        Track track;
        if (!isShuffled()) {
            track = tracks.remove(number);
        } else {
            int index = shuffledIndices[number];
            track = tracks.remove(index);
        }

        track.removedFromPlaylist(this);
        if (sizeChangeListener != null) sizeChangeListener.onChange(this);
        unshuffle();

        return track;
    }

    public void setLastAccessedTrack(Track track){
        lastAccessedTrackIndex = indexOf(track);
        if (sizeChangeListener != null) sizeChangeListener.onChange(this);
    }

    public void clear(){
        for (Track track : tracks){
            track.removedFromPlaylist(this);
        }
        tracks = new ArrayList<>();
        shuffledIndices = null;
        lastAccessedTrackIndex = UNDEFINED;
    }

    public boolean removeTrack(Track track) {
        int index = indexOf(track);
        if (index > 0) {
            removeTrack(index);
            return true;
        }
        return false;
    }

    private int indexOf(Track track, int start_index, int end_index) {
        if (start_index > end_index) {
            return UNDEFINED;
        } else {
            int size = (end_index - start_index) + 1;
            int index = start_index + size / 2;
            int compared = trackComparator.compare(track, tracks.get(index));

            if (compared == 0) {
                // the track was found in the playlist
                return index;
            } else if (compared > 0) {
                // the track might be on the right of the checked track
                return indexOf(track, index + 1, end_index);
            } else {
                // the track might be on the left of the checked track
                return indexOf(track, start_index, index - 1);
            }
        }
    }

    public void ensureCapacity(int capacity){
        tracks.ensureCapacity(capacity);
    }

    public String toString() {
        return getTitle();
    }

    final boolean isEditable() {
        return editable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle(){
        return title;
    }

    boolean isShuffled(){
        return shuffled;
    }

    private void setShuffled(boolean shuffled){
        this.shuffled = shuffled;
    }

    public final int size() {
        return tracks.size();
    }

    public final boolean isEmpty() {
        return size() == 0;
    }

    public Track getTrack(int number){
        lastAccessedTrackIndex = number;
        return getTrackSilently(number);
    }

    // this method doesn't update last accessed track index
    private Track getTrackSilently(int number) {
        if (isShuffled()) {
            return tracks.get(shuffledIndices[number]);
        } else {
            return tracks.get(number);
        }
    }

    public boolean deleteTracks(final int [] indices, Context context){

        boolean currentTrackRemoved = false;

        final int LENGTH = size() - indices.length;

        if (indices.length > 0){
            final ArrayList<Track> newTracks = new ArrayList<>(LENGTH);

            if (isShuffled()){

                // creates an array of actual removed indices sorted in ascending order
                // this eliminates the need to sort the tracks ArrayList
                final int [] removedIndices = new int[indices.length];
                for (int i = 0; i < removedIndices.length; i++){
                    removedIndices[i] = shuffledIndices[indices[i]];
                }
                Arrays.sort(removedIndices);

                final IndexTransformer transformer = new IndexTransformer(removedIndices, true);

                int j = 0;

                for (int i = 0; i < tracks.size(); i++){
                    Track track = tracks.get(i);

                    if (j >= indices.length || i != removedIndices[j]){
                        newTracks.add(track);
                    } else {
                        // something got removed
                        if (lastAccessedTrackIndex == i){
                            // last accessed track was removed
                            currentTrackRemoved = true;
                            lastAccessedTrackIndex = UNDEFINED;
                        }
                        track.delete(context);
                        j++;
                    }
                }

                if (lastAccessedTrackIndex != UNDEFINED){
                    lastAccessedTrackIndex = transformer.transform(lastAccessedTrackIndex);
                }

                final int [] newShuffledIndices = new int[LENGTH];
                int k = 0;
                int n = 0;
                for (int i = 0; i < shuffledIndices.length; i++){
                    if (k >= indices.length || i != indices[k]){
                        newShuffledIndices[n++] = transformer.transform(shuffledIndices[i]);
                    } else {
                        k++;
                    }
                }

                shuffledIndices = newShuffledIndices;

                if (stateChangeListener != null){
                    stateChangeListener.onChange(this, true);
                }
            } else {
                // index must be compared against a constant value
                final int lastAccessedConst = lastAccessedTrackIndex;

                int j = 0;

                for (int i = 0; i < tracks.size(); i++){
                    Track track = tracks.get(i);

                    // add track if removed indices ended or if it was not removed
                    if (j >= indices.length || i != indices[j]){
                        newTracks.add(track);
                    } else {
                        // a track was removed
                        if (lastAccessedConst == i){
                            // last accessed track was removed
                            currentTrackRemoved = true;
                            lastAccessedTrackIndex = UNDEFINED;
                        } else if (lastAccessedConst > i) {
                            // last accessed track index must be decremented
                            lastAccessedTrackIndex--;
                        }

                        track.delete(context);
                        j++;
                    }
                }
            }

            tracks = newTracks;
            if (sizeChangeListener != null) sizeChangeListener.onChange(this);
        }
        return currentTrackRemoved;
    }

    public boolean removeTracks(final int [] indices){

        boolean currentTrackRemoved = false;

        final int LENGTH = size() - indices.length;

        if (indices.length > 0){
            final ArrayList<Track> newTracks = new ArrayList<>(LENGTH);

            if (isShuffled()){

                // creates an array of actual removed indices sorted in ascending order
                // this eliminates the need to sort the tracks ArrayList
                final int [] removedIndices = new int[indices.length];
                for (int i = 0; i < removedIndices.length; i++){
                    removedIndices[i] = shuffledIndices[indices[i]];
                }
                Arrays.sort(removedIndices);

                final IndexTransformer transformer = new IndexTransformer(removedIndices, true);

                int j = 0;

                for (int i = 0; i < tracks.size(); i++){
                    Track track = tracks.get(i);

                    if (j >= indices.length || i != removedIndices[j]){
                        newTracks.add(track);
                    } else {
                        // something got removed
                        if (lastAccessedTrackIndex == i){
                            // last accessed track was removed
                            currentTrackRemoved = true;
                            lastAccessedTrackIndex = UNDEFINED;
                        }
                        track.removedFromPlaylist(this);
                        j++;
                    }
                }

                if (lastAccessedTrackIndex != UNDEFINED){
                    lastAccessedTrackIndex = transformer.transform(lastAccessedTrackIndex);
                }

                final int [] newShuffledIndices = new int[LENGTH];
                int k = 0;
                int n = 0;
                for (int i = 0; i < shuffledIndices.length; i++){
                    if (k >= indices.length || i != indices[k]){
                        newShuffledIndices[n++] = transformer.transform(shuffledIndices[i]);
                    } else {
                        k++;
                    }
                }

                shuffledIndices = newShuffledIndices;

                if (stateChangeListener != null){
                    stateChangeListener.onChange(this, true);
                }
            } else {
                // index must be compared against a constant value
                final int lastAccessedConst = lastAccessedTrackIndex;

                int j = 0;

                for (int i = 0; i < tracks.size(); i++){
                    Track track = tracks.get(i);

                    // add track if removed indices ended or if it was not removed
                    if (j >= indices.length || i != indices[j]){
                        newTracks.add(track);
                    } else {
                        // a track was removed
                        if (lastAccessedConst == i){
                            // last accessed track was removed
                            currentTrackRemoved = true;
                            lastAccessedTrackIndex = UNDEFINED;
                        } else if (lastAccessedConst > i) {
                            // last accessed track index must be decremented
                            lastAccessedTrackIndex--;
                        }

                        track.removedFromPlaylist(this);
                        j++;
                    }
                }
            }

            tracks = newTracks;
            if (sizeChangeListener != null) sizeChangeListener.onChange(this);
        }
        return currentTrackRemoved;
    }

    Track getTrackWithoutShuffle(int i){
        return tracks.get(i);
    }

    int[] getShuffledIndices(){
        return shuffledIndices;
    }

    public final ShuffleSettings getSettings(){
        return new ShuffleSettings(this);
    }

    // This class serves for encapsulation purposes:
    // it does not allow to change shuffledIndices with a usual setter
    public static final class ShuffleSettings implements Parcelable {
        private int [] shuffledInd;

        private ShuffleSettings(AbstractPlaylist playlist){
            shuffledInd = playlist.shuffledIndices;
        }

        int [] getShuffledIndices(){
            return shuffledInd;
        }

        // PARCELABLE

        private ShuffleSettings(Parcel in){
            in.readIntArray(shuffledInd);
        }

        public static final Parcelable.Creator<ShuffleSettings> CREATOR
                = new Parcelable.Creator<ShuffleSettings>() {
            public ShuffleSettings createFromParcel(Parcel in) {
                return new ShuffleSettings(in);
            }

            public ShuffleSettings[] newArray(int size) {
                return new ShuffleSettings[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeIntArray(shuffledInd);
        }
    }

    static class TitleComparator implements Comparator<Track> {
        @Override
        public int compare(Track track1, Track track2) {
            int compared = track1.getTitle().compareTo(track2.getTitle());
            // if titles are the same check artists
            if (compared == 0) {
                compared = track1.getArtist().compareTo(track2.getArtist());
                // if artists are the same check albums
                if (compared == 0) {
                    compared = track1.getAlbum().compareTo(track2.getAlbum());
                }
            }
            return compared;
        }
    }

    static class ArtistComparator implements Comparator<Track> {
        @Override
        public int compare(Track track1, Track track2) {
            int compared = track1.getArtist().compareTo(track2.getArtist());
            if (compared == 0) {
                compared = track1.getYear() - track2.getYear();
            }
            return compared;
        }
    }

    @Override
    public EffectBundle getPreset() {
        return preset;
    }

    @Override
    public void setPreset(EffectBundle preset) {
        this.preset = preset;
    }

    @Override
    public boolean hasPreset() {
        return !preset.isStandard() && !preset.isDeleted();
    }

    // returns a parcelable copy of this playlist
    public PlaylistParcel toParcelable(boolean isCurrent){
        return new PlaylistParcel(this, isCurrent);
    }

    // INNER INTERFACES

    public interface OnShuffleStateChangeListener{
        void onChange(AbstractPlaylist playlist, boolean shuffled);
    }

    public interface OnSizeChangeListener{
        void onChange(AbstractPlaylist playlist);
    }

    private static class PlaylistAppender{

        private ArrayList<Element> elements;
        private ArrayList<Element> shuffledElements;

        private PlaylistAppender(final AbstractPlaylist playlist, final List<Track> appendWith){

            final ArrayList<Track> tracks = playlist.tracks;
            final int INITIAL_CAPACITY = playlist.size() + appendWith.size();

            // sort the appended part to ensure insertion order
            Collections.sort(appendWith, playlist.trackComparator);

            elements = new ArrayList<>(INITIAL_CAPACITY);

            for (int i = 0; i < tracks.size(); i++){
                elements.add(new Element(i, tracks.get(i)));
            }

            shuffledElements = new ArrayList<>(INITIAL_CAPACITY);

            for (int index : playlist.shuffledIndices){
                shuffledElements.add(elements.get(index));
            }

            // actual expansion of the playlist
            int i = 0;
            int j = 0;

            final Comparator<Track> comparator = playlist.trackComparator;

            for (; j < elements.size(); j++){
                final Track addedTrack = appendWith.get(i);
                final Track comparedTrack = elements.get(j).getTrack();

                final int compared = comparator.compare(addedTrack, comparedTrack);
                if (compared <= 0){
                    Element e = new Element(j, addedTrack);

                    elements.add(j, e);
                    shuffledElements.add(e);
                    j++;
                    i++;
                }
            }

            // adding every appendix left to the end
            for (; i < appendWith.size(); i++){
                final Track track = appendWith.get(i);
                Element e = new Element(j, track);

                elements.add(j++, e);
                shuffledElements.add(e);
            }
        }

        public int [] getShuffledIndices(){
            final int [] shuffledIndices = new int[shuffledElements.size()];
            for (int i = 0; i < shuffledIndices.length; i++){
                shuffledIndices[i] = shuffledElements.get(i).getIndex();
            }
            return shuffledIndices;
        }

        public ArrayList<Track> getTracks(){
            final ArrayList<Track> tracks = new ArrayList<>(elements.size());
            for (Element e : elements){
                tracks.add(e.getTrack());
            }
            return tracks;
        }

        private class Element{
            private int index;
            private Track track;

            private Element(int index, Track track){
                this.track = track;
                this.index = index;
            }

            public int getIndex() {
                return index;
            }

            public void setIndex(int index) {
                this.index = index;
            }

            public Track getTrack() {
                return track;
            }

            public void setTrack(Track track) {
                this.track = track;
            }
        }
    }
}
