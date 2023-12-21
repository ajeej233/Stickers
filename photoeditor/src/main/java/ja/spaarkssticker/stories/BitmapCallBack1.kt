package ja.spaarkssticker.stories

import android.graphics.Bitmap

public interface BitmapCallback1 {
    fun onBitmapReady(bitmap: Bitmap)
    fun onBitmapError(error: Throwable)
}