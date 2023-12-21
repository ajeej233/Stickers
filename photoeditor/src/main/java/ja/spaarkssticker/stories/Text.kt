package ja.spaarkssticker.stories

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ja.spaarkssticker.stories.uifiles.tools.TextEditorDialogFragment


/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https:></https:>//github.com/burhanrashid52>
 */
internal class Text(
    private val mPhotoEditorView: PhotoEditorView,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    private val mDefaultTextTypeface: Typeface?,
    private val mGraphicManager: GraphicManager) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = mGraphicManager,
    viewType = ViewType.TEXT,
    layoutId = R.layout.view_photo_editor_text) {

    private var mTextView: TextView? = null
    var backgroundColorSpan: BackgroundColorSpan? = null;
    var drawable: Drawable? = null;
    var value :Int  = 0;


    @SuppressLint("ResourceAsColor")
    fun buildView(text: String?, styleBuilder: TextStyleBuilder?) {
        val spannable: Spannable = SpannableString(text)
        if(TextEditorDialogFragment.isbackGroudAvailable) {
          //  backgroundColorSpan = BackgroundColorSpan(R.color.semi_black_transparent)
            drawable =             context.resources.getDrawable(R.drawable.backgroun_color);
            backgroundColorSpan = BackgroundColorSpan(ContextCompat.getColor(context,R.color.tranparent_white))


        }else{
            backgroundColorSpan = BackgroundColorSpan(ContextCompat.getColor(context,R.color.tranparent_white))
            val cd = ColorDrawable(ContextCompat.getColor(context,R.color.tranparent_white))

            drawable = cd;


        }
        value =TextEditorDialogFragment.textSizeValues.size+1;
        TextEditorDialogFragment.textSizeValues.put(value,TextEditorDialogFragment.textSize);
        spannable.setSpan(backgroundColorSpan, 0, text!!.length , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)



        mTextView?.apply {
            this.text =spannable

            styleBuilder?.applyStyle(this)
        }
        val typeface = ResourcesCompat.getFont(mTextView?.context!!, TextEditorDialogFragment.textFont);
        mTextView!!.setTextColor(TextEditorDialogFragment.textColor)
        mTextView!!.setPadding(15,10,15,10)
        mTextView!!.textSize = TextEditorDialogFragment.textSize.toFloat();
        mTextView!!.typeface = typeface
        mTextView!!.textAlignment = TextEditorDialogFragment.textAlignment
        mTextView!!.background = drawable!!;
    }

    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        mTextView = rootView.findViewById(R.id.tvPhotoEditorText)
        mTextView?.run {
            gravity = Gravity.CENTER
            typeface = mDefaultTextTypeface
        }
    }

    override fun updateView(view: View) {
        val textInput = mTextView?.text.toString()
        val currentTextColor = mTextView?.currentTextColor ?: 0
        val photoEditorListener = mGraphicManager.onPhotoEditorListener
        Log.e("updateView"," ");
        photoEditorListener?.onEditTextChangeListener(view, textInput, currentTextColor,mTextView!!.background,
            mTextView!!.textSize,value)
    }

    init {
        setupGesture()
    }
}