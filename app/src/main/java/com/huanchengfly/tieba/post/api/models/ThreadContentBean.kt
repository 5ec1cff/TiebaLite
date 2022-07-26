package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.api.adapters.*
import com.huanchengfly.tieba.post.models.BaseBean

data class ThreadContentBean(
    @SerializedName("error_code")
    val errorCode: String? = null,

    @SerializedName("error_msg")
    val errorMsg: String? = null,

    @SerializedName("post_list")
    val postList: List<PostListItemBean>? = null,
    val page: PageInfoBean? = null,
    val user: UserInfoBean? = null,
    val forum: ForumInfoBean? = null,

    @SerializedName("display_forum")
    val displayForum: ForumInfoBean? = null,

    @SerializedName("has_floor")
    val hasFloor: String? = null,

    @SerializedName("is_new_url")
    val isNewUrl: String? = null,

    @SerializedName("user_list")
    val userList: List<UserInfoBean>? = null,
    val thread: ThreadBean? = null,
    val anti: AntiInfoBean? = null
): BaseBean() {

    data class AntiInfoBean(
        val tbs: String? = null
    ): BaseBean()

    data class ThreadInfoBean(
        @SerializedName("thread_id")
        val threadId: String? = null,

        @SerializedName("first_post_id")
        val firstPostId: String? = null
    ): BaseBean()

    data class AgreeBean(
        @SerializedName("agree_num")
        val agreeNum: String? = null,

        @SerializedName("disagree_num")
        val disagreeNum: String? = null,

        @SerializedName("diff_agree_num")
        val diffAgreeNum: String? = null,

        @SerializedName("has_agree")
        val hasAgree: String? = null,
    ): BaseBean()

    data class ThreadBean(
        val id: String? = null,
        val title: String? = null,

        @SerializedName("thread_info")
        val threadInfo: ThreadInfoBean? = null,

        @SerializedName("origin_thread_info")
        val originThreadInfo: OriginThreadInfo? = null,
        val author: UserInfoBean? = null,

        @SerializedName("reply_num")
        val replyNum: String? = null,

        @SerializedName("collect_status")
        val collectStatus: String? = null,

        @SerializedName("agree_num")
        val agreeNum: String? = null,

        @SerializedName("create_time")
        val createTime: String? = null,

        @SerializedName("post_id")
        val postId: String? = null,

        @SerializedName("thread_id")
        val threadId: String? = null,
        val agree: AgreeBean? = null,
        @SerializedName("poll_info")
        val pollInfo: PollInfoBean? = null
    ): BaseBean()

    data class PollOptionBean(
        val id: String,
        val num: Int,
        val text: String
    ): BaseBean()

    data class PollInfoBean(
        val title: String,
        @SerializedName("options_count")
        @JsonAdapter(StringToIntAdapter::class)
        val optionsCount: Int,
        val options: List<PollOptionBean>,
        @SerializedName("end_time")
        val endTime: String,
        /**
         * 是否多选
         */
        @SerializedName("is_multi")
        @JsonAdapter(StringToBooleanAdapter::class)
        val isMulti: Boolean,
        /**
         * 是否投票过
         */
        @SerializedName("is_polled")
        @JsonAdapter(StringToBooleanAdapter::class)
        val isPolled: Boolean,
        @SerializedName("last_time")
        val lastTime: String,
        @SerializedName("polled_value")
        val polledValue: String,
        /**
         * 投票人数
         */
        @SerializedName("total_num")
        @JsonAdapter(StringToIntAdapter::class)
        val totalNum: Int,
        /**
         * 投票票数
         */
        @SerializedName("total_poll")
        @JsonAdapter(StringToIntAdapter::class)
        val totalPoll: Int
    ): BaseBean()

    data class UserInfoBean(
        @SerializedName("is_login")
        val isLogin: String? = null,
        val id: String? = null,
        val name: String? = null,

        @SerializedName("name_show")
        val nameShow: String? = null,

        @JsonAdapter(PortraitAdapter::class)
        val portrait: String? = null,
        val type: String? = null,

        @SerializedName("level_id")
        val levelId: String? = null,

        @SerializedName("is_like")
        val isLike: String? = null,

        @SerializedName("is_bawu")
        val isBawu: String? = null,

        @SerializedName("bawu_type")
        val bawuType: String? = null,

        @SerializedName("ip_address")
        val ipAddress: String? = null,
    ): BaseBean()

    data class ForumInfoBean(
        val id: String? = null,
        val name: String? = null,

        @SerializedName("is_exists")
        val isExists: String? = null,
        val avatar: String? = null,

        @SerializedName("first_class")
        val firstClass: String? = null,

        @SerializedName("second_class")
        val secondClass: String? = null,

        @SerializedName("is_liked")
        val isLiked: String? = null,

        @SerializedName("is_brand_forum")
        val isBrandForum: String? = null
    ): BaseBean()

    data class PageInfoBean(
        val offset: String? = null,

        @SerializedName("current_page")
        val currentPage: String? = null,

        @SerializedName("total_page")
        val totalPage: String? = null,

        @SerializedName("has_more")
        val hasMore: String? = null,

        @SerializedName("has_prev")
        val hasPrev: String? = null
    ): BaseBean()

    data class OriginThreadInfo(
        val title: String? = null,

        @JsonAdapter(ContentMsgAdapter::class)
        val content: List<ContentBean>? = null
    ): BaseBean()

    data class PostListItemBean(
        val id: String? = null,
        val title: String? = null,
        val floor: String? = null,
        val time: String? = null,

        @JsonAdapter(ContentMsgAdapter::class)
        val content: List<ContentBean>? = null,
        val agree: AgreeBean? = null,

        @SerializedName("author_id")
        val authorId: String? = null,
        val author: UserInfoBean? = null,

        @SerializedName("sub_post_number")
        val subPostNumber: String? = null,

        @SerializedName("sub_post_list")
        @JsonAdapter(SubPostListAdapter::class)
        val subPostList: SubPostListBean? = null
    ): BaseBean()

    data class SubPostListBean(
        val pid: String? = null,

        @SerializedName("sub_post_list")
        val subPostList: MutableList<PostListItemBean>? = null
    ): BaseBean()

    data class ContentBean(
        val type: String? = null,
        var text: String? = null,
        val link: String? = null,
        val src: String? = null,
        val uid: String? = null,

        @SerializedName("origin_src")
        val originSrc: String? = null,

        @SerializedName("cdn_src")
        val cdnSrc: String? = null,

        @SerializedName("cdn_src_active")
        val cdnSrcActive: String? = null,

        @SerializedName("big_cdn_src")
        val bigCdnSrc: String? = null,

        @SerializedName("during_time")
        val duringTime: String? = null,
        val bsize: String? = null,
        val c: String? = null,
        val width: String? = null,
        val height: String? = null,

        @SerializedName("is_long_pic")
        val isLongPic: String? = null,

        @SerializedName("voice_md5")
        val voiceMD5: String? = null
    ): BaseBean()
}