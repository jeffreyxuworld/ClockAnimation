package com.worldtech.clockanimation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private var downloadDialog: ClockDownloadDialog? = null
    var countDownTime: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = Runnable {
        countDownTime ++
        showDownloadDialog(countDownTime)
        if(countDownTime == 100){
            stopTimeRunnable()
        } else {
            startTimeRunnable()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startTimeRunnable()
    }


    private fun showDownloadDialog(progress: Int) {
        if (downloadDialog == null) {
            downloadDialog = ClockDownloadDialog(this)
            downloadDialog!!.setCancelable(false)
            downloadDialog!!.show()
            downloadDialog!!.setDesc("V4.2.0" + "新版本下载中，请稍后...")
        }
        downloadDialog!!.setProgress(progress)
    }

    private fun closeDownloadDialog() {
        if (downloadDialog != null) {
            downloadDialog!!.dismiss()
            downloadDialog = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeDownloadDialog()
        stopTimeRunnable()
    }

    private fun startTimeRunnable() {
        stopTimeRunnable()
        handler.postDelayed(runnable, 200)
    }

    private fun stopTimeRunnable() {
        handler.removeCallbacks(runnable)
    }

}