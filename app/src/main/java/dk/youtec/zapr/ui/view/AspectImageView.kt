package dk.youtec.zapr.ui.view

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet

class AspectImageView : AppCompatImageView {

    private var mMeasurer: ViewAspectRatioMeasurer? = null

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    fun setAspectRatio(width: Int, height: Int) {
        val ratio = width.toDouble() / height
        if (mMeasurer?.aspectRatio != ratio) {
            mMeasurer = ViewAspectRatioMeasurer(ratio)
        }
    }

    fun setAspectRatio(ratio: Double) {
        if (mMeasurer?.aspectRatio != ratio) {
            mMeasurer = ViewAspectRatioMeasurer(ratio)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mMeasurer != null) {
            mMeasurer!!.measure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(mMeasurer!!.measuredWidth, mMeasurer!!.measuredHeight)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
