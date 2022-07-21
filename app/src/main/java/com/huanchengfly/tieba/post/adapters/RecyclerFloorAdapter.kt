package com.huanchengfly.tieba.post.adapters

import com.huanchengfly.tieba.post.activities.ReplyActivity.Companion.start
import com.huanchengfly.tieba.post.utils.TiebaUtil.reportPost
import com.huanchengfly.tieba.post.utils.AccountUtil.getLoginInfo
import com.huanchengfly.tieba.post.api.TiebaApi.getInstance
import com.huanchengfly.tieba.post.plugins.PluginManager.performPluginMenuClick
import com.huanchengfly.tieba.post.plugins.PluginManager.initPluginMenu
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.adapters.base.BaseSingleTypeAdapter
import com.huanchengfly.tieba.post.api.models.SubFloorListBean.PostInfo
import com.huanchengfly.tieba.post.api.models.SubFloorListBean
import com.huanchengfly.tieba.post.fragments.MenuDialogFragment
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import android.text.TextUtils
import com.huanchengfly.tieba.post.fragments.ConfirmDialogFragment
import com.huanchengfly.tieba.post.api.models.CommonResponse
import android.widget.Toast
import com.huanchengfly.tieba.post.plugins.PluginManager
import com.huanchengfly.tieba.post.components.MyViewHolder
import android.content.Context
import com.huanchengfly.tieba.post.widgets.MyLinearLayout
import android.view.*
import com.huanchengfly.tieba.post.adapters.base.OnItemClickListener
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder

class RecyclerFloorAdapter(context: Context) : BaseSingleTypeAdapter<PostInfo>(context, null) {
    private val helper: RecyclerFloorAdapterHelper by lazy { RecyclerFloorAdapterHelper(context) }

    private var dataBean: SubFloorListBean? = null
    fun setData(data: SubFloorListBean) {
        dataBean = data.copy(subPostList = mutableListOf<PostInfo>().apply {
            data.post?.let { add(it) }
            addAll(data.subPostList!!)
        })
        setData(dataBean!!.subPostList)
    }

    fun addData(data: SubFloorListBean) {
        dataBean = data
        insert(data.subPostList!!)
    }

    private fun showMenu(postInfo: PostInfo, position: Int) {
        val userInfoBean = postInfo.author
        MenuDialogFragment.newInstance(R.menu.menu_thread_item, null)
            .setOnNavigationItemSelectedListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_reply -> {
                        start(context, dataBean!!, postInfo)
                        return@setOnNavigationItemSelectedListener true
                    }
                    R.id.menu_report -> {
                        reportPost(context, postInfo.id)
                        return@setOnNavigationItemSelectedListener true
                    }
                    R.id.menu_copy -> {
                        TiebaUtil.copyText(
                            context,
                            contentBeansToSimpleString(
                                postInfo.content
                            )
                        )
                        return@setOnNavigationItemSelectedListener true
                    }
                    R.id.menu_copy_json -> {
                        TiebaUtil.copyText(context as BaseActivity, postInfo.toJson())
                    }
                    R.id.menu_delete -> {
                        if (TextUtils.equals(getLoginInfo(context)!!.uid, postInfo.author.id)) {
                            ConfirmDialogFragment.newInstance(context.getString(R.string.title_dialog_del_post))
                                .setOnConfirmListener {
                                    getInstance()
                                        .delPost(
                                            dataBean!!.forum!!.id,
                                            dataBean!!.forum!!.name,
                                            dataBean!!.thread!!.id,
                                            postInfo.id,
                                            dataBean!!.anti!!.tbs,
                                            true,
                                            true
                                        )
                                        .enqueue(object : Callback<CommonResponse?> {
                                            override fun onResponse(
                                                call: Call<CommonResponse?>,
                                                response: Response<CommonResponse?>
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.toast_success,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                remove(position)
                                            }

                                            override fun onFailure(
                                                call: Call<CommonResponse?>,
                                                t: Throwable
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    t.message,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        })
                                }
                                .show(
                                    (context as BaseActivity).supportFragmentManager,
                                    postInfo.id + "_Confirm"
                                )
                        }
                        return@setOnNavigationItemSelectedListener true
                    }
                }
                performPluginMenuClick(PluginManager.MENU_SUB_POST_ITEM, item.itemId, postInfo)
            }
            .setInitMenuCallback { menu: Menu ->
                initPluginMenu(menu, PluginManager.MENU_SUB_POST_ITEM)
                menu.findItem(R.id.menu_view_floor).isVisible = false
                if (TextUtils.equals(getLoginInfo(context)!!.uid, postInfo.author.id)) {
                    menu.findItem(R.id.menu_delete).isVisible = true
                }
            }
            .show((context as BaseActivity).supportFragmentManager, postInfo.id + "_Menu")
    }

    override fun convert(viewHolder: MyViewHolder, item: PostInfo, position: Int) {
        val userInfoBean = item.author
        if (dataBean != null && dataBean!!.thread != null && dataBean!!.thread!!.author != null && userInfoBean != null && userInfoBean.id != null && userInfoBean.id == dataBean!!.thread!!.author.id) {
            viewHolder.setVisibility(R.id.thread_list_item_user_lz_tip, View.VISIBLE)
        } else {
            viewHolder.setVisibility(R.id.thread_list_item_user_lz_tip, View.GONE)
        }
        viewHolder.itemView.setOnLongClickListener { v: View? ->
            showMenu(item, position)
            true
        }
        viewHolder.setText(
            R.id.thread_list_item_user_name,
            StringUtil.getUsernameString(
                context,
                userInfoBean.name,
                userInfoBean.nameShow
            )
        )
        viewHolder.setText(R.id.thread_list_item_user_time, getRelativeTimeString(context, item.time))
        val levelId =
            if (userInfoBean.levelId == null || TextUtils.isEmpty(userInfoBean.levelId)) "?" else userInfoBean.levelId
        ThemeUtil.setChipThemeByLevel(
            levelId,
            viewHolder.getView(R.id.thread_list_item_user_status),
            viewHolder.getView(R.id.thread_list_item_user_level),
            viewHolder.getView(R.id.thread_list_item_user_lz_tip)
        )
        viewHolder.setText(R.id.thread_list_item_user_level, levelId)
        viewHolder.setOnClickListener(R.id.thread_list_item_user_avatar) { view: View? ->
            NavigationHelper.toUserSpaceWithAnim(
                context,
                userInfoBean.id,
                StringUtil.getAvatarUrl(userInfoBean.portrait),
                view
            )
        }
        ImageUtil.load(
            viewHolder.getView(R.id.thread_list_item_user_avatar),
            ImageUtil.LOAD_TYPE_AVATAR,
            userInfoBean.portrait
        )
        initContentView(viewHolder, item)
    }

    override fun getItemLayoutId(): Int {
        return R.layout.item_thread_list
    }

    private fun getContentViews(postListItemBean: PostInfo): List<View> =
        helper.getContentViews(postListItemBean.content, postListItemBean.floor)

    private fun initContentView(viewHolder: MyViewHolder, postListItemBean: PostInfo) {
        val myLinearLayout =
            viewHolder.getView<MyLinearLayout>(R.id.thread_list_item_content_content)
        myLinearLayout.removeAllViews()
        myLinearLayout.addViews(getContentViews(postListItemBean))
    }

    companion object {
        const val TAG = "RecyclerFloorAdapter"
    }

    init {
        setOnItemClickListener(object : OnItemClickListener<PostInfo> {
            override fun onClick(viewHolder: MyViewHolder, item: PostInfo, position: Int) {
                start(
                    context,
                    dataBean!!,
                    item
                )
            }
        })
    }
}