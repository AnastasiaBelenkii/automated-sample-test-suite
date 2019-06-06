/*
 *****************************************************************************
 * MediaWrapper.java
 *****************************************************************************
 * Copyright © 2011-2015 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.medialibrary.media;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.Nullable;

import org.videolan.libvlc.Media;
import org.videolan.medialibrary.Medialibrary;
import org.videolan.medialibrary.Tools;
import org.videolan.medialibrary.interfaces.media.AMediaWrapper;

import java.util.Locale;

@SuppressWarnings("JniMissingFunction")
public class MediaWrapper extends AMediaWrapper {
    public final static String TAG = "VLC/MediaWrapper";

    public MediaWrapper(long id, String mrl, long time, long length, int type, String title,
                         String filename, String artist, String genre, String album, String albumArtist,
                         int width, int height, String artworkURL, int audio, int spu, int trackNumber,
                         int discNumber, long lastModified, long seen, boolean isThumbnailGenerated) {
        super(id, mrl, time, length, type, title, filename, artist,
                genre, album, albumArtist, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified,
                seen, isThumbnailGenerated);
    }

    public MediaWrapper(Uri uri, long time, long length, int type,
                        Bitmap picture, String title, String artist, String genre, String album, String albumArtist,
                        int width, int height, String artworkURL, int audio, int spu, int trackNumber, int discNumber, long lastModified, long seen) {
        super(uri, time, length, type, picture, title, artist,
                genre, album, albumArtist, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified, seen);
    }

    public MediaWrapper(Uri uri) { super(uri); }
    public MediaWrapper(Media media) { super(media); }
    public MediaWrapper(Parcel in) { super(in); }

    public void rename(String name) {
        final Medialibrary ml = Medialibrary.getInstance();
        if (mId != 0 && ml.isInitiated()) nativeSetMediaTitle(ml, mId, name);
    }

<<<<<<< HEAD
    public void removeFromHistory() {
        if (mId != 0L) {
            final Medialibrary ml = Medialibrary.getInstance();
            if (ml.isInitiated()) nativeRemoveFromHistory(ml, mId);
        }
    }

    public void setArtist(String artist) {
        mArtist = artist;
    }

    @Override
    public String getTitle() {
        if (!TextUtils.isEmpty(mDisplayTitle))
            return mDisplayTitle;
        if (!TextUtils.isEmpty(mTitle))
            return mTitle;
        String fileName = getFileName();
        if (fileName == null)
            return "";
        int end = fileName.lastIndexOf(".");
        if (end <= 0)
            return fileName;
        return fileName.substring(0, end);
    }

    public String getReferenceArtist() {
        return mAlbumArtist == null ? mArtist : mAlbumArtist;
    }

    public String getArtist() {
        return mArtist;
    }

    public Boolean isArtistUnknown() {
        return mArtist == null;
    }

    public String getGenre() {
        if (mGenre == null)
            return null;
        else if (mGenre.length() > 1)/* Make genres case insensitive via normalisation */
            return Character.toUpperCase(mGenre.charAt(0)) + mGenre.substring(1).toLowerCase(Locale.getDefault());
        else
            return mGenre;
    }

    public String getCopyright() {
        return mCopyright;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getAlbumArtist() {
        return mAlbumArtist;
    }

    public Boolean isAlbumUnknown() {
        return mAlbum == null;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public int getDiscNumber() {
        return mDiscNumber;
    }

    public String getRating() {
        return mRating;
    }

    public String getDate() {
        return mDate;
    }

    public String getSettings() {
        return mSettings;
    }

    public String getNowPlaying() {
        return mNowPlaying;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public String getEncodedBy() {
        return mEncodedBy;
    }

    public String getTrackID() {
        return mTrackID;
    }

    public String getArtworkURL() {
        return mArtworkURL;
    }

    public boolean isThumbnailGenerated() {
        return mThumbnailGenerated;
    }

    @Override
    public String getArtworkMrl() {
        return mArtworkURL;
    }

    public void setArtworkURL(String url) {
        mArtworkURL = url;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public void setLastModified(long mLastModified) {
        this.mLastModified = mLastModified;
    }

    public long getSeen() {
        return mSeen;
    }

    public void setSeen(long seen) {
        mSeen = seen;
    }

    public void addFlags(int flags) {
        mFlags |= flags;
    }

    public void setFlags(int flags) {
        mFlags = flags;
    }

    public int getFlags() {
        return mFlags;
    }

    public boolean hasFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    public void removeFlags(int flags) {
        mFlags &= ~flags;
    }

=======
>>>>>>> Medialibrary: move MediaWrapper to AMediaWrapper
    public long getMetaLong(int metaDataType) {
        Medialibrary ml = Medialibrary.getInstance();
        return mId == 0 || !ml.isInitiated() ? 0L : nativeGetMediaLongMetadata(ml, mId, metaDataType);
    }

    public String getMetaString(int metaDataType) {
        Medialibrary ml = Medialibrary.getInstance();
        return mId == 0 || !ml.isInitiated() ? null : nativeGetMediaStringMetadata(ml, mId, metaDataType);
    }

    public boolean setLongMeta(int metaDataType, long metadataValue) {
        Medialibrary ml = Medialibrary.getInstance();
        if (mId != 0 && ml.isInitiated())
            nativeSetMediaLongMetadata(ml, mId, metaDataType, metadataValue);
        return mId != 0;
    }

    public boolean setStringMeta(int metaDataType, String metadataValue) {
        Medialibrary ml = Medialibrary.getInstance();
        if (mId != 0 && ml.isInitiated())
            nativeSetMediaStringMetadata(ml, mId, metaDataType, metadataValue);
        return mId != 0;
    }

    public void setThumbnail(String mrl) {
        mArtworkURL = mrl;
        final Medialibrary ml = Medialibrary.getInstance();
        if (mId != 0 && ml.isInitiated()) nativeSetMediaThumbnail(ml, mId, Tools.encodeVLCMrl(mrl));
    }

    private native long nativeGetMediaLongMetadata(Medialibrary ml, long id, int metaDataType);
    private native String nativeGetMediaStringMetadata(Medialibrary ml, long id, int metaDataType);
    private native void nativeSetMediaStringMetadata(Medialibrary ml, long id, int metaDataType, String metadataValue);
    private native void nativeSetMediaLongMetadata(Medialibrary ml, long id, int metaDataType, long metadataValue);
    private native void nativeSetMediaThumbnail(Medialibrary ml, long id, String mrl);
    private native void nativeSetMediaTitle(Medialibrary ml, long id, String name);
    private native void nativeRemoveFromHistory(Medialibrary ml, long id);

}
