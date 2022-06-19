package com.huanchengfly.tieba.post.components.spans

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.DynamicDrawableSpan
import android.widget.TextView
import com.huanchengfly.tieba.post.utils.EmotionManager

class EmotionSpanV2(private val name: String, private val mContext: Context) : DynamicDrawableSpan(ALIGN_BASELINE) {
    var size: Int = 0

    private lateinit var mDrawable: Drawable

    override fun getDrawable(): Drawable? {
        if (this::mDrawable.isInitialized) return mDrawable
        return EmotionManager.getEmotionDrawable(name, mContext)?.also {
            it.setBounds(0, 0, size, size)
            mDrawable = it
        }
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
        val drawable = drawable ?: return
        val fm = paint.fontMetricsInt
        val transY = y - drawable.bounds.bottom + fm.descent
        canvas.save()
        canvas.translate(x, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }
}