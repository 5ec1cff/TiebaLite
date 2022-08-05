package com.huanchengfly.tieba.post.adapters

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.huanchengfly.tieba.post.*
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.activities.ForumActivity
import com.huanchengfly.tieba.post.activities.PhotoViewActivity
import com.huanchengfly.tieba.post.activities.ReplyActivity
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.components.LinkMovementClickMethod
import com.huanchengfly.tieba.post.components.spans.RoundBackgroundColorSpan
import com.huanchengfly.tieba.post.databinding.ItemThreadListBinding
import com.huanchengfly.tieba.post.databinding.ItemThreadListPostBinding
import com.huanchengfly.tieba.post.fragments.FloorFragment
import com.huanchengfly.tieba.post.models.PhotoViewBean
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.*
import com.huanchengfly.tieba.post.viewmodels.PostItem
import com.huanchengfly.tieba.post.widgets.MyLinearLayout
import com.huanchengfly.tieba.post.widgets.theme.TintTextView
import java.util.*
import kotlin.math.roundToInt

sealed class ThreadPagingViewHolder(view: View): RecyclerView.ViewHolder(view) {
    class FloorViewHolder(
        val binding: ItemThreadListBinding
        ): ThreadPagingViewHolder(binding.root)
    class MainFloorViewHolder(
        val binding: ItemThreadListPostBinding
        ): ThreadPagingViewHolder(binding.root)
}

object PostComparator : DiffUtil.ItemCallback<PostItem>() {
    override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
        return oldItem.post.id!! == newItem.post.id!!
    }

    override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
        return oldItem == newItem
    }
}

class ThreadPagingAdapter(
    private var activity: BaseActivity?,
    private val showForum: Boolean = true,
    private val photoBeansMap: TreeMap<Int, List<PhotoViewBean>>,
    private val userInfoBeanMap: MutableMap<String, ThreadContentBean.UserInfoBean>
) : PagingDataAdapter<PostItem, ThreadPagingViewHolder>(PostComparator) {

    companion object {
        const val TYPE_FLOOR = 1
        const val TYPE_MAIN_FLOOR = 2
    }

    override fun onBindViewHolder(holder: ThreadPagingViewHolder, position: Int) {
        when (holder) {
            is ThreadPagingViewHolder.FloorViewHolder -> {
                bindForFloor(holder, position)
            }
            is ThreadPagingViewHolder.MainFloorViewHolder -> {
                bindForMainFloor(holder, position)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ThreadPagingViewHolder = when (viewType) {
        TYPE_FLOOR -> ThreadPagingViewHolder.FloorViewHolder(
            ItemThreadListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
        TYPE_MAIN_FLOOR -> ThreadPagingViewHolder.MainFloorViewHolder(
            ItemThreadListPostBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
        else -> throw IllegalArgumentException("unknown viewType $viewType")
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position)!!.post.floor == "1")
            TYPE_MAIN_FLOOR
        else
            TYPE_FLOOR

    private fun bindForMainFloor(viewHolder: ThreadPagingViewHolder.MainFloorViewHolder, position: Int) {
        val data = getItem(position)!!
        val context = viewHolder.itemView.context
        val binding = viewHolder.binding
        val userInfoBean = data.thread.thread!!.author!!
        val post = data.post
        data.thread.forum?.also { forumInfoBean ->
            val forumView = binding.forumBar
            val forumNameView = binding.forumBarName
            val forumAvatarView: ImageView = binding.forumBarAvatar
            if (!showForum || !context.appPreferences.showShortcutInThread || "0" == forumInfoBean.isExists || forumInfoBean.name?.isEmpty() == true) {
                forumView.visibility = View.GONE
                return
            }
            forumView.visibility = View.VISIBLE
            forumView.setOnClickListener {
                ForumActivity.launch(
                    it.context,
                    forumInfoBean.name!!
                )
            }
            forumNameView.text = forumInfoBean.name
            ImageUtil.load(forumAvatarView, ImageUtil.LOAD_TYPE_AVATAR, forumInfoBean.avatar)
        }
        binding.threadListItemUserLzTip.visibility = View.VISIBLE
        setUser(
            context,
            userInfoBean,
            binding.threadListItemUserStatus,
            binding.threadListItemUserName,
            binding.threadListItemUserLevel,
            binding.threadListItemUserAvatar,
            binding.threadListItemUserLzTip,
        )
        binding.threadListItemUserTime.text = getTime(context, post, userInfoBean)
        binding.threadListItemContentTitle.text = data.thread.thread.title
        binding.threadListItemContentContent.apply {
            removeAllViews()
            addViews(FloorContentHelper(context, photoBeansMap, data.thread).getContentViews(post))
        }
        viewHolder.itemView.setOnLongClickListener {
            showMenu(it.context, userInfoBean, data.thread, data.post)
            true
        }
        // TODO: poll
    }

    private fun bindForFloor(viewHolder: ThreadPagingViewHolder.FloorViewHolder, position: Int) {
        val data = getItem(position)!!
        val post = data.post
        val context = viewHolder.itemView.context
        val binding = viewHolder.binding
        viewHolder.binding.threadListItemUserLzTip.visibility =
            if (data.post.authorId == data.thread.thread!!.author!!.id) View.VISIBLE
            else View.GONE
        val userInfoBean = userInfoBeanMap[data.post.authorId]
        if (userInfoBean != null) {
            setUser(
                context,
                userInfoBean,
                binding.threadListItemUserStatus,
                binding.threadListItemUserName,
                binding.threadListItemUserLevel,
                binding.threadListItemUserAvatar,
                binding.threadListItemUserLzTip,
            )
        } else {
            binding.threadListItemUserName.text = data.post.authorId
        }
        binding.threadListItemContentContent.apply {
            removeAllViews()
            addViews(FloorContentHelper(context, photoBeansMap, data.thread).getContentViews(post))
        }
        binding.threadListItemUserTime.text = getTime(context, post, userInfoBean)
        initFloorView(context, binding, data)
        // TODO: PureRead
        binding.threadListItemAgreeBtn.text = if (post.agree?.diffAgreeNum != "0") {
                context.getString(R.string.btn_agree_post, post.agree?.diffAgreeNum)
            } else {
                context.getString(R.string.btn_agree_post_default)
            }
        viewHolder.itemView.apply {
            background = getItemBackgroundDrawable(
                context,
                position,
                itemCount,
                positionOffset = 1,
                radius = 16f.dpToPxFloat()
            )
            setOnClickListener {
                ReplyActivity.start(context, data.thread,
                    pid = data.post.id,
                    floorNum = data.post.floor,
                    replyUser = if (userInfoBean != null) userInfoBean.nameShow else "")
            }
            setOnLongClickListener {
                activity?.also { showMenu(it, userInfoBean, data.thread, data.post) }
                true
            }
        }
    }

    private fun setUser(
        context: Context,
        userInfoBean: ThreadContentBean.UserInfoBean,
        parent: ViewGroup,
        viewUserName: TextView,
        viewUserLevel: TextView,
        viewUserAvatar: ImageView,
        viewLzTip: TextView
    ) {
        viewUserName.text = getUserName(context, userInfoBean)
        val levelId =
            if (userInfoBean.levelId == null || TextUtils.isEmpty(userInfoBean.levelId)) "?" else userInfoBean.levelId
        ThemeUtil.setChipThemeByLevel(
            levelId,
            parent,
            viewUserLevel,
            viewLzTip
        )
        viewUserLevel.text = levelId
        viewUserAvatar.setOnClickListener { view: View ->
            NavigationHelper.toUserSpaceWithAnim(
                view.context,
                userInfoBean.id,
                StringUtil.getAvatarUrl(userInfoBean.portrait),
                view
            )
        }
        ImageUtil.load(
            viewUserAvatar,
            ImageUtil.LOAD_TYPE_AVATAR,
            userInfoBean.portrait
        )
    }

    private fun getUserName(context: Context, userInfoBean: ThreadContentBean.UserInfoBean): CharSequence {
        var username = StringUtil.getUsernameString(
            context,
            userInfoBean.name!!,
            userInfoBean.nameShow
        )
        if (userInfoBean.isBawu == "1") {
            val bawuType = if (userInfoBean.bawuType == "manager") "吧主" else "小吧主"
            username = SpannableStringBuilder(username)
                .append(" ")
                .append(
                    bawuType, RoundBackgroundColorSpan(
                        context,
                        Util.alphaColor(ThemeUtils.getColorByAttr(context, R.attr.colorAccent), 30),
                        ThemeUtils.getColorByAttr(context, R.attr.colorAccent),
                        DisplayUtil.dp2px(context, 10f).toFloat()
                    ), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
        }
        return username
    }

    private fun getTime(
        context: Context,
        post: ThreadContentBean.PostListItemBean,
        userInfoBean: ThreadContentBean.UserInfoBean?
    ): CharSequence {
        var timeText = context.getString(
            R.string.tip_thread_item,
            post.floor,
            DateTimeUtils.getRelativeTimeString(context, post.time!!)
        )
        if (userInfoBean?.ipAddress?.isNotEmpty() == true) {
            timeText += " " + context.getString(R.string.text_ip_location, userInfoBean.ipAddress)
        }
        return timeText
    }

    private fun initFloorView(
        context: Context,
        binding: ItemThreadListBinding,
        data: PostItem) {
        val more = binding.threadListItemContentFloorMore
        val myLinearLayout = binding.threadListItemContentFloor
        myLinearLayout.removeAllViews()
        val bean = data.post
        if (bean.subPostNumber != null && bean.subPostList != null && bean.subPostList.subPostList != null && bean.subPostList.subPostList.size > 0) {
            binding.threadListItemContentFloorCard.visibility = View.VISIBLE
            val count = bean.subPostNumber.toInt()
            var subPostList = bean.subPostList.subPostList
            when {
                subPostList.size > ThreadReplyAdapter.MAX_SUB_POST_SHOW -> {
                    subPostList = subPostList.subList(0, ThreadReplyAdapter.MAX_SUB_POST_SHOW)
                    more.visibility = View.VISIBLE
                }
                subPostList.size < count -> {
                    more.visibility = View.VISIBLE
                }
                else -> {
                    more.visibility = View.GONE
                }
            }
            more.text = context.getString(
                R.string.tip_floor_more_count,
                (count - subPostList.size).toString()
            )
            myLinearLayout.addViews(
                SubFloorContentViewHelper(context).getContentViewForSubPosts(
                    data.post.subPostList!!.subPostList!!,
                    data.post,
                    data.thread,
                    userInfoBeanMap
                )
            )
            more.setOnClickListener {
                try {
                    activity ?: return@setOnClickListener
                    val tid = data.thread.thread!!.id
                    FloorFragment.newInstance(tid, data.post.id, null, true)
                        .show(activity!!.supportFragmentManager, "${tid}_Floor")
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }
        } else {
            binding.threadListItemContentFloorCard.visibility = View.GONE
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        activity = null
    }
}

class FloorContentHelper(
    private val context: Context,
    private val photoViewBeansMap: TreeMap<Int, List<PhotoViewBean>>,
    private val dataBean: ThreadContentBean
): ThreadContentViewHelper(context) {
    var seeLz: Boolean = false
    var pureRead: Boolean = false

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
                    dataBean.forum!!.name,
                    dataBean.forum.id,
                    dataBean.thread!!.id,
                    seeLz,
                    PhotoViewActivity.OBJ_TYPE_THREAD_PAGE
                )
                break
            }
        }
    }

    fun getContentViews(
        postListItemBean: ThreadContentBean.PostListItemBean
    ): List<View> = mutableListOf<View>().also {
            getContentViews(postListItemBean.content!!, postListItemBean.floor!!, it)
        }

}

class SubFloorContentViewHelper(private val context: Context): ThreadContentViewHelper(context) {
    companion object {
        private val defaultLayoutParamsWithNoMargins = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    override fun onCreateTextView() = TintTextView(context).apply {
        tintResId = R.color.default_color_text
        movementMethod = LinkMovementClickMethod
        isFocusable = false
        isClickable = false
        isLongClickable = false
        setTextIsSelectable(false)
        setOnClickListener(null)
        setOnLongClickListener(null)
        letterSpacing = 0.02f
        layoutParams = defaultLayoutParamsWithNoMargins
    }
}