package com.example.autoclickdemo;

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.autoclickdemo.App.Companion.context
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import java.util.Calendar

class TransparentActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置透明背景
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 使用当前时间作为选择器的默认时间
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        // 创建 TimePickerDialog 实例
        val timePickerDialog = TimePickerDialog.newInstance(
            this, // 回调方法
            hour, // 初始小时
            minute, // 初始分钟
            true // 是否使用24小时制
        )
        timePickerDialog.setOnCancelListener {
            finish()
        }
        // 显示对话框
        timePickerDialog.show(supportFragmentManager, "TimePickerDialog")
    }


    @SuppressLint("ScheduleExactAlarm")
    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        // 获取当前时间
        val currentTimeMillis = System.currentTimeMillis()

        // 设置用户选择的时间
        val selectedTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 判断用户选择的时间是否已经过去
        if (selectedTime.timeInMillis <= currentTimeMillis) {
            Toast.makeText(context, "选择的时间已经过去，请选择未来的时间", Toast.LENGTH_SHORT)
                .show()
        } else {
            // 使用 AlarmManager 设置在指定时间触发任务
            Constant.alarmExecute = true
            val alarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(App.context, TaskReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                App.context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,  // 使用真实时间唤醒设备
                selectedTime.timeInMillis,  // 设置目标时间
                pendingIntent
            )
            Toast.makeText(context, "任务将在 $hourOfDay:$minute 执行", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}