package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.text.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.huanchengfly.tieba.post.BaseApplication
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.PhotoViewActivity.Companion.OBJ_TYPE_THREAD_PAGE
import com.huanchengfly.tieba.post.activities.WebViewActivity
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.api.models.ThreadContentBean.PostListItemBean
import com.huanchengfly.tieba.post.components.LinkTouchMovementMethod
import com.huanchengfly.tieba.post.components.spans.EmotionSpanV2
import com.huanchengfly.tieba.post.components.spans.MyImageSpan
import com.huanchengfly.tieba.post.components.spans.MyURLSpan
import com.huanchengfly.tieba.post.components.spans.MyUserSpan
import com.huanchengfly.tieba.post.dpToPx
import com.huanchengfly.tieba.post.isTablet
import com.huanchengfly.tieba.post.models.PhotoViewBean
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.BilibiliUtil.replaceVideoNumberSpan
import com.huanchengfly.tieba.post.widgets.MyImageView
import com.huanchengfly.tieba.post.widgets.VideoPlayerStandard
import com.huanchengfly.tieba.post.widgets.VoicePlayerView
import com.huanchengfly.tieba.post.widgets.theme.TintMySpannableTextView
import java.util.*
import kotlin.math.roundToInt

class PostListAdapterHelper(
    private val context: Context
): ThreadContentViewHelper(context) {
    var seeLz: Boolean = false
    var pureRead: Boolean = false
    private var dataBean: ThreadContentBean? = null
    private var photoViewBeansMap: TreeMap<Int, List<PhotoViewBean>> = TreeMap()

    fun setData(data: ThreadContentBean) {
        dataBean = data
        setPic(data.postList)
    }

    fun addData(data: ThreadContentBean) {
        dataBean = data
        addPic(data.postList)
    }

    private fun setPic(postListItemBeans: List<PostListItemBean>?) {
        photoViewBeansMap = TreeMap()
        addPic(postListItemBeans)
    }

    private fun addPic(postListItemBeans: List<PostListItemBean>?) {
        if (postListItemBeans != null) {
            for (postListItemBean in postListItemBeans) {
                val photoViewBeans: MutableList<PhotoViewBean> = ArrayList()
                if (postListItemBean.content.isNullOrEmpty() || postListItemBean.floor == null) {
                    continue
                }
                for (contentBean in postListItemBean.content) {
                    if (contentBean.type == "3") {
                        if (contentBean.originSrc == null) {
                            continue
                        }
                        val url = ImageUtil.getUrl(
                            context,
                            true,
                            contentBean.originSrc,
                            contentBean.bigCdnSrc,
                            contentBean.cdnSrcActive,
                            contentBean.cdnSrc
                        )
                        if (url.isNullOrEmpty()) {
                            continue
                        }
                        photoViewBeans.add(
                            PhotoViewBean(
                                url,
                                ImageUtil.getNonNullString(
                                    contentBean.originSrc,
                                    contentBean.bigCdnSrc,
                                    contentBean.cdnSrcActive,
                                    contentBean.cdnSrc
                                ),
                                "1" == contentBean.isLongPic
                            )
                        )
                    }
                }
                photoViewBeansMap[Integer.valueOf(postListItemBean.floor)] = photoViewBeans
            }
        }
    }

    override fun onCreateTextView(): TextView {
        val textView: TextView = super.onCreateTextView()
        if (pureRead) {
            textView.setLineSpacing(0.5f, 1.3f)
        } else {
            textView.setLineSpacing(0.5f, 1.2f)
        }
        return textView
    }

    private fun getMaxWidth(floor: String): Float {
        var maxWidth: Float =
            BaseApplication.ScreenInfo.EXACT_SCREEN_WIDTH.toFloat() - (24 * 2 + 38).dpToPx()
        if (pureRead || "1" == floor) {
            maxWidth =
                BaseApplication.ScreenInfo.EXACT_SCREEN_WIDTH.toFloat() - (16 * 2 + 4).dpToPx()
        }
        if (context.isTablet) {
            return maxWidth / 2;
        }
        return maxWidth
    }

    override fun onCreateLayoutParams(
        contentBean: ThreadContentBean.ContentBean,
        floor: String
    ): LinearLayout.LayoutParams {
        var widthFloat: Float
        var heightFloat: Float
        when (contentBean.type) {
            CONTENT_TYPE_IMAGE,
            CONTENT_TYPE_MEME_IMAGE -> {
                val strings = contentBean.bsize!!.split(",".toRegex()).toTypedArray()
                widthFloat = java.lang.Float.valueOf(strings[0])
                heightFloat = java.lang.Float.valueOf(strings[1])
                heightFloat *= getMaxWidth(floor) / widthFloat
                widthFloat = getMaxWidth(floor)
            }
            CONTENT_TYPE_VIDEO -> {
                val width = java.lang.Float.valueOf(contentBean.width!!)
                widthFloat = getMaxWidth(floor)
                heightFloat = java.lang.Float.valueOf(contentBean.height!!)
                heightFloat *= widthFloat / width
            }
            else -> {
                return super.onCreateLayoutParams(contentBean, floor)
            }
        }
        val width = widthFloat.roundToInt()
        val height = heightFloat.roundToInt()
        val layoutParams = LinearLayout.LayoutParams(width, height)
        layoutParams.gravity = if (context.isTablet) {
            Gravity.START
        } else {
            Gravity.CENTER_HORIZONTAL
        }
        val dp16 = 16f.dpToPx()
        val dp4 = 4f.dpToPx()
        val dp2 = 2f.dpToPx()
        if ("1" == floor) {
            layoutParams.setMargins(dp16, dp2, dp16, dp2)
        } else {
            layoutParams.setMargins(dp4, dp2, dp4, dp2)
        }
        if (context.isTablet) {
            layoutParams.setMargins(0, dp2, 0, dp2)
        }
        return layoutParams
    }

    private fun getPhotoViewBeans(): List<PhotoViewBean> {
        val photoViewBeans: MutableList<PhotoViewBean> = mutableListOf()
        for (key in photoViewBeansMap.keys) {
            if (photoViewBeansMap.get(key) != null) photoViewBeans.addAll(
                photoViewBeansMap[key]
                    ?: emptyList()
            )
        }
        return photoViewBeans
    }

    override fun onInitImageView(view: ImageView, contentBean: ThreadContentBean.ContentBean) {
        super.onInitImageView(view, contentBean)
        val photoViewBeans: List<PhotoViewBean> = getPhotoViewBeans()
        for (photoViewBean in photoViewBeans) {
            if (TextUtils.equals(photoViewBean.originUrl, contentBean.originSrc)) {
                ImageUtil.initImageView(
                    view,
                    photoViewBeans,
                    photoViewBeans.indexOf(photoViewBean),
                    dataBean!!.forum!!.name,
                    dataBean!!.forum!!.id,
                    dataBean!!.thread!!.id,
                    seeLz,
                    OBJ_TYPE_THREAD_PAGE
                )
                break
            }
        }
    }

    fun getContentViews(postListItemBean: PostListItemBean, outList: MutableList<View>) {
        super.getContentViews(postListItemBean.content!!, postListItemBean.floor!!, outList)
    }

    fun getContentViews(postListItemBean: PostListItemBean): List<View> =
        mutableListOf<View>().also {
            getContentViews(postListItemBean.content!!, postListItemBean.floor!!, it)
        }

}