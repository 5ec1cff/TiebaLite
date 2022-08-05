package com.huanchengfly.tieba.post.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.vlayout.DelegateAdapter
import com.alibaba.android.vlayout.LayoutHelper
import com.alibaba.android.vlayout.layout.SingleLayoutHelper
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.ForumActivity.Companion.launch
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.components.MyViewHolder
import com.huanchengfly.tieba.post.components.spans.RoundBackgroundColorSpan
import com.huanchengfly.tieba.post.ui.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.*
import com.huanchengfly.tieba.post.widgets.MyLinearLayout


class ThreadMainPostAdapter(
        private val context: Context
) : DelegateAdapter.Adapter<MyViewHolder>() {
    var dataBean: ThreadContentBean? = null
        set(value) {
            field = value
            if (value != null) helper.setData(value)
            notifyDataSetChanged()
        }
    var showForum: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val helper: PostListAdapterHelper = PostListAdapterHelper(context)

    var pureRead: Boolean = false
        set(value) {
            field = value
            helper.pureRead = value
            notifyDataSetChanged()
        }
        get() = helper.pureRead

    var seeLz: Boolean = false
        set(value) {
            field = value
            helper.seeLz = value
        }
        get() = helper.seeLz

    val threadPostBean: ThreadContentBean.PostListItemBean?
        get() {
            return if (dataBean?.postList.isNullOrEmpty() || dataBean?.postList!![0].floor != "1") {
                null
            } else {
                dataBean?.postList!![0]
            }
        }

    val threadBean: ThreadContentBean.ThreadBean
        get() = dataBean?.thread!!

    private val contentBeans: List<ThreadContentBean.ContentBean>
        get() = if (threadPostBean == null) emptyList() else threadPostBean!!.content ?: emptyList()

    private val user: ThreadContentBean.UserInfoBean
        get() = dataBean?.thread?.author!!

    private val title: String
        get() = dataBean?.thread?.title ?: ""

    override fun onCreateLayoutHelper(): LayoutHelper {
        return SingleLayoutHelper()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(context, R.layout.item_thread_list_post)
    }

    private fun showMenu() {
        showMenu(context, user, dataBean!!, threadPostBean!!)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        refreshForumView(dataBean?.forum, holder.getView(R.id.forum_bar))
        holder.itemView.setOnLongClickListener {
            showMenu()
            true
        }
        holder.setVisibility(R.id.thread_list_item_user_lz_tip, true)
        var username: CharSequence = StringUtil.getUsernameString(context, user.name, user.nameShow)
        if (user.isBawu == "1") {
            val bawuType = if (user.bawuType == "manager") "吧主" else "小吧主"
            username = SpannableStringBuilder(username).apply {
                append(" ")
                append(
                    bawuType, RoundBackgroundColorSpan(
                        context,
                        Util.alphaColor(ThemeUtils.getColorByAttr(context, R.attr.colorAccent), 30),
                        ThemeUtils.getColorByAttr(context, R.attr.colorAccent),
                        DisplayUtil.dp2px(context, 10f).toFloat()
                    ), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        holder.setText(
            R.id.thread_list_item_user_name,
            username
        )

        val levelId =
            if (user.levelId == null || TextUtils.isEmpty(user.levelId)) "?" else user.levelId
        ThemeUtil.setChipThemeByLevel(
            levelId,
            holder.getView(R.id.thread_list_item_user_status),
            holder.getView(R.id.thread_list_item_user_level),
            holder.getView(R.id.thread_list_item_user_lz_tip)
        )
        holder.setText(R.id.thread_list_item_user_level, levelId)
        holder.setOnClickListener(R.id.thread_list_item_user_avatar) {
            NavigationHelper.toUserSpaceWithAnim(
                context,
                user.id,
                StringUtil.getAvatarUrl(user.portrait),
                it
            )
        }
        ImageUtil.load(
            holder.getView(R.id.thread_list_item_user_avatar),
            ImageUtil.LOAD_TYPE_AVATAR,
            user.portrait
        )
        var timeText =
            context.getString(
                R.string.tip_thread_item_thread,
                DateTimeUtils.getRelativeTimeString(context, threadBean.createTime!!)
            )
        if (user.ipAddress?.isNotEmpty() == true) {
            timeText += " "
            timeText += context.getString(R.string.text_ip_location, user.ipAddress)
        }
        holder.setText(
            R.id.thread_list_item_user_time,
            timeText
        )
        holder.setText(R.id.thread_list_item_content_title, title)
        val views = mutableListOf<View>()
        if (threadPostBean != null) {
            helper.getContentViews(threadPostBean!!, views)
        }
        threadBean.pollInfo?.let { pollInfoBean ->
            LayoutInflater.from(context).inflate(R.layout.layout_poll, null).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                findViewById<RecyclerView>(R.id.poll_items).apply {
                    adapter = PollAdapter(pollInfoBean)
                }
                val pollDesc = SpannableStringBuilder()
                    .append("投票", RoundBackgroundColorSpan(
                        context,
                        Util.alphaColor(ThemeUtils.getColorByAttr(context, R.attr.colorAccent), 30),
                        ThemeUtils.getColorByAttr(context, R.attr.colorAccent),
                        DisplayUtil.dp2px(context, 10f).toFloat()
                    ), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append(" ")
                    .append(pollInfoBean.title, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append("\n")
                    .append("${if (pollInfoBean.isMulti) "多选" else "单选"} ${pollInfoBean.totalNum}人参与 共${pollInfoBean.totalPoll}票（当前不支持投票）",
                    ForegroundColorSpan(ThemeUtils.getColorByAttr(context, R.attr.color_text_disabled)),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                findViewById<TextView>(R.id.poll_desc).text = pollDesc
                views.add(this)
            }

        }
        if (views.isNotEmpty()) {
            holder.getView<MyLinearLayout>(R.id.thread_list_item_content_content).apply {
                removeAllViews()
                addViews(views)
            }
        }
    }

    override fun getItemCount(): Int {
        return 1
    }

    private fun refreshForumView(
        forumInfoBean: ThreadContentBean.ForumInfoBean?,
        forumView: ViewGroup?
    ) {
        if (forumView == null || forumInfoBean == null) {
            return
        }
        val forumNameView = forumView.findViewById<TextView>(R.id.forum_bar_name)
        val forumAvatarView: ImageView = forumView.findViewById(R.id.forum_bar_avatar)
        if (!showForum || !context.appPreferences.showShortcutInThread || "0" == forumInfoBean.isExists || forumInfoBean.name?.isEmpty() == true) {
            forumView.visibility = View.GONE
            return
        }
        forumView.visibility = View.VISIBLE
        forumView.setOnClickListener(View.OnClickListener { launch(context, forumInfoBean.name!!) })
        forumNameView.text = forumInfoBean.name
        ImageUtil.load(forumAvatarView, ImageUtil.LOAD_TYPE_AVATAR, forumInfoBean.avatar)
    }
}