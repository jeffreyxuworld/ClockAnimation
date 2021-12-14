package com.worldtech.clockanimation

import android.content.Context
import android.view.View
import android.widget.TextView
import com.worldtech.clockanimation.widgets.BaseDialog

class ClockDownloadDialog(context: Context?) : BaseDialog(context!!) {
    private var circleProgressBar: ClockProgressBar? = null
    private var tv_progress: TextView? = null
    private var tv_desc: TextView? = null
    private var view_hand: View? = null
    override val layoutId: Int
        get() = R.layout.dialog_clock_download

    override fun onViewCreate() {
        circleProgressBar = mRootView!!.findViewById(R.id.custom_progress)
        tv_progress = mRootView!!.findViewById(R.id.tv_progress)
        view_hand = mRootView!!.findViewById(R.id.view_hand)
        tv_desc = mRootView!!.findViewById(R.id.tv_desc)
    }

    fun setProgress(progress: Int) {
        circleProgressBar!!.progress = progress
        tv_progress!!.text = "$progress%"
    }

    fun setDesc(desc: String?) {
        tv_desc!!.text = desc
    }
}