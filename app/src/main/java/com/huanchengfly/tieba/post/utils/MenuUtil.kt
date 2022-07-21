package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.activities.ReplyActivity
import com.huanchengfly.tieba.post.api.models.ThreadContentBean
import com.huanchengfly.tieba.post.fragments.FloorFragment
import com.huanchengfly.tieba.post.fragments.MenuDialogFragment
import com.huanchengfly.tieba.post.plugins.PluginManager
import com.huanchengfly.tieba.post.toJson

private fun showFloorFragment(context: Context, tid: String, pid: String) {
    FloorFragment.newInstance(tid, pid, null, true)
        .show((context as BaseActivity).supportFragmentManager, "${tid}_Floor")
}

fun showMenu(context: Context, userInfoBean: ThreadContentBean.UserInfoBean?,
             dataBean: ThreadContentBean,
             postBean: ThreadContentBean.PostListItemBean,
             subPostListItemBean: ThreadContentBean.PostListItemBean? = null,
             deleteHandler: ((ThreadContentBean.PostListItemBean, Boolean) -> Unit)? = null) {
    val isSubPost = subPostListItemBean != null
    val targetPostItemBean = if (isSubPost) subPostListItemBean!! else postBean
    val menuId = if (isSubPost) PluginManager.MENU_SUB_POST_ITEM else PluginManager.MENU_POST_ITEM
    MenuDialogFragment.newInstance(R.menu.menu_thread_item, null).apply {
        setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_reply -> {
                    ReplyActivity.start(
                        context, dataBean,
                        pid = postBean.id,
                        floorNum = postBean.floor,
                        spid = subPostListItemBean?.id,
                        replyUser = if (userInfoBean != null) userInfoBean.nameShow else ""
                    )
                }
                R.id.menu_report -> {
                    TiebaUtil.reportPost(context, postBean.id!!)
                }
                R.id.menu_copy -> {
                    TiebaUtil.copyText(
                        context as BaseActivity,
                        contentBeansToSimpleString(
                            targetPostItemBean.content!!
                        )
                    )
                }
                R.id.menu_copy_json -> {
                    TiebaUtil.copyText(context as BaseActivity, targetPostItemBean.toJson())

                }
                R.id.menu_delete -> {
                    deleteHandler?.invoke(targetPostItemBean, isSubPost)
                }
                R.id.menu_view_floor -> {
                    showFloorFragment(context, dataBean.thread!!.id!!, postBean.id!!)
                }
                else -> {
                    PluginManager.performPluginMenuClick(
                        menuId,
                        item.itemId,
                        targetPostItemBean
                    )
                }
            }
            return@setOnNavigationItemSelectedListener true
        }
        setInitMenuCallback { menu: Menu ->
            PluginManager.initPluginMenu(menu, menuId)
            if (isSubPost) {
                menu.findItem(R.id.menu_report).isVisible = false
                menu.findItem(R.id.menu_view_floor).isVisible = false
            }
            if (TextUtils.equals(
                    AccountUtil.getUid(context),
                    targetPostItemBean.authorId
                ) || TextUtils.equals(dataBean.user!!.id, dataBean.thread!!.author!!.id)
            ) {
                menu.findItem(R.id.menu_delete).isVisible = true
            }
        }
        show(
            (context as BaseActivity).supportFragmentManager,
            "${targetPostItemBean.id}_${postBean.id}_Menu"
        )
    }
}
