package com.worldtech.clockanimation.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import com.worldtech.clockanimation.R
import com.worldtech.clockanimation.utils.CommonUtil.dip2px
import com.worldtech.clockanimation.utils.CommonUtil.getScreenWidth

abstract class BaseDialog(protected var mContext: Context) {
    private var mDialog: AlertDialog? = null
    @JvmField
    protected var mRootView: View? = null
    protected var mButtonState = ButtonState.BOTH
    private fun init() {
        mRootView = LayoutInflater.from(mContext).inflate(layoutId, null, false)
        onViewCreate()
        mDialog = AlertDialog.Builder(mContext, R.style.AlertDialogStyle)
            .setView(mRootView)
            .create()
    }

    fun show() {
        val activity = mContext as Activity
        if (activity == null || activity.isFinishing) {
            return
        }
        if (mDialog != null && !mDialog!!.isShowing) {
            try {
                mDialog!!.show()
            } catch (pE: Exception) {
                pE.printStackTrace()
                return
            }
        }
        val window = mDialog!!.window
        if (window != null) {
            val windowParams = window.attributes
            windowParams.width = getScreenWidth(mContext) - dip2px(80f)
            window.attributes = windowParams
        }
    }

    val isShowing: Boolean
        get() = mDialog != null && mDialog!!.isShowing

    fun dismiss() {
        if (mDialog == null) {
            return
        }
        val activity = mContext as Activity
        if (activity == null || activity.isFinishing) {
            return
        }
        try {
            mDialog!!.dismiss()
        } catch (pE: Exception) {
            pE.printStackTrace()
        }
    }

    fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        if (mDialog != null) {
            mDialog!!.setOnDismissListener(listener)
        }
    }

    fun setCancelable(cancelable: Boolean) {
        if (mDialog != null) {
            mDialog!!.setCancelable(cancelable)
        }
    }

    fun setCanceledOnTouchOutside(cancel: Boolean) {
        if (mDialog != null) {
            mDialog!!.setCanceledOnTouchOutside(cancel)
        }
    }

    abstract val layoutId: Int
    abstract fun onViewCreate()
    enum class ButtonState {
        CONFIRM, CANCEL, BOTH
    }

    init {
        init()
    }
}