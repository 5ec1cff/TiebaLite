package com.huanchengfly.tieba.post.models

import android.text.TextUtils
import com.huanchengfly.tieba.post.utils.GsonUtil
import java.util.*

data class ReplyInfoBean(
    val threadId: String,
    val forumId: String,
    val forumName: String,
    val tbs: String,
    val nickName: String,
    var pid: String? = null,
    var spid: String? = null,
    var floorNum: String? = null, 
    var replyUser: String? = null,
    var pn: String? = null) {

    val isSubFloor: Boolean
        get() = spid != null && !TextUtils.equals(pid, spid)

    fun hash(): String {
        return "$threadId-$pid-$spid"
    }

    override fun toString(): String {
        return GsonUtil.getGson().toJson(this)
    }

    override fun hashCode(): Int {
        return Objects.hash(threadId, forumId, pid)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReplyInfoBean) return false
        val that = other
        return threadId == that.threadId &&
                forumId == that.forumId &&
                pid == that.pid
    }
}