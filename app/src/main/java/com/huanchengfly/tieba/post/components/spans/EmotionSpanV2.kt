package com.huanchengfly.tieba.post.components.spans

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan
import android.util.Log
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class EmotionSpanV2 : DynamicDrawableSpan {
    private val size: Int
    private var mDrawable: Drawable? = null

    constructor(textView: TextView): this(textView, null)

    constructor(textView: TextView, drawable: Drawable?): super(ALIGN_BASELINE) {
        val paint = textView.paint
        size = (-paint.ascent() + paint.descent()).roundToInt()
        mDrawable = drawable?.apply { setBounds(0, 0, size, size) }
    }

    override fun getDrawable(): Drawable? {
       return mDrawable
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        if (fm != null) {
            val fontFm = paint.fontMetricsInt
            fm.ascent = fontFm.top
            fm.descent = fontFm.bottom
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return size
    }

    override fun draw(
        canvas: Canvas, text: CharSequence?, start: Int, end: Int,
        x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val drawable = mDrawable ?: return
        val fm = paint.fontMetricsInt
        val transY = y - drawable.bounds.bottom + fm.descent
        canvas.save()
        canvas.translate(x, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }

    override fun toString(): String {
        return "{EmotionSpanV2:$mDrawable}"
    }
}