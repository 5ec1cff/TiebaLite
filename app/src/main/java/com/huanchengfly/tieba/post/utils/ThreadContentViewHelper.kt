package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.WebViewActivity
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.components.LinkTouchMovementMethod
import com.huanchengfly.tieba.post.components.spans.MyImageSpan
import com.huanchengfly.tieba.post.components.spans.MyURLSpan
import com.huanchengfly.tieba.post.components.spans.MyUserSpan
import com.huanchengfly.tieba.post.models.PhotoViewBean
import com.huanchengfly.tieba.post.ui.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.widgets.MyImageView
import com.huanchengfly.tieba.post.widgets.VideoPlayerStandard
import com.huanchengfly.tieba.post.widgets.VoicePlayerView
import com.huanchengfly.tieba.post.widgets.theme.TintMySpannableTextView

open class ThreadContentViewHelper(private val mContext: Context) {
    companion object {
        const val CONTENT_TYPE_TEXT = "0"
        const val CONTENT_TYPE_LINK = "1"
        const val CONTENT_TYPE_EMOTION = "2"
        const val CONTENT_TYPE_IMAGE = "3"
        const val CONTENT_TYPE_USER = "4"
        const val CONTENT_TYPE_VIDEO = "5"
        const val CONTENT_TYPE_TEXT_ALT = "9"
        const val CONTENT_TYPE_VOICE = "10"
        const val CONTENT_TYPE_HASHTAG = "18" // link
        const val CONTENT_TYPE_MEME_IMAGE = "20"
    }

    private val defaultLayoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 8, 0, 8) }
    
    protected open fun onCreateTextView(): TextView = TintMySpannableTextView(mContext).apply {
        setTintResId(R.color.default_color_text)
        setLinkTouchMovementMethod(LinkTouchMovementMethod.getInstance())
        isFocusable = false
        isClickable = false
        isLongClickable = false
        setTextIsSelectable(false)
        setOnClickListener(null)
        setOnLongClickListener(null)
        letterSpacing = 0.02f
        textSize = 16f
    }
    
    protected open fun onInitImageView(view: ImageView, contentBean: ThreadContentBean.ContentBean) {}

    protected open fun onCreateLayoutParams(
        contentBean: ThreadContentBean.ContentBean,
        floor: String
    ): LinearLayout.LayoutParams {
        return defaultLayoutParams
    }

    protected open fun onCreateText(s: CharSequence): CharSequence {
        return BilibiliUtil.replaceVideoNumberSpan(mContext, s)
    }

    protected open fun onCreateLinkContent(
        newContent: CharSequence?,
        url: String,
        addIcon: Boolean = true): CharSequence = SpannableStringBuilder().apply {
        if (addIcon) {
            var bitmap = Util.getBitmapFromVectorDrawable(mContext, R.drawable.ic_link)
            val size = DisplayUtil.sp2px(mContext, 16f)
            val color: Int = ThemeUtils.getColorByAttr(mContext, R.attr.colorAccent)
            bitmap = Bitmap.createScaledBitmap(bitmap!!, size, size, true)
            bitmap = Util.tintBitmap(bitmap, color)
            append("[链接]", MyImageSpan(mContext, bitmap), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        append(
            newContent ?: url,
            MyURLSpan(mContext, url),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    protected open fun onCreateUserContent(
        newContent: CharSequence?,
        uid: String): CharSequence = SpannableStringBuilder().apply {
        append(newContent ?: "用户 ${uid}",
            MyUserSpan(mContext, uid),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private val mViews by lazy { mutableListOf<View>() }
    private var mCurrentText: SpannableStringBuilder? = null
    private val mEmotionLoader by lazy { EmotionLoader(mContext) }

    private fun performCreateTextView(): TextView = onCreateTextView().apply {
        layoutParams = defaultLayoutParams
    }

    protected fun appendText(s: CharSequence): Pair<Int, Int> {
        if (mCurrentText == null) mCurrentText = SpannableStringBuilder()
        val start = mCurrentText!!.length
        mCurrentText!!.append(s)
        val end = mCurrentText!!.length
        return Pair(start, end)
    }

    protected fun appendView(v: View) {
        appendTextView()
        mViews.add(v)
    }

    private fun appendTextView() {
        if (mCurrentText != null) {
            val textView = performCreateTextView()
            textView.text = mCurrentText
            mViews.add(textView)
            mEmotionLoader.into(textView)
            mCurrentText = null
        }
    }
    
    open fun getContentViews(contentBeans: List<ThreadContentBean.ContentBean>, floor: String): List<View> {
        mViews.clear()
        mCurrentText = null
        for (contentBean in contentBeans) {
            when (contentBean.type) {
                // text
                CONTENT_TYPE_TEXT, CONTENT_TYPE_TEXT_ALT ->
                    appendText(onCreateText(contentBean.text!!))
                CONTENT_TYPE_LINK, CONTENT_TYPE_HASHTAG ->
                    appendText(onCreateLinkContent(contentBean.text, contentBean.link!!))
                CONTENT_TYPE_USER ->
                    appendText(onCreateUserContent(contentBean.text, contentBean.uid!!))
                CONTENT_TYPE_EMOTION -> {
                    val (start, end) = appendText("#(${contentBean.c})")
                    mEmotionLoader.add(contentBean.text!!, start, end)
                }
                // media
                CONTENT_TYPE_IMAGE -> {
                    val url = (contentBean.originSrc ?: contentBean.src)?.let {
                        ImageUtil.getUrl(
                            mContext,
                            true,
                            it,
                            contentBean.bigCdnSrc,
                            contentBean.cdnSrcActive,
                            contentBean.cdnSrc
                        )
                    }
                    if (TextUtils.isEmpty(url)) {
                        continue
                    }
                    val imageView = MyImageView(mContext)
                    imageView.apply {
                        layoutParams = onCreateLayoutParams(contentBean, floor)
                        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    ImageUtil.load(imageView, ImageUtil.LOAD_TYPE_SMALL_PIC, url)
                    onInitImageView(imageView, contentBean)
                    appendView(imageView)
                }
                CONTENT_TYPE_VIDEO -> if (contentBean.src != null && contentBean.width != null && contentBean.height != null) {
                    if (contentBean.link != null) {
                        val videoPlayerStandard = VideoPlayerStandard(mContext)
                        videoPlayerStandard.setUp(contentBean.link, "")
                        videoPlayerStandard.layoutParams =
                            onCreateLayoutParams(contentBean, floor)
                        videoPlayerStandard.id = R.id.video_player
                        ImageUtil.load(
                            videoPlayerStandard.posterImageView,
                            ImageUtil.LOAD_TYPE_SMALL_PIC,
                            contentBean.src,
                            true
                        )
                        appendView(videoPlayerStandard)
                    } else {
                        val videoImageView = MyImageView(mContext)
                        videoImageView.layoutParams =
                            onCreateLayoutParams(contentBean, floor)
                        videoImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        ImageUtil.load(
                            videoImageView,
                            ImageUtil.LOAD_TYPE_SMALL_PIC,
                            contentBean.src,
                            true
                        )
                        videoImageView.setOnClickListener {
                            WebViewActivity.launch(it.context, contentBean.text)
                        }
                        appendView(videoImageView)
                    }
                } else {
                    appendText(onCreateLinkContent("[视频] ${contentBean.text}", contentBean.text!!))
                }
                CONTENT_TYPE_VOICE -> {
                    val voiceUrl =
                        "http://c.tieba.baidu.com/c/p/voice?voice_md5=${contentBean.voiceMD5}&play_from=pb_voice_play"
                    val voicePlayerView = VoicePlayerView(mContext)
                    voicePlayerView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    voicePlayerView.duration = contentBean.duringTime!!.toInt()
                    voicePlayerView.url = voiceUrl
                    appendView(voicePlayerView)
                }
                CONTENT_TYPE_MEME_IMAGE -> {
                    val memeImageView = MyImageView(mContext)
                    memeImageView.layoutParams =
                        onCreateLayoutParams(contentBean, floor)
                    memeImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    ImageUtil.load(memeImageView, ImageUtil.LOAD_TYPE_SMALL_PIC, contentBean.src)
                    ImageUtil.initImageView(
                        memeImageView,
                        PhotoViewBean(contentBean.src, contentBean.src, false)
                    )
                    appendView(memeImageView)
                }
                else -> contentBean.text?.let { appendText(it) }
            }
        }
        appendTextView()
        return mViews
    }
}