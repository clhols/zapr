package dk.youtec.zapr.ui.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView

import java.util.Hashtable

import dk.youtec.zapr.R

/**
 * TextView where we can set custom settings etc. fonts
 */
class FontTextView : TextView {

    constructor(context: Context) : super(context) {
        setupFont(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupFont(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setupFont(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setupFont(context, attrs)
    }

    protected fun setupFont(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FontTextView)
        val fontName = a.getText(R.styleable.FontTextView_font_name)
        a.recycle()
        if (fontName?.isNotEmpty() ?: false) {
            try {
                val face = get(fontName.toString(), context)
                typeface = face
            } catch (e: RuntimeException) {
                try {
                    Log.e(TAG, e.message, e)
                    throw RuntimeException(e)
                } catch (ex: Exception) {
                }
            }
        }
    }

    companion object {

        private val TAG = FontTextView::class.java.simpleName
        private val fontCache = Hashtable<String, Typeface>()

        operator fun get(name: String, context: Context): Typeface? {
            var tf: Typeface? = fontCache[name]
            if (tf == null) {
                try {
                    tf = Typeface.createFromAsset(context.assets, name)
                } catch (e: Exception) {
                    return null
                }

                fontCache.put(name, tf)
            }
            return tf
        }
    }
}
