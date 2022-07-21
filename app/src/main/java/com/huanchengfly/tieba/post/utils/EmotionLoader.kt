package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.huanchengfly.tieba.post.components.spans.EmotionSpanV2
import java.lang.ref.WeakReference

class EmotionLoader(private val mContext: Context) {
    companion object {
        private const val TAG = "EmotionUtilsV2"

        private fun getEmotionUrl(name: String): String {
            // dirty hack
            val realName = if (name == "image_emoticon") "image_emoticon1" else name
            return "http://static.tieba.baidu.com/tb/editor/images/client/$realName.png"
        }
    }

    private data class LoadItem(val name: String, val start: Int, val stop: Int)

    private val mEmotions by lazy { mutableListOf<LoadItem>() }

    private fun Spannable.replaceSpan(oldSpan: Any?, newSpan: Any?, flags: Int): Boolean {
        if (oldSpan == null || newSpan == null) {
            return false
        }
        val start = getSpanStart(oldSpan)
        val end = getSpanEnd(oldSpan)
        if (start == -1 || end == -1) {
            return false
        }
        removeSpan(oldSpan)
        setSpan(newSpan, start, end, flags)
        return true
    }

    fun add(name: String, start: Int, end: Int) {
        mEmotions.add(LoadItem(name, start, end))
    }

    fun into(textView: TextView) {
        val tv = WeakReference(textView)
        mEmotions.forEach { item ->
            val (name, start, end) = item
            val placeHolder = EmotionSpanV2(textView)
            (textView.text as? Spannable)?.setSpan(placeHolder, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            Glide.with(mContext)
                .load(getEmotionUrl(name))
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        tv.get()?.let {
                            val spannable = textView.text as? Spannable
                            if (spannable == null) {
                                Log.e(TAG, "failed to replace span: $name (${it.text})")
                            } else {
                                spannable.setSpan(
                                    EmotionSpanV2(it, resource),
                                    start,
                                    end,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                it.invalidate()
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Log.e(TAG, "failed to load $name")
                    }
                })
        }
        mEmotions.clear()
    }

}