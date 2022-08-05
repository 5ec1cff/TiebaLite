package com.huanchengfly.tieba.post.adapters

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils

class PollAdapter(private val pollInfo: ThreadContentBean.PollInfoBean) : RecyclerView.Adapter<PollAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pollInfo.options[position]
        holder.itemView.apply {
            findViewById<TextView>(R.id.poll_item_desc).apply {
                text = SpannableStringBuilder().append(item.text).append("(${item.num} 票)",
                    ForegroundColorSpan(ThemeUtils.getColorByAttr(context, R.attr.color_text_disabled)),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            findViewById<ProgressBar>(R.id.poll_percentage).apply {
                progress = (item.num * 100.0f / pollInfo.totalNum).toInt()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_poll_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = pollInfo.optionsCount
}