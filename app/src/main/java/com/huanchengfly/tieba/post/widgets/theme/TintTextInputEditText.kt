package com.huanchengfly.tieba.post.widgets.theme

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.interfaces.Tintable
import com.huanchengfly.tieba.post.ui.common.theme.utils.ColorStateListUtils
import com.huanchengfly.tieba.post.interfaces.OnBackPressedListener
import com.huanchengfly.tieba.post.ui.theme.interfaces.Tintable
import com.huanchengfly.tieba.post.ui.theme.utils.ColorStateListUtils

class TintTextInputEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr), Tintable {
    private var textColorResId: Int
    private var textColorHintResId: Int

    init {
        if (isInEditMode || attrs == null) {
            textColorResId = 0
            textColorHintResId = 0
        } else {
            val array = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.TintTextInputEditText,
                defStyleAttr,
                0
            )
            textColorResId = array.getResourceId(R.styleable.TintTextInputEditText_textColor, 0)
            textColorHintResId =
                array.getResourceId(R.styleable.TintTextInputEditText_textColorHint, 0)
            array.recycle()
        }
        tint()
    }

    override fun tint() {
        if (textColorResId != 0) {
            setTextColor(ColorStateListUtils.createColorStateList(context, textColorResId))
        }
        if (textColorHintResId != 0) {
            setHintTextColor(ColorStateListUtils.createColorStateList(context, textColorHintResId))
        }
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
            mOnBackPressedListener?.onBackPressed(this)
        }
        return super.dispatchKeyEventPreIme(event)
    }

    private var mOnBackPressedListener: OnBackPressedListener? = null

    fun setOnBackPressedListener(onBackPressedListener: OnBackPressedListener) {
        mOnBackPressedListener = onBackPressedListener
    }
}