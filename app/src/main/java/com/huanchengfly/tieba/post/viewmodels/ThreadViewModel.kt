package com.huanchengfly.tieba.post.viewmodels

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.huanchengfly.tieba.post.BaseApplication
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.interfaces.ITiebaApi
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.models.PhotoViewBean
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ThreadContentViewHelper
import kotlinx.parcelize.Parcelize
import java.util.*

data class PostItem(
    val post: ThreadContentBean.PostListItemBean,
    val thread: ThreadContentBean
)

data class AgreeStatus(
    val isAgree: Boolean = false,
    val agreeNum: Int = 0
)

/**
 * 帖子浏览配置
 * 变更会导致刷新
 */
@Parcelize
data class ThreadConfig(
    val reverse: Boolean = false,
    val seeLz: Boolean = false
): Parcelable

class ThreadViewModel : ViewModel() {
    lateinit var threadId: String
    lateinit var maxPostId: String
    lateinit var from: String
    var dataBean: ThreadContentBean? = null

    private inner class ThreadPagingSource(
        private val api: ITiebaApi,
        private val tid: String,
        private val threadConfig: ThreadConfig
    ): PagingSource<String, PostItem>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, PostItem> {
            try {
                val result: LoadResult<String, PostItem>
                val data: ThreadContentBean = api.threadContentSuspend(
                    tid, params.key?.toInt() ?: 1,
                    threadConfig.seeLz, threadConfig.reverse
                )
                result = LoadResult.Page(
                    data = data.postList!!.map { post ->
                        PostItem(post, data)
                    },
                    prevKey = if (data.page!!.hasPrev == "1") "${data.page.currentPage!!.toInt() - 1}" else null,
                    nextKey = if (data.page.hasMore == "1") "${data.page.currentPage!!.toInt() + 1}" else null,
                    )
                if (data.thread != null) {
                    title.postValue(data.thread.title!!)
                    collect.postValue(data.thread.collectStatus == "1")
                    agreeStatus.postValue(
                        AgreeStatus(
                            data.thread.agree!!.hasAgree == "1",
                            data.thread.agree.agreeNum!!.toInt()
                    ))
                    totalPage.postValue(data.page.totalPage!!.toInt())
                }
                dataBean = data.copy(errorCode = null, errorMsg = null, postList = null)
                updatePhotoBean(data.postList)
                updateUserInfoBean(data.userList!!)
                return result
            } catch (t: Throwable) {
                return LoadResult.Error(t)
            }
        }

        override fun getRefreshKey(state: PagingState<String, PostItem>): String? = null
    }

    val title by lazy { MutableLiveData("") }
    val collect by lazy { MutableLiveData(false) }
    val tip by lazy { MutableLiveData(false) }
    val threadConfig by lazy { MutableLiveData(ThreadConfig()) }
    val page by lazy { MutableLiveData(0) }
    val totalPage by lazy { MutableLiveData(0) }
    val pureRead by lazy { MutableLiveData(false) }
    val agreeStatus by lazy { MutableLiveData(AgreeStatus()) }

    val photoViewBeansMap by lazy { TreeMap<Int, List<PhotoViewBean>>() }
    val userInfoBeanMap by lazy { mutableMapOf<String, ThreadContentBean.UserInfoBean>() }

    val threadPageFlow = Pager(PagingConfig(
        pageSize = 30
    )) {
        photoViewBeansMap.clear()
        userInfoBeanMap.clear()
        ThreadPagingSource(TiebaApi.getInstance(), threadId, threadConfig.value!!)
    }.flow.cachedIn(viewModelScope)

    private fun updatePhotoBean(postList: List<ThreadContentBean.PostListItemBean>) {
        for (postListItemBean in postList) {
            val photoViewBeans = mutableListOf<PhotoViewBean>()
            if (postListItemBean.content.isNullOrEmpty() || postListItemBean.floor == null) {
                continue
            }
            for (contentBean in postListItemBean.content) {
                if (contentBean.type == ThreadContentViewHelper.CONTENT_TYPE_IMAGE) {
                    if (contentBean.originSrc == null) {
                        continue
                    }
                    val url = ImageUtil.getUrl(
                        BaseApplication.INSTANCE,
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
            photoViewBeansMap[postListItemBean.floor.toInt()] = photoViewBeans
        }
    }

    private fun updateUserInfoBean(userList: List<ThreadContentBean.UserInfoBean>) {
        for (userInfoBean in userList) {
            // TODO: block user?
            userInfoBeanMap[userInfoBean.id!!] = userInfoBean
        }
    }


}