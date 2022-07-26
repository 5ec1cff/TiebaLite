package com.huanchengfly.tieba.post.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun interface ThreadLoadFailedCallback {
    fun onLoadFailed(t: Throwable)
}

data class ThreadState(
    val tid: String,
    val from: String,
    var seeLz: Boolean,
    var reverse: Boolean,
    var collected: Boolean,
    var tip: Boolean,
    var page: Int,
    var agreed: Boolean,
    var agreeNum: Int,
    var dataBean: ThreadContentBean
)

class ThreadViewModel : ViewModel() {
    private val threadState: MutableLiveData<ThreadState> by lazy { MutableLiveData<ThreadState>() }
    private val postItemList: MutableLiveData<MutableList<ThreadContentBean.PostListItemBean>> by lazy {
        MutableLiveData<MutableList<ThreadContentBean.PostListItemBean>>()
    }

    fun updateAgreeState() {}

    fun loadThread(
        tid: String,
        seeLz: Boolean,
        reverse: Boolean,
        pid: String? = null, page: Int = 0,
        errorCallback: ThreadLoadFailedCallback? = null) {
        val api = TiebaApi.getInstance()
        val callback = object : Callback<ThreadContentBean> {
            override fun onFailure(call: Call<ThreadContentBean>, t: Throwable) {
                errorCallback?.onLoadFailed(t)
            }

            override fun onResponse(
                call: Call<ThreadContentBean>,
                response: Response<ThreadContentBean>
            ) {
                TODO("Not yet implemented")
            }
        }
        if (pid != null) {
            api.threadContent(tid, pid, seeLz, reverse).enqueue(callback)
        } else if (page > 0) {
            api.threadContent(tid, page, seeLz, reverse).enqueue(callback)
        }
        throw IllegalArgumentException("pid == null and page <= 0")
    }
}