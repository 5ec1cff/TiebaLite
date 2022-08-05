package com.huanchengfly.tieba.post.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.huanchengfly.tieba.post.BaseApplication
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.adapters.ThreadPagingAdapter
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.databinding.ActivityThreadBinding
import com.huanchengfly.tieba.post.fragments.threadmenu.IThreadMenuFragment
import com.huanchengfly.tieba.post.fragments.threadmenu.MIUIThreadMenuFragment
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.Util
import com.huanchengfly.tieba.post.viewmodels.AgreeStatus
import com.huanchengfly.tieba.post.viewmodels.ThreadConfig
import com.huanchengfly.tieba.post.viewmodels.ThreadViewModel
import com.huanchengfly.tieba.post.widgets.VideoPlayerStandard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.properties.Delegates

class ThreadActivityV2 : BaseActivity(), View.OnClickListener {
    companion object {
        private const val LAST_THREAD_CONFIG = "last_thread_config"
    }

    private lateinit var binding: ActivityThreadBinding
    private lateinit var viewModel: ThreadViewModel
    private lateinit var threadId: String
    private var lastThreadConfig: ThreadConfig? = null

    private val url
        get() = "https://tieba.baidu.com/p/$threadId?see_lz=${if (viewModel.threadConfig.value!!.seeLz) "1" else "0"}"

    private val mActionListener = object: IThreadMenuFragment.OnActionsListener {
        override fun onToggleSeeLz(seeLz: Boolean) {
            viewModel.threadConfig.value = viewModel.threadConfig.value!!.copy(seeLz = seeLz)
        }

        override fun onToggleCollect(collect: Boolean) {
        }

        override fun onTogglePureRead(pureRead: Boolean) {
        }

        override fun onToggleSort(sort: Boolean) {
            viewModel.threadConfig.value = viewModel.threadConfig.value!!.copy(reverse = sort)
        }

        override fun onReport() {
            val dataBean = viewModel.dataBean
            if (dataBean != null) TiebaUtil.reportPost(this@ThreadActivityV2, dataBean.thread?.postId!!)
        }

        override fun onJumpPage() {
        }

        override fun onCopyLink() {
            viewModel.dataBean ?: return
            TiebaUtil.copyText(this@ThreadActivityV2, url)
        }

        override fun onShare() {
            val dataBean = viewModel.dataBean ?: return
            TiebaUtil.shareText(this@ThreadActivityV2, url, dataBean.thread?.title)
        }

        override fun onDelete() {
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastThreadConfig = savedInstanceState.getParcelable(LAST_THREAD_CONFIG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(LAST_THREAD_CONFIG, viewModel.threadConfig.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            lastThreadConfig = savedInstanceState.getParcelable(LAST_THREAD_CONFIG)
        }
        binding = ActivityThreadBinding.inflate(layoutInflater)
        val tid = intent.getStringExtra(ThreadActivity.EXTRA_THREAD_ID)
        if (tid == null) {
            toastShort(R.string.toast_param_error)
            finish()
            return
        }
        threadId = tid
        setContentView(binding.root)
        ThemeUtil.setTranslucentThemeBackground(binding.background)
        val viewModel by viewModels<ThreadViewModel>()
        this.viewModel = viewModel
        viewModel.apply {
            threadId = tid
            threadConfig.value = ThreadConfig(
                seeLz = intent.getBooleanExtra(ThreadActivity.EXTRA_SEE_LZ, false)
            )
            from = intent.getStringExtra(ThreadActivity.EXTRA_FROM) ?: ThreadActivity.FROM_NONE
        }
        // TODO: header adapter
        val adapter = ThreadPagingAdapter(
            activity = this,
            photoBeansMap = viewModel.photoViewBeansMap,
            userInfoBeanMap = viewModel.userInfoBeanMap
        )
        val refreshLayout = binding.threadRefreshView
        val recyclerView = binding.threadRecyclerView
        adapter.addLoadStateListener { states ->
            if (states.refresh !is LoadState.Loading)
                refreshLayout.finishRefresh()
        }
        refreshLayout.apply {
            ThemeUtil.setThemeForSmartRefreshLayout(this)
            refreshLayout.setOnRefreshListener {
                adapter.refresh()
            }
            /*
            refreshLayout.setOnLoadMoreListener {

            }*/
        }
        recyclerView.apply {
            this.adapter = adapter
            this.layoutManager = LinearLayoutManager(this@ThreadActivityV2)
            if (!appPreferences.loadPictureWhenScroll) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (!Util.canLoadGlide(this@ThreadActivityV2)) {
                            return
                        }
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            Glide.with(this@ThreadActivityV2)
                                .resumeRequests()
                        } else {
                            Glide.with(this@ThreadActivityV2)
                                .pauseRequests()
                        }
                    }
                })
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    refreshTitle()
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    refreshTitle()
                }
            })
            addOnChildAttachStateChangeListener(object :
                RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {}
                override fun onChildViewDetachedFromWindow(view: View) {
                    if (refreshLayout.isRefreshing) {
                        return
                    }
                    val videoPlayerStandard: VideoPlayerStandard? =
                        view.findViewById(R.id.video_player)
                    if (videoPlayerStandard != null && Jzvd.CURRENT_JZVD != null &&
                        videoPlayerStandard.jzDataSource.containsTheUrl(Jzvd.CURRENT_JZVD.jzDataSource.currentUrl)
                    ) {
                        if (Jzvd.CURRENT_JZVD != null && Jzvd.CURRENT_JZVD.screen != Jzvd.SCREEN_FULLSCREEN) {
                            Jzvd.releaseAllVideos()
                        }
                    }
                }
            })
        }
        val toolbar = binding.appBarLayout.toolbar
        val moreBtn = binding.threadBottomBarLayout.threadBottomBarMoreBtn
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.threadBottomBarLayout.threadBottomBarAgree.setOnClickListener(this)
        binding.threadBottomBarLayout.threadReplyBar.setOnClickListener(this)
        moreBtn.setOnClickListener(this)
        toolbar.setOnClickListener(this)
        lifecycleScope.launch {
            viewModel.threadPageFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
        viewModel.agreeStatus.observe(this, this::invalidateAgreeStatus)
        viewModel.threadConfig.observe(this) {
            if (it == lastThreadConfig) return@observe
            refreshLayout.autoRefresh()
            lastThreadConfig = it
        }
    }

    private val isTitleVisible: Boolean
        get() = true

    private fun refreshTitle() {
        binding.appBarLayout.toolbar.title = if (isTitleVisible) viewModel.title.value else null
    }

    private fun invalidateAgreeStatus(agreeStatus: AgreeStatus) {
        val agreeBtn = binding.threadBottomBarLayout.threadBottomBarAgreeBtn
        val agreeNumTextView = binding.threadBottomBarLayout.threadBottomBarAgreeNum
        val color = ThemeUtils.getColorByAttr(this, R.attr.colorAccent)
        agreeNumTextView.text = "${agreeStatus.agreeNum}"
        if (agreeBtn.imageTintList != null) {
            val agreeBtnAnimator: ValueAnimator
            val agreeNumAnimator: ValueAnimator
            if (agreeStatus.isAgree) {
                agreeNumAnimator =
                    colorAnim(agreeNumTextView, ThemeUtil.getTextColor(this), color)
                agreeBtnAnimator =
                    colorAnim(agreeBtn, ThemeUtil.getTextColor(this), color)
                agreeNumAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        agreeNumTextView.setTextColor(color)
                        super.onAnimationEnd(animation)
                    }
                })
                agreeBtnAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        agreeBtn.imageTintList = ColorStateList.valueOf(color)
                        agreeBtn.contentDescription = getString(R.string.title_agreed)
                        super.onAnimationEnd(animation)
                    }

                    override fun onAnimationStart(animation: Animator) {
                        agreeBtn.setImageResource(R.drawable.ic_twotone_like)
                        super.onAnimationStart(animation)
                    }
                })
            } else {
                agreeNumAnimator =
                    colorAnim(agreeNumTextView, color, ThemeUtil.getTextColor(this))
                agreeBtnAnimator =
                    colorAnim(agreeBtn, color, ThemeUtil.getTextColor(this))
                agreeNumAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        agreeNumTextView.setTextColor(ThemeUtil.getTextColor(this@ThreadActivityV2))
                        super.onAnimationEnd(animation)
                    }
                })
                agreeBtnAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        agreeBtn.imageTintList =
                            ColorStateList.valueOf(ThemeUtil.getTextColor(this@ThreadActivityV2))
                        agreeBtn.contentDescription = getString(R.string.title_agree)
                        super.onAnimationEnd(animation)
                    }

                    override fun onAnimationStart(animation: Animator) {
                        agreeBtn.setImageResource(R.drawable.ic_outline_like)
                        super.onAnimationStart(animation)
                    }
                })
            }
            agreeNumAnimator.setDuration(150).start()
            agreeBtnAnimator.setDuration(150).start()
        } else {
            if (agreeStatus.isAgree) {
                agreeBtn.setImageResource(R.drawable.ic_twotone_like)
                agreeBtn.imageTintList = ColorStateList.valueOf(color)
                agreeNumTextView.setTextColor(ColorStateList.valueOf(color))
                agreeBtn.contentDescription = getString(R.string.title_agreed)
            } else {
                agreeBtn.setImageResource(R.drawable.ic_outline_like)
                agreeBtn.imageTintList = ColorStateList.valueOf(ThemeUtil.getTextColor(this))
                agreeNumTextView.setTextColor(ColorStateList.valueOf(ThemeUtil.getTextColor(this)))
                agreeBtn.contentDescription = getString(R.string.title_agree)
            }
        }
    }

    override fun onClick(v: View) {
        val dataBean = viewModel.dataBean
        when (v.id) {
            R.id.thread_reply_bar -> if (dataBean?.thread != null) {
                ReplyActivity.start(this, dataBean)
            }
            R.id.toolbar -> binding.threadRecyclerView.scrollToPosition(0)
            R.id.thread_bottom_bar_more_btn -> {
                val threadConfig = viewModel.threadConfig.value!!
                MIUIThreadMenuFragment(
                    threadConfig.seeLz,
                    viewModel.collect.value!!,
                    viewModel.pureRead.value!!,
                    threadConfig.reverse,
                    false // canDelete()
                ).apply {
                    setOnActionsListener(mActionListener)
                    show(supportFragmentManager, "Menu")
                }
            }
            R.id.thread_bottom_bar_agree -> if (dataBean?.thread != null) {
                val agreeStatus = viewModel.agreeStatus
                val (agree, agreeNum) = agreeStatus.value!!
                if (!agree) {
                    TiebaApi.getInstance().agree(
                        dataBean.thread.threadInfo?.threadId!!,
                        dataBean.thread.threadInfo.firstPostId!!
                    ).enqueue(object : Callback<AgreeBean> {
                        override fun onFailure(call: Call<AgreeBean>, t: Throwable) {
                            agreeStatus.postValue(AgreeStatus(true, agreeNum))
                            Toast.makeText(
                                this@ThreadActivityV2,
                                getString(R.string.toast_agree_failed, t.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onResponse(
                            call: Call<AgreeBean>,
                            response: Response<AgreeBean>
                        ) {
                            agreeStatus.postValue(AgreeStatus(true, agreeNum + 1))
                        }
                    })
                } else {
                    TiebaApi.getInstance().disagree(
                        dataBean.thread.threadInfo?.threadId!!,
                        dataBean.thread.threadInfo.firstPostId!!
                    ).enqueue(object : Callback<AgreeBean> {
                        override fun onFailure(call: Call<AgreeBean>, t: Throwable) {
                            agreeStatus.postValue(AgreeStatus(true, agreeNum))
                            Toast.makeText(
                                this@ThreadActivityV2,
                                getString(R.string.toast_unagree_failed, t.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onResponse(
                            call: Call<AgreeBean>,
                            response: Response<AgreeBean>
                        ) {
                            agreeStatus.postValue(AgreeStatus(false, agreeNum - 1))
                        }
                    })
                }
            }
        }
    }
}