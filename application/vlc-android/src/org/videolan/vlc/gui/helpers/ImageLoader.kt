package org.videolan.vlc.gui.helpers

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import androidx.leanback.widget.ImageCardView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Folder
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.interfaces.media.Playlist
import org.videolan.medialibrary.media.DummyItem
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.AppContextProvider
import org.videolan.resources.DUMMY_NEW_GROUP
import org.videolan.resources.HEADER_MOVIES
import org.videolan.resources.HEADER_TV_SHOW
import org.videolan.tools.BitmapCache
import org.videolan.tools.HttpImageLoader
import org.videolan.tools.Settings
import org.videolan.tools.sanitizePath
import org.videolan.vlc.BR
import org.videolan.vlc.R
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.ThumbnailsProvider.obtainBitmap
import org.videolan.vlc.util.isSchemeHttpOrHttps
import org.videolan.vlc.util.scope

private val sMedialibrary = Medialibrary.getInstance()
@Volatile
private var defaultImageWidth = 0
private var defaultImageWidthTV = 0
private const val TAG = "ImageLoader"

@MainThread
@BindingAdapter(value = ["media", "imageWidth", "tv", "card"], requireAll = false)
fun loadImage(v: View, item: MediaLibraryItem?, imageWidth: Int = 0, tv: Boolean = false, card: Boolean = false) {
    if (item === null) return
    v.tag = item.title

    if (item.itemType == MediaLibraryItem.TYPE_PLAYLIST || item.itemType == MediaLibraryItem.TYPE_GENRE) {
        if (imageWidth != 0) {
            loadPlaylistImageWithWidth(v as ImageView, item, imageWidth, card)
        }
        return
    }

    val binding = DataBindingUtil.findBinding<ViewDataBinding>(v)
    val isMedia = item.itemType == MediaLibraryItem.TYPE_MEDIA
    if (!Settings.showVideoThumbs && ((isMedia && (item as MediaWrapper).type == MediaWrapper.TYPE_VIDEO) || item.itemType == MediaLibraryItem.TYPE_VIDEO_GROUP || item is Folder) ) {
        updateImageView(UiTools.getDefaultVideoDrawable(v.context).bitmap, v, binding, tv = tv, card = card)
        return
    }
    val isGroup = isMedia && item.itemType == MediaLibraryItem.TYPE_VIDEO_GROUP
    val isFolder = !isMedia && item.itemType == MediaLibraryItem.TYPE_FOLDER
    val cacheWidth = if (imageWidth != 0) imageWidth else v.width
    val cacheKey = when {
        isGroup -> "videogroup:${item.title}"
        isFolder -> "folder:${(item as Folder).mMrl.sanitizePath()}"
        else -> ThumbnailsProvider.getMediaCacheKey(isMedia, item, cacheWidth.toString())
    }
    val bitmap = if (cacheKey !== null) BitmapCache.getBitmapFromMemCache(cacheKey) else null
    if (bitmap !== null) updateImageView(bitmap, v, binding, tv = tv, card = card)
    else {
        v.scope.takeIf { it.isActive }?.launch { getImage(v, findInLibrary(item, isMedia), binding, imageWidth, tv = tv, card = card) }
    }
}

fun loadPlaylistImageWithWidth(v: ImageView, item: MediaLibraryItem?, imageWidth: Int, card: Boolean) {
    if (imageWidth == 0) return
    if (item == null) return
    val binding = DataBindingUtil.findBinding<ViewDataBinding>(v)
    v.scope.takeIf { it.isActive }?.launch { getPlaylistOrGenreImage(v, item, binding, imageWidth, card) }
}

fun getAudioIconDrawable(context: Context?, type: Int, big: Boolean = false): BitmapDrawable? = context?.let {
    when (type) {
        MediaLibraryItem.TYPE_ALBUM -> if (big) UiTools.getDefaultAlbumDrawableBig(it) else UiTools.getDefaultAlbumDrawable(it)
        MediaLibraryItem.TYPE_ARTIST -> if (big) UiTools.getDefaultArtistDrawableBig(it) else UiTools.getDefaultArtistDrawable(it)
        MediaLibraryItem.TYPE_MEDIA -> if (big) UiTools.getDefaultAudioDrawableBig(it) else UiTools.getDefaultAudioDrawable(it)
        MediaLibraryItem.TYPE_PLAYLIST -> if (big) UiTools.getDefaultPlaylistDrawableBig(it) else UiTools.getDefaultPlaylistDrawable(it)
        else -> null
    }
}

fun getMediaIconDrawable(context: Context?, type: Int, big: Boolean = false): BitmapDrawable? = context?.let {
    when (type) {
        MediaWrapper.TYPE_ALBUM -> if (big) UiTools.getDefaultAlbumDrawableBig(it) else UiTools.getDefaultAlbumDrawable(it)
        MediaWrapper.TYPE_ARTIST -> if (big) UiTools.getDefaultArtistDrawableBig(it) else UiTools.getDefaultArtistDrawable(it)
        MediaWrapper.TYPE_AUDIO -> if (big) UiTools.getDefaultAudioDrawableBig(it) else UiTools.getDefaultAudioDrawable(it)
        MediaWrapper.TYPE_VIDEO -> if (big) UiTools.getDefaultVideoDrawableBig(it) else UiTools.getDefaultAudioDrawable(it)
        MediaWrapper.TYPE_DIR -> if (big) UiTools.getDefaultFolderDrawableBig(it) else UiTools.getDefaultFolderDrawable(it)
        else -> null
    }
}

fun getMoviepediaIconDrawable(context: Context?, type: Long, big: Boolean = false): BitmapDrawable? = context?.let {
    when (type) {
        HEADER_MOVIES -> if (big) UiTools.getDefaultMovieDrawableBig(it) else UiTools.getDefaultMovieDrawable(it)
        HEADER_TV_SHOW -> if (big) UiTools.getDefaultTvshowDrawableBig(it) else UiTools.getDefaultTvshowDrawable(it)
        else -> null
    }
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun getBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(context, drawableId)

    return if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else if (drawable is VectorDrawableCompat || drawable is VectorDrawable) {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        bitmap
    } else {
        throw IllegalArgumentException("unsupported drawable type")
    }
}

fun getMediaIconDrawable(context: Context, type: Int): BitmapDrawable? = when (type) {
    MediaWrapper.TYPE_VIDEO -> UiTools.getDefaultVideoDrawable(context)
    else -> UiTools.getDefaultAudioDrawable(context)
}

fun getDummyItemIcon(context: Context, item:DummyItem) = when (item.id) {
    DUMMY_NEW_GROUP -> BitmapDrawable(context.resources, getBitmapFromDrawable(context, R.drawable.ic_add_to_group))
    else -> null
}

private var placeholderTvBg: Drawable? = null
@MainThread
@BindingAdapter("placeholder")
fun placeHolderView(v: View, item: Any?) {
    if (item == null) {
        if (placeholderTvBg === null) placeholderTvBg = ContextCompat.getDrawable(v.context, R.drawable.rounded_corners_grey)
        v.background = placeholderTvBg
    } else {
        v.background = null
    }
}

@MainThread
@BindingAdapter("placeholderImage")
fun placeHolderImageView(v: View, item: MediaLibraryItem?) {
    if (item == null) {
        v.background = ContextCompat.getDrawable(v.context, R.drawable.rounded_corners_grey)
    } else {
        v.background = UiTools.getDefaultAudioDrawable(v.context)
    }
}

@BindingAdapter("icvTitle")
fun imageCardViewTitle(v: View, title: String?) {
    if (v is ImageCardView) {
        v.titleText = title
    }
}

@BindingAdapter("icvContent")
fun imageCardViewContent(v: View, content: String?) {
    if (v is ImageCardView) {
        v.contentText = content
    }
}

@BindingAdapter(value = ["imageUri", "tv" ], requireAll = false)
fun downloadIcon(v: View, imageUri: Uri?, tv: Boolean = true) {
    if (isSchemeHttpOrHttps(imageUri?.scheme)) {
        v.scope.takeIf { it.isActive }?.launch {
            val image = HttpImageLoader.downloadBitmap(imageUri.toString())
            updateImageView(image, v, DataBindingUtil.findBinding(v), tv = tv)
        }
    }
}

@BindingAdapter(value = ["imageUrl", "tv" ], requireAll = false)
fun downloadIcon(v: View, imageUrl: String?, tv: Boolean = true) {
    if (imageUrl.isNullOrEmpty()) return
    val imageUri = imageUrl.toUri()
    if (isSchemeHttpOrHttps(imageUri.scheme)) {
        v.scope.takeIf { it.isActive }?.launch {
            val image = HttpImageLoader.downloadBitmap(imageUri.toString())
            updateImageView(image, v, DataBindingUtil.findBinding(v), tv = tv)
        }
    }
}


private suspend fun getImage(v: View, item: MediaLibraryItem, binding: ViewDataBinding?, imageWidth: Int = 0, tv: Boolean = false, card: Boolean = false) {
    var bindChanged = false
    val rebindCallbacks = if (binding !== null) object : OnRebindCallback<ViewDataBinding>() {
        override fun onPreBind(binding: ViewDataBinding): Boolean {
            bindChanged = true
            return super.onPreBind(binding)
        }
    } else null
    if (binding !== null) {
        binding.executePendingBindings()
        binding.addOnRebindCallback(rebindCallbacks!!)
    }
    val width = when {
        tv -> {
            if (defaultImageWidthTV == 0) {
                defaultImageWidthTV = v.context.resources.getDimensionPixelSize(R.dimen.tv_grid_card_thumb_width)
            }
            defaultImageWidthTV
        }
        imageWidth > 0 -> imageWidth
        v.width > 0 -> v.width
        defaultImageWidth > 0 -> defaultImageWidth
        else -> {
            defaultImageWidth = v.context.resources.getDimensionPixelSize(if (v is ImageCardView) R.dimen.tv_grid_card_thumb_width else R.dimen.audio_browser_item_size)
            defaultImageWidth
        }
    }
    val image = if (!bindChanged) obtainBitmap(item, width) else null
    if (image == null && tv) {
        // binding is set to null to be sure to set the src and not the cover (background)
        if (!bindChanged) updateImageViewTv(getTvIconRes(item), v)
        binding?.removeOnRebindCallback(rebindCallbacks!!)
        return
    }

    if (image == null) {
        //keep the default image
        binding?.setVariable(BR.scaleType, ImageView.ScaleType.CENTER_INSIDE)
        if (!bindChanged) binding?.setVariable(BR.showProgress, false)
        binding?.removeOnRebindCallback(rebindCallbacks!!)
        return
    }

    if (!bindChanged) updateImageView(image, v, binding, tv = tv, card = card)
    binding?.removeOnRebindCallback(rebindCallbacks!!)
}

private suspend fun getPlaylistOrGenreImage(v: View, item: MediaLibraryItem, binding: ViewDataBinding?, width: Int, card: Boolean) {
    var bindChanged = false
    val rebindCallbacks = if (binding !== null) object : OnRebindCallback<ViewDataBinding>() {
        override fun onPreBind(binding: ViewDataBinding): Boolean {
            bindChanged = true
            return super.onPreBind(binding)
        }
    } else null
    if (binding !== null) {
        binding.executePendingBindings()
        binding.addOnRebindCallback(rebindCallbacks!!)
    }

    var playlistImage = if (!bindChanged) {
        val tracks = withContext(Dispatchers.IO) { item.tracks.toList() }
        ThumbnailsProvider.getPlaylistOrGenreImage("${if (item is MediaWrapper && item.type == MediaWrapper.TYPE_PLAYLIST)"playlist" else "genre"}:${item.id}_$width", tracks, width)
    } else null
    if (playlistImage == null) return
    if (!bindChanged) updateImageView(playlistImage, v, binding, card = card)

    binding?.removeOnRebindCallback(rebindCallbacks!!)
}

@MainThread
fun updateImageViewTv(@DrawableRes res: Int, target: View) {
    when (target) {
        is ImageView -> {
            target.scaleType = ImageView.ScaleType.CENTER_CROP
            target.setImageResource(res)
            target.visibility = View.VISIBLE
        }
        is ImageCardView -> {
            target.mainImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            target.mainImageView.setImageResource(res)
        }
    }
}

@MainThread
fun updateImageView(bitmap: Bitmap?, target: View, vdb: ViewDataBinding?, updateScaleType: Boolean = true, tv: Boolean = false, card: Boolean = false) {
    if (bitmap === null || bitmap.width <= 1 || bitmap.height <= 1) return
    if (vdb !== null && !tv) {
        vdb.setVariable(BR.scaleType, if (card) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER)
        vdb.setVariable(BR.cover, BitmapDrawable(target.resources, bitmap))
        vdb.setVariable(BR.protocol, null)
        vdb.setVariable(BR.showProgress, false)
    } else when (target) {
        is ImageView -> {
            if (updateScaleType) target.scaleType = if (tv) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            target.setImageBitmap(bitmap)
            target.visibility = View.VISIBLE
        }
        is TextView -> {
            ViewCompat.setBackground(target, BitmapDrawable(target.context.resources, bitmap))
            target.text = null
        }
        is ImageCardView -> {
            target.mainImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            target.mainImage = BitmapDrawable(target.getResources(), bitmap)
        }
    }
}

private suspend fun findInLibrary(item: MediaLibraryItem, isMedia: Boolean): MediaLibraryItem {
    if (isMedia && item.id == 0L) {
        val mw = item as MediaWrapper
        val type = mw.type
        val isMediaFile = type == MediaWrapper.TYPE_AUDIO || type == MediaWrapper.TYPE_VIDEO
        val uri = mw.uri
        val scheme = try {
            uri.scheme
        } catch (e: NullPointerException) {
            ""
        }
        if (!isMediaFile && !(type == MediaWrapper.TYPE_DIR && "upnp" == scheme)) return item
        if (isMediaFile && "file" == scheme) return withContext(Dispatchers.IO) { sMedialibrary.getMedia(uri) }
                ?: item
    }
    return item
}

@MainThread
@BindingAdapter(value = ["constraintRatio", "coverWidth"], requireAll = false)
fun constraintRatio(v: View, isSquare: Boolean, imageWidth: Int = -2) {
    if (imageWidth != -2) {
        val layoutParams: ViewGroup.LayoutParams = v.layoutParams
        layoutParams.width = imageWidth
        v.layoutParams = layoutParams
    }
    val constraintLayout = v.parent as? ConstraintLayout
    constraintLayout?.let {
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.setDimensionRatio(v.id, if (isSquare) "1" else "16:10")
        constraintSet.applyTo(constraintLayout)
    }
}