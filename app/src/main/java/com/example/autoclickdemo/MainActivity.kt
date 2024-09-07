package com.example.autoclickdemo

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_OVERLAY_PERMISSION = 100
    }

    private fun startFloatingService() {
        // 判断 AutoClickService 是否已经启动
        if (isAccessibilityServiceEnabled(this, AutoClickService::class.java)) {
            val intent = Intent(this, FloatingService::class.java)
            startService(intent)
        } else {
            // 提示用户启用辅助功能服务
            val intent2 = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent2)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.btn_request_permission).setOnClickListener {
            checkAccessibilityPermission() // 跳转到辅助功能设置页面
        }
        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = App.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    // 引导用户到系统设置中授予精确闹钟的权限
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } else {
                    // 已经拥有权限，继续执行任务
                    checkOverlayPermission() // 检查并申请悬浮窗权限
                }
            } else {
                // Android 12 以下版本，不需要请求权限
                checkOverlayPermission() // 检查并申请悬浮窗权限
            }
        }
    }
    override fun onBackPressed() {
//        super.onBackPressed()
        // 自定义返回按钮的逻辑
        Toast.makeText(this, "返回按钮被拦截，请用home键（上拉）后台使用", Toast.LENGTH_SHORT).show()
        // 如果想阻止默认的返回行为，不调用super
        // super.onBackPressed()  // 注释掉这行代码将拦截返回按钮
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                Toast.makeText(this, "悬浮窗权限未授予", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilityServiceEnabled(this, AutoClickService::class.java)) {
            Toast.makeText(this, "无障碍权限未打开", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        if (AutoClickService.instance == null) {
            return false
        }
        val serviceId = ComponentName(context, service).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        ) == 1

        // 检查当前应用的辅助功能服务是否在已启用的服务列表中
        if (accessibilityEnabled && !TextUtils.isEmpty(enabledServices)) {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(serviceId, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "请在辅助功能设置中开启服务", Toast.LENGTH_LONG).show()
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } else {
            startFloatingService()
        }
    }

}