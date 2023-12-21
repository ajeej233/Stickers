package ja.spaarkssticker.stories

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import ja.spaarkssticker.stories.FilterImageView.OnImageChangedListener

/**
 *
 *
 * This ViewGroup will have the [DrawingView] to draw paint on it with [ImageView]
 * which our source image
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 1/18/2018
 */
class PhotoEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private var mImgSource: FilterImageView = FilterImageView(context)
    private var mImgSource1: FilterImageView = FilterImageView(context)

    internal var drawingView: DrawingView
        private set

    private var mImageFilterView: ImageFilterView
    private var mScaleGestureDetector: ScaleGestureDetector? = null

    private var mImageFilterVie: ImageFilterView
    private var gestureDetector: GestureDetector? = null
    private var clipSourceImage = false
    private var mScaleFactor: Float = 1.0f
    var maxScale = 3.0f


    init {
        //Setup image attributes
        val sourceParam = setupImageSource(attrs)
        //Setup GLSurface attributes
        mImageFilterView = ImageFilterView(context)
        mImageFilterVie = ImageFilterView(context)
        val filterParam = setupFilterView()






        mImgSource.setOnImageChangedListener(object : OnImageChangedListener {
            override fun onBitmapLoaded(sourceBitmap: Bitmap?) {
                mImageFilterView.setFilterEffect(PhotoFilter.NONE)
                mImageFilterView.setSourceBitmap(sourceBitmap)


                Log.d(TAG, "onBitmapLoaded() called with: sourceBitmap = [$sourceBitmap]")
            }
        })


        //Setup drawing view
        drawingView = DrawingView(context)
        val brushParam = setupDrawingView()

        //Add image source
        addView(mImgSource, sourceParam)

        //Add Gl FilterView
        addView(mImageFilterView, filterParam)

        //Add brush view
        addView(drawingView, brushParam)
    }

    @SuppressLint("Recycle")
    private fun setupImageSource(attrs: AttributeSet?): LayoutParams {
        mImgSource.id = imgSrcId
        mImgSource.adjustViewBounds = true
        mImgSource.scaleType = ImageView.ScaleType.CENTER_INSIDE

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.PhotoEditorView)
            val imgSrcDrawable = a.getDrawable(R.styleable.PhotoEditorView_photo_src)
            if (imgSrcDrawable != null) {
                mImgSource.setImageDrawable(imgSrcDrawable)
            }
        }

        var widthParam = ViewGroup.LayoutParams.MATCH_PARENT
        if (clipSourceImage) {
            widthParam = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        val params = LayoutParams(widthParam, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(CENTER_IN_PARENT, TRUE)
        return params
    }

    private fun setupDrawingView(): LayoutParams {
        drawingView.visibility = GONE
        drawingView.id = shapeSrcId

        // Align drawing view to the size of image view
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        params.addRule(CENTER_IN_PARENT, TRUE)

        return params
    }

    private fun setupFilterView(): LayoutParams {
        mImageFilterView.visibility = GONE
        mImageFilterView.id = glFilterId

        //Align brush to the size of image view
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(CENTER_IN_PARENT, TRUE)
        params.addRule(ALIGN_TOP, imgSrcId)
        params.addRule(ALIGN_BOTTOM, imgSrcId)
        return params
    }

    /**
     * Source image which you want to edit
     *
     * @return source ImageView
     */
    val source: ImageView
        get() = mImgSource
   val source1: ImageFilterView
        get() = mImageFilterView

    internal suspend fun saveFilter(): Bitmap {
        return if (mImageFilterView.visibility == VISIBLE) {
            val saveBitmap = try {
                mImageFilterView.saveBitmap()
            } catch (t: Throwable) {
                throw RuntimeException("Couldn't save bitmap with filter", t)
            }
            mImgSource.setImageBitmap(saveBitmap)
            mImageFilterView.visibility = GONE
            saveBitmap
        } else {
            mImgSource.bitmap!!
        }
    }

    internal fun setFilterEffect(filterType: PhotoFilter) {
        mImageFilterView.visibility = VISIBLE
        mImageFilterView.setFilterEffect(filterType)
    }

    internal fun setFilterEffect(customEffect: CustomEffect?) {
        mImageFilterView.visibility = VISIBLE
        mImageFilterView.setFilterEffect(customEffect)
    }

    internal fun setClipSourceImage(clip: Boolean) {
        clipSourceImage = clip
        val param = setupImageSource(null)
        mImgSource.layoutParams = param
    } // endregion

    companion object {
        private const val TAG = "PhotoEditorView"
        private const val imgSrcId = 1
        private const val shapeSrcId = 2
        private const val glFilterId = 3
    }
}