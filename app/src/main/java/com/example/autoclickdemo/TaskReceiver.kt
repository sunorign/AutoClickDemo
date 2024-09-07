package com.example.autoclickdemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TaskReceiver: BroadcastReceiver()  {
    companion object {
        private lateinit var clickTask:ClickTask

        fun setClickTask(task: ClickTask) {
            this.clickTask = task
        }
    }

    interface ClickTask {
        fun onClick()
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("TaskReceiver","执行定时点击任务")
        // 执行点击任务
        clickTask.onClick()
    }

}
