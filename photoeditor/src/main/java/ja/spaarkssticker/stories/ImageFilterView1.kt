package ja.spaarkssticker.stories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Environment
import android.util.AttributeSet
import androidx.exifinterface.media.ExifInterface
import ja.spaarkssticker.stories.BitmapUtil.createBitmapFromGLSurface
import ja.spaarkssticker.stories.GLToolbox.initTexParams
import ja.spaarkssticker.stories.PhotoFilter.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
public class ImageFilterView1 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer {

    private val mTextures = IntArray(2)
    private var mEffectContext: EffectContext? = null
    private var mEffect: Effect? = null
    private val mTexRenderer: TextureRenderer = TextureRenderer()
    private var mImageWidth = 0
    private var mImageHeight = 0
    private var mInitialized = false
    private var viewRotationDegrees: Float = 0f

    fun updateRotation(degrees: Float) {
        viewRotationDegrees = degrees
        requestRender()
    }

    private var mCurrentEffect: PhotoFilter = NONE
    private var mSourceBitmap: Bitmap? = null
    private var mCustomEffect: CustomEffect? = null
    private var bitmapReadyContinuation: Continuation<Bitmap>? = null
    private val mutex = Mutex()
    private var rotationAngle: Float = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
        setFilterEffect(NONE)
    }

    fun setSourceBitmap(sourceBitmap: Bitmap?) {
        mSourceBitmap = sourceBitmap
        mInitialized = false
    }

    public fun rotateBitmap(angle: Float) {
        rotationAngle = angle
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {}

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mTexRenderer.updateViewSize(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        try {
            if (!mInitialized) {
                mEffectContext = EffectContext.createWithCurrentGlContext()
                mTexRenderer.init()
                loadTextures()
                mInitialized = true
            }
            if (mCurrentEffect != NONE || mCustomEffect != null) {
                initEffect()
                applyEffect()
            }


            // Handle rotation separately
            if (mCurrentEffect == ROTATE) {
                mTexRenderer.renderTexture(mTextures[0])
            }
            renderResult()
        } catch (t: Throwable) {
            val continuation = bitmapReadyContinuation
            if (continuation != null) {
                bitmapReadyContinuation = null
                continuation.resumeWithException(t)
            } else {
                throw t
            }
        }

          val continuation = bitmapReadyContinuation
        if (continuation != null) {
            bitmapReadyContinuation = null

            val filterBitmap = try {
                createBitmapFromGLSurface(this, gl)
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
                null
            }

            if (filterBitmap != null) continuation.resume(filterBitmap)
        }
    }
    public  fun getBitmapOriginal():Bitmap{
        return  mSourceBitmap!!;
    }
    private fun initEffect() {
        mEffectContext?.factory?.apply {
            mEffect?.release()

            if (mCustomEffect != null) {
                mEffect = createEffect(mCustomEffect!!.effectName)
                val parameters = mCustomEffect!!.parameters
                for ((key, value) in parameters) {
                    mEffect?.setParameter(key, value)
                }
            } else {
                // Initialize the correct effect based on the selected menu/action item
                when (mCurrentEffect) {
                    PhotoFilter.AUTO_FIX -> {
                        mEffect = createEffect(EffectFactory.EFFECT_AUTOFIX)
                        mEffect?.setParameter("scale", 0.5f)
                    }
                    PhotoFilter.BLACK_WHITE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_BLACKWHITE)
                        mEffect?.setParameter("black", .1f)
                        mEffect?.setParameter("white", .7f)
                    }
                    PhotoFilter.BRIGHTNESS -> {
                        mEffect = createEffect(EffectFactory.EFFECT_BRIGHTNESS)
                        mEffect?.setParameter("brightness", 2.0f)
                    }
                    PhotoFilter.CONTRAST -> {
                        mEffect = createEffect(EffectFactory.EFFECT_CONTRAST)
                        mEffect?.setParameter("contrast", 1.4f)
                    }
                    PhotoFilter.CROSS_PROCESS -> mEffect =
                        createEffect(EffectFactory.EFFECT_CROSSPROCESS)
                    PhotoFilter.DOCUMENTARY -> mEffect =
                        createEffect(EffectFactory.EFFECT_DOCUMENTARY)
                    PhotoFilter.DUE_TONE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_DUOTONE)
                        mEffect?.setParameter("first_color", Color.YELLOW)
                        mEffect?.setParameter("second_color", Color.DKGRAY)
                    }
                    PhotoFilter.FILL_LIGHT -> {
                        mEffect = createEffect(EffectFactory.EFFECT_FILLLIGHT)
                        mEffect?.setParameter("strength", .8f)
                    }
                    PhotoFilter.FISH_EYE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_FISHEYE)
                        mEffect?.setParameter("scale", .5f)
                    }
                    PhotoFilter.FLIP_HORIZONTAL -> {
                        mEffect = createEffect(EffectFactory.EFFECT_FLIP)
                        mEffect?.setParameter("horizontal", true)
                    }
                    PhotoFilter.FLIP_VERTICAL -> {
                        mEffect = createEffect(EffectFactory.EFFECT_FLIP)
                        mEffect?.setParameter("vertical", true)
                    }
                    PhotoFilter.GRAIN -> {
                        mEffect = createEffect(EffectFactory.EFFECT_GRAIN)
                        mEffect?.setParameter("strength", 1.0f)
                    }
                    PhotoFilter.GRAY_SCALE -> mEffect =
                        createEffect(EffectFactory.EFFECT_GRAYSCALE)
                    PhotoFilter.LOMISH -> mEffect =
                        createEffect(EffectFactory.EFFECT_LOMOISH)
                    PhotoFilter.NEGATIVE -> mEffect =
                        createEffect(EffectFactory.EFFECT_NEGATIVE)
                    PhotoFilter.NONE -> {}
                    PhotoFilter.POSTERIZE -> mEffect =
                        createEffect(EffectFactory.EFFECT_POSTERIZE)
                    PhotoFilter.ROTATE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_ROTATE)
                        mEffect?.setParameter("angle", 180)
                    }
                    PhotoFilter.SATURATE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_SATURATE)
                        mEffect?.setParameter("scale", .5f)
                    }
                    PhotoFilter.SEPIA -> mEffect =
                        createEffect(EffectFactory.EFFECT_SEPIA)
                    PhotoFilter.SHARPEN -> mEffect =
                        createEffect(EffectFactory.EFFECT_SHARPEN)
                    PhotoFilter.TEMPERATURE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_TEMPERATURE)
                        mEffect?.setParameter("scale", .9f)
                    }
                    PhotoFilter.TINT -> {
                        mEffect = createEffect(EffectFactory.EFFECT_TINT)
                        mEffect?.setParameter("tint", Color.MAGENTA)
                    }
                    PhotoFilter.VIGNETTE -> {
                        mEffect = createEffect(EffectFactory.EFFECT_VIGNETTE)
                        mEffect?.setParameter("scale", .5f)
                    }
                }
            }
        }
    }


    fun setFilterEffect(effect: PhotoFilter) {
        mCurrentEffect = effect
        mCustomEffect = null
        requestRender()
    }

    internal fun setFilterEffect(customEffect: CustomEffect?) {
        mCustomEffect = customEffect
        requestRender()
    }

    internal suspend fun saveBitmap(): Bitmap = mutex.withLock {
        suspendCoroutine { continuation ->
            bitmapReadyContinuation = continuation
            requestRender()
        }
    }

    private fun loadTextures() {
        GLES20.glGenTextures(2, mTextures, 0)

        mSourceBitmap?.let {
            val rotatedBitmap = rotateBitmapIfRequired(it)
            mImageWidth = rotatedBitmap.width
            mImageHeight = rotatedBitmap.height
            mTexRenderer.updateTextureSize(mImageWidth, mImageHeight)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, rotatedBitmap, 0)

            initTexParams()
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap): Bitmap {
        try {
            val ei = ExifInterface(createTempImageFile()?.absolutePath ?: "")
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            return when (orientation) {
            /*    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270)*/
                else -> bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun createTempImageFile(): File? {
        try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                Date()
            )
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
            )
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return null
    }


    fun rotateBitmap(): Bitmap {
        val matrix = Matrix().apply {
            postRotate(viewRotationDegrees)
        }
        return mSourceBitmap?.let {
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Return a 1x1 pixel bitmap if mSourceBitmap is null
    }

    private fun renderResult() {
        if (mCurrentEffect != NONE || mCustomEffect != null) {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[1])
        } else {
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[0])
        }

        applyRotation(rotationAngle)
    }

    private fun applyEffect() {
        mEffect?.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1])
    }

    private fun applyRotation(angle: Float) {
        if (angle != 0f) {
            val matrix = Matrix()
            matrix.postRotate(angle)
            mSourceBitmap = Bitmap.createBitmap(mSourceBitmap!!, 0, 0, mSourceBitmap!!.width, mSourceBitmap!!.height, matrix, true)
        }
    }

    companion object {
        private const val TAG = "ImageFilterView1"
    }
}
