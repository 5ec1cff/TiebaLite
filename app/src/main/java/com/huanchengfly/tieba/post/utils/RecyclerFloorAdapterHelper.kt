package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.models.PhotoViewBean
import java.util.ArrayList

class RecyclerFloorAdapterHelper(private val mContext: Context) : ThreadContentViewHelper(mContext) {
    private val maxWidth: Float

    init {
        val dm = mContext.resources.displayMetrics
        maxWidth = dm.widthPixels.toFloat()
    }

    override fun onInitImageView(view: ImageView, contentBean: ThreadContentBean.ContentBean) {
        val photoViewBeans: MutableList<PhotoViewBean> = ArrayList()
        photoViewBeans.add(
            PhotoViewBean(
                ImageUtil.getNonNullString(contentBean.src, contentBean.originSrc),
                ImageUtil.getNonNullString(contentBean.originSrc, contentBean.src),
                "1" == contentBean.isLongPic
            )
        )
        ImageUtil.initImageView(view, photoViewBeans, 0)
    }

    override fun onCreateLayoutParams(
        contentBean: ThreadContentBean.ContentBean,
        floor: String
    ): LinearLayout.LayoutParams {
        if (contentBean.type != "3" && contentBean.type != "5") {
            return super.onCreateLayoutParams(contentBean, floor)
        }
        var widthFloat: Float
        var heightFloat: Float
        if (contentBean.type == "3" || contentBean.type == "20") {
            val strings = contentBean.bsize!!.split(",").toTypedArray()
            widthFloat = strings[0].toFloat()
            heightFloat = strings[1].toFloat()
            heightFloat *= maxWidth / widthFloat
            widthFloat = maxWidth
        } else {
            val width = contentBean.width!!.toFloat()
            widthFloat = maxWidth
            heightFloat = contentBean.height!!.toFloat()
            heightFloat *= widthFloat / width
        }
        val width = Math.round(widthFloat)
        val height = Math.round(heightFloat)
        val layoutParams = LinearLayout.LayoutParams(width, height)
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        layoutParams.setMargins(0, 8, 0, 8)
        return layoutParams
    }
}